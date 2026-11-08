package meteordevelopment.meteorclient.systems.hud.elements;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public class PotionTimersHud extends HudElement {
   public static final HudElementInfo<PotionTimersHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "potion-timers", "Displays active potion effects with timers.", PotionTimersHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<List<MobEffect>> hiddenEffects = this.sgGeneral
      .add(new StatusEffectListSetting.Builder().name("hidden-effects").description("Which effects not to show in the list.").build());
   private final Setting<Boolean> showAmbient = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("show-ambient")
            .description("Whether to show ambient effects like from beacons and conduits.")
            .defaultValue(true)
            .build()
      );
   private final Setting<PotionTimersHud.ColorMode> colorMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("color-mode"))
                  .description("What color to use for effects."))
               .defaultValue(PotionTimersHud.ColorMode.Effect))
            .build()
      );
   private final Setting<SettingColor> flatColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("flat-color")
            .description("Color for flat color mode.")
            .defaultValue(new SettingColor(225, 25, 25))
            .visible(() -> this.colorMode.get() == PotionTimersHud.ColorMode.Flat)
            .build()
      );
   private final Setting<Double> rainbowSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rainbow-speed")
            .description("Rainbow speed of rainbow color mode.")
            .defaultValue(0.05)
            .sliderMin(0.01)
            .sliderMax(0.2)
            .decimalPlaces(4)
            .visible(() -> this.colorMode.get() == PotionTimersHud.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Double> rainbowSpread = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rainbow-spread")
            .description("Rainbow spread of rainbow color mode.")
            .defaultValue(0.01)
            .sliderMin(0.001)
            .sliderMax(0.05)
            .decimalPlaces(4)
            .visible(() -> this.colorMode.get() == PotionTimersHud.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Double> rainbowSaturation = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rainbow-saturation")
            .description("Saturation of rainbow color mode.")
            .defaultValue(1.0)
            .sliderRange(0.0, 1.0)
            .visible(() -> this.colorMode.get() == PotionTimersHud.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Double> rainbowBrightness = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rainbow-brightness")
            .description("Brightness of rainbow color mode.")
            .defaultValue(1.0)
            .sliderRange(0.0, 1.0)
            .visible(() -> this.colorMode.get() == PotionTimersHud.ColorMode.Rainbow)
            .build()
      );
   private final Setting<Boolean> shadow = this.sgGeneral
      .add(new BoolSetting.Builder().name("shadow").description("Renders shadow behind text.").defaultValue(true).build());
   private final Setting<Alignment> alignment = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("alignment")).description("Horizontal alignment."))
               .defaultValue(Alignment.Auto))
            .build()
      );
   private final Setting<Integer> border = this.sgGeneral
      .add(new IntSetting.Builder().name("border").description("How much space to add around the element.").defaultValue(0).build());
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
   private final List<Pair<MobEffectInstance, String>> texts = new ArrayList<>();
   private double rainbowHue;

   public PotionTimersHud() {
      super(INFO);
   }

   @Override
   public void setSize(double width, double height) {
      super.setSize(width + this.border.get() * 2, height + this.border.get() * 2);
   }

   @Override
   protected double alignX(double width, Alignment alignment) {
      return this.box.alignX(this.getWidth() - this.border.get() * 2, width, alignment);
   }

   @Override
   public void tick(HudRenderer renderer) {
      if (MeteorClient.mc.player != null && (!this.isInEditor() || !this.hasNoVisibleEffects())) {
         double width = 0.0;
         double height = 0.0;
         this.texts.clear();

         for (MobEffectInstance statusEffectInstance : MeteorClient.mc.player.getActiveEffects()) {
            if (!this.hiddenEffects.get().contains(statusEffectInstance.getEffect().value()) && (this.showAmbient.get() || !statusEffectInstance.isAmbient())) {
               String text = this.getString(statusEffectInstance);
               this.texts.add(new ObjectObjectImmutablePair<>(statusEffectInstance, text));
               width = Math.max(width, renderer.textWidth(text, this.shadow.get(), this.getScale()));
               height += renderer.textHeight(this.shadow.get(), this.getScale());
            }
         }

         this.setSize(width, height);
      } else {
         this.setSize(renderer.textWidth("Potion Timers 0:00", this.shadow.get(), this.getScale()), renderer.textHeight(this.shadow.get(), this.getScale()));
      }
   }

   @Override
   public void render(HudRenderer renderer) {
      double x = this.x + this.border.get();
      double y = this.y + this.border.get();
      if (this.background.get()) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      }

      if (MeteorClient.mc.player != null && (!this.isInEditor() || !this.hasNoVisibleEffects())) {
         this.rainbowHue = this.rainbowHue + this.rainbowSpeed.get() * renderer.delta;
         if (this.rainbowHue > 1.0) {
            this.rainbowHue--;
         } else if (this.rainbowHue < -1.0) {
            this.rainbowHue++;
         }

         double localRainbowHue = this.rainbowHue;

         for (Pair<MobEffectInstance, String> potionEffectEntry : this.texts) {
            Color color = switch ((PotionTimersHud.ColorMode)this.colorMode.get()) {
               case Effect -> {
                  int c = potionEffectEntry.left().getEffect().value().getColor();
                  yield new Color(c).a(255);
               }
               case Flat -> {
                  this.flatColor.get().update();
                  yield this.flatColor.get();
               }
               case Rainbow -> {
                  localRainbowHue += this.rainbowSpread.get();
                  int c = java.awt.Color.HSBtoRGB((float)localRainbowHue, this.rainbowSaturation.get().floatValue(), this.rainbowBrightness.get().floatValue());
                  yield new Color(c);
               }
            };
            String text = potionEffectEntry.right();
            renderer.text(
               text,
               x + this.alignX(renderer.textWidth(text, this.shadow.get(), this.getScale()), this.alignment.get()),
               y,
               color,
               this.shadow.get(),
               this.getScale()
            );
            y += renderer.textHeight(this.shadow.get(), this.getScale());
         }
      } else {
         renderer.text("Potion Timers 0:00", x, y, Color.WHITE, this.shadow.get(), this.getScale());
      }
   }

   private String getString(MobEffectInstance statusEffectInstance) {
      return String.format(
         "%s %d (%s)",
         Names.get(statusEffectInstance.getEffect().value()),
         statusEffectInstance.getAmplifier() + 1,
         MobEffectUtil.formatDuration(statusEffectInstance, 1.0F, MeteorClient.mc.level.tickRateManager().tickrate()).getString()
      );
   }

   private double getScale() {
      return this.customScale.get() ? this.scale.get() : Hud.get().getTextScale();
   }

   private boolean hasNoVisibleEffects() {
      for (MobEffectInstance statusEffectInstance : MeteorClient.mc.player.getActiveEffects()) {
         if (!this.hiddenEffects.get().contains(statusEffectInstance.getEffect().value()) && (this.showAmbient.get() || !statusEffectInstance.isAmbient())) {
            return false;
         }
      }

      return true;
   }

   public enum ColorMode {
      Effect,
      Flat,
      Rainbow;
   }
}
