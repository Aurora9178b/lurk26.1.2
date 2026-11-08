package meteordevelopment.meteorclient.gui.themes.meteor.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WTriangle;

public class WMeteorSection extends WSection {
   public WMeteorSection(String title, boolean expanded, WWidget headerWidget) {
      super(title, expanded, headerWidget);
   }

   @Override
   protected WSection.WHeader createHeader() {
      return new WMeteorSection.WMeteorHeader(this.title);
   }

   protected static class WHeaderTriangle extends WTriangle implements MeteorWidget {
      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         renderer.rotatedQuad(this.x, this.y, this.width, this.height, this.rotation, GuiRenderer.TRIANGLE, this.theme().textColor.get());
      }
   }

   protected class WMeteorHeader extends WSection.WHeader {
      private WTriangle triangle;

      public WMeteorHeader(String title) {
         super(title);
      }

      @Override
      public void init() {
         this.add(this.theme.horizontalSeparator(this.title)).expandX();
         if (WMeteorSection.this.headerWidget != null) {
            this.add(WMeteorSection.this.headerWidget);
         }

         this.triangle = new WMeteorSection.WHeaderTriangle();
         this.triangle.theme = this.theme;
         this.triangle.action = () -> this.onClick();
         this.add(this.triangle);
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         this.triangle.rotation = (1.0 - WMeteorSection.this.animProgress) * -90.0;
      }
   }
}
