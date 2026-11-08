package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TPAttack extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> attackRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Attack range.").defaultValue(100.0).min(0.0).max(257.0).sliderRange(0.0, 257.0).build());
   private final Setting<Integer> attackDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("Delay between attacks in ticks.").defaultValue(10).min(0).max(1000).sliderRange(0, 1000).build());
   private final Setting<Boolean> breakShield = this.sgGeneral
      .add(new BoolSetting.Builder().name("break-shield").description("Swaps to an axe against blocking players.").defaultValue(true).build());
   private final Setting<Boolean> onlyWeapon = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-weapon").description("Only attacks while holding a weapon.").defaultValue(false).build());
   private final Setting<Double> moveDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("move-distance")
            .description("Maximum distance per teleport packet step.")
            .defaultValue(20.0)
            .min(1.0)
            .max(128.0)
            .sliderRange(1.0, 128.0)
            .build()
      );
   private int timer;

   public TPAttack() {
      super(Categories.Combat, "TPAttack", "Allows you to attack targets from a long range.");
   }

   @Override
   public void onActivate() {
      this.timer = this.attackDelay.get();
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         if (this.mc.options.keyAttack.isDown()) {
            if (this.timer < this.attackDelay.get()) {
               this.timer++;
            } else {
               Entity target = this.getCrosshairTarget(this.mc.player.getYRot(), this.mc.player.getXRot(), this.attackRange.get().floatValue(), true);
               if (this.shouldAttack(target)) {
                  if (target instanceof Player player && player.isBlocking() && this.breakShield.get()) {
                     InvUtils.swap(this.getAxeItem(), false);
                  }

                  Vec3 from = this.mc.player.position();
                  Vec3 to = target.position();
                  this.doTp(from, to, false);
                  this.mc.gameMode.attack(this.mc.player, target);
                  this.mc.player.swing(InteractionHand.MAIN_HAND);
                  this.doTp(to, from, false);
                  this.mc.options.keyAttack.setDown(false);
                  this.timer = 0;
               }
            }
         }
      }
   }

   private boolean shouldAttack(Entity entity) {
      if (entity != null && entity != this.mc.player && entity != this.mc.getCameraEntity() && !(entity instanceof FakePlayerEntity)) {
         if (!(entity instanceof LivingEntity living && (living.isDeadOrDying() || !living.isAlive()))) {
            if (entity instanceof Player player) {
               if (!Friends.get().shouldAttack(player)) {
                  return false;
               }

               if (player.isBlocking() && this.breakShield.get()) {
                  return true;
               }
            }

            return !this.onlyWeapon.get() || this.isWeapon(this.mc.player.getMainHandItem().getItem());
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isWeapon(Item item) {
      return item instanceof AxeItem || item instanceof MaceItem || this.mc.player.getMainHandItem().is(ItemTags.SWORDS);
   }

   private int getAxeItem() {
      int slot = this.mc.player.getInventory().getSelectedSlot();

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (stack.getItem() instanceof AxeItem && stack.getDamageValue() < stack.getMaxDamage() - 10) {
            slot = i;
            break;
         }
      }

      return slot;
   }

   private Entity getCrosshairTarget(float yaw, float pitch, float distance, boolean ignoreWalls) {
      if (this.mc.player != null && this.mc.level != null) {
         HitResult blockHit = ignoreWalls ? null : this.rayTrace(distance, yaw, pitch);
         Vec3 eyePos = this.mc.player.getEyePosition();
         double maxDistSq = distance * distance;
         if (blockHit != null) {
            maxDistSq = blockHit.getLocation().distanceToSqr(eyePos);
         }

         Vec3 look = Vec3.directionFromRotation(pitch, yaw);
         Vec3 end = eyePos.add(look.x * distance, look.y * distance, look.z * distance);
         AABB box = this.mc.player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0);
         EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            this.mc.player, eyePos, end, box, entityx -> !entityx.isSpectator() && entityx.isPickable(), maxDistSq
         );
         if (hit == null) {
            return null;
         }

         Entity entity = hit.getEntity();
         Vec3 hitPos = hit.getLocation();
         return !(eyePos.distanceToSqr(hitPos) <= maxDistSq) && blockHit != null ? null : entity;
      } else {
         return null;
      }
   }

   private HitResult rayTrace(double distance, float yaw, float pitch) {
      Vec3 eyePos = this.mc.player.getEyePosition();
      Vec3 look = Vec3.directionFromRotation(pitch, yaw);
      Vec3 end = eyePos.add(look.x * distance, look.y * distance, look.z * distance);
      return this.mc.level.clip(new ClipContext(eyePos, end, Block.OUTLINE, Fluid.NONE, this.mc.player));
   }

   private void doTp(Vec3 from, Vec3 to, boolean onGround) {
      double maxMove = this.moveDistance.get();
      if (maxMove <= 0.0) {
         LTpUtils.sendMovePacket(to, onGround);
      } else {
         if (QTPUtils.allowSendP()) {
            QTPUtils.doTp(from, to, maxMove, onGround);
         } else {
            LTpUtils.doTp(from, to, maxMove, onGround);
         }
      }
   }
}
