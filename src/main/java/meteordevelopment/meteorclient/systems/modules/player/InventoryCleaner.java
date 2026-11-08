package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryCleaner extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> items = this.sgGeneral.add(new ItemListSetting.Builder().name("items").description("Items to drop.").build());
   private final Setting<InventoryCleaner.DropWhen> dropWhen = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("drop-when"))
                  .description("When to drop matching items from your inventory."))
               .defaultValue(InventoryCleaner.DropWhen.Inventory))
            .build()
      );
   private final Setting<Boolean> cleanChests = this.sgGeneral
      .add(new BoolSetting.Builder().name("clean-chests").description("Drops matching items from open chests.").defaultValue(false).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("Delay between drops in milliseconds.").defaultValue(60).min(0).sliderRange(0, 500).build());
   private long timer;
   private boolean dirty;

   public InventoryCleaner() {
      super(Categories.Player, "inventory-cleaner", "Drops selected items from your inventory and chests.");
   }

   @Override
   public void onActivate() {
      this.timer = System.currentTimeMillis();
      this.dirty = false;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.gameMode != null) {
         boolean inInventory = this.mc.screen instanceof InventoryScreen;
         if (this.cleanChests.get() && this.mc.player.containerMenu instanceof ChestMenu chest) {
            int containerSlots = chest.getRowCount() * 9;

            for (int i = 0; i < containerSlots; i++) {
               Slot slot = chest.getSlot(i);
               if (slot.hasItem() && this.shouldDrop(slot.getItem()) && this.tryDropSlot(i)) {
                  this.dirty = true;
                  return;
               }
            }
         }

         if (this.shouldCleanInventory(inInventory)) {
            for (int i = 0; i < 36; i++) {
               ItemStack stack = this.mc.player.getInventory().getItem(i);
               if (this.shouldDrop(stack)) {
                  int slotId = SlotUtils.indexToId(i);
                  if (slotId != -1 && this.tryDropSlot(slotId)) {
                     this.dirty = true;
                     return;
                  }
               }
            }

            if (this.dirty && this.passed(this.delay.get().intValue() + 100L)) {
               this.mc.player.connection.send(new ServerboundContainerClosePacket(this.mc.player.containerMenu.containerId));
               this.dirty = false;
            }
         }
      }
   }

   private boolean shouldCleanInventory(boolean inInventory) {
      return switch ((InventoryCleaner.DropWhen)this.dropWhen.get()) {
         case Inventory -> inInventory;
         case Always -> true;
         case NotInInventory -> !inInventory;
      };
   }

   private boolean shouldDrop(ItemStack stack) {
      return !stack.isEmpty() && this.items.get().contains(stack.getItem());
   }

   private boolean tryDropSlot(int slotId) {
      if (!this.passed(this.delay.get().intValue())) {
         return false;
      }

      this.mc.gameMode.handleContainerInput(this.mc.player.containerMenu.containerId, slotId, 1, ContainerInput.THROW, this.mc.player);
      this.timer = System.currentTimeMillis();
      return true;
   }

   private boolean passed(long delay) {
      return System.currentTimeMillis() - this.timer >= delay;
   }

   public enum DropWhen {
      Inventory,
      Always,
      NotInInventory;
   }
}
