package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class LiquidGlassSetting extends Module {
   private static LiquidGlassSetting INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgEffects = this.settings.createGroup("Effects");
   private final SettingGroup sgGlow = this.settings.createGroup("Glow");
   private final Setting<Boolean> enabled = this.sgGeneral
      .add(new BoolSetting.Builder().name("enabled").description("Enables LiquidGlass rendering helpers.").defaultValue(true).build());
   private final Setting<Double> blur = this.sgEffects
      .add(
         new DoubleSetting.Builder()
            .name("blur")
            .description("How much the captured background is blurred.")
            .defaultValue(12.0)
            .min(0.0)
            .max(40.0)
            .sliderRange(0.0, 40.0)
            .build()
      );
   private final Setting<Double> noise = this.sgEffects
      .add(
         new DoubleSetting.Builder()
            .name("noise")
            .description("Subtle noise mixed into the glass.")
            .defaultValue(0.03)
            .min(0.0)
            .max(0.25)
            .sliderRange(0.0, 0.25)
            .build()
      );
   private final Setting<Double> refraction = this.sgEffects
      .add(
         new DoubleSetting.Builder()
            .name("refraction")
            .description("How strongly the glass bends the background.")
            .defaultValue(0.65)
            .min(0.0)
            .max(2.0)
            .sliderRange(0.0, 2.0)
            .build()
      );
   private final Setting<Double> glow = this.sgGlow
      .add(
         new DoubleSetting.Builder()
            .name("glow")
            .description("Angular light intensity inside the glass.")
            .defaultValue(0.18)
            .min(0.0)
            .max(1.5)
            .sliderRange(0.0, 1.5)
            .build()
      );
   private final Setting<Double> glowBias = this.sgGlow
      .add(
         new DoubleSetting.Builder()
            .name("glow-bias")
            .description("Base brightness added to the glass.")
            .defaultValue(0.03)
            .min(-0.25)
            .max(0.5)
            .sliderRange(-0.25, 0.5)
            .build()
      );
   private final Setting<Double> glowEdge0 = this.sgGlow
      .add(
         new DoubleSetting.Builder()
            .name("glow-edge-0")
            .description("Lower edge of the glow mask.")
            .defaultValue(0.15)
            .min(0.0)
            .max(1.0)
            .sliderRange(0.0, 1.0)
            .build()
      );
   private final Setting<Double> glowEdge1 = this.sgGlow
      .add(
         new DoubleSetting.Builder()
            .name("glow-edge-1")
            .description("Upper edge of the glow mask.")
            .defaultValue(0.9)
            .min(0.0)
            .max(1.0)
            .sliderRange(0.0, 1.0)
            .build()
      );

   public LiquidGlassSetting() {
      super(Categories.Client, "liquid-glass-setting", "Global LiquidGlass rendering settings.");
      INSTANCE = this;
   }

   public static boolean isEnabled() {
      return INSTANCE == null || INSTANCE.enabled.get();
   }

   public static float getBlur() {
      return INSTANCE == null ? 12.0F : INSTANCE.blur.get().floatValue();
   }

   public static float getNoise() {
      return INSTANCE == null ? 0.03F : INSTANCE.noise.get().floatValue();
   }

   public static float getRefraction() {
      return INSTANCE == null ? 0.65F : INSTANCE.refraction.get().floatValue();
   }

   public static float getGlow() {
      return INSTANCE == null ? 0.18F : INSTANCE.glow.get().floatValue();
   }

   public static float getGlowBias() {
      return INSTANCE == null ? 0.03F : INSTANCE.glowBias.get().floatValue();
   }

   public static float getGlowEdge0() {
      return INSTANCE == null ? 0.15F : INSTANCE.glowEdge0.get().floatValue();
   }

   public static float getGlowEdge1() {
      return INSTANCE == null ? 0.9F : INSTANCE.glowEdge1.get().floatValue();
   }
}
