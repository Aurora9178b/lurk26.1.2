package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;

public abstract class WTooltip extends WContainer implements WRoot {
   private boolean valid;
   protected String text;

   public WTooltip(String text) {
      this.text = text;
   }

   @Override
   public void init() {
      this.add(this.theme.label(this.text)).pad(4.0);
   }

   @Override
   public void invalidate() {
      this.valid = false;
   }

   @Override
   public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (!this.valid) {
         this.calculateSize();
         this.calculateWidgetPositions();
         this.valid = true;
      }

      return super.render(renderer, mouseX, mouseY, delta);
   }
}
