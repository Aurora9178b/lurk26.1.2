package meteordevelopment.meteorclient.gui.widgets.pressable;

import net.minecraft.client.input.MouseButtonEvent;

public class WConfirmedMinus extends WMinus {
   protected boolean pressedOnce = false;

   @Override
   public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
      boolean pressed = super.onMouseClicked(click, doubled);
      if (!pressed) {
         this.pressedOnce = false;
      }

      return pressed;
   }

   @Override
   public boolean onMouseReleased(MouseButtonEvent click) {
      if (this.pressed && this.pressedOnce) {
         super.onMouseReleased(click);
      }

      this.pressedOnce = this.pressed;
      return this.pressed = false;
   }
}
