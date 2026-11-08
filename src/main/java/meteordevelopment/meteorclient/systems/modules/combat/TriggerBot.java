package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.rotation.ThunderHack.ThunderRotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;

public final class TriggerBot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSmartCrit = this.settings.createGroup("SmartCrit");
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Attack range.")
            .defaultValue(3.0)
            .range(1.0, 7.0)
            .sliderRange(1.0, 7.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Boolean> ignoreWalls = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("ignore-walls")
            .description("Allows attacking through walls when the crosshair ray hits an entity.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> pauseEating = this.sgGeneral
      .add(new BoolSetting.Builder().name("pause-while-eating").description("Pauses while using items.").defaultValue(false).build());
   private final Setting<Integer> minDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("random-delay-min")
            .description("Minimum random delay after attacking in ticks.")
            .defaultValue(2)
            .range(0, 20)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Integer> maxDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("random-delay-max")
            .description("Maximum random delay after attacking in ticks.")
            .defaultValue(13)
            .range(0, 20)
            .sliderRange(0, 20)
            .build()
      );
   private final Setting<Boolean> smartCrit = this.sgSmartCrit
      .add(new BoolSetting.Builder().name("enabled").description("Waits for critical hits when possible.").defaultValue(true).build());
   private final Setting<Boolean> onlySpace = this.sgSmartCrit
      .add(
         new BoolSetting.Builder()
            .name("only-crit")
            .description("Only waits for crits while the jump key is pressed.")
            .defaultValue(false)
            .visible(this.smartCrit::get)
            .build()
      );
   private final Setting<Boolean> autoJump = this.sgSmartCrit
      .add(
         new BoolSetting.Builder()
            .name("auto-jump")
            .description("Automatically jumps before attacking for crits.")
            .defaultValue(false)
            .visible(this.smartCrit::get)
            .build()
      );
   private int delay;

   public TriggerBot() {
      super(Categories.Combat, "TriggerBot", "Automatically attacks the entity under your crosshair.");
   }

   @Override
   public void onActivate() {
      this.delay = 0;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         if (!this.pauseEating.get() || !this.mc.player.isUsingItem()) {
            if (!this.mc.options.keyJump.isDown() && this.mc.player.onGround() && this.autoJump.get()) {
               this.mc.player.jumpFromGround();
            }

            if (!this.canAutoCrit() && this.delay > 0) {
               this.delay--;
            } else {
               Entity target = this.getTarget();
               if (this.isValidTarget(target)) {
                  this.mc.gameMode.attack(this.mc.player, target);
                  this.mc.player.swing(InteractionHand.MAIN_HAND);
                  int min = Math.min(this.minDelay.get(), this.maxDelay.get());
                  int max = Math.max(this.minDelay.get(), this.maxDelay.get());
                  this.delay = ThreadLocalRandom.current().nextInt(min, max + 1);
               }
            }
         }
      }
   }

   private Entity getTarget() {
      EntityHitResult result = ThunderRotationUtil.getRtxTarget(
         this.mc.player.getYRot(), this.mc.player.getXRot(), this.range.get().floatValue(), this.ignoreWalls.get()
      );
      return result == null ? null : result.getEntity();
   }

   private boolean isValidTarget(Entity entity) {
      if (entity instanceof LivingEntity living) {
         if (entity == this.mc.player || entity == this.mc.getCameraEntity()) {
            return false;
         } else if (entity.isAlive() && !living.isDeadOrDying()) {
            return entity instanceof Player player && !Friends.get().shouldAttack(player)
               ? false
               : this.mc.player.getEyePosition().distanceTo(entity.getBoundingBox().getCenter()) <= this.range.get();
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean canAutoCrit() {
      if (!this.smartCrit.get()) {
         return true;
      } else {
         boolean reasonForSkipCrit = this.mc.player.getAbilities().flying
            || this.mc.player.isFallFlying()
            || this.mc.player.hasEffect(MobEffects.BLINDNESS)
            || this.mc.player.onClimbable()
            || this.mc.level.getBlockState(BlockPos.containing(this.mc.player.position())).is(Blocks.COBWEB);
         if (this.mc.player.fallDistance > 1.0 && this.mc.player.fallDistance < 1.14F) {
            return false;
         } else if (this.mc.player.getAttackStrengthScale(0.5F) < (this.mc.player.onGround() ? 1.0F : 0.9F)) {
            return false;
         } else if (!this.mc.options.keyJump.isDown() && !this.onlySpace.get() && !this.autoJump.get()) {
            return true;
         } else if (this.mc.player.isInLava()) {
            return true;
         } else {
            return reasonForSkipCrit ? true : !this.mc.player.onGround() && this.mc.player.fallDistance > 0.0;
         }
      }
   }
}
