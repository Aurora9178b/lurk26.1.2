package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WLabel;

public class WMeteorLabel extends WLabel implements MeteorWidget {
   public WMeteorLabel(String text, boolean title) {
      super(text, title);
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (!this.text.isEmpty()) {
         renderer.text(
            this.text,
            this.x,
            this.y,
            this.color != null ? this.color : (this.title ? this.theme().titleTextColor.get() : this.theme().textColor.get()),
            this.title
         );
      }
   }
}
