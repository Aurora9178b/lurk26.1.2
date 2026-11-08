package meteordevelopment.meteorclient.utils.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

public class BundleTooltipComponent implements ClientTooltipComponent, MeteorTooltipData {
   private static final Identifier BUNDLE_SLOT_BACKGROUND_TEXTURE = Identifier.withDefaultNamespace("container/bundle/slot_background");
   private static final Identifier BUNDLE_PROGRESS_BAR_BORDER_TEXTURE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
   private static final Identifier BUNDLE_PROGRESS_BAR_FILL_TEXTURE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
   private static final Identifier BUNDLE_PROGRESS_BAR_FULL_TEXTURE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
   private static final Identifier BUNDLE_SLOT_HIGHLIGHT_BACK_TEXTURE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
   private static final Identifier BUNDLE_SLOT_HIGHLIGHT_FRONT_TEXTURE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
   private static final int SLOTS_PER_ROW = 8;
   private static final int SLOT_DIMENSION = 24;
   private static final int ROW_WIDTH = 208;
   private static final int PROGRESS_BAR_WIDTH = 94;
   private static final int PROGRESS_BAR_HEIGHT = 13;
   private static final Component BUNDLE_FULL = Component.translatable("item.minecraft.bundle.full");
   private final ItemStack[] items;
   private final BundleContents bundleContents;
   private final int width;
   private final int height;

   public BundleTooltipComponent(ItemStack[] items, BundleContents bundleContents) {
      this.items = items;
      this.bundleContents = bundleContents;
      int rows = (items.length + 8 - 1) / 8;
      this.width = 208;
      this.height = 8 + rows * 24 + 8 + 13 + 4;
   }

   @Override
   public ClientTooltipComponent getComponent() {
      return this;
   }

   @Override
   public int getHeight(Font textRenderer) {
      return this.height;
   }

   @Override
   public int getWidth(Font textRenderer) {
      return this.width;
   }

   @Override
   public boolean showTooltipWithItemInHand() {
      return true;
   }

   @Override
   public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
      int row = 0;
      int col = 0;

      for (ItemStack itemStack : this.items) {
         if (!itemStack.isEmpty()) {
            int slotX = x + 8 + col * 24;
            int slotY = y + 8 + row * 24;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_BACKGROUND_TEXTURE, slotX, slotY, 24, 24);
            this.drawItem(itemStack, row * 8 + col, slotX, slotY, font, graphics);
            graphics.itemDecorations(font, itemStack, slotX + 4, slotY + 4);
         }

         if (++col >= 8) {
            col = 0;
            row++;
         }
      }

      this.drawSelectedItemTooltip(font, graphics, x, y, width);
      int progressBarX = x + (this.width - 94) / 2;
      int progressBarY = y + this.height - 13 - 4;
      this.drawProgressBar(progressBarX, progressBarY, font, graphics);
   }

   private void drawItem(ItemStack itemStack, int index, int x, int y, Font font, GuiGraphicsExtractor graphics) {
      boolean bl = this.bundleContents.getSelectedItemIndex() == index;
      if (bl) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_HIGHLIGHT_BACK_TEXTURE, x, y, 24, 24);
      } else {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_BACKGROUND_TEXTURE, x, y, 24, 24);
      }

      graphics.item(itemStack, x + 4, y + 4, 0);
      graphics.itemDecorations(font, itemStack, x + 4, y + 4);
      if (bl) {
         graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_SLOT_HIGHLIGHT_FRONT_TEXTURE, x, y, 24, 24);
      }
   }

   private void drawSelectedItemTooltip(Font font, GuiGraphicsExtractor graphics, int x, int y, int width) {
      if (this.bundleContents.getSelectedItemIndex() != -1) {
         ItemStack itemStack = this.bundleContents.getSelectedItem().create();
         Component text = itemStack.getStyledHoverName();
         int i = font.width(text.getVisualOrderText());
         int j = x + width / 2 - 12;
         ClientTooltipComponent tooltipComponent = ClientTooltipComponent.create(text.getVisualOrderText());
         graphics.tooltip(font, List.of(tooltipComponent), j - i / 2, y - 37, DefaultTooltipPositioner.INSTANCE, itemStack.get(DataComponents.TOOLTIP_STYLE));
      }
   }

   private void drawProgressBar(int x, int y, Font font, GuiGraphicsExtractor graphics) {
      int fillAmount = Mth.clamp(Mth.mulAndTruncate(this.bundleContents.weight().getOrThrow(), 94), 0, 94);
      Identifier fillTexture = this.bundleContents.weight().getOrThrow().compareTo(Fraction.ONE) >= 0
         ? BUNDLE_PROGRESS_BAR_FULL_TEXTURE
         : BUNDLE_PROGRESS_BAR_FILL_TEXTURE;
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fillTexture, x + 1, y, fillAmount, 13);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUNDLE_PROGRESS_BAR_BORDER_TEXTURE, x, y, 94, 13);
      Component label = this.getProgressBarLabel();
      if (label != null) {
         graphics.centeredText(font, label, x + 47, y + 3, -1);
      }
   }

   private Component getProgressBarLabel() {
      return this.bundleContents.weight().getOrThrow().compareTo(Fraction.ONE) >= 0
         ? BUNDLE_FULL
         : Component.literal(String.format("%.2f%%", this.bundleContents.weight().getOrThrow().floatValue() * 100.0F));
   }
}
