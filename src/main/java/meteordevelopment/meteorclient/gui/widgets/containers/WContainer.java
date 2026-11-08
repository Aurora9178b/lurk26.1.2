package meteordevelopment.meteorclient.gui.widgets.containers;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WContainer extends WWidget {
   public final List<Cell<?>> cells = new ArrayList<>();

   public <T extends WWidget> Cell<T> add(T widget) {
      widget.parent = this;
      widget.theme = this.theme;
      Cell<T> cell = new Cell<T>(widget).centerY();
      this.cells.add(cell);
      widget.init();
      this.invalidate();
      return cell;
   }

   public void clear() {
      if (!this.cells.isEmpty()) {
         this.cells.clear();
         this.invalidate();
      }
   }

   public void remove(Cell<?> cell) {
      if (this.cells.remove(cell)) {
         this.invalidate();
      }
   }

   @Override
   public void move(double deltaX, double deltaY) {
      super.move(deltaX, deltaY);

      for (Cell<?> cell : this.cells) {
         cell.move(deltaX, deltaY);
      }
   }

   public void moveCells(double deltaX, double deltaY) {
      for (Cell<?> cell : this.cells) {
         cell.move(deltaX, deltaY);
         MouseHandler mouse = MeteorClient.mc.mouseHandler;
         cell.widget().mouseMoved(mouse.xpos(), mouse.ypos(), mouse.xpos(), mouse.ypos());
      }
   }

   @Override
   public boolean isFocused() {
      if (this.focused) {
         return true;
      }

      for (Cell<?> cell : this.cells) {
         if (cell.widget().isFocused()) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void calculateSize() {
      for (Cell<?> cell : this.cells) {
         cell.widget().calculateSize();
      }

      super.calculateSize();
   }

   @Override
   protected void onCalculateSize() {
      this.width = 0.0;
      this.height = 0.0;

      for (Cell<?> cell : this.cells) {
         this.width = Math.max(this.width, cell.padLeft() + cell.widget().width + cell.padRight());
         this.height = Math.max(this.height, cell.padTop() + cell.widget().height + cell.padBottom());
      }
   }

   @Override
   public void calculateWidgetPositions() {
      super.calculateWidgetPositions();

      for (Cell<?> cell : this.cells) {
         cell.widget().calculateWidgetPositions();
      }
   }

   @Override
   protected void onCalculateWidgetPositions() {
      for (Cell<?> cell : this.cells) {
         cell.x = this.x + cell.padLeft();
         cell.y = this.y + cell.padTop();
         cell.width = this.width - cell.padLeft() - cell.padRight();
         cell.height = this.height - cell.padTop() - cell.padBottom();
         cell.alignWidget();
      }
   }

   @Override
   public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (super.render(renderer, mouseX, mouseY, delta)) {
         return true;
      }

      WView view = this.getView();
      double windowHeight = Utils.getWindowHeight();

      for (Cell<?> cell : this.cells) {
         WWidget widget = cell.widget();
         if (widget.y > windowHeight) {
            break;
         }

         if (!(widget.y + widget.height <= 0.0) && this.shouldRenderWidget(widget, view)) {
            this.renderWidget(widget, renderer, mouseX, mouseY, delta);
         }
      }

      return false;
   }

   protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      widget.render(renderer, mouseX, mouseY, delta);
   }

   private boolean shouldRenderWidget(WWidget widget, WView view) {
      if (view == null) {
         return true;
      }

      if (!view.isWidgetInView(widget)) {
         return false;
      }

      if (widget.mouseOver && !view.mouseOver) {
         widget.mouseOver = false;
      }

      return true;
   }

   protected boolean propagateEvents(WWidget widget) {
      return true;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget()) && cell.widget().mouseClicked(click, doubled)) {
               return true;
            }
         }
      } catch (ConcurrentModificationException var5) {
      }

      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent click) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget()) && cell.widget().mouseReleased(click)) {
               return true;
            }
         }
      } catch (ConcurrentModificationException var4) {
      }

      return super.mouseReleased(click);
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget())) {
               cell.widget().mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
            }
         }
      } catch (ConcurrentModificationException var11) {
      }

      super.mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
   }

   @Override
   public boolean mouseScrolled(double amount) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget()) && cell.widget().mouseScrolled(amount)) {
               return true;
            }
         }
      } catch (ConcurrentModificationException var5) {
      }

      return super.mouseScrolled(amount);
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget()) && cell.widget().keyPressed(input)) {
               return true;
            }
         }
      } catch (ConcurrentModificationException var4) {
      }

      return this.onKeyPressed(input);
   }

   @Override
   public boolean keyRepeated(KeyEvent input) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget()) && cell.widget().keyRepeated(input)) {
               return true;
            }
         }
      } catch (ConcurrentModificationException var4) {
      }

      return this.onKeyRepeated(input);
   }

   @Override
   public boolean charTyped(CharacterEvent input) {
      try {
         for (Cell<?> cell : this.cells) {
            if (this.propagateEvents(cell.widget()) && cell.widget().charTyped(input)) {
               return true;
            }
         }
      } catch (ConcurrentModificationException var4) {
      }

      return super.charTyped(input);
   }
}
