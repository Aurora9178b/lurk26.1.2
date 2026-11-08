package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.systems.modules.client.ClickGUI;

public class WMeteorWindow extends WWindow implements MeteorWidget {
   private static final double RADIUS = 12.0;

   public WMeteorWindow(WWidget icon, String title) {
      super(icon, title);
   }

   @Override
   protected WWindow.WHeader header(WWidget icon) {
      return new WMeteorWindow.WMeteorHeader(icon);
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (this.expanded || this.animProgress > 0.0) {
         double bodyY = this.y + this.header.height;
         double bodyHeight = this.height - this.header.height;
         ClickGUI.renderBackground(renderer, this.x, bodyY, this.width, bodyHeight, 12.0, this.theme().backgroundColor.get());
      }
   }

   private class WMeteorHeader extends WWindow.WHeader {
      public WMeteorHeader(WWidget icon) {
         super(icon);
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         ClickGUI.renderBackground(renderer, this.x, this.y, this.width, this.height, 12.0, WMeteorWindow.this.theme().accentColor.get());
      }
   }
}
