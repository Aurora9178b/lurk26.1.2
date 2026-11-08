package meteordevelopment.meteorclient.utils.rotation;

public class Rotation {
   private float yaw;
   private float pitch;

   public Rotation() {
      this(0.0F, 0.0F);
   }

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void set(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public void set(Rotation rotation) {
      this.yaw = rotation.yaw;
      this.pitch = rotation.pitch;
   }

   public Rotation snapToSensitivity(float sensitivity) {
      float scaled = sensitivity * 0.6F + 0.2F;
      float step = scaled * scaled * scaled * 1.2F;
      this.yaw = this.yaw - this.yaw % step;
      this.pitch = this.pitch - this.pitch % step;
      return this;
   }

   public Rotation copy() {
      return new Rotation(this.yaw, this.pitch);
   }

   public Rotation clone() {
      return this.copy();
   }
}
