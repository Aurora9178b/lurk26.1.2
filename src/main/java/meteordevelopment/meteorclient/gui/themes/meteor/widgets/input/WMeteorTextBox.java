package meteordevelopment.meteorclient.gui.themes.meteor.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorLabel;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

public class WMeteorTextBox extends WTextBox implements MeteorWidget {
   private boolean cursorVisible;
   private double cursorTimer;
   private double animProgress;

   public WMeteorTextBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
      super(text, placeholder, filter, renderer);
   }

   @Override
   protected WContainer createCompletionsRootWidget() {
      return new WVerticalList() {
         @Override
         protected void onRender(GuiRenderer renderer1, double mouseX, double mouseY, double delta) {
            MeteorGuiTheme theme1 = WMeteorTextBox.this.theme();
            double s = theme1.scale(2.0);
            Color c = theme1.outlineColor.get();
            Color col = theme1.backgroundColor.get();
            int preA = col.a;
            col.a = col.a + col.a / 2;
            col.validate();
            renderer1.quad(this, col);
            col.a = preA;
            renderer1.quad(this.x, this.y + this.height - s, this.width, s, c);
            renderer1.quad(this.x, this.y, s, this.height - s, c);
            renderer1.quad(this.x + this.width - s, this.y, s, this.height - s, c);
         }
      };
   }

   @Override
   protected <T extends WWidget & WTextBox.ICompletionItem> T createCompletionsValueWidth(String completion, boolean selected) {
      return (T)(new WMeteorTextBox.CompletionItem(completion, false, selected));
   }

   @Override
   protected void onCursorChanged() {
      this.cursorVisible = true;
      this.cursorTimer = 0.0;
   }

   @Override
   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (this.cursorTimer >= 1.0) {
         this.cursorVisible = !this.cursorVisible;
         this.cursorTimer = 0.0;
      } else {
         this.cursorTimer += delta * 1.75;
      }

      this.renderBackground(renderer, this, false, false);
      MeteorGuiTheme theme = this.theme();
      double pad = this.pad();
      double overflowWidth = this.getOverflowWidthForRender();
      renderer.scissorStart(this.x + pad, this.y + pad, this.width - pad * 2.0, this.height - pad * 2.0);
      if (!this.text.isEmpty()) {
         this.renderer.render(renderer, this.x + pad - overflowWidth, this.y + pad, this.text, theme.textColor.get());
      } else if (this.placeholder != null) {
         this.renderer.render(renderer, this.x + pad - overflowWidth, this.y + pad, this.placeholder, theme.placeholderColor.get());
      }

      if (this.focused && (this.cursor != this.selectionStart || this.cursor != this.selectionEnd)) {
         double selStart = this.x + pad + this.getTextWidth(this.selectionStart) - overflowWidth;
         double selEnd = this.x + pad + this.getTextWidth(this.selectionEnd) - overflowWidth;
         renderer.quad(selStart, this.y + pad, selEnd - selStart, theme.textHeight(), theme.textHighlightColor.get());
      }

      this.animProgress = this.animProgress + delta * 10.0 * (this.focused && this.cursorVisible ? 1 : -1);
      this.animProgress = Mth.clamp(this.animProgress, 0.0, 1.0);
      if (this.focused && this.cursorVisible || this.animProgress > 0.0) {
         renderer.setAlpha(this.animProgress);
         renderer.quad(this.x + pad + this.getTextWidth(this.cursor) - overflowWidth, this.y + pad, theme.scale(1.0), theme.textHeight(), theme.textColor.get());
         renderer.setAlpha(1.0);
      }

      renderer.scissorEnd();
   }

   private static class CompletionItem extends WMeteorLabel implements WTextBox.ICompletionItem {
      private static final Color SELECTED_COLOR = new Color(255, 255, 255, 15);
      private boolean selected;

      public CompletionItem(String text, boolean title, boolean selected) {
         super(text, title);
         this.selected = selected;
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         super.onRender(renderer, mouseX, mouseY, delta);
         if (this.selected) {
            renderer.quad(this, SELECTED_COLOR);
         }
      }

      @Override
      public boolean isSelected() {
         return this.selected;
      }

      @Override
      public void setSelected(boolean selected) {
         this.selected = selected;
      }

      @Override
      public String getCompletion() {
         return this.text;
      }
   }
}
