package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.CrossbowItemAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.math.ProjectilePredictor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;

public class AimBot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Target range.")
            .defaultValue(50.0)
            .range(1.0, 120.0)
            .sliderRange(1.0, 120.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("priority")).description("How to select targets."))
               .defaultValue(SortPriority.ClosestAngle))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(
         new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entity types to lock onto.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
      );
   private final Setting<Boolean> predict = this.sgGeneral
      .add(new BoolSetting.Builder().name("predict").description("Predicts target movement before calculating projectile rotation.").defaultValue(true).build());
   private LivingEntity target;
   private Rotation lastRotation;
   private boolean modifyingUseItemPacket;

   public AimBot() {
      super(Categories.Combat, "AimBot", "Automatically aims bows and crossbows at targets.");
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      RotationManager.INSTANCE.reset();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && PlayerUtils.isAlive()) {
         ItemStack stack = this.mc.player.getMainHandItem();
         if (this.isRangedWeapon(stack) && this.isReadyToAim(stack)) {
            this.target = this.findTarget();
            Rotation rotation = this.getProjectileRotation(stack, this.target);
            if (rotation != null) {
               this.lastRotation = rotation;
               RotationManager.INSTANCE.setRotations(rotation, 10.0, 5.0, Priority.High);
            } else {
               this.lastRotation = null;
            }
         } else {
            this.target = null;
            this.lastRotation = null;
         }
      }
   }

   @EventHandler(priority = 200)
   private void onPacketSend(PacketEvent.Send event) {
      if (!this.modifyingUseItemPacket && this.mc.player != null && this.lastRotation != null) {
         if (event.packet instanceof ServerboundUseItemPacket packet) {
            ItemStack stack = packet.getHand() == InteractionHand.MAIN_HAND ? this.mc.player.getMainHandItem() : this.mc.player.getOffhandItem();
            if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
               event.cancel();
               this.modifyingUseItemPacket = true;
               event.sendSilently(
                  new ServerboundUseItemPacket(packet.getHand(), packet.getSequence(), this.lastRotation.getYaw(), this.lastRotation.getPitch())
               );
               this.modifyingUseItemPacket = false;
            }
         }
      }
   }

   private LivingEntity findTarget() {
      return TargetUtils.get(this::isValidTarget, this.priority.get()) instanceof LivingEntity living ? living : null;
   }

   private boolean isValidTarget(Entity entity) {
      if (!(entity instanceof LivingEntity living && entity != this.mc.player && entity != this.mc.getCameraEntity())) {
         return false;
      } else {
         if (!entity.isAlive() || living.isDeadOrDying()) {
            return false;
         }

         if (!PlayerUtils.isWithin(entity, this.range.get())) {
            return false;
         }

         if (!this.entities.get().contains(entity.getType())) {
            return false;
         }

         if (!PlayerUtils.canSeeEntity(entity)) {
            return false;
         }

         if (entity instanceof Player player) {
            if (player.isCreative()) {
               return false;
            }

            if (!Friends.get().shouldAttack(player)) {
               return false;
            }
         }

         return true;
      }
   }

   private boolean isRangedWeapon(ItemStack stack) {
      return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
   }

   private boolean isReadyToAim(ItemStack stack) {
      return !(stack.getItem() instanceof CrossbowItem) ? this.mc.player.isUsingItem() : CrossbowItem.isCharged(stack) || this.mc.player.isUsingItem();
   }

   private Rotation getProjectileRotation(ItemStack stack, LivingEntity target) {
      if (target == null) {
         return null;
      } else {
         AimBot.ProjectileData projectile = this.getProjectileData(stack);
         if (projectile != null && !(projectile.velocity() <= 0.05)) {
            Vec3 targetPos = this.getTargetPosition(target);
            ProjectilePredictor.Prediction prediction = ProjectilePredictor.predict(
               this.mc.player.getEyePosition(), targetPos, Vec3.ZERO, projectile.velocity(), projectile.gravity()
            );
            return prediction == null ? null : prediction.rotation();
         } else {
            return null;
         }
      }
   }

   private Vec3 getTargetPosition(LivingEntity target) {
      Vec3 targetPos = target.position().add(0.0, target.getBbHeight() * 0.55, 0.0);
      if (!this.predict.get()) {
         return targetPos;
      }

      Vec3 predicted = PredictUtil.predictEntity(target);
      return predicted == null ? targetPos : predicted.add(0.0, target.getBbHeight() * 0.55, 0.0);
   }

   private AimBot.ProjectileData getProjectileData(ItemStack stack) {
      if (stack.getItem() instanceof BowItem) {
         float charge = BowItem.getPowerForTime(this.mc.player.getTicksUsingItem());
         return charge <= 0.1F ? null : new AimBot.ProjectileData(charge * 3.0, 0.05);
      }

      if (stack.getItem() instanceof CrossbowItem) {
         ChargedProjectiles projectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
         if (projectiles == null) {
            return new AimBot.ProjectileData(3.15, 0.05);
         } else {
            return projectiles.contains(Items.FIREWORK_ROCKET)
               ? new AimBot.ProjectileData(1.6, 0.0)
               : new AimBot.ProjectileData(CrossbowItemAccessor.meteor$getSpeed(projectiles), 0.05);
         }
      } else {
         return null;
      }
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }

   private record ProjectileData(double velocity, double gravity) {
   }
}
