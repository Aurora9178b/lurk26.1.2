package meteordevelopment.meteorclient.systems.hud.screens;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.other.Snapper;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

public class HudEditorScreen extends WidgetScreen implements Snapper.Container {
   private static final Color SPLIT_LINES_COLOR = new Color(255, 255, 255, 75);
   private static final Color INACTIVE_BG_COLOR = new Color(200, 25, 25, 50);
   private static final Color INACTIVE_OL_COLOR = new Color(200, 25, 25, 200);
   private static final Color HOVER_BG_COLOR = new Color(200, 200, 200, 50);
   private static final Color HOVER_OL_COLOR = new Color(200, 200, 200, 200);
   private static final Color SELECTION_BG_COLOR = new Color(225, 225, 225, 25);
   private static final Color SELECTION_OL_COLOR = new Color(225, 225, 225, 100);
   private final Hud hud;
   private final Snapper snapper;
   private Snapper.Element selectionSnapBox;
   private int lastMouseX;
   private int lastMouseY;
   private boolean pressed;
   private int clickX;
   private int clickY;
   private final List<HudElement> selection = new ArrayList<>();
   private boolean moved;
   private boolean dragging;
   private HudElement addedHoveredToSelectionWhenClickedElement;
   private double splitLinesAnimation;

   public HudEditorScreen(GuiTheme theme) {
      super(theme, "Hud Editor");
      this.hud = Hud.get();
      this.snapper = new Snapper(this);
   }

   @Override
   public void initWidgets() {
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      double s = MeteorClient.mc.getWindow().getGuiScale();
      double mouseX = click.x();
      double mouseY = click.y();
      mouseX *= s;
      mouseY *= s;
      if (click.button() == 0) {
         this.pressed = true;
         this.selectionSnapBox = null;
         HudElement hovered = this.getHovered((int)mouseX, (int)mouseY);
         this.dragging = hovered != null;
         if (this.dragging) {
            if (!this.selection.contains(hovered)) {
               this.selection.clear();
               this.selection.add(hovered);
               this.addedHoveredToSelectionWhenClickedElement = hovered;
            }
         } else {
            this.selection.clear();
         }

         this.clickX = (int)mouseX;
         this.clickY = (int)mouseY;
      }

      return false;
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      double s = MeteorClient.mc.getWindow().getGuiScale();
      mouseX *= s;
      mouseY *= s;
      if (this.dragging && !this.selection.isEmpty()) {
         if (this.selectionSnapBox == null) {
            this.selectionSnapBox = new HudEditorScreen.SelectionBox();
         }

         this.snapper.move(this.selectionSnapBox, (int)mouseX - this.lastMouseX, (int)mouseY - this.lastMouseY);
      }

      if (this.pressed) {
         this.moved = true;
      }

      this.lastMouseX = (int)mouseX;
      this.lastMouseY = (int)mouseY;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent click) {
      double s = MeteorClient.mc.getWindow().getGuiScale();
      double mouseX = click.x();
      double mouseY = click.y();
      mouseX *= s;
      mouseY *= s;
      if (click.button() == 0) {
         this.pressed = false;
      }

      if (this.addedHoveredToSelectionWhenClickedElement != null) {
         this.selection.remove(this.addedHoveredToSelectionWhenClickedElement);
         this.addedHoveredToSelectionWhenClickedElement = null;
      }

      if (this.moved) {
         if (click.button() == 0 && !this.dragging) {
            this.fillSelection((int)mouseX, (int)mouseY);
         }
      } else if (click.button() == 0) {
         HudElement hovered = this.getHovered((int)mouseX, (int)mouseY);
         if (hovered != null) {
            hovered.toggle();
         }
      } else if (click.button() == 1) {
         HudElement hovered = this.getHovered((int)mouseX, (int)mouseY);
         if (hovered != null) {
            MeteorClient.mc.setScreen(new HudElementScreen(this.theme, hovered));
         } else {
            MeteorClient.mc.setScreen(new AddHudElementScreen(this.theme, this.lastMouseX, this.lastMouseY));
         }
      }

      if (click.button() == 0) {
         this.snapper.unsnap();
         this.moved = this.dragging = false;
      }

      return false;
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      if (!this.pressed) {
         if (input.key() == 257 || input.key() == 335) {
            HudElement hovered = this.getHovered(this.lastMouseX, this.lastMouseY);
            if (hovered != null) {
               hovered.toggle();
            }
         } else if (input.key() == 261) {
            HudElement hovered = this.getHovered(this.lastMouseX, this.lastMouseY);
            if (hovered != null) {
               hovered.remove();
            } else {
               for (HudElement element : this.selection) {
                  element.remove();
               }

               this.selection.clear();
            }
         } else if (!this.selection.isEmpty()) {
            int pixels = !Input.isKeyPressed(341) && !Input.isKeyPressed(345) ? 1 : 10;
            int dx = 0;
            int dy = 0;
            switch (input.key()) {
               case 262:
                  dx = pixels;
                  break;
               case 263:
                  dx = -pixels;
                  break;
               case 264:
                  dy = pixels;
                  break;
               case 265:
                  dy = -pixels;
            }

            for (HudElement element : this.selection) {
               element.move(dx, dy);
            }
         }
      }

      return super.keyPressed(input);
   }

