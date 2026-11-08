package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.client.ClickGUI;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface MeteorWidget extends BaseWidget {
   default MeteorGuiTheme theme() {
      return (MeteorGuiTheme)this.getTheme();
   }

   default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
      MeteorGuiTheme theme = this.theme();
      double radius = 9.0;
      ClickGUI.renderBackground(renderer, widget.x, widget.y, widget.width, widget.height, radius, theme.backgroundColor2.get());
   }

   default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
      MeteorGuiTheme theme = this.theme();
      this.renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
   }
}
