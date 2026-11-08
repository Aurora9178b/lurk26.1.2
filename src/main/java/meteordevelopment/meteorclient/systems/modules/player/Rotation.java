package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Rotation extends Module {
   private final SettingGroup sgYaw = this.settings.createGroup("Yaw");
   private final SettingGroup sgPitch = this.settings.createGroup("Pitch");
   private final Setting<Rotation.LockMode> yawLockMode = this.sgYaw
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("yaw-lock-mode"))
                  .description("The way in which your yaw is locked."))
               .defaultValue(Rotation.LockMode.Simple))
            .build()
      );
   private final Setting<Double> yawAngle = this.sgYaw
      .add(
         new DoubleSetting.Builder()
            .name("yaw-angle")
            .description("Yaw angle in degrees.")
            .defaultValue(0.0)
            .sliderMax(360.0)
            .max(360.0)
            .visible(() -> this.yawLockMode.get() == Rotation.LockMode.Simple)
            .build()
      );
   private final Setting<Rotation.LockMode> pitchLockMode = this.sgPitch
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("pitch-lock-mode"))
                  .description("The way in which your pitch is locked."))
               .defaultValue(Rotation.LockMode.Simple))
            .build()
      );
   private final Setting<Double> pitchAngle = this.sgPitch
      .add(
         new DoubleSetting.Builder()
            .name("pitch-angle")
            .description("Pitch angle in degrees.")
            .defaultValue(0.0)
            .range(-90.0, 90.0)
            .sliderRange(-90.0, 90.0)
            .visible(() -> this.pitchLockMode.get() == Rotation.LockMode.Simple)
            .build()
      );

   public Rotation() {
      super(Categories.Player, "rotation", "Changes/locks your yaw and pitch.");
   }

   @Override
   public void onActivate() {
      this.onTick(null);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      switch ((Rotation.LockMode)this.yawLockMode.get()) {
         case Smart:
            this.setYawAngle(this.getSmartYawDirection());
            break;
         case Simple:
            this.setYawAngle(this.yawAngle.get().floatValue());
      }

      switch ((Rotation.LockMode)this.pitchLockMode.get()) {
         case Smart:
            this.mc.player.setXRot(this.getSmartPitchDirection());
            break;
         case Simple:
            this.mc.player.setXRot(this.pitchAngle.get().floatValue());
      }
   }

   private float getSmartYawDirection() {
      return Math.round((this.mc.player.getYRot() + 1.0F) / 45.0F) * 45.0F;
   }

   private float getSmartPitchDirection() {
      return Math.round((this.mc.player.getXRot() + 1.0F) / 30.0F) * 30.0F;
   }

   private void setYawAngle(float yawAngle) {
      this.mc.player.setYRot(yawAngle);
      this.mc.player.yHeadRot = yawAngle;
      this.mc.player.yBodyRot = yawAngle;
   }

   public enum LockMode {
      Smart,
      Simple,
      None;
   }
}
