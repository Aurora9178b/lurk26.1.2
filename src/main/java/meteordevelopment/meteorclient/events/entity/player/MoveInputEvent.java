package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.world.entity.player.Input;

public class MoveInputEvent {
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;
   private boolean sprint;
   private boolean movementFixed;

   public MoveInputEvent(Input input) {
      this(input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.shift(), input.sprint());
   }

   public MoveInputEvent(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
      this.forward = calculateImpulse(forward, backward);
      this.strafe = calculateImpulse(left, right);
      this.jump = jump;
      this.sneak = sneak;
      this.sprint = sprint;
   }

   private static float calculateImpulse(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }

   public float getForward() {
      return this.forward;
   }

   public void setForward(float forward) {
      this.forward = forward;
   }

   public float getStrafe() {
      return this.strafe;
   }

   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   public boolean isJump() {
      return this.jump;
   }

   public void setJump(boolean jump) {
      this.jump = jump;
   }

   public boolean isSneak() {
      return this.sneak;
   }

   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   public boolean isSprint() {
      return this.sprint;
   }

   public void setSprint(boolean sprint) {
      this.sprint = sprint;
   }

   public Input toInput() {
      return new Input(this.forward > 0.0F, this.forward < 0.0F, this.strafe > 0.0F, this.strafe < 0.0F, this.jump, this.sneak, this.sprint);
   }

   public boolean isMovementFixed() {
      return this.movementFixed;
   }

   public void setMovementFixed(boolean movementFixed) {
      this.movementFixed = movementFixed;
   }
}
