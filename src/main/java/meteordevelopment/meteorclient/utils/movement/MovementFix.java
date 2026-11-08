package meteordevelopment.meteorclient.utils.movement;

import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class MovementFix {
   public static boolean shouldFix() {
      return true;
   }

   public static boolean shouldFix(RotationManager rotationManager) {
      return shouldFix() && rotationManager.isActive() && rotationManager.rotations != null;
   }

   private static float getDirection(float forward, float strafe) {
      if (Minecraft.getInstance().player == null) {
         return 0.0F;
      }

      float direction = Minecraft.getInstance().player.getYRot();
      boolean isMovingForward = forward > 0.0F;
      boolean isMovingBack = forward < 0.0F;
      boolean isMovingRight = strafe > 0.0F;
      boolean isMovingLeft = strafe < 0.0F;
      boolean isMovingSideways = isMovingRight || isMovingLeft;
      boolean isMovingStraight = isMovingForward || isMovingBack;
      if (forward != 0.0F || strafe != 0.0F) {
         if (isMovingBack && !isMovingSideways) {
            return direction + 180.0F;
         }

         if (isMovingForward && isMovingLeft) {
            return direction + 45.0F;
         }

         if (isMovingForward && isMovingRight) {
            return direction - 45.0F;
         }

         if (!isMovingStraight && isMovingLeft) {
            return direction + 90.0F;
         }

         if (!isMovingStraight && isMovingRight) {
            return direction - 90.0F;
         }

         if (isMovingBack && isMovingLeft) {
            return direction + 135.0F;
         }

         if (isMovingBack) {
            return direction - 135.0F;
         }
      }

      return direction;
   }

   public static void fixMovement(MoveInputEvent event, float yaw) {
      if (Minecraft.getInstance().player != null && !event.isMovementFixed()) {
         float forward = event.getForward();
         float strafe = event.getStrafe();
         int angleUnit = 45;
         float angleTolerance = 22.5F;
         float directionFactor = Math.max(Math.abs(forward), Math.abs(strafe));
         double angleDifference = Mth.wrapDegrees(getDirection(forward, strafe) - yaw);
         double angleDistance = Math.abs(angleDifference);
         forward = 0.0F;
         strafe = 0.0F;
         if (angleDistance <= angleUnit + angleTolerance) {
            forward++;
         } else if (angleDistance >= 180.0F - angleUnit - angleTolerance) {
            forward--;
         }

         if (angleDifference >= angleUnit - angleTolerance && angleDifference <= 180.0F - angleUnit + angleTolerance) {
            strafe--;
         } else if (angleDifference <= -angleUnit + angleTolerance && angleDifference >= -180.0F + angleUnit - angleTolerance) {
            strafe++;
         }

         forward *= directionFactor;
         strafe *= directionFactor;
         event.setForward(forward);
         event.setStrafe(strafe);
         event.setMovementFixed(true);
      }
   }
}
