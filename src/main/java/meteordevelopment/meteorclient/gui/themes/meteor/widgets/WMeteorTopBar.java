package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class WMeteorTopBar extends WTopBar implements MeteorWidget {
   @Override
   protected Color getButtonColor(boolean pressed, boolean hovered) {
      return this.theme().backgroundColor.get(pressed, hovered);
   }

   @Override
   protected Color getNameColor() {
      return this.theme().textColor.get();
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      super.onRender(renderer, mouseX, mouseY, delta);
   }
}
