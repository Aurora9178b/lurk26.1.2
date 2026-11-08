package meteordevelopment.meteorclient.gui.themes.meteor.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.systems.modules.client.ClickGUI;

public class WMeteorDropdown<T> extends WDropdown<T> implements MeteorWidget {
   public WMeteorDropdown(T[] values, T value) {
      super(values, value);
   }

   @Override
   protected WDropdown.WDropdownRoot createRootWidget() {
      return new WMeteorDropdown.WRoot();
   }

   @Override
   protected WDropdown<T>.WDropdownValue createValueWidget() {
      return new WMeteorDropdown.WValue();
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      MeteorGuiTheme theme = this.theme();
      double pad = this.pad();
      double s = theme.textHeight();
      this.renderBackground(renderer, this, this.pressed, this.mouseOver);
      String text = this.get().toString();
      double w = theme.textWidth(text);
      renderer.text(text, this.x + pad + this.maxValueWidth / 2.0 - w / 2.0, this.y + pad, theme.textColor.get(), false);
      renderer.rotatedQuad(this.x + pad + this.maxValueWidth + pad, this.y + pad, s, s, 0.0, GuiRenderer.TRIANGLE, theme.textColor.get());
   }

   private static class WRoot extends WDropdown.WDropdownRoot implements MeteorWidget {
      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double radius = 9.0;
         ClickGUI.renderBackground(renderer, this.x, this.y, this.width, this.height, radius, theme.backgroundColor2.get());
      }
   }

   private class WValue extends WDropdown<T>.WDropdownValue implements MeteorWidget {
      @Override
      protected void onCalculateSize() {
         double pad = this.pad();
         this.width = pad + this.theme.textWidth(this.value.toString()) + pad;
         this.height = pad + this.theme.textHeight() + pad;
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         MeteorGuiTheme theme = this.theme();
         double radius = 9.0;
         ClickGUI.renderBackground(renderer, this.x, this.y, this.width, this.height, radius, theme.backgroundColor2.get());
         String text = this.value.toString();
         renderer.text(text, this.x + this.width / 2.0 - theme.textWidth(text) / 2.0, this.y + this.pad(), theme.textColor.get(), false);
      }
   }
}
