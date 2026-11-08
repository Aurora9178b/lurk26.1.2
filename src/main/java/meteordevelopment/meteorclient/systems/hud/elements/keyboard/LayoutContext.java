package meteordevelopment.meteorclient.systems.hud.elements.keyboard;

import meteordevelopment.meteorclient.utils.misc.Keybind;
import net.minecraft.client.KeyMapping;

final class LayoutContext {
   final double keyUnit;
   final double keyGap;
   final double step;
   final double functionRowGap;

   LayoutContext(double keyUnit, double keyGap, double functionRowGap) {
      this.keyUnit = keyUnit;
      this.keyGap = keyGap;
      this.step = keyUnit + keyGap;
      this.functionRowGap = functionRowGap;
   }

   double ux(double units) {
      return units * this.step;
   }

   double y(double rows) {
      return rows * this.step;
   }

   double uy(double rows) {
      return rows * this.step + (rows > 0.0 ? this.functionRowGap : 0.0);
   }

   double px(KeyDimensions d) {
      return d.toPixels(this.keyUnit, this.keyGap);
   }

   KeyboardHud.Key key(Keybind kb, double x, double y) {
      return new KeyboardHud.Key(kb, null, x, y, this.px(KeyDimensions.STANDARD), this.px(KeyDimensions.STANDARD));
   }

   KeyboardHud.Key key(Keybind kb, double x, double y, KeyDimensions w) {
      return new KeyboardHud.Key(kb, null, x, y, this.px(w), this.px(KeyDimensions.STANDARD));
   }

   KeyboardHud.Key key(Keybind kb, double x, double y, KeyDimensions w, KeyDimensions h) {
      return new KeyboardHud.Key(kb, null, x, y, this.px(w), this.px(h));
   }

   KeyboardHud.Key keyNamed(Keybind kb, String name, double x, double y, KeyDimensions w) {
      return new KeyboardHud.Key(kb, name, x, y, this.px(w), this.px(KeyDimensions.STANDARD));
   }

   KeyboardHud.Key key(KeyMapping kb, double x, double y) {
      return new KeyboardHud.Key(kb, null, x, y, this.px(KeyDimensions.STANDARD), this.px(KeyDimensions.STANDARD));
   }

   KeyboardHud.Key key(KeyMapping kb, double x, double y, KeyDimensions w) {
      return new KeyboardHud.Key(kb, null, x, y, this.px(w), this.px(KeyDimensions.STANDARD));
   }

   KeyboardHud.Key key(KeyMapping kb, String name, double x, double y) {
      return new KeyboardHud.Key(kb, name, x, y, this.px(KeyDimensions.STANDARD), this.px(KeyDimensions.STANDARD));
   }
}
