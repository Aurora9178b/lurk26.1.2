package meteordevelopment.meteorclient.utils.rotation.ThunderHack;

import net.minecraft.util.Mth;

public class ThunderRotation {
   private float yaw;
   private float pitch;

   public ThunderRotation() {
      this(0.0F, 0.0F);
   }

   public ThunderRotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
   }

   public float yaw() {
      return this.yaw;
   }

   public float pitch() {
      return this.pitch;
   }

   public ThunderRotation set(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
      return this;
   }

   public ThunderRotation set(ThunderRotation rotation) {
      return this.set(rotation.yaw, rotation.pitch);
   }

   public ThunderRotation copy() {
      return new ThunderRotation(this.yaw, this.pitch);
   }

   public boolean valid() {
      return !Float.isNaN(this.yaw) && !Float.isNaN(this.pitch) && !Float.isInfinite(this.yaw) && !Float.isInfinite(this.pitch);
   }
}
