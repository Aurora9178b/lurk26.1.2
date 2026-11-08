package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Strafe extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> airStop = this.sgGeneral
      .add(new BoolSetting.Builder().name("air-stop").description("Stops horizontal motion when there is no movement input.").defaultValue(true).build());
   private final Setting<Boolean> slowCheck = this.sgGeneral
      .add(new BoolSetting.Builder().name("slow-check").description("Prevents speed-effect boosting while slowness is active.").defaultValue(true).build());
   private final Setting<Boolean> blockStrafe = this.sgGeneral
      .add(new BoolSetting.Builder().name("block-strafe").description("Allows strafing while stuck inside blocks.").defaultValue(true).build());
   private final Setting<Integer> speed = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("speed")
            .description("Block strafe speed.")
            .defaultValue(9)
            .sliderMin(1)
            .sliderMax(100)
            .visible(this.blockStrafe::get)
            .build()
      );

   public Strafe() {
      super(Categories.Movement, "Strafe", "Strafe.");
   }

   @EventHandler
   private void onMove(PlayerMoveEvent event) {
      if (this.mc.player != null && this.mc.level != null && event.type == MoverType.SELF) {
         if (this.isInsideBlock()) {
            this.doBlockStrafe(event);
         } else if (!this.shouldSkip()) {
            if (!this.isInvalidNormalBlock()) {
               if (!PlayerUtils.isMoving()) {
                  if (this.airStop.get()) {
                     ((IVec3)event.movement).meteor$setXZ(0.0, 0.0);
                  }
               } else {
                  double[] velocity = this.directionSpeed(this.getBaseMoveSpeed());
                  ((IVec3)event.movement).meteor$setXZ(velocity[0], velocity[1]);
               }
            }
         }
      }
   }

   private void doBlockStrafe(PlayerMoveEvent event) {
      if (this.blockStrafe.get()) {
         if (!EntityUtils.isInCobweb(this.mc.player)) {
            double moveSpeed = 0.002873 * this.speed.get().intValue();
            double forward = this.mc.player.zza;
            double strafe = this.mc.player.xxa;
            float yaw = this.mc.player.getYRot();
            if (forward == 0.0 && strafe == 0.0) {
               ((IVec3)event.movement).meteor$setXZ(0.0, 0.0);
            } else {
               if (forward != 0.0 && strafe != 0.0) {
                  forward *= Math.sin(Math.PI / 4);
                  strafe *= Math.cos(Math.PI / 4);
               }

               double sin = Math.sin(Math.toRadians(yaw));
               double cos = Math.cos(Math.toRadians(yaw));
               double x = forward * moveSpeed * -sin + strafe * moveSpeed * cos;
               double z = forward * moveSpeed * cos + strafe * moveSpeed * sin;
               ((IVec3)event.movement).meteor$setXZ(x, z);
            }
         }
      }
   }

   private double getBaseMoveSpeed() {
      double base = 0.2873;
      if (this.mc.player.hasEffect(MobEffects.SPEED) && (!this.slowCheck.get() || !this.mc.player.hasEffect(MobEffects.SLOWNESS))) {
         base *= 1.0 + 0.2 * (this.mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1);
      }

      return base;
   }

   private double[] directionSpeed(double speed) {
      double forward = this.mc.player.zza;
      double strafe = this.mc.player.xxa;
      float yaw = this.mc.player.getYRot();
      if (forward != 0.0) {
         if (strafe > 0.0) {
            yaw += forward > 0.0 ? -45.0F : 45.0F;
         } else if (strafe < 0.0) {
            yaw += forward > 0.0 ? 45.0F : -45.0F;
         }

         strafe = 0.0;
         forward = forward > 0.0 ? 1.0 : -1.0;
      }

      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      return new double[]{forward * speed * cos + strafe * speed * sin, forward * speed * sin - strafe * speed * cos};
   }

   private boolean isInsideBlock() {
      return EntityUtils.isInCobweb(this.mc.player)
         || this.mc.player.isInWall()
         || this.mc.level.collidesWithSuffocatingBlock(this.mc.player, this.mc.player.getBoundingBox());
   }

   private boolean isInvalidNormalBlock() {
      BlockState state = this.mc.level.getBlockState(this.mc.player.blockPosition());
      return state.is(Blocks.WATER) || state.is(Blocks.LAVA) || state.is(Blocks.COBWEB) || state.is(Blocks.POWDER_SNOW);
   }

   private boolean shouldSkip() {
      return this.mc.player.isShiftKeyDown()
         || Modules.get().isActive(Speed.class)
         || this.mc.player.isFallFlying()
         || this.mc.player.onClimbable()
         || this.mc.player.isInLava()
         || this.mc.player.isInWater()
         || this.mc.player.getAbilities().flying
         || this.mc.player.getVehicle() != null;
   }
}
