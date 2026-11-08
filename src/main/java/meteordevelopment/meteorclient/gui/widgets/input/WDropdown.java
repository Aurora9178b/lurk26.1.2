package meteordevelopment.meteorclient.gui.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WRoot;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

public abstract class WDropdown<T> extends WPressable {
   public Runnable action;
   protected T[] values;
   protected T value;
   protected double maxValueWidth;
   protected WDropdown.WDropdownRoot root;
   protected boolean expanded;
   protected double animProgress;

   public WDropdown(T[] values, T value) {
      this.values = values;
      this.set(value);
   }

   @Override
   public void init() {
      this.root = this.createRootWidget();
      this.root.theme = this.theme;
      this.root.spacing = 0.0;

      for (int i = 0; i < this.values.length; i++) {
         WDropdown<T>.WDropdownValue widget = this.createValueWidget();
         widget.theme = this.theme;
         widget.value = this.values[i];
         Cell<?> cell = this.root.add(widget).padHorizontal(2.0).expandWidgetX();
         if (i >= this.values.length - 1) {
            cell.padBottom(2.0);
         }
      }
   }

   protected abstract WDropdown.WDropdownRoot createRootWidget();

   protected abstract WDropdown<T>.WDropdownValue createValueWidget();

   @Override
   protected void onCalculateSize() {
      double pad = this.pad();
      this.maxValueWidth = 0.0;

      for (T value : this.values) {
         double valueWidth = this.theme.textWidth(value.toString());
         this.maxValueWidth = Math.max(this.maxValueWidth, valueWidth);
      }

      this.root.calculateSize();
      this.width = pad + this.maxValueWidth + pad + this.theme.textHeight() + pad;
      this.height = pad + this.theme.textHeight() + pad;
      this.root.width = this.width;
   }

   @Override
   protected void onCalculateWidgetPositions() {
      super.onCalculateWidgetPositions();
      this.root.x = this.x;
      this.root.y = this.y + this.height;
      this.root.calculateWidgetPositions();
   }

   @Override
   protected void onPressed(int button) {
      this.expanded = !this.expanded;
      this.root.setFocused(this.expanded);
      this.setFocused(this.expanded);
   }

   public T get() {
      return this.value;
   }

   public void set(T value) {
      this.value = value;
   }

   @Override
   public void move(double deltaX, double deltaY) {
      super.move(deltaX, deltaY);
      this.root.move(deltaX, deltaY);
   }

   @Override
   public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      boolean render = super.render(renderer, mouseX, mouseY, delta);
      this.animProgress = this.animProgress + (this.expanded ? 1 : -1) * delta * 14.0;
      this.animProgress = Mth.clamp(this.animProgress, 0.0, 1.0);
      WView view = this.getView();
      boolean rootInView = view == null || view.isWidgetInView(this);
      if (!render && this.animProgress > 0.0 && rootInView) {
         double dropdownY = this.y + this.height;
         double scissorHeight = Math.min(this.root.height * this.animProgress, Utils.getWindowHeight() - dropdownY);
         renderer.absolutePost(() -> {
            renderer.scissorStart(this.x, dropdownY, this.width, scissorHeight);
            this.root.render(renderer, mouseX, mouseY, delta);
            renderer.scissorEnd();
         });
      }

      if (this.expanded && this.root.mouseOver) {
         this.theme.disableHoverColor = true;
      }

      return render;
   }

   @Override
   public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
      boolean used = false;
      if (!this.mouseOver && !this.root.mouseOver) {
         this.expanded = false;
      }

      if (super.onMouseClicked(click, doubled)) {
         used = true;
      }

      if (this.expanded && this.root.mouseClicked(click, doubled)) {
         used = true;
      }

      return used;
   }

   @Override
   public boolean onMouseReleased(MouseButtonEvent click) {
      return super.onMouseReleased(click) ? true : this.expanded && this.root.mouseReleased(click);
   }

   @Override
   public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
      super.onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
      if (this.expanded) {
         this.root.mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
      }
   }

   @Override
   public boolean onMouseScrolled(double amount) {
      if (super.onMouseScrolled(amount)) {
         return true;
      } else {
         return this.expanded ? this.root.mouseScrolled(amount) : false;
      }
   }

   @Override
   public boolean onKeyPressed(KeyEvent input) {
      return super.onKeyPressed(input) ? true : this.expanded && this.root.keyPressed(input);
   }

   @Override
   public boolean onKeyRepeated(KeyEvent input) {
      return super.onKeyRepeated(input) ? true : this.expanded && this.root.keyRepeated(input);
   }

   @Override
   public boolean onCharTyped(CharacterEvent input) {
      return super.onCharTyped(input) ? true : this.expanded && this.root.charTyped(input);
   }

   protected abstract static class WDropdownRoot extends WVerticalList implements WRoot {
      @Override
      public void invalidate() {
      }
   }

   protected abstract class WDropdownValue extends WPressable {
      protected T value;

      @Override
      protected void onPressed(int button) {
         boolean isNew = !WDropdown.this.value.equals(this.value);
         WDropdown.this.value = this.value;
         WDropdown.this.expanded = false;
         if (isNew && WDropdown.this.action != null) {
            WDropdown.this.action.run();
         }
      }
   }
}
