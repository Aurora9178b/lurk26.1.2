package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BlurSetting extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> enabled = this.sgGeneral.add(new BoolSetting.Builder().name("enabled").defaultValue(true).build());
   private final Setting<Double> blurStrength = this.sgGeneral
      .add(new DoubleSetting.Builder().name("blur-strength").defaultValue(10.0).min(0.0).max(30.0).sliderMin(0.0).sliderMax(30.0).build());
   private static BlurSetting INSTANCE;

   public BlurSetting() {
      super(Categories.Client, "BlurSetting", "HUD blur settings.");
      INSTANCE = this;
   }

   public static boolean isGloballyEnabled() {
      return INSTANCE != null && INSTANCE.isActive() && INSTANCE.enabled.get();
   }

   public static float getBlurStrength() {
      return INSTANCE != null && INSTANCE.isActive() && INSTANCE.enabled.get() ? INSTANCE.blurStrength.get().floatValue() : 0.0F;
   }

   public static float getBlurStrengthDirect() {
      return INSTANCE != null && INSTANCE.enabled.get() ? INSTANCE.blurStrength.get().floatValue() : 0.0F;
   }
}