   private void fillSelection(int mouseX, int mouseY) {
      int x1 = Math.min(this.clickX, mouseX);
      int x2 = Math.max(this.clickX, mouseX);
      int y1 = Math.min(this.clickY, mouseY);
      int y2 = Math.max(this.clickY, mouseY);

      for (HudElement e : this.hud) {
         if (e.getX() <= x2 && e.getX2() >= x1 && e.getY() <= y2 && e.getY2() >= y1) {
            this.selection.add(e);
         }
      }
   }

   @Override
   public Iterable<Snapper.Element> getElements() {
      return () -> new Iterator<Snapper.Element>() {
         private final Iterator<HudElement> it = HudEditorScreen.this.hud.iterator();

         @Override
         public boolean hasNext() {
            return this.it.hasNext();
         }

         public Snapper.Element next() {
            return this.it.next();
         }
      };
   }

   @Override
   public boolean shouldNotSnapTo(Snapper.Element element) {
      return this.selection.contains((HudElement)element);
   }

   @Override
   public int getSnappingRange() {
      return this.hud.snappingRange.get();
   }

   private void onRender(int mouseX, int mouseY) {
      for (HudElement element : this.hud) {
         if (!element.isActive()) {
            this.renderElement(element, INACTIVE_BG_COLOR, INACTIVE_OL_COLOR);
         }
      }

      if (this.pressed && !this.dragging) {
         this.fillSelection(mouseX, mouseY);
      }

      for (HudElement element : this.selection) {
         this.renderElement(element, HOVER_BG_COLOR, HOVER_OL_COLOR);
      }

      if (this.pressed && !this.dragging) {
         this.selection.clear();
      }

      if (this.pressed && !this.dragging) {
         int x1 = Math.min(this.clickX, mouseX);
         int x2 = Math.max(this.clickX, mouseX);
         int y1 = Math.min(this.clickY, mouseY);
         int y2 = Math.max(this.clickY, mouseY);
         this.renderQuad(x1, y1, x2 - x1, y2 - y1, SELECTION_BG_COLOR, SELECTION_OL_COLOR);
      }

      if (!this.pressed) {
         HudElement hovered = this.getHovered(mouseX, mouseY);
         if (hovered != null) {
            this.renderElement(hovered, HOVER_BG_COLOR, HOVER_OL_COLOR);
         }
      }
   }

   private HudElement getHovered(int mouseX, int mouseY) {
      for (HudElement element : this.hud) {
         if (mouseX >= element.x && mouseX <= element.x + element.getWidth() && mouseY >= element.y && mouseY <= element.y + element.getHeight()) {
            return element;
         }
      }

      return null;
   }

   private void renderQuad(double x, double y, double w, double h, Color bgColor, Color olColor) {
      Renderer2D.COLOR.quad(x + 1.0, y + 1.0, w - 2.0, h - 2.0, bgColor);
      Renderer2D.COLOR.quad(x, y, w, 1.0, olColor);
      Renderer2D.COLOR.quad(x, y + h - 1.0, w, 1.0, olColor);
      Renderer2D.COLOR.quad(x, y + 1.0, 1.0, h - 2.0, olColor);
      Renderer2D.COLOR.quad(x + w - 1.0, y + 1.0, 1.0, h - 2.0, olColor);
   }

   private void renderElement(HudElement element, Color bgColor, Color olColor) {
      this.renderQuad(element.x, element.y, element.getWidth(), element.getHeight(), bgColor, olColor);
   }

