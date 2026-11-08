package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WVerticalSeparator;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorVerticalSeparator extends WVerticalSeparator implements MeteorWidget {
   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      MeteorGuiTheme theme = this.theme();
      Color colorEdges = theme.separatorEdges.get();
      Color colorCenter = theme.separatorCenter.get();
      double s = theme.scale(1.0);
      double offsetX = Math.round(this.width / 2.0);
      renderer.quad(this.x + offsetX, this.y, s, this.height / 2.0, colorEdges, colorEdges, colorCenter, colorCenter);
      renderer.quad(this.x + offsetX, this.y + this.height / 2.0, s, this.height / 2.0, colorCenter, colorCenter, colorEdges, colorEdges);
   }
}
