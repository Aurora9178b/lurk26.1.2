package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;

public class WMeteorHorizontalSeparator extends WHorizontalSeparator implements MeteorWidget {
   public WMeteorHorizontalSeparator(String text) {
      super(text);
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (this.text == null) {
         this.renderWithoutText(renderer);
      } else {
         this.renderWithText(renderer);
      }
   }

   private void renderWithoutText(GuiRenderer renderer) {
      MeteorGuiTheme theme = this.theme();
      double s = theme.scale(1.0);
      double w = this.width / 2.0;
      renderer.quad(this.x, this.y + s, w, s, theme.separatorEdges.get(), theme.separatorCenter.get());
      renderer.quad(this.x + w, this.y + s, w, s, theme.separatorCenter.get(), theme.separatorEdges.get());
   }

   private void renderWithText(GuiRenderer renderer) {
      MeteorGuiTheme theme = this.theme();
      double s = theme.scale(2.0);
      double h = theme.scale(1.0);
      double textStart = Math.round(this.width / 2.0 - this.textWidth / 2.0 - s);
      double textEnd = s + textStart + this.textWidth + s;
      double offsetY = Math.round(this.height / 2.0);
      renderer.quad(this.x, this.y + offsetY, textStart, h, theme.separatorEdges.get(), theme.separatorCenter.get());
      renderer.text(this.text, this.x + textStart + s, this.y, theme.separatorText.get(), false);
      renderer.quad(this.x + textEnd, this.y + offsetY, this.width - textEnd, h, theme.separatorCenter.get(), theme.separatorEdges.get());
   }
}
