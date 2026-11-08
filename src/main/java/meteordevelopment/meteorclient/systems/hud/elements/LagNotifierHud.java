package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;

public class LagNotifierHud extends HudElement {
   public static final HudElementInfo<LagNotifierHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "lag-notifier", "Displays if the server is lagging in ticks.", LagNotifierHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Boolean> shadow = this.sgGeneral.add(new BoolSetting.Builder().name("shadow").description("Text shadow.").defaultValue(true).build());
   private final Setting<SettingColor> textColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("text-color").description("A.").defaultValue(new SettingColor()).build());
   private final Setting<SettingColor> color1 = this.sgGeneral
      .add(new ColorSetting.Builder().name("color-1").description("First color.").defaultValue(new SettingColor(255, 255, 5)).build());
   private final Setting<SettingColor> color2 = this.sgGeneral
      .add(new ColorSetting.Builder().name("color-2").description("Second color.").defaultValue(new SettingColor(235, 158, 52)).build());
   private final Setting<SettingColor> color3 = this.sgGeneral
      .add(new ColorSetting.Builder().name("color-3").description("Third color.").defaultValue(new SettingColor(225, 45, 45)).build());
   private final Setting<Boolean> customScale = this.sgScale
      .add(new BoolSetting.Builder().name("custom-scale").description("Applies a custom scale to this hud element.").defaultValue(false).build());
   private final Setting<Double> scale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(this.customScale::get)
            .defaultValue(1.0)
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   private final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );

   public LagNotifierHud() {
      super(INFO);
   }

   @Override
   public void render(HudRenderer renderer) {
      float timeSinceLastTick = TickRate.INSTANCE.getTimeSinceLastTick();
      if (!(timeSinceLastTick < 1.1F) || this.isInEditor()) {
         Color color = !this.isInEditor() && !(timeSinceLastTick > 10.0F)
            ? (timeSinceLastTick > 3.0F ? this.color2.get() : this.color1.get())
            : this.color3.get();
         String info = this.isInEditor() ? "10.2" : String.format("%.1f", timeSinceLastTick);
         this.render(renderer, info, color);
         if (this.background.get()) {
            renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
         }
      }
   }

   private void render(HudRenderer renderer, String right, Color rightColor) {
      double x2 = renderer.text("Time since last tick ", this.x, this.y, this.textColor.get(), this.shadow.get(), this.getScale());
      x2 = renderer.text(right, x2, this.y, rightColor, this.shadow.get(), this.getScale());
      this.setSize(x2 - this.x, renderer.textHeight(this.shadow.get(), this.getScale()));
   }

   private double getScale() {
      return this.customScale.get() ? this.scale.get() : Hud.get().getTextScale();
   }
}
