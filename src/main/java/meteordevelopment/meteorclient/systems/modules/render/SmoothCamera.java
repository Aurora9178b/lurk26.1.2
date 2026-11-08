package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;

public class SmoothCamera extends Module {
   private static SmoothCamera INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> enableFirstPOV = this.sgGeneral
      .add(new BoolSetting.Builder().name("enable-first-pov").description("Smooths the camera in first person.").defaultValue(false).build());
   private final Setting<Boolean> resetOnPerspectiveChange = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("reset-on-perspective-change")
            .description("Resets the smoothed position when changing perspective.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Double> horizontalFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("horizontal-factor")
            .description("Horizontal smoothing factor.")
            .defaultValue(0.9)
            .range(0.0, 1.0)
            .sliderRange(0.0, 1.0)
            .build()
      );
   private final Setting<Double> verticalFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("vertical-factor")
            .description("Vertical smoothing factor.")
            .defaultValue(0.93)
            .range(0.0, 1.0)
            .sliderRange(0.0, 1.0)
            .build()
      );
   private Vec3 smoothPos = Vec3.ZERO;
   private CameraType lastPerspective;

   public SmoothCamera() {
      super(Categories.Render, "smooth-camera", "Makes the camera move smoother.");
      INSTANCE = this;
   }

   @Override
   public void onDeactivate() {
      this.smoothPos = Vec3.ZERO;
      this.lastPerspective = null;
   }

   public void update(Vec3 pos, CameraType currentPerspective) {
      if (this.resetOnPerspectiveChange.get() && this.lastPerspective != currentPerspective) {
         this.setSmoothPos(pos, currentPerspective);
      } else {
         this.lastPerspective = currentPerspective;
         if (!this.enableFirstPOV.get() && currentPerspective.isFirstPerson()) {
            this.setSmoothPos(pos, currentPerspective);
         } else {
            if (this.smoothPos.equals(Vec3.ZERO)) {
               this.smoothPos = pos;
            }

            double h = this.horizontalFactor.get();
            double v = this.verticalFactor.get();
            this.smoothPos = new Vec3(
               this.smoothPos.x * h + pos.x * (1.0 - h), this.smoothPos.y * v + pos.y * (1.0 - v), this.smoothPos.z * h + pos.z * (1.0 - h)
            );
         }
      }
   }

   public static boolean shouldApply() {
      return INSTANCE != null && INSTANCE.isActive();
   }

   public Vec3 getSmoothPos() {
      return this.smoothPos;
   }

   public static SmoothCamera get() {
      if (INSTANCE == null) {
         INSTANCE = Modules.get().get(SmoothCamera.class);
      }

      return INSTANCE;
   }

   private void setSmoothPos(Vec3 pos, CameraType currentPerspective) {
      this.smoothPos = pos;
      this.lastPerspective = currentPerspective;
   }
}
