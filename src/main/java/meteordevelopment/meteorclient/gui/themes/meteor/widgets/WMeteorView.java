package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;

public class WMeteorView extends WView implements MeteorWidget {
   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (this.canScroll && this.hasScrollBar) {
         renderer.quad(
            this.handleX(), this.handleY(), this.handleWidth(), this.handleHeight(), this.theme().scrollbarColor.get(this.focused, this.handleMouseOver)
         );
      }
   }
}
