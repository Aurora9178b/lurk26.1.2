package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;

public class Pitch40 extends ElytraFlightMode {
   private boolean pitchingDown = true;
   private float pitch;

   public Pitch40() {
      super(ElytraFlightModes.Pitch40);
   }

   @Override
   public void onActivate() {
      if (this.mc.player.getY() < this.elytraFly.pitch40upperBounds.get()) {
         this.elytraFly.error("Player must be above upper bounds!");
         this.elytraFly.toggle();
      } else if (this.mc.player.getY() - 40.0 < this.elytraFly.pitch40lowerBounds.get()) {
         this.elytraFly.error("Player must be at least 40 blocks above the lower bounds!");
         this.elytraFly.toggle();
      }

      this.pitch = 37.72F;
   }

   private float randPitch(float pitch, float bound) {
      return (float)(pitch + bound * (Math.random() - 0.5));
   }

   @Override
   public void onTick() {
      super.onTick();
      if (this.pitchingDown && this.mc.player.getY() <= this.elytraFly.pitch40lowerBounds.get()) {
         this.pitchingDown = false;
      } else if (!this.pitchingDown && this.mc.player.getY() >= this.elytraFly.pitch40upperBounds.get()) {
         this.pitchingDown = true;
      }

      if (!this.pitchingDown) {
         this.pitch = this.pitch - this.randPitch(this.elytraFly.pitch40rotationSpeedUp.get().floatValue(), 1.0F);
         if (this.pitch < -54.77F) {
            this.pitch = -54.77F;
            this.pitchingDown = true;
         }
      } else if (this.pitch < 37.72F) {
         this.pitch = this.pitch + this.randPitch(this.elytraFly.pitch40rotationSpeedDown.get().floatValue(), 0.5F);
      }

      this.mc.player.setXRot(this.pitch);
   }

   @Override
   public void autoTakeoff() {
   }

   @Override
   public void handleHorizontalSpeed(PlayerMoveEvent event) {
      this.velX = event.movement.x;
      this.velZ = event.movement.z;
   }

   @Override
   public void handleVerticalSpeed(PlayerMoveEvent event) {
   }

   @Override
   public void handleFallMultiplier() {
   }

   @Override
   public void handleAutopilot() {
   }
}
