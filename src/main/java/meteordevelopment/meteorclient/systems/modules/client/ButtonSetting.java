package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ButtonSetting extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final SettingGroup sgSearchBox = this.settings.createGroup("SearchBox");
   public final Setting<Boolean> enabled = this.sgGeneral
      .add(new BoolSetting.Builder().name("Enabled").description("Enable custom button colors.").defaultValue(true).build());
   public final Setting<SettingColor> normalColor = this.sgGeneral
      .add(
         new ColorSetting.Builder().name("Normal Color").description("Button color when not hovered.").defaultValue(new SettingColor(30, 30, 30, 150)).build()
      );
   public final Setting<SettingColor> hoverColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("Hover Color")
            .description("Button color when mouse is over it.")
            .defaultValue(new SettingColor(255, 255, 255, 128))
            .build()
      );
   public final Setting<SettingColor> disabledColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("Disabled Color")
            .description("Button color when not clickable.")
            .defaultValue(new SettingColor(160, 160, 160, 150))
            .build()
      );
   public final Setting<Double> animationSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("Animation Speed")
            .description("Speed of button color animation.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 5.0)
            .build()
      );
   public final Setting<ButtonSetting.BackgroundMode> backgroundMode = this.sgBackground
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for button backgrounds."))
               .defaultValue(ButtonSetting.BackgroundMode.Blur))
            .build()
      );
   public final Setting<Double> backgroundRadius = this.sgBackground
      .add(
         new DoubleSetting.Builder()
            .name("background-radius")
            .description("Radius of button background corners.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   public final Setting<Boolean> enableGlow = this.sgBackground
      .add(new BoolSetting.Builder().name("enable-glow").description("Renders glow around button backgrounds.").defaultValue(false).build());
   public final Setting<Boolean> searchBoxBloom = this.sgSearchBox
      .add(new BoolSetting.Builder().name("search-box-bloom").description("Renders bloom around ClickGUI search boxes.").defaultValue(false).build());
   public final Setting<SettingColor> searchBoxColor = this.sgSearchBox
      .add(
         new ColorSetting.Builder()
            .name("search-box-background-color")
            .description("ClickGUI search box background color.")
            .defaultValue(new SettingColor(18, 18, 22, 142))
            .build()
      );

   public ButtonSetting() {
      super(Categories.Client, "ButtonSetting", "Button render settings.");
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }
}
