package meteordevelopment.meteorclient.gui.widgets.containers;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

public abstract class WSection extends WVerticalList {
   public Runnable action;
   protected String title;
   protected boolean expanded;
   protected double animProgress;
   private WSection.WHeader header;
   protected final WWidget headerWidget;
   private double actualWidth;
   private double actualHeight;
   private double forcedHeight = -1.0;
   private boolean firstTime = true;

   public WSection(String title, boolean expanded, WWidget headerWidget) {
      this.title = title;
      this.expanded = expanded;
      this.headerWidget = headerWidget;
      this.animProgress = expanded ? 1.0 : 0.0;
   }

   @Override
   public void init() {
      this.header = this.createHeader();
      this.header.theme = this.theme;
      super.add(this.header).expandX();
   }

   @Override
   public <T extends WWidget> Cell<T> add(T widget) {
      return super.<T>add(widget).padHorizontal(6.0);
   }

   protected abstract WSection.WHeader createHeader();

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   @Override
   protected void onCalculateSize() {
      if (this.forcedHeight == -1.0) {
         super.onCalculateSize();
         this.actualWidth = this.width;
         this.actualHeight = this.height;
      } else {
         this.width = this.actualWidth;
         this.height = this.forcedHeight;
         if (this.animProgress == 1.0) {
            this.forcedHeight = -1.0;
         }
      }

      if (this.firstTime) {
         this.firstTime = false;
         this.forcedHeight = (this.actualHeight - this.header.height) * this.animProgress + this.header.height;
         this.onCalculateSize();
      }
   }

   @Override
   public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (!this.visible) {
         return true;
      }

      double preProgress = this.animProgress;
      this.animProgress = this.animProgress + (this.expanded ? 1 : -1) * delta * 14.0;
      this.animProgress = Mth.clamp(this.animProgress, 0.0, 1.0);
      if (this.animProgress != preProgress) {
         this.forcedHeight = (this.actualHeight - this.header.height) * this.animProgress + this.header.height;
         this.invalidate();
      }

      boolean scissor = this.animProgress != 0.0 && this.animProgress != 1.0 || this.expanded && this.animProgress != 1.0;
      if (scissor) {
         renderer.scissorStart(this.x, this.y, this.width, (this.height - this.header.height) * this.animProgress + this.header.height);
      }

      boolean toReturn = super.render(renderer, mouseX, mouseY, delta);
      if (scissor) {
         renderer.scissorEnd();
      }

      return toReturn;
   }

   @Override
   protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      if (this.expanded || this.animProgress > 0.0 || widget instanceof WSection.WHeader) {
         widget.render(renderer, mouseX, mouseY, delta);
      }
   }

   @Override
   protected boolean propagateEvents(WWidget widget) {
      return this.expanded || widget instanceof WSection.WHeader;
   }

   protected abstract class WHeader extends WHorizontalList {
      protected String title;

      public WHeader(String title) {
         this.title = title;
      }

      @Override
      public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
         if (this.mouseOver && click.button() == 0 && !doubled) {
            this.onClick();
            return true;
         } else {
            return false;
         }
      }

      protected void onClick() {
         WSection.this.setExpanded(!WSection.this.expanded);
         if (WSection.this.action != null) {
            WSection.this.action.run();
         }
      }
   }
}
