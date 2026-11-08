package meteordevelopment.meteorclient.systems.modules.client;

import java.awt.Color;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class GlowSetting extends Module {
   private static GlowSetting INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<GlowSetting.Style> style = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("style")).description("Glow rendering style."))
               .defaultValue(GlowSetting.Style.Lurk))
            .build()
      );
   private final Setting<Double> lurkStrength = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("lurk-strength")
            .description("Lurk glow strength.")
            .defaultValue(4.5)
            .min(0.0)
            .max(30.0)
            .sliderRange(0.0, 30.0)
            .visible(() -> this.style.get() == GlowSetting.Style.Lurk)
            .build()
      );
   private final Setting<SettingColor> lurkColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("lurk-color")
            .description("Lurk glow color.")
            .defaultValue(new SettingColor(100, 150, 255, 150))
            .visible(() -> this.style.get() == GlowSetting.Style.Lurk)
            .build()
      );
   private final Setting<Boolean> acidFlow = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("acid-flow")
            .description("Animates Acid colors like the original CaptureMark color source.")
            .defaultValue(true)
            .visible(() -> this.style.get() == GlowSetting.Style.Acid)
            .build()
      );
   private final Setting<GlowSetting.AcidFlowMode> acidFlowMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("acid-flow-mode"))
                     .description("Acid animated color mode."))
                  .defaultValue(GlowSetting.AcidFlowMode.Sky))
               .visible(() -> this.style.get() == GlowSetting.Style.Acid && this.acidFlow.get()))
            .build()
      );
   private final Setting<Integer> acidFlowSpeed = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("acid-flow-speed")
            .description("Acid color flow speed.")
            .defaultValue(8)
            .range(1, 100)
            .sliderRange(2, 54)
            .visible(() -> this.style.get() == GlowSetting.Style.Acid && this.acidFlow.get())
            .build()
      );
   private final Setting<SettingColor> acidColor1 = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("acid-color-1")
            .description("Acid shader color 1. Its alpha is also used as Acid external alpha.")
            .defaultValue(new SettingColor(0, 0, 0, 255))
            .visible(() -> this.style.get() == GlowSetting.Style.Acid)
            .build()
      );
   private final Setting<SettingColor> acidColor2 = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("acid-color-2")
            .description("Acid shader color 2.")
            .defaultValue(new SettingColor(0, 0, 0, 255))
            .visible(() -> this.style.get() == GlowSetting.Style.Acid)
            .build()
      );
   private final Setting<SettingColor> acidColor3 = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("acid-color-3")
            .description("Acid shader color 3.")
            .defaultValue(new SettingColor(0, 0, 0, 255))
            .visible(() -> this.style.get() == GlowSetting.Style.Acid)
            .build()
      );
   private final Setting<SettingColor> acidColor4 = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("acid-color-4")
            .description("Acid shader color 4.")
            .defaultValue(new SettingColor(0, 0, 0, 255))
            .visible(() -> this.style.get() == GlowSetting.Style.Acid)
            .build()
      );

   public GlowSetting() {
      super(Categories.Client, "GlowSetting", "HUD glow settings.");
      INSTANCE = this;
   }

   public static boolean isGlowEnabled() {
      return getStyle() == GlowSetting.Style.Acid ? getAcidColor1().a > 0 : getLurkStrength() > 0.0F && getLurkColor().a > 0;
   }

   public static GlowSetting.Style getStyle() {
      return INSTANCE == null ? GlowSetting.Style.Lurk : INSTANCE.style.get();
   }

   public static float getLurkStrength() {
      return INSTANCE == null ? 0.0F : INSTANCE.lurkStrength.get().floatValue();
   }

   public static SettingColor getLurkColor() {
      return INSTANCE == null ? new SettingColor(100, 150, 255, 150) : INSTANCE.lurkColor.get();
   }

   public static SettingColor getAcidColor1() {
      return INSTANCE == null ? new SettingColor(0, 0, 0, 255) : INSTANCE.getAcidColor(270, INSTANCE.acidColor1.get());
   }

   public static SettingColor getAcidColor2() {
      return INSTANCE == null ? new SettingColor(0, 0, 0, 255) : INSTANCE.getAcidColor(0, INSTANCE.acidColor2.get());
   }

   public static SettingColor getAcidColor3() {
      return INSTANCE == null ? new SettingColor(0, 0, 0, 255) : INSTANCE.getAcidColor(180, INSTANCE.acidColor3.get());
   }

   public static SettingColor getAcidColor4() {
      return INSTANCE == null ? new SettingColor(0, 0, 0, 255) : INSTANCE.getAcidColor(90, INSTANCE.acidColor4.get());
   }

   public static float getAcidGlow() {
      return 0.5F;
   }

   public static float getAcidInternalAlpha() {
      return 0.0F;
   }

   private SettingColor getAcidColor(int offset, SettingColor fallback) {
      if (!this.acidFlow.get()) {
         return fallback;
      }

      int alpha = this.acidColor1.get().a;
      int speed = Math.max(1, this.acidFlowSpeed.get());

      return switch ((GlowSetting.AcidFlowMode)this.acidFlowMode.get()) {
         case Sky -> skyRainbow(speed, offset, alpha);
         case LightRainbow -> rainbow(speed, offset, 0.6F, 1.0F, alpha);
         case Rainbow -> rainbow(speed, offset, 1.0F, 1.0F, alpha);
         case Fade -> fade(speed, offset, this.acidColor1.get(), alpha);
         case DoubleColor -> twoColor(this.acidColor1.get(), this.acidColor2.get(), speed, offset, alpha, false);
         case Analogous -> twoColor(this.acidColor1.get(), analogous(this.acidColor2.get(), alpha), speed, offset, alpha, true);
      };
   }

   private static SettingColor skyRainbow(int speed, int index, int alpha) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      int hueIndex = angle % 360;
      float hue = hueIndex / 360.0F;
      if (hue < 0.5F) {
         hue = -hue;
      }

      return fromAwt(Color.getHSBColor(hue, 0.5F, 1.0F), alpha);
   }

   private static SettingColor rainbow(int speed, int index, float saturation, float brightness, int alpha) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      Color color = new Color(Color.HSBtoRGB(angle / 360.0F, saturation, brightness));
      return fromAwt(color, alpha);
   }

   private static SettingColor fade(int speed, int index, SettingColor color, int alpha) {
      float[] hsb = Color.RGBtoHSB(color.r, color.g, color.b, null);
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = (angle > 180 ? 360 - angle : angle) + 180;
      Color faded = new Color(Color.HSBtoRGB(hsb[0], hsb[1], angle / 360.0F));
      return fromAwt(faded, alpha);
   }

   private static SettingColor twoColor(SettingColor start, SettingColor end, int speed, int index, int alpha, boolean hue) {
      int angle = (int)((System.currentTimeMillis() / speed + index) % 360L);
      angle = (angle >= 180 ? 360 - angle : angle) * 2;
      float progress = Math.min(1.0F, Math.max(0.0F, angle / 360.0F));
      return hue ? interpolateHue(start, end, progress, alpha) : interpolateRgb(start, end, progress, alpha);
   }

   private static SettingColor analogous(SettingColor color, int alpha) {
      float[] hsb = Color.RGBtoHSB(color.r, color.g, color.b, null);
      Color analogous = new Color(Color.HSBtoRGB(hsb[0] - 0.84F, hsb[1], hsb[2]));
      return fromAwt(analogous, alpha);
   }

   private static SettingColor interpolateRgb(
      meteordevelopment.meteorclient.utils.render.color.Color start, meteordevelopment.meteorclient.utils.render.color.Color end, float progress, int alpha
   ) {
      return new SettingColor(interpolate(start.r, end.r, progress), interpolate(start.g, end.g, progress), interpolate(start.b, end.b, progress), alpha);
   }

   private static SettingColor interpolateHue(
      meteordevelopment.meteorclient.utils.render.color.Color start, meteordevelopment.meteorclient.utils.render.color.Color end, float progress, int alpha
   ) {
      float[] startHsb = Color.RGBtoHSB(start.r, start.g, start.b, null);
      float[] endHsb = Color.RGBtoHSB(end.r, end.g, end.b, null);
      Color color = Color.getHSBColor(lerp(startHsb[0], endHsb[0], progress), lerp(startHsb[1], endHsb[1], progress), lerp(startHsb[2], endHsb[2], progress));
      return fromAwt(color, alpha);
   }

   private static SettingColor fromAwt(Color color, int alpha) {
      return new SettingColor(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   private static int interpolate(int start, int end, float progress) {
      return (int)(start + (end - start) * progress);
   }

   private static float lerp(float start, float end, float progress) {
      return start + (end - start) * progress;
   }

   public enum AcidFlowMode {
      Sky,
      LightRainbow,
      Rainbow,
      Fade,
      DoubleColor,
      Analogous;
   }

   public enum Style {
      Lurk,
      Acid;
   }
}
