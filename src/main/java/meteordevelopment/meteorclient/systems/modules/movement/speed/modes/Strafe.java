package meteordevelopment.meteorclient.systems.modules.movement.speed.modes;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Anchor;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedMode;
import meteordevelopment.meteorclient.systems.modules.movement.speed.SpeedModes;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import org.joml.Vector2d;

public class Strafe extends SpeedMode {
   private long timer = 0L;

   public Strafe() {
      super(SpeedModes.Strafe);
   }

   @Override
   public void onMove(PlayerMoveEvent event) {
      switch (this.stage) {
         case 0:
            if (PlayerUtils.isMoving()) {
               this.stage = 1;
               this.speed = 1.18F * this.getDefaultSpeed() - 0.01;
            }
            break;
         case 1:
            if (PlayerUtils.isMoving() && this.mc.player.onGround()) {
               ((IVec3)event.movement).meteor$setY(this.getHop(0.40123128));
               this.speed = this.speed * this.settings.ncpSpeed.get();
               this.stage = 2;
            }
            break;
         case 2:
            this.speed = this.distance - 0.76 * (this.distance - this.getDefaultSpeed());
            this.stage = 3;
            break;
         case 3:
            if (!this.mc.level.noCollision(this.mc.player.getBoundingBox().move(0.0, this.mc.player.getDeltaMovement().y, 0.0))
               || this.mc.player.verticalCollision && this.stage > 0) {
               this.stage = 0;
            }

            this.speed = this.distance - this.distance / 159.0;
      }

      this.speed = Math.max(this.speed, this.getDefaultSpeed());
      if (this.settings.ncpSpeedLimit.get()) {
         if (System.currentTimeMillis() - this.timer > 2500L) {
            this.timer = System.currentTimeMillis();
         }

         this.speed = Math.min(this.speed, System.currentTimeMillis() - this.timer > 1250L ? 0.44 : 0.43);
      }

      Vector2d change = transformStrafe(this.speed);
      Anchor anchor = Modules.get().get(Anchor.class);
      if (anchor.isActive() && anchor.controlMovement) {
         change.set(anchor.deltaX, anchor.deltaZ);
      }

      ((IVec3)event.movement).meteor$setXZ(change.x, change.y);
   }

   public static Vector2d transformStrafe(double speed) {
      float forward = Math.signum(MeteorClient.mc.player.input.getMoveVector().y);
      float side = Math.signum(MeteorClient.mc.player.input.getMoveVector().x);
      float yaw = MeteorClient.mc.player.getYRot(MeteorClient.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
      if (forward == 0.0F && side == 0.0F) {
         return new Vector2d();
      }

      float strafe = 90.0F * side;
      if (forward != 0.0F) {
         strafe *= forward * 0.5F;
      }

      yaw -= strafe;
      if (forward < 0.0F) {
         yaw -= 180.0F;
      }

      double yawRadians = Math.toRadians(yaw);
      return new Vector2d(-Math.sin(yawRadians) * speed, Math.cos(yawRadians) * speed);
   }

   @Override
   public void onTick() {
      this.distance = Math.sqrt(
         (this.mc.player.getX() - this.mc.player.xo) * (this.mc.player.getX() - this.mc.player.xo)
            + (this.mc.player.getZ() - this.mc.player.zo) * (this.mc.player.getZ() - this.mc.player.zo)
      );
   }
}
