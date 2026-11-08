package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardHud extends HudElement {
   public static final HudElementInfo<ScoreboardHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "scoreboard", "ScoreBoard", "Displays the server scoreboard sidebar.", ScoreboardHud::new
   );
   private static final double PADDING_X = 5.0;
   private static final double PADDING_Y = 4.0;
   private static final double TITLE_GAP = 4.0;
   private static final double SCORE_GAP = 7.0;
   private static final double MIN_WIDTH = 86.0;
   private static final Color TEXT = new Color(245, 245, 245, 255);
   private static final Color PLACEHOLDER = new Color(170, 172, 180, 255);
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<ScoreboardHud.BackgroundMode> backgroundMode = this.sgBackground
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for the scoreboard background."))
               .defaultValue(ScoreboardHud.BackgroundMode.LiquidGlass))
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
         new ColorSetting.Builder().name("background-color").description("Color used for the background.").defaultValue(new SettingColor(0, 0, 0, 92)).build()
      );
   private final Setting<Boolean> enableGlow = this.sgBackground
      .add(new BoolSetting.Builder().name("enable-glow").description("Renders glow around the scoreboard background.").defaultValue(true).build());

   private ScoreboardHud() {
      super(INFO);
      this.setSize(86.0, 24.0);
   }

   @Override
   public void render(HudRenderer renderer) {
      ScoreboardHud.ScoreData data = this.collectScoreData();
      if (data == null) {
         if (!this.isInEditor()) {
            this.setSize(86.0, 24.0);
            return;
         }

         data = new ScoreboardHud.ScoreData(
            List.of(new ScoreboardHud.Segment("ScoreBoard", PLACEHOLDER)),
            List.of(new ScoreboardHud.ScoreLine(List.of(new ScoreboardHud.Segment("Waiting for sidebar objective", PLACEHOLDER)), List.of()))
         );
      }

      ScoreboardHud.Layout layout = this.calculateLayout(renderer, data);
      this.setSize(layout.width(), layout.height());
      this.drawBackground(renderer, layout.width(), layout.height());
      double titleY = this.y + 4.0;
      this.drawSegmentsCentered(renderer, data.title(), this.x + 5.0, titleY, layout.width() - 10.0);
      double rowY = titleY + layout.titleHeight() + 4.0;

      for (ScoreboardHud.ScoreLine line : data.lines()) {
         double textY = rowY + (layout.lineHeight() - layout.textHeight()) * 0.5;
         this.drawSegments(renderer, line.name(), this.x + 5.0, textY);
         if (!line.score().isEmpty()) {
            double scoreWidth = this.segmentsWidth(renderer, line.score());
            this.drawSegments(renderer, line.score(), this.x + layout.width() - 5.0 - scoreWidth, textY);
         }

         rowY += layout.lineHeight();
      }
   }

   public static boolean shouldHideVanilla() {
      Hud hud = Hud.get();
      if (hud != null && hud.active) {
         for (HudElement element : hud) {
            if (element instanceof ScoreboardHud && element.isActive()) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private ScoreboardHud.Layout calculateLayout(HudRenderer renderer, ScoreboardHud.ScoreData data) {
      double titleHeight = renderer.textHeight(false);
      double textHeight = renderer.textHeight(false);
      double lineHeight = textHeight + 2.0;
      double contentWidth = this.segmentsWidth(renderer, data.title());

      for (ScoreboardHud.ScoreLine line : data.lines()) {
         double lineWidth = this.segmentsWidth(renderer, line.name());
         if (!line.score().isEmpty()) {
            lineWidth += 7.0 + this.segmentsWidth(renderer, line.score());
         }

         contentWidth = Math.max(contentWidth, lineWidth);
      }

      double width = Math.max(86.0, contentWidth + 10.0);
      double height = 8.0 + titleHeight + (data.lines().isEmpty() ? 0.0 : 4.0 + data.lines().size() * lineHeight);
      return new ScoreboardHud.Layout(width, height, titleHeight, textHeight, lineHeight);
   }

   private ScoreboardHud.ScoreData collectScoreData() {
      if (MeteorClient.mc.level == null) {
         return null;
      }

      Scoreboard scoreboard = MeteorClient.mc.level.getScoreboard();
      Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (objective == null) {
         return null;
      }

      NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
      List<ScoreboardHud.ScoreLine> lines = scoreboard.listPlayerScores(objective)
         .stream()
         .filter(entry -> !entry.isHidden())
         .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed().thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
         .map(entry -> {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            return new ScoreboardHud.ScoreLine(this.toSegments(name), this.toSegments(entry.formatValue(numberFormat)));
         })
         .toList();
      return new ScoreboardHud.ScoreData(this.toSegments(objective.getDisplayName()), lines);
   }

   private void drawBackground(HudRenderer renderer, double width, double height) {
      double radius = this.backgroundRadius.get();
      Color color = this.backgroundColor.get();
      if (this.backgroundMode.get() == ScoreboardHud.BackgroundMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
         LiquidGlassUtil.render(renderer, this.x, this.y, width, height, radius, color);
      } else {
         BlurUtil.render(Math.round(this.x), Math.round(this.y), (int)Math.ceil(width), (int)Math.ceil(height), (float)radius);
         Render2DUtil.drawRoundedRect(renderer, this.x, this.y, width, height, radius, color);
      }

      if (this.enableGlow.get()) {
         Render2DUtil.drawRoundedGlow(this.x, this.y, width, height, radius);
      }
   }

   private List<ScoreboardHud.Segment> toSegments(Component component) {
      List<ScoreboardHud.Segment> segments = new ArrayList<>();
      component.visit((style, part) -> {
         this.appendLegacyAware(segments, part, this.colorFromStyle(style));
         return Optional.empty();
      }, Style.EMPTY);
      return segments;
   }

   private void appendLegacyAware(List<ScoreboardHud.Segment> segments, String text, Color baseColor) {
      if (text != null && !text.isEmpty()) {
         Color current = baseColor == null ? TEXT : baseColor;
         StringBuilder part = new StringBuilder();

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 167 && i + 1 < text.length()) {
               if (!part.isEmpty()) {
                  segments.add(new ScoreboardHud.Segment(part.toString(), current));
                  part.setLength(0);
               }

               Color mapped = this.colorFromLegacy(text.charAt(++i));
               if (mapped != null) {
                  current = mapped;
               }
            } else {
               part.append(c);
            }
         }

         if (!part.isEmpty()) {
            segments.add(new ScoreboardHud.Segment(part.toString(), current));
         }
      }
   }

   private Color colorFromStyle(Style style) {
      TextColor color = style == null ? null : style.getColor();
      if (color == null) {
         return TEXT;
      }

      int value = color.getValue();
      return new Color(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF);
   }

   private Color colorFromLegacy(char code) {
      return switch (Character.toLowerCase(code)) {
         case '0' -> new Color(0, 0, 0);
         case '1' -> new Color(0, 0, 170);
         case '2' -> new Color(0, 170, 0);
         case '3' -> new Color(0, 170, 170);
         case '4' -> new Color(170, 0, 0);
         case '5' -> new Color(170, 0, 170);
         case '6' -> new Color(255, 170, 0);
         case '7' -> new Color(170, 170, 170);
         case '8' -> new Color(85, 85, 85);
         case '9' -> new Color(85, 85, 255);
         default -> null;
         case 'a' -> new Color(85, 255, 85);
         case 'b' -> new Color(85, 255, 255);
         case 'c' -> new Color(255, 85, 85);
         case 'd' -> new Color(255, 85, 255);
         case 'e' -> new Color(255, 255, 85);
         case 'f', 'r' -> Color.WHITE;
      };
   }

   private double segmentsWidth(HudRenderer renderer, List<ScoreboardHud.Segment> segments) {
      double width = 0.0;

      for (ScoreboardHud.Segment segment : segments) {
         width += renderer.textWidth(segment.text(), false);
      }

      return width;
   }

   private void drawSegmentsCentered(HudRenderer renderer, List<ScoreboardHud.Segment> segments, double x, double y, double width) {
      double textWidth = this.segmentsWidth(renderer, segments);
      this.drawSegments(renderer, segments, x + Math.max(0.0, (width - textWidth) * 0.5), y);
   }

   private void drawSegments(HudRenderer renderer, List<ScoreboardHud.Segment> segments, double x, double y) {
      double cursor = x;

      for (ScoreboardHud.Segment segment : segments) {
         if (!segment.text().isEmpty()) {
            renderer.text(segment.text(), cursor, y, segment.color(), false);
            cursor += renderer.textWidth(segment.text(), false);
         }
      }
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }

   private record Layout(double width, double height, double titleHeight, double textHeight, double lineHeight) {
   }

   private record ScoreData(List<ScoreboardHud.Segment> title, List<ScoreboardHud.ScoreLine> lines) {
   }

   private record ScoreLine(List<ScoreboardHud.Segment> name, List<ScoreboardHud.Segment> score) {
   }

   private record Segment(String text, Color color) {
   }
}
