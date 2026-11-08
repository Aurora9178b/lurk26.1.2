package meteordevelopment.meteorclient.gui.screens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;

public class ContainerInventoryScreen extends Screen {
   private static final Identifier SLOT_TEXTURE = Identifier.withDefaultNamespace("container/slot");
   private static final int SLOT_SIZE = 18;
   private static final int SCREEN_WIDTH = 176;
   private final List<ItemStack> containerItems;
   private final Inventory playerInventory = MeteorClient.mc.player.getInventory();
   private final int containerRows;
   private int x;
   private int y;
   private int baseX;
   private int baseY;
   private int playerY;

   public ContainerInventoryScreen(ItemStack containerItem) {
      super(containerItem.getHoverName());
      this.containerItems = new ArrayList<>();
      if (containerItem.getItem() instanceof BundleItem) {
         BundleContents bundleContents = containerItem.get(DataComponents.BUNDLE_CONTENTS);
         if (bundleContents != null) {
            for (ItemStackTemplate template : bundleContents.items()) {
               this.containerItems.add(template.create());
            }
         }
      } else {
         ItemStack[] tempItems = new ItemStack[64];
         Utils.getItemsInContainerItem(containerItem, tempItems);
         Collections.addAll(this.containerItems, tempItems);
      }

      this.containerRows = Math.max(1, Mth.positiveCeilDiv(this.containerItems.size(), 9));
   }

   @Override
   public void init() {
      super.init();
      this.x = (this.width - 176) / 2;
      this.y = (this.height - (114 + this.containerRows * 18 + 20)) / 2;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      super.extractRenderState(graphics, mouseX, mouseY, delta);
      this.baseX = this.x + 8;
      this.baseY = this.y + 18;
      this.playerY = this.baseY + this.containerRows * 18 + 20;

      for (int row = 0; row < this.containerRows + 4; row++) {
         for (int col = 0; col < 9; col++) {
            int slotY = row < this.containerRows ? this.baseY + row * 18 : this.playerY + (row - this.containerRows) * 18;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, this.baseX + col * 18, slotY, 18, 18);
         }
      }

      for (int i = 0; i < this.containerItems.size(); i++) {
         ItemStack item = this.containerItems.get(i);
         if (!item.isEmpty()) {
            int itemX = this.baseX + i % 9 * 18 + 1;
            int itemY = this.baseY + i / 9 * 18 + 1;
            graphics.item(item, itemX, itemY);
            graphics.itemDecorations(this.font, item, itemX, itemY);
         }
      }

      for (int row = 0; row < 4; row++) {
         for (int col = 0; col < 9; col++) {
            int slotIndex = row < 3 ? 9 + row * 9 + col : col;
            ItemStack item = this.playerInventory.getItem(slotIndex);
            if (!item.isEmpty()) {
               int itemX = this.baseX + col * 18 + 1;
               int itemY = this.playerY + row * 18 + 1;
               graphics.item(item, itemX, itemY);
               graphics.itemDecorations(this.font, item, itemX, itemY);
            }
         }
      }

      graphics.pose().pushMatrix();
      graphics.pose().translate(this.x, this.y);
      if (this.font != null) {
         graphics.text(this.font, this.title, 8, 6, -12566464, false);
         graphics.text(this.font, this.playerInventory.getDisplayName(), 8, 18 + this.containerRows * 18 + 10, -12566464, false);
      }

      graphics.pose().popMatrix();
      ItemStack item = this.getSelectedItem(mouseX, mouseY);
      if (!item.isEmpty()) {
         graphics.setTooltipForNextFrame(this.font, getTooltipFromItem(MeteorClient.mc, item), item.getTooltipImage(), mouseX, mouseY);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
      ItemStack stack = this.getSelectedItem((int)click.x(), (int)click.y());
      return tooltips.shouldOpenContents(click) ? tooltips.openContent(stack) : false;
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
      ItemStack stack = this.getSelectedItem(
         (int)MeteorClient.mc.mouseHandler.getScaledXPos(MeteorClient.mc.getWindow()),
         (int)MeteorClient.mc.mouseHandler.getScaledYPos(MeteorClient.mc.getWindow())
      );
      if (tooltips.shouldOpenContents(input)) {
         return tooltips.openContent(stack);
      }

      if (input.key() != 256 && !MeteorClient.mc.options.keyInventory.matches(input)) {
         return false;
      }

      this.onClose();
      return true;
   }

   private ItemStack getSelectedItem(int mouseX, int mouseY) {
      if (mouseX >= this.baseX && mouseX <= this.baseX + 162) {
         int col = (mouseX - this.baseX) / 18;
         if (col > 8) {
            return ItemStack.EMPTY;
         } else if (mouseY >= this.baseY && mouseY < this.baseY + this.containerRows * 18) {
            int index = (mouseY - this.baseY) / 18 * 9 + col;
            return index < this.containerItems.size() ? this.containerItems.get(index) : ItemStack.EMPTY;
         } else if (mouseY >= this.playerY && mouseY < this.playerY + 72) {
            int row = (mouseY - this.playerY) / 18;
            int slotIndex = row < 3 ? 9 + row * 9 + col : col;
            return this.playerInventory.getItem(slotIndex);
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }
}
