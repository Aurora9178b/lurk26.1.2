package meteordevelopment.meteorclient.utils.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;

public class PeekScreen extends ShulkerBoxScreen {
   private final Identifier TEXTURE = Identifier.parse("textures/gui/container/shulker_box.png");
   private final ItemStack storageBlock;

   public PeekScreen(ItemStack storageBlock, ItemStack[] contents) {
      super(
         new ShulkerBoxMenu(0, MeteorClient.mc.player.getInventory(), new SimpleContainer(contents)),
         MeteorClient.mc.player.getInventory(),
         storageBlock.getHoverName()
      );
      this.storageBlock = storageBlock;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
      BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
      if (tooltips.shouldOpenContents(click)
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && MeteorClient.mc.player.containerMenu.getCarried().isEmpty()) {
         ItemStack itemStack = this.hoveredSlot.getItem();
         return tooltips.openContent(itemStack);
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent click) {
      return false;
   }

   @Override
   public boolean keyPressed(KeyEvent input) {
      BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
      if (tooltips.shouldOpenContents(input)
         && this.hoveredSlot != null
         && !this.hoveredSlot.getItem().isEmpty()
         && MeteorClient.mc.player.containerMenu.getCarried().isEmpty()) {
         ItemStack itemStack = this.hoveredSlot.getItem();
         if (tooltips.openContent(itemStack)) {
            return true;
         }
      }

      if (input.key() != 256 && !MeteorClient.mc.options.keyInventory.matches(input)) {
         return false;
      }

      this.onClose();
      return true;
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      Color color = Utils.getShulkerColor(this.storageBlock);
      int i = (this.width - this.imageWidth) / 2;
      int j = (this.height - this.imageHeight) / 2;
      graphics.blit(
         RenderPipelines.GUI_TEXTURED,
         this.TEXTURE,
         i,
         j,
         0.0F,
         0.0F,
         this.imageWidth,
         this.imageHeight,
         this.imageWidth,
         this.imageHeight,
         256,
         256,
         ARGB.colorFromFloat(color.a / 255.0F, color.r / 255.0F, color.g / 255.0F, color.b / 255.0F)
      );
   }
}
