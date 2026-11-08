package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;

public class PotionHUD extends HudElement {
   public static final HudElementInfo<PotionHUD> INFO = new HudElementInfo<>(
      Hud.GROUP, "potion-hud", "Potion HUD", "Displays active potion effects with cards.", PotionHUD::new
   );
   private static final double ROW_SPACING = 2.0;
   private static final double INNER_PADDING_X = 6.0;
   private static final double INNER_PADDING_Y = 4.0;
   private static final double ICON_TEXT_GAP = 6.0;
   private static final double PILL_WIDTH = 3.0;
   private static final double PILL_GAP = 6.0;
   private static final double NAME_DURATION_GAP = 1.0;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSize = this.settings.createGroup("Size");
   private final SettingGroup sgText = this.settings.createGroup("Text");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<List<MobEffect>> hiddenEffects = this.sgGeneral
      .add(new StatusEffectListSetting.Builder().name("hidden-effects").description("Which effects not to show in the list.").build());
   private final Setting<Boolean> showIcon = this.sgGeneral
      .add(new BoolSetting.Builder().name("show-icon").description("Shows potion effect icons.").defaultValue(true).build());
   private final Setting<Boolean> showProgressBar = this.sgGeneral
      .add(new BoolSetting.Builder().name("show-progress-bar").description("Shows the remaining duration progress bar.").defaultValue(true).build());
   private final Setting<Boolean> tintNameWithEffect = this.sgGeneral
      .add(new BoolSetting.Builder().name("tint-name-with-effect").description("Tints potion names with their effect color.").defaultValue(true).build());
   private final Setting<Boolean> hideAmbient = this.sgGeneral
      .add(new BoolSetting.Builder().name("hide-ambient").description("Hides ambient potion effects.").defaultValue(false).build());
   private final Setting<Double> animationSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("animation-speed")
            .description("Speed of potion row animations.")
            .defaultValue(10.0)
            .min(1.0)
            .sliderRange(1.0, 20.0)
            .build()
      );
   private final Setting<Double> scale = this.sgSize
      .add(new DoubleSetting.Builder().name("scale").description("Scale of the potion HUD.").defaultValue(1.0).min(0.5).sliderRange(0.5, 2.5).build());
   private final Setting<Double> rowHeight = this.sgSize
      .add(
         new DoubleSetting.Builder().name("row-height").description("Height of each potion row.").defaultValue(22.0).min(16.0).sliderRange(16.0, 40.0).build()
      );
   private final Setting<Double> iconSize = this.sgSize
      .add(
         new DoubleSetting.Builder()
            .name("icon-size")
            .description("Size of potion effect icons.")
            .defaultValue(14.0)
            .min(8.0)
            .sliderRange(8.0, 32.0)
            .visible(this.showIcon::get)
            .build()
      );
   private final Setting<Double> nameTextScale = this.sgText
      .add(
         new DoubleSetting.Builder()
            .name("name-text-scale")
            .description("Scale of potion name text.")
            .defaultValue(0.78)
            .min(0.4)
            .sliderRange(0.4, 1.5)
            .build()
      );
   private final Setting<Double> durationTextScale = this.sgText
      .add(
         new DoubleSetting.Builder()
            .name("duration-text-scale")
            .description("Scale of potion duration text.")
            .defaultValue(0.62)
            .min(0.4)
            .sliderRange(0.4, 1.5)
            .build()
      );
   private final Setting<Boolean> textShadow = this.sgText
      .add(new BoolSetting.Builder().name("text-shadow").description("Renders text shadow.").defaultValue(false).build());
   private final Setting<PotionHUD.BackgroundMode> backgroundMode = this.sgBackground
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for the potion row backgrounds."))
               .defaultValue(PotionHUD.BackgroundMode.LiquidGlass))
            .build()
      );
   private final Setting<Double> backgroundRadius = this.sgBackground
      .add(
         new DoubleSetting.Builder()
            .name("background-radius")
            .description("Radius of the background corners.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for potion row backgrounds.")
            .defaultValue(new SettingColor(15, 15, 15, 145))
            .build()
      );
   private final Setting<Double> blurStrength = this.sgBackground
      .add(
         new DoubleSetting.Builder()
            .name("blur-strength")
            .description("Strength of the blur background.")
            .defaultValue(5.0)
            .min(1.0)
            .sliderRange(1.0, 16.0)
            .visible(() -> this.backgroundMode.get() == PotionHUD.BackgroundMode.Blur)
            .build()
      );
   private final Setting<Boolean> enableGlow = this.sgBackground
      .add(new BoolSetting.Builder().name("enable-glow").description("Renders glow around potion row backgrounds.").defaultValue(false).build());
   private final Map<Holder<MobEffect>, Float> alphaMap = new HashMap<>();
   private final Map<Holder<MobEffect>, Integer> maxDurationMap = new HashMap<>();

   public PotionHUD() {
      super(INFO);
   }

   @Override
   public void render(HudRenderer renderer) {
      List<PotionHUD.EffectInfo> items = this.collectItems(renderer);
      if (items.isEmpty()) {
         this.setSize(0.0, 0.0);
      } else {
         double totalHeight = 0.0;
         double maxWidth = 0.0;
         double rowSpacing = 2.0 * this.scale.get();
         boolean first = true;

         for (PotionHUD.EffectInfo info : items) {
            if (!(info.alpha <= 0.001F)) {
               maxWidth = Math.max(maxWidth, info.totalWidth);
               totalHeight += info.rowHeight;
               if (!first) {
                  totalHeight += rowSpacing;
               }

               first = false;
            }
         }

         this.setSize(maxWidth, totalHeight);
         this.updatePos();
         double currentY = this.y;
         first = true;

         for (PotionHUD.EffectInfo info : items) {
            if (!(info.alpha <= 0.001F)) {
               float alpha = Mth.clamp(info.alpha, 0.0F, 1.0F);
               if (!first) {
                  currentY += rowSpacing;
               }

               first = false;
               double rowX = this.x + this.alignX(info.totalWidth, Alignment.Auto);
               this.drawRow(renderer, info, rowX, currentY, alpha);
               currentY += info.rowHeight;
            }
         }
      }
   }

   private List<PotionHUD.EffectInfo> collectItems(HudRenderer renderer) {
      if (MeteorClient.mc.player == null) {
         return HudEditorScreen.isOpen() ? List.of(this.createPreview(renderer)) : List.of();
      }

      Collection<MobEffectInstance> active = MeteorClient.mc.player.getActiveEffects();
      double s = this.scale.get();
      Set<Holder<MobEffect>> stillActive = new HashSet<>();
      Map<Holder<MobEffect>, MobEffectInstance> instanceMap = new HashMap<>();

      for (MobEffectInstance instance : active) {
         if (!this.hiddenEffects.get().contains(instance.getEffect().value()) && (!this.hideAmbient.get() || !instance.isAmbient())) {
            Holder<MobEffect> holder = instance.getEffect();
            stillActive.add(holder);
            instanceMap.put(holder, instance);
            this.alphaMap.put(holder, AnimationUtility.ease(this.alphaMap.getOrDefault(holder, 0.0F), 1.0F, this.animationSpeed.get().floatValue()));
            int duration = instance.getDuration();
            if (duration > 0) {
               int previousMax = this.maxDurationMap.getOrDefault(holder, 0);
               if (duration > previousMax) {
                  this.maxDurationMap.put(holder, duration);
               }
            }
         }
      }

      Iterator<Entry<Holder<MobEffect>, Float>> iterator = this.alphaMap.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry<Holder<MobEffect>, Float> entry = iterator.next();
         if (!stillActive.contains(entry.getKey())) {
            float alpha = AnimationUtility.ease(entry.getValue(), 0.0F, this.animationSpeed.get().floatValue());
            if (alpha < 0.001F) {
               iterator.remove();
               this.maxDurationMap.remove(entry.getKey());
            } else {
               entry.setValue(alpha);
            }
         }
      }

      if (this.alphaMap.isEmpty()) {
         return HudEditorScreen.isOpen() ? List.of(this.createPreview(renderer)) : List.of();
      }

      List<PotionHUD.EffectInfo> items = new ArrayList<>(this.alphaMap.size());

      for (Entry<Holder<MobEffect>, Float> entry : this.alphaMap.entrySet()) {
         Holder<MobEffect> holder = entry.getKey();
         MobEffectInstance instance = instanceMap.get(holder);
         int amplifier = instance == null ? 0 : instance.getAmplifier();
         int duration = instance == null ? 0 : instance.getDuration();
         boolean infinite = instance != null && instance.isInfiniteDuration();
         String name = Names.get(holder.value()) + romanLevel(amplifier);
         String durationText = infinite ? "∞" : formatDuration(instance, duration);
         Color effectColor = effectColor(holder);
         float progress = this.progress(holder, duration, infinite);
         items.add(this.createInfo(renderer, name, durationText, entry.getValue(), effectColor, progress, getIconTexture(holder), s));
      }

      items.sort(Comparator.comparing(info -> info.name));
      return items;
   }

   private PotionHUD.EffectInfo createPreview(HudRenderer renderer) {
      return this.createInfo(renderer, "Potion HUD", "0:00", 1.0F, new Color(120, 170, 255, 255), 1.0F, null, this.scale.get());
   }

   private PotionHUD.EffectInfo createInfo(
      HudRenderer renderer, String name, String duration, float alpha, Color effectColor, float progress, Identifier icon, double scale
   ) {
      double nameScale = this.nameTextScale.get() * scale;
      double durationScale = this.durationTextScale.get() * scale;
      double padX = 6.0 * scale;
      double padY = 4.0 * scale;
      double iconSize = this.iconSize.get() * scale;
      double iconTextGap = 6.0 * scale;
      double pillWidth = 3.0 * scale;
      double pillGap = 6.0 * scale;
      double nameHeight = renderer.textHeight(this.textShadow.get(), nameScale);
      double durationHeight = renderer.textHeight(this.textShadow.get(), durationScale);
      double textBlockHeight = nameHeight + durationHeight + 1.0 * scale;
      double rowHeight = Math.max(this.rowHeight.get() * scale, Math.max(textBlockHeight, this.showIcon.get() && icon != null ? iconSize : 0.0) + padY * 2.0);
      double textWidth = Math.max(
         renderer.textWidth(name, this.textShadow.get(), nameScale), renderer.textWidth(duration, this.textShadow.get(), durationScale)
      );
      double totalWidth = padX;
      if (this.showIcon.get() && icon != null) {
         totalWidth += iconSize + iconTextGap;
      }

      totalWidth += textWidth;
      if (this.showProgressBar.get()) {
         totalWidth += pillGap + pillWidth;
      }

      totalWidth += padX;
      return new PotionHUD.EffectInfo(name, duration, alpha, effectColor, totalWidth, rowHeight, progress, icon);
   }

   private void drawRow(HudRenderer renderer, PotionHUD.EffectInfo info, double rowX, double rowY, float alpha) {
      double s = this.scale.get();
      double rowHeight = info.rowHeight;
      double radius = this.backgroundRadius.get() * s;
      double padX = 6.0 * s;
      double padY = 4.0 * s;
      double iconSize = this.iconSize.get() * s;
      double iconTextGap = 6.0 * s;
      double pillWidth = 3.0 * s;
      double nameScale = this.nameTextScale.get() * s;
      double durationScale = this.durationTextScale.get() * s;
      this.drawBackground(renderer, rowX, rowY, info.totalWidth, rowHeight, radius, withAlpha(this.backgroundColor.get(), alpha));
      double cursorX = rowX + padX;
      if (this.showIcon.get() && info.iconTexture != null) {
         renderer.texture(
            info.iconTexture, cursorX, rowY + (rowHeight - iconSize) * 0.5, iconSize, iconSize, new Color(255, 255, 255, Math.round(255.0F * alpha))
         );
         cursorX += iconSize + iconTextGap;
      }

      double nameHeight = renderer.textHeight(this.textShadow.get(), nameScale);
      double durationHeight = renderer.textHeight(this.textShadow.get(), durationScale);
      double textGap = 1.0 * s;
      double textBlockHeight = nameHeight + durationHeight + textGap;
      double nameY = rowY + (rowHeight - textBlockHeight) * 0.5;
      double durationY = nameY + nameHeight + textGap;
      Color nameColor = this.tintNameWithEffect.get()
         ? withAlpha(brighten(info.effectColor, 1.15F), alpha)
         : new Color(255, 255, 255, Math.round(235.0F * alpha));
      Color durationColor = new Color(180, 180, 180, Math.round(220.0F * alpha));
      renderer.text(info.name, cursorX, nameY, nameColor, this.textShadow.get(), nameScale);
      renderer.text(info.duration, cursorX, durationY, durationColor, this.textShadow.get(), durationScale);
      if (this.showProgressBar.get()) {
         double pillX = rowX + info.totalWidth - padX - pillWidth;
         double pillY = rowY + padY;
         double pillHeight = rowHeight - padY * 2.0;
         double pillRadius = pillWidth * 0.5;
         Render2DUtil.drawRoundedRect(renderer, pillX, pillY, pillWidth, pillHeight, pillRadius, new Color(0, 0, 0, Math.round(140.0F * alpha)));
         double fillHeight = pillHeight * info.progress;
         if (fillHeight > 0.5) {
            double fillY = pillY + pillHeight - fillHeight;
            Render2DUtil.drawRoundedRect(renderer, pillX, fillY, pillWidth, fillHeight, pillRadius, withAlpha(brighten(info.effectColor, 1.3F), alpha));
         }
      }
   }

   private void drawBackground(HudRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
      if (this.backgroundMode.get() == PotionHUD.BackgroundMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
         LiquidGlassUtil.render(renderer, x, y, width, height, radius, color, this.blurStrength.get().floatValue());
      } else {
         BlurUtil.render(
            (int)Math.round(x), (int)Math.round(y), (int)Math.ceil(width), (int)Math.ceil(height), (float)radius, this.blurStrength.get().floatValue()
         );
         Render2DUtil.drawRoundedRect(renderer, x, y, width, height, radius, color);
      }

      if (this.enableGlow.get()) {
         Render2DUtil.drawRoundedGlow(x, y, width, height, radius);
      }
   }

   private static Identifier getIconTexture(Holder<MobEffect> holder) {
      return holder.unwrapKey()
         .map(ResourceKey::identifier)
         .map(id -> Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png"))
         .orElse(null);
   }

   private static String formatDuration(MobEffectInstance instance, int duration) {
      if (instance != null && MeteorClient.mc.level != null) {
         return MobEffectUtil.formatDuration(instance, 1.0F, MeteorClient.mc.level.tickRateManager().tickrate()).getString();
      }

      int totalSeconds = Math.max(0, duration / 20);
      int minutes = totalSeconds / 60;
      int seconds = totalSeconds % 60;
      return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
   }

   private float progress(Holder<MobEffect> holder, int duration, boolean infinite) {
      if (infinite) {
         return 1.0F;
      }

      int maxDuration = this.maxDurationMap.getOrDefault(holder, Math.max(duration, 1));
      return Mth.clamp((float)duration / Math.max(1, maxDuration), 0.0F, 1.0F);
   }

   private static String romanLevel(int amplifier) {
      int level = amplifier + 1;
      if (level <= 1) {
         return "";
      }

      return " " + switch (level) {
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> Integer.toString(level);
      };
   }

   private static Color effectColor(Holder<MobEffect> holder) {
      int rgb = holder.value().getColor() & 16777215;
      return new Color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255);
   }

   private static Color withAlpha(Color color, float alphaMul) {
      return new Color(color.r, color.g, color.b, Mth.clamp(Math.round(color.a * alphaMul), 0, 255));
   }

   private static Color brighten(Color color, float factor) {
      return new Color(
         Mth.clamp(Math.round(color.r * factor), 0, 255),
         Mth.clamp(Math.round(color.g * factor), 0, 255),
         Mth.clamp(Math.round(color.b * factor), 0, 255),
         color.a
      );
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }

   private record EffectInfo(
      String name, String duration, float alpha, Color effectColor, double totalWidth, double rowHeight, float progress, Identifier iconTexture
   ) {
   }
}
