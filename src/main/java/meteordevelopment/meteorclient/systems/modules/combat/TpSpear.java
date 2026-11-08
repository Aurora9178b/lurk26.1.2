package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class TpSpear extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> moveDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("move-distance")
            .description("Maximum movement speed when returning to the original position.")
            .defaultValue(10.0)
            .min(1.0)
            .max(128.0)
            .sliderRange(1.0, 128.0)
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder().name("range").description("Maximum target range.").defaultValue(10.0).min(0.0).max(100.0).sliderRange(0.0, 100.0).build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("Delay between teleports in ticks.").defaultValue(20).min(1).max(40).sliderRange(1, 40).build());
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(new ColorSetting.Builder().name("target-color").description("Target box color.").defaultValue(new SettingColor(255, 0, 0)).build());
   private Entity target;
   private boolean wasCharging;
   private int tickTimer;
   private Vec3 originalPos;
   private boolean goingForward = true;

   public TpSpear() {
      super(Categories.Combat, "TpSpear", "Auto rotate and TP when charging spear.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.wasCharging = false;
      this.tickTimer = 0;
      this.originalPos = null;
      this.goingForward = true;
   }

   @Override
   public void onDeactivate() {
      RotationManager.INSTANCE.reset();
      this.target = null;
      this.wasCharging = false;
      this.tickTimer = 0;
      this.originalPos = null;
      this.goingForward = true;
   }

   @EventHandler(priority = 1199)
   private void onTickPre(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         boolean hasSpear = this.mc.player.getMainHandItem().is(ItemTags.SPEARS) || this.mc.player.getOffhandItem().is(ItemTags.SPEARS);
         boolean charging = hasSpear && this.mc.options.keyUse.isDown();
         if (!charging) {
            if (this.wasCharging) {
               RotationManager.INSTANCE.reset();
            }

            this.wasCharging = false;
            if (!this.restorePosition()) {
               this.target = null;
               this.originalPos = null;
               this.goingForward = true;
               this.tickTimer = 0;
            }
         } else {
            this.target = this.findTarget();
            if (this.target == null || this.mc.player.distanceTo(this.target) > this.range.get()) {
               RotationManager.INSTANCE.reset();
            } else if (this.target instanceof LivingEntity) {
               if (!(this.target instanceof Player p && !Friends.get().shouldAttack(p))) {
                  Rotation rotation = RotationUtil.calculate(this.target.getEyePosition());
                  RotationManager.INSTANCE.setRotations(rotation, 10.0, Priority.High);
                  if (!this.wasCharging) {
                     this.wasCharging = true;
                     this.tickTimer = 0;
                     this.originalPos = this.mc.player.position();
                     this.goingForward = true;
                  } else if (this.tickTimer++ >= this.delay.get()) {
                     Vec3 playerPos = this.mc.player.position();
                     Vec3 targetPos = this.target.position();
                     Vec3 direction = targetPos.subtract(playerPos).normalize();
                     Vec3 forwardPos = targetPos.add(direction.scale(-2.0));
                     Vec3 targetMovePos = this.goingForward ? forwardPos : this.originalPos;
                     double maxMove = this.getMoveDistance();
                     double distance = playerPos.distanceTo(targetMovePos);
                     if (!(distance <= maxMove) && !(maxMove <= 0.0)) {
                        this.mc.player.setDeltaMovement(targetMovePos.subtract(playerPos).normalize().scale(maxMove));
                     } else {
                        this.mc.player.setDeltaMovement(targetMovePos.subtract(playerPos));
                        if (distance < 0.5) {
                           this.goingForward = !this.goingForward;
                           this.tickTimer = 0;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.target != null) {
         double x1 = this.target.getX() - this.target.getBbWidth() / 2.0;
         double y1 = this.target.getY();
         double z1 = this.target.getZ() - this.target.getBbWidth() / 2.0;
         double x2 = this.target.getX() + this.target.getBbWidth() / 2.0;
         double y2 = this.target.getY() + this.target.getBbHeight();
         double z2 = this.target.getZ() + this.target.getBbWidth() / 2.0;
         event.renderer
            .box(
               x1,
               y1,
               z1,
               x2,
               y2,
               z2,
               new SettingColor(this.targetColor.get().r, this.targetColor.get().g, this.targetColor.get().b, 10),
               this.targetColor.get(),
               ShapeMode.Both,
               0
            );
      }
   }

   @Override
   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   private Entity findTarget() {
      return TargetUtils.get(
         entity -> {
            if (entity != this.mc.player && entity != this.mc.getCameraEntity()) {
               if (!this.entities.get().contains(entity.getType())) {
                  return false;
               } else if (this.range.get() > 0.0 && this.mc.player.position().distanceTo(entity.position()) > this.range.get()) {
                  return false;
               } else {
                  return entity instanceof LivingEntity living && (living.isDeadOrDying() || !living.isAlive() || living.getHealth() <= 0.0F)
                     ? false
                     : !(entity instanceof Player player && !Friends.get().shouldAttack(player));
               }
            } else {
               return false;
            }
         },
         SortPriority.LowestDistance
      );
   }

   private boolean restorePosition() {
      if (this.originalPos != null && this.mc.player != null) {
         Vec3 playerPos = this.mc.player.position();
         double distance = playerPos.distanceTo(this.originalPos);
         if (distance <= 0.5) {
            return false;
         }

         double maxMove = this.getMoveDistance();
         Vec3 velocity = this.originalPos.subtract(playerPos);
         if (maxMove > 0.0 && distance > maxMove) {
            velocity = velocity.normalize().scale(maxMove);
         }

         this.mc.player.setDeltaMovement(velocity);
         return true;
      } else {
         return false;
      }
   }

   private double getMoveDistance() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveDistance() : this.moveDistance.get();
   }
}
