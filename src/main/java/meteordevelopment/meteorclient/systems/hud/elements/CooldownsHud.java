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
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class CooldownsHud extends HudElement {
   public static final HudElementInfo<CooldownsHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "cooldowns", "Cooldowns", "Displays attack and hurt cooldowns.", CooldownsHud::new
   );
   private static final double BASE_WIDTH = 100.0;
   private static final double BASE_HEIGHT = 40.0;
   private static final double BAR_X = 30.0;
   private static final double BAR_WIDTH = 65.0;
   private static final double BAR_HEIGHT = 5.0;
   private static final double BAR_RADIUS = 1.5;
   private static final double TITLE_SCALE = 0.72;
   private static final double LABEL_SCALE = 0.58;
   private static final float ANIMATION_SPEED = 10.0F;
   private static final Color TEXT = new Color(245, 245, 245, 255);
   private static final Color TEXT_DIM = new Color(214, 216, 226, 235);
   private static final Color BAR_TRACK = new Color(42, 47, 64, 168);
   private static final Color BAR_FILL = new Color(255, 255, 255, 236);
   private static final Color LINE = new Color(255, 255, 255, 84);
   private static final Color LINE_CLEAR = new Color(255, 255, 255, 0);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("Scale of the cooldowns HUD.").defaultValue(1.0).min(0.5).sliderRange(0.5, 3.0).build());
   private final Setting<CooldownsHud.BackgroundMode> backgroundMode = this.sgBackground
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for the cooldowns background."))
               .defaultValue(CooldownsHud.BackgroundMode.LiquidGlass))
            .build()
      );
   private final Setting<Double> backgroundRadius = this.sgBackground
      .add(
         new DoubleSetting.Builder()
            .name("background-radius")
            .description("Radius of the background corners.")
            .defaultValue(5.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder().name("background-color").description("Color used for the background.").defaultValue(new SettingColor(0, 0, 0, 96)).build()
      );
   private final Setting<Boolean> enableGlow = this.sgBackground
      .add(new BoolSetting.Builder().name("enable-glow").description("Renders glow around the cooldowns background.").defaultValue(true).build());
   private float attackProgress = 1.0F;
   private float hurtProgress = 1.0F;

   private CooldownsHud() {
      super(INFO);
      this.setSize(100.0, 40.0);
   }

   @Override
   public void render(HudRenderer renderer) {
      double s = this.scale.get();
      double width = 100.0 * s;
      double height = 40.0 * s;
      this.setSize(width, height);
      float attackTarget = MeteorClient.mc.player == null ? 1.0F : clamp(MeteorClient.mc.player.getAttackStrengthScale(0.5F));
      float hurtTarget = MeteorClient.mc.player == null ? 1.0F : clamp(1.0F - MeteorClient.mc.player.hurtTime / 10.0F);
      this.attackProgress = AnimationUtility.ease(this.attackProgress, attackTarget, 10.0F);
      this.hurtProgress = AnimationUtility.ease(this.hurtProgress, hurtTarget, 10.0F);
      this.drawBackground(renderer, width, height, s);
      this.drawTitle(renderer, width, s);
      this.drawDivider(renderer, width, s);
      this.drawRow(renderer, "Attack", this.y + 20.0 * s, this.attackProgress, s);
      this.drawRow(renderer, "Hurt", this.y + 30.0 * s, this.hurtProgress, s);
   }

   private void drawBackground(HudRenderer renderer, double width, double height, double scale) {
      double radius = this.backgroundRadius.get() * scale;
      Color color = this.backgroundColor.get();
      if (this.backgroundMode.get() == CooldownsHud.BackgroundMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
         LiquidGlassUtil.render(renderer, this.x, this.y, width, height, radius, color);
      } else {
         BlurUtil.render(Math.round(this.x), Math.round(this.y), (int)Math.ceil(width), (int)Math.ceil(height), (float)radius);
         Render2DUtil.drawRoundedRect(renderer, this.x, this.y, width, height, radius, color);
      }

      if (this.enableGlow.get()) {
         Render2DUtil.drawRoundedGlow(this.x, this.y, width, height, radius);
      }
   }

   private void drawTitle(HudRenderer renderer, double width, double scale) {
      String title = "Cooldowns";
      double textScale = textScale(0.72, scale);
      double titleWidth = renderer.textWidth(title, false, textScale);
      renderer.text(title, this.x + (width - titleWidth) * 0.5, this.y + 4.0 * scale, TEXT, false, textScale);
   }

   private void drawDivider(HudRenderer renderer, double width, double scale) {
      double center = this.x + width * 0.5;
      double lineY = this.y + 13.7 * scale;
      renderer.quad(this.x + 3.0 * scale, lineY, width * 0.5 - 3.0 * scale, 0.45 * scale, LINE_CLEAR, LINE, LINE, LINE_CLEAR);
      renderer.quad(center, lineY, width * 0.5 - 3.0 * scale, 0.45 * scale, LINE, LINE_CLEAR, LINE_CLEAR, LINE);
   }

   private void drawRow(HudRenderer renderer, String label, double rowY, float progress, double scale) {
      double textScale = textScale(0.58, scale);
      double barHeight = 5.0 * scale;
      double barRadius = 1.5 * scale;
      double barWidth = 65.0 * scale;
      double labelY = rowY - renderer.textHeight(false, textScale) * 0.5 + 1.0 * scale;
      double barY = rowY - barHeight * 0.5;
      double barX = this.x + 30.0 * scale;
      renderer.text(label, this.x + 5.0 * scale, labelY, TEXT_DIM, false, textScale);
      Render2DUtil.drawRoundedRect(renderer, barX, barY, barWidth, barHeight, barRadius, BAR_TRACK);
      double fillWidth = barWidth * clamp(progress);
      if (fillWidth > 0.25) {
         Render2DUtil.drawRoundedRect(renderer, barX, barY, fillWidth, barHeight, Math.min(barRadius, fillWidth * 0.5), BAR_FILL);
      }
   }

   private static double textScale(double baseScale, double layoutScale) {
      return baseScale * Math.sqrt(layoutScale);
   }

   private static float clamp(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }
}
