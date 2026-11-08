package meteordevelopment.meteorclient.gui.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WConfirmedButton extends WButton {
   protected boolean pressedOnce = false;
   protected String confirmText;

   public WConfirmedButton(String text, String confirmText, GuiTexture texture) {
      super(text, texture);
      this.confirmText = confirmText;
   }

   @Override
   public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
      boolean pressed = super.onMouseClicked(click, doubled);
      if (!pressed) {
         this.pressedOnce = false;
         this.invalidate();
      }

      return pressed;
   }

   @Override
   public boolean onMouseReleased(MouseButtonEvent click) {
      if (this.pressed && this.pressedOnce) {
         super.onMouseReleased(click);
      }

      this.pressedOnce = this.pressed;
      this.invalidate();
      return this.pressed = false;
   }

   @Override
   public String getText() {
      return this.pressedOnce ? this.confirmText : this.text;
   }

   public void set(String text, String confirmText) {
      super.set(text);
      this.confirmText = confirmText;
   }
}
