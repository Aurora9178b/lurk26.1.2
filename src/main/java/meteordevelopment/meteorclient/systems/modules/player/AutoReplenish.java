package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.AutoTotem;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AutoReplenish extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> minCount = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-count")
            .description("Replenish a slot when it reaches this item count.")
            .defaultValue(8)
            .min(1)
            .sliderRange(1, 63)
            .build()
      );
   private final Setting<Integer> tickDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("How long in ticks to wait between replenishing your hotbar.").defaultValue(1).min(0).build());
   private final Setting<Boolean> offhand = this.sgGeneral
      .add(new BoolSetting.Builder().name("offhand").description("Whether or not to replenish items in your offhand.").defaultValue(true).build());
   private final Setting<Boolean> unstackable = this.sgGeneral
      .add(new BoolSetting.Builder().name("unstackable").description("Replenish unstackable items.").defaultValue(true).build());
   private final Setting<Boolean> sameEnchants = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("same-enchants")
            .description("Only replace unstackables with items that have the same enchants.")
            .defaultValue(true)
            .visible(this.unstackable::get)
            .build()
      );
   private final Setting<Boolean> searchHotbar = this.sgGeneral
      .add(new BoolSetting.Builder().name("search-hotbar").description("Combine stacks in your hotbar/offhand as a last resort.").defaultValue(false).build());
   private final Setting<List<Item>> excludedItems = this.sgGeneral
      .add(new ItemListSetting.Builder().name("excluded-items").description("Items that won't be replenished.").build());
   private final ItemStack[] items = new ItemStack[10];
   private boolean prevHadOpenScreen;
   private int tickDelayLeft;

   public AutoReplenish() {
      super(Categories.Player, "auto-replenish", "Automatically refills items in your hotbar, main hand, or offhand.");
   }

   @Override
   public void onActivate() {
      this.fillItems();
      this.tickDelayLeft = this.tickDelay.get();
      this.prevHadOpenScreen = this.mc.screen != null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.screen == null && this.prevHadOpenScreen) {
         this.fillItems();
      }

      this.prevHadOpenScreen = this.mc.screen != null;
      if (this.mc.player.containerMenu.getItems().size() == 46 && this.mc.screen == null) {
         if (this.tickDelayLeft > 0) {
            this.tickDelayLeft--;
         } else {
            for (int i = 0; i < 9; i++) {
               ItemStack stack = this.mc.player.getInventory().getItem(i);
               this.checkSlot(i, stack);
            }

            if (this.offhand.get() && !Modules.get().get(AutoTotem.class).isLocked()) {
               ItemStack stack = this.mc.player.getOffhandItem();
               this.checkSlot(9, stack);
            }

            this.tickDelayLeft = this.tickDelay.get();
         }
      }
   }

   private void checkSlot(int slot, ItemStack stack) {
      ItemStack prevStack = this.items[slot];
      this.items[slot] = stack.copy();
      if (slot == 9) {
         slot = 40;
      }

      if (!this.excludedItems.get().contains(stack.getItem())) {
         if (!this.excludedItems.get().contains(prevStack.getItem())) {
            int fromSlot = -1;
            if (stack.isStackable() && !stack.isEmpty() && stack.getCount() <= this.minCount.get()) {
               fromSlot = this.findItem(stack, slot, this.minCount.get() - stack.getCount() + 1, true);
            }

            if (prevStack.isStackable() && stack.isEmpty() && !prevStack.isEmpty()) {
               fromSlot = this.findItem(prevStack, slot, this.minCount.get() - stack.getCount() + 1, false);
            }

            if (this.unstackable.get() && !prevStack.isStackable() && stack.isEmpty() && !prevStack.isEmpty()) {
               fromSlot = this.findItem(prevStack, slot, 1, false);
            }

            if (fromSlot != this.mc.player.getInventory().getSelectedSlot() && fromSlot != 40) {
               if (fromSlot >= 9 || fromSlot >= slot || slot == this.mc.player.getInventory().getSelectedSlot() || slot == 40) {
                  InvUtils.move().from(fromSlot).to(slot);
               }
            }
         }
      }
   }

   private int findItem(ItemStack lookForStack, int excludedSlot, int goodEnoughCount, boolean mustCombine) {
      int slot = -1;
      int count = 0;

      for (int i = this.mc.player.getInventory().getContainerSize() - 2; i >= (this.searchHotbar.get() ? 0 : 9); i--) {
         if (i != excludedSlot) {
            ItemStack stack = this.mc.player.getInventory().getItem(i);
            if (stack.getItem() == lookForStack.getItem()
               && (!mustCombine || ItemStack.isSameItemSameComponents(lookForStack, stack))
               && (!this.sameEnchants.get() || stack.getEnchantments().equals(lookForStack.getEnchantments()))
               && stack.getCount() > count) {
               slot = i;
               count = stack.getCount();
               if (count >= goodEnoughCount) {
                  break;
               }
            }
         }
      }

      return slot;
   }

   private void fillItems() {
      for (int i = 0; i < 9; i++) {
         this.items[i] = this.mc.player.getInventory().getItem(i).copy();
      }

      this.items[9] = this.mc.player.getOffhandItem().copy();
   }
}
