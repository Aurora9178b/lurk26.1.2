package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Mth;

public class CompassHud extends HudElement {
   public static final HudElementInfo<CompassHud> INFO = new HudElementInfo<>(Hud.GROUP, "compass", "Displays a compass.", CompassHud::new);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<CompassHud.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("type"))
                  .description("Which type of direction information to show."))
               .defaultValue(CompassHud.Mode.Axis))
            .build()
      );
   private final Setting<SettingColor> colorNorth = this.sgGeneral
      .add(new ColorSetting.Builder().name("color-north").description("Color of north.").defaultValue(new SettingColor(225, 45, 45)).build());
   private final Setting<SettingColor> colorOther = this.sgGeneral
      .add(new ColorSetting.Builder().name("color-other").description("Color of other directions.").defaultValue(new SettingColor()).build());
   private final Setting<Boolean> shadow = this.sgGeneral.add(new BoolSetting.Builder().name("shadow").description("Text shadow.").defaultValue(false).build());
   private final Setting<Boolean> customScale = this.sgScale
      .add(
         new BoolSetting.Builder()
            .name("custom-scale")
            .description("Apply custom scales to this hud element.")
            .defaultValue(false)
            .onChanged(var1 -> this.calculateSize())
            .build()
      );
   private final Setting<Double> textScale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("text-scale")
            .description("Scale to use for the letters.")
            .visible(this.customScale::get)
            .defaultValue(1.0)
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   private final Setting<Double> compassScale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("compass-scale")
            .description("Scale of the whole HUD element.")
            .visible(this.customScale::get)
            .defaultValue(1.0)
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .onChanged(var1 -> this.calculateSize())
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

   public CompassHud() {
      super(INFO);
      this.calculateSize();
   }

   private void calculateSize() {
      this.setSize(100.0 * this.getCompassScale(), 100.0 * this.getCompassScale());
   }

   @Override
   public void render(HudRenderer renderer) {
      double x = this.x + this.getWidth() / 2.0;
      double y = this.y + this.getHeight() / 2.0;
      double pitch = this.isInEditor() ? 120.0 : Mth.clamp(MeteorClient.mc.player.getXRot() + 30.0F, -90.0F, 90.0F);
      pitch = Math.toRadians(pitch);
      double yaw = this.isInEditor() ? 180.0 : Mth.wrapDegrees(MeteorClient.mc.player.getYRot());
      yaw = Math.toRadians(yaw);

      for (CompassHud.Direction direction : CompassHud.Direction.values()) {
         String axis = this.mode.get() == CompassHud.Mode.Axis ? direction.getAxis() : direction.name();
         renderer.text(
            axis,
            x + this.getX(direction, yaw) - renderer.textWidth(axis, this.shadow.get(), this.getTextScale()) / 2.0,
            y + this.getY(direction, yaw, pitch) - renderer.textHeight(this.shadow.get(), this.getTextScale()) / 2.0,
            direction == CompassHud.Direction.N ? this.colorNorth.get() : this.colorOther.get(),
            this.shadow.get(),
            this.getTextScale()
         );
      }

      if (this.background.get()) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      }
   }

   private double getX(CompassHud.Direction direction, double yaw) {
      return Math.sin(this.getPos(direction, yaw)) * this.getCompassScale() * 40.0;
   }

   private double getY(CompassHud.Direction direction, double yaw, double pitch) {
      return Math.cos(this.getPos(direction, yaw)) * Math.sin(pitch) * this.getCompassScale() * 40.0;
   }

   private double getPos(CompassHud.Direction direction, double yaw) {
      return yaw + direction.ordinal() * Math.PI / 2.0;
   }

   private double getTextScale() {
      return this.customScale.get() ? this.textScale.get() : Hud.get().getTextScale();
   }

   private double getCompassScale() {
      return this.customScale.get() ? this.compassScale.get() : Hud.get().getTextScale();
   }

   private enum Direction {
      N("Z-"),
      W("X-"),
      S("Z+"),
      E("X+");

      private final String axis;

      Direction(String axis) {
         this.axis = axis;
      }

      public String getAxis() {
         return this.axis;
      }
   }

   public enum Mode {
      Direction,
      Axis;
   }
}
