package meteordevelopment.meteorclient.events.render;

public class RotationAnimationEvent {
   private static final RotationAnimationEvent INSTANCE = new RotationAnimationEvent();
   private float yaw;
   private float lastYaw;
   private float pitch;
   private float lastPitch;
   private boolean modified;
   private RotationAnimationEvent.Part part;

   public static RotationAnimationEvent get(float yaw, float lastYaw, float pitch, float lastPitch) {
      return get(RotationAnimationEvent.Part.All, yaw, lastYaw, pitch, lastPitch);
   }

   public static RotationAnimationEvent get(RotationAnimationEvent.Part part, float yaw, float lastYaw, float pitch, float lastPitch) {
      INSTANCE.yaw = yaw;
      INSTANCE.lastYaw = lastYaw;
      INSTANCE.pitch = pitch;
      INSTANCE.lastPitch = lastPitch;
      INSTANCE.modified = false;
      INSTANCE.part = part;
      return INSTANCE;
   }

   public float getYaw() {
      return this.yaw;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
      this.modified = true;
   }

   public float getLastYaw() {
      return this.lastYaw;
   }

   public void setLastYaw(float lastYaw) {
      this.lastYaw = lastYaw;
      this.modified = true;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
      this.modified = true;
   }

   public float getLastPitch() {
      return this.lastPitch;
   }

   public void setLastPitch(float lastPitch) {
      this.lastPitch = lastPitch;
      this.modified = true;
   }

   public boolean isModified() {
      return this.modified;
   }

   public RotationAnimationEvent.Part getPart() {
      return this.part;
   }

   public enum Part {
      All,
      Body,
      Head,
      Pitch;
   }
}
