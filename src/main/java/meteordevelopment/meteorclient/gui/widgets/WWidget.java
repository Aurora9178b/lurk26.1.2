package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WWidget implements BaseWidget {
   public boolean visible = true;
   public GuiTheme theme;
   public double x;
   public double y;
   public double width;
   public double height;
   public double minWidth;
   public WWidget parent;
   public String tooltip;
   public boolean mouseOver;
   public boolean focused;
   protected boolean instantTooltips;
   protected double mouseOverTimer;

   public void init() {
   }

   public void move(double deltaX, double deltaY) {
      this.x = Math.round(this.x + deltaX);
      this.y = Math.round(this.y + deltaY);
   }

   @Override
   public GuiTheme getTheme() {
      return this.theme;
   }

   public double pad() {
      return this.theme.pad();
   }

   public void calculateSize() {
      this.onCalculateSize();
      double minWidth = this.theme.scale(this.minWidth);
      if (this.width < minWidth) {
         this.width = minWidth;
      }

      this.width = Math.round(this.width);
      this.height = Math.round(this.height);
   }

   protected void onCalculateSize() {
   }

   public void calculateWidgetPositions() {
      this.x = Math.round(this.x);
      this.y = Math.round(this.y);
      this.onCalculateWidgetPositions();
   }

   protected void onCalculateWidgetPositions() {
   }

   public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (!this.visible) {
         return true;
      }

      if (this.isOver(mouseX, mouseY)) {
         this.mouseOverTimer += delta;
         if ((this.instantTooltips || this.mouseOverTimer >= 1.0) && this.tooltip != null) {
            WView view = this.getView();
            if (view == null || view.mouseOver) {
               renderer.tooltip(this.tooltip);
            }
         }
      } else {
         this.mouseOverTimer = 0.0;
      }

      this.onRender(renderer, mouseX, mouseY, delta);
      return false;
   }

   protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      return this.onMouseClicked(click, doubled);
   }

   public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
      return false;
   }

   public boolean mouseReleased(MouseButtonEvent click) {
      return this.onMouseReleased(click);
   }

   public boolean onMouseReleased(MouseButtonEvent click) {
      return false;
   }

   public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
      this.mouseOver = this.isOver(mouseX, mouseY);
      this.onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
   }

   public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
   }

   public boolean mouseScrolled(double amount) {
      return this.onMouseScrolled(amount);
   }

   public boolean onMouseScrolled(double amount) {
      return false;
   }

   public boolean keyPressed(KeyEvent input) {
      return this.onKeyPressed(input);
   }

   public boolean onKeyPressed(KeyEvent input) {
      return false;
   }

   public boolean keyRepeated(KeyEvent input) {
      return this.onKeyRepeated(input);
   }

   public boolean onKeyRepeated(KeyEvent input) {
      return false;
   }

   public boolean charTyped(CharacterEvent input) {
      return this.onCharTyped(input);
   }

   public boolean onCharTyped(CharacterEvent input) {
      return false;
   }

   public void invalidate() {
      WWidget root = this.getRoot();
      if (root != null) {
         root.invalidate();
      }
   }

   protected WWidget getRoot() {
      return this.parent != null ? this.parent.getRoot() : (this instanceof WRoot ? this : null);
   }

   public WView getView() {
      if (this instanceof WView view) {
         return view;
      } else {
         return this.parent != null ? this.parent.getView() : null;
      }
   }

   public boolean isOver(double x, double y) {
      return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void setFocused(boolean focused) {
      if (this.focused != focused) {
         this.focused = focused;
      }
   }
}
