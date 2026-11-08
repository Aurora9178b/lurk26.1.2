package meteordevelopment.meteorclient.events.entity.player;

public class SendMovementPacketsEvent {
   public static class Post {
      private static final SendMovementPacketsEvent.Post INSTANCE = new SendMovementPacketsEvent.Post();

      public static SendMovementPacketsEvent.Post get() {
         return INSTANCE;
      }
   }

   public static class Pre {
      private static final SendMovementPacketsEvent.Pre INSTANCE = new SendMovementPacketsEvent.Pre();
      private double x;
      private double y;
      private double z;
      private float yaw;
      private float pitch;
      private boolean onGround;
      private boolean positionModified;
      private boolean rotationModified;
      private boolean onGroundModified;

      public static SendMovementPacketsEvent.Pre get() {
         return get(0.0, 0.0, 0.0, 0.0F, 0.0F, false);
      }

      public static SendMovementPacketsEvent.Pre get(double x, double y, double z, float yaw, float pitch, boolean onGround) {
         INSTANCE.x = x;
         INSTANCE.y = y;
         INSTANCE.z = z;
         INSTANCE.yaw = yaw;
         INSTANCE.pitch = pitch;
         INSTANCE.onGround = onGround;
         INSTANCE.positionModified = false;
         INSTANCE.rotationModified = false;
         INSTANCE.onGroundModified = false;
         return INSTANCE;
      }

      public void updateUnmodified(double x, double y, double z, float yaw, float pitch, boolean onGround) {
         if (!this.positionModified) {
            this.x = x;
            this.y = y;
            this.z = z;
         }

         if (!this.rotationModified) {
            this.yaw = yaw;
            this.pitch = pitch;
         }

         if (!this.onGroundModified) {
            this.onGround = onGround;
         }
      }

      public double getX() {
         return this.x;
      }

      public void setX(double x) {
         this.x = x;
         this.positionModified = true;
      }

      public double getY() {
         return this.y;
      }

      public void setY(double y) {
         this.y = y;
         this.positionModified = true;
      }

      public double getZ() {
         return this.z;
      }

      public void setZ(double z) {
         this.z = z;
         this.positionModified = true;
      }

      public float getYaw() {
         return this.yaw;
      }

      public void setYaw(float yaw) {
         this.yaw = yaw;
         this.rotationModified = true;
      }

      public float getPitch() {
         return this.pitch;
      }

      public void setPitch(float pitch) {
         this.pitch = pitch;
         this.rotationModified = true;
      }

      public boolean isOnGround() {
         return this.onGround;
      }

      public void setOnGround(boolean onGround) {
         this.onGround = onGround;
         this.onGroundModified = true;
      }
   }
}
