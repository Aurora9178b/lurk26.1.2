package meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

public class WMeteorCheckbox extends WCheckbox implements MeteorWidget {
   private static final Color TOGGLE_OFF = new Color(70, 70, 74, 132);
   private static final Color OUTLINE_OFF = new Color(120, 120, 126, 155);
   private static final Color TOGGLE_ON = new Color(138, 255, 160, 235);
   private static final Color OUTLINE_ON = new Color(138, 255, 160, 255);
   private static final Color KNOB = new Color(255, 255, 255, 245);
   private double animProgress;

   public WMeteorCheckbox(boolean checked) {
      super(checked);
      this.animProgress = checked ? 1.0 : 0.0;
   }

   @Override
   protected void onCalculateSize() {
      double s = this.theme.textHeight();
      this.width = s * 2.35;
      this.height = s * 1.35;
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      MeteorGuiTheme theme = this.theme();
      this.animProgress = this.animProgress + (this.checked ? 1 : -1) * delta * 10.0;
      this.animProgress = Mth.clamp(this.animProgress, 0.0, 1.0);
      double toggleW = this.width;
      double toggleH = this.height;
      double outlineSize = theme.scale(1.2);
      double toggleX = this.x;
      double toggleY = this.y;
      double pad = theme.scale(1.5);
      Color toggleBg = this.lerp(TOGGLE_OFF, TOGGLE_ON, this.animProgress);
      Color outline = this.lerp(OUTLINE_OFF, OUTLINE_ON, this.animProgress);
      this.renderPill(renderer, toggleX - outlineSize, toggleY - outlineSize, toggleW + outlineSize * 2.0, toggleH + outlineSize * 2.0, outline);
      this.renderPill(renderer, toggleX, toggleY, toggleW, toggleH, toggleBg);
      double baseKnobSize = toggleH - pad * 2.0;
      double knobSize = baseKnobSize * (0.86 + 0.18 * this.animProgress);
      double knobMin = toggleX + pad;
      double knobMax = toggleX + toggleW - knobSize - pad;
      double knobX = knobMin + (knobMax - knobMin) * this.animProgress;
      double knobY = toggleY + (toggleH - knobSize) * 0.5;
      this.renderPill(renderer, knobX, knobY, knobSize, knobSize, KNOB);
   }

   private void renderPill(GuiRenderer renderer, double x, double y, double width, double height, Color color) {
      if (!(width <= 0.0) && !(height <= 0.0)) {
         if (width <= height) {
            renderer.quad(x, y, width, height, GuiRenderer.CIRCLE, color);
         } else {
            double radius = height * 0.5;
            renderer.quad(x, y, height, height, GuiRenderer.CIRCLE, color);
            renderer.quad(x + radius, y, width - height, height, color);
            renderer.quad(x + width - height, y, height, height, GuiRenderer.CIRCLE, color);
         }
      }
   }

   private Color lerp(Color from, Color to, double progress) {
      double t = Mth.clamp(progress, 0.0, 1.0);
      return new Color((int)Mth.lerp(t, from.r, to.r), (int)Mth.lerp(t, from.g, to.g), (int)Mth.lerp(t, from.b, to.b), (int)Mth.lerp(t, from.a, to.a));
   }
}