   @Override
   protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      boolean renderSplitLines = this.pressed && !this.selection.isEmpty() && this.moved;
      if (renderSplitLines || this.splitLinesAnimation > 0.0) {
         this.renderSplitLines(renderSplitLines, delta / 20.0F);
      }

      Renderer2D.COLOR.begin();
      this.onRender(mouseX, mouseY);
      Renderer2D.COLOR.render();
      this.runAfterRenderTasks();
   }

   private void renderSplitLines(boolean increment, double delta) {
      if (increment) {
         this.splitLinesAnimation += delta * 6.0;
      } else {
         this.splitLinesAnimation -= delta * 6.0;
      }

      this.splitLinesAnimation = Mth.clamp(this.splitLinesAnimation, 0.0, 1.0);
      Renderer2D renderer = Renderer2D.COLOR;
      renderer.begin();
      double w = Utils.getWindowWidth();
      double h = Utils.getWindowHeight();
      double w3 = w / 3.0;
      double h3 = h / 3.0;
      int prevA = SPLIT_LINES_COLOR.a;
      SPLIT_LINES_COLOR.a = (int)(SPLIT_LINES_COLOR.a * this.splitLinesAnimation);
      this.renderSplitLine(renderer, w3, 0.0, w3, h);
      this.renderSplitLine(renderer, w3 * 2.0, 0.0, w3 * 2.0, h);
      this.renderSplitLine(renderer, 0.0, h3, w, h3);
      this.renderSplitLine(renderer, 0.0, h3 * 2.0, w, h3 * 2.0);
      SPLIT_LINES_COLOR.a = prevA;
      renderer.render();
   }

   private void renderSplitLine(Renderer2D renderer, double x, double y, double destX, double destY) {
      double incX = 0.0;
      double incY = 0.0;
      if (x == destX) {
         incY = Utils.getWindowWidth() / 25.0;
      } else {
         incX = Utils.getWindowWidth() / 25.0;
      }

      do {
         renderer.line(x, y, x + incX, y + incY, SPLIT_LINES_COLOR);
         x += incX * 2.0;
         y += incY * 2.0;
      } while (x < destX || y < destY);
   }

   public static boolean isOpen() {
      Screen s = MeteorClient.mc.screen;
      return s instanceof HudEditorScreen || s instanceof AddHudElementScreen || s instanceof HudElementPresetsScreen || s instanceof HudElementScreen;
   }

   private class SelectionBox implements Snapper.Element {
      private int x;
      private int y;
      private final int width;
      private final int height;

      public SelectionBox() {
         int x1 = Integer.MAX_VALUE;
         int y1 = Integer.MAX_VALUE;
         int x2 = 0;
         int y2 = 0;

         for (HudElement element : HudEditorScreen.this.selection) {
            if (element.getX() < x1) {
               x1 = element.getX();
            } else if (element.getX() > x2) {
               x2 = element.getX();
            }

            if (element.getX2() < x1) {
               x1 = element.getX2();
            } else if (element.getX2() > x2) {
               x2 = element.getX2();
            }

            if (element.getY() < y1) {
               y1 = element.getY();
            } else if (element.getY() > y2) {
               y2 = element.getY();
            }

            if (element.getY2() < y1) {
               y1 = element.getY2();
            } else if (element.getY2() > y2) {
               y2 = element.getY2();
            }
         }

         this.x = x1;
         this.y = y1;
         this.width = x2 - x1;
         this.height = y2 - y1;
      }

      @Override
      public int getX() {
         return this.x;
      }

      @Override
      public int getY() {
         return this.y;
      }

      @Override
      public int getWidth() {
         return this.width;
      }

      @Override
      public int getHeight() {
         return this.height;
      }

      @Override
      public void setPos(int x, int y) {
         for (HudElement element : HudEditorScreen.this.selection) {
            element.setPos(x + (element.x - this.x), y + (element.y - this.y));
         }

         this.x = x;
         this.y = y;
      }

      @Override
      public void move(int deltaX, int deltaY) {
         int lastX = this.x;
         int lastY = this.y;
         int border = Hud.get().border.get();
         this.x = Mth.clamp(this.x + deltaX, border, Utils.getWindowWidth() - this.width - border);
         this.y = Mth.clamp(this.y + deltaY, border, Utils.getWindowHeight() - this.height - border);

         for (HudElement element : HudEditorScreen.this.selection) {
            element.move(this.x - lastX, this.y - lastY);
         }
      }
   }
}
