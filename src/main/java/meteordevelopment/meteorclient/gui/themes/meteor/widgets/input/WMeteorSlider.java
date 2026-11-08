package meteordevelopment.meteorclient.gui.themes.meteor.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;

public class WMeteorSlider extends WSlider implements MeteorWidget {
   public WMeteorSlider(double value, double min, double max) {
      super(value, min, max);
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      double valueWidth = this.valueWidth();
      this.renderBar(renderer, valueWidth);
      this.renderHandle(renderer, valueWidth);
   }

   private void renderBar(GuiRenderer renderer, double valueWidth) {
      MeteorGuiTheme theme = this.theme();
      double s = theme.scale(3.0);
      double handleSize = this.handleSize();
      double x = this.x + handleSize / 2.0;
      double y = this.y + this.height / 2.0 - s / 2.0;
      renderer.quad(x, y, valueWidth, s, theme.sliderLeft.get());
      renderer.quad(x + valueWidth, y, this.width - valueWidth - handleSize, s, theme.sliderRight.get());
   }

   private void renderHandle(GuiRenderer renderer, double valueWidth) {
      MeteorGuiTheme theme = this.theme();
      double s = this.handleSize();
      renderer.quad(this.x + valueWidth, this.y, s, s, GuiRenderer.CIRCLE, theme.sliderHandle.get(this.dragging, this.handleMouseOver));
   }
}
