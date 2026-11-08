package meteordevelopment.meteorclient.utils.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.mixininterface.ISlot;
import meteordevelopment.meteorclient.utils.render.PeekScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Tuple;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class InventorySorter {
   private final AbstractContainerScreen<?> screen;
   private final InventorySorter.InvPart originInvPart;
   private boolean invalid;
   private List<InventorySorter.Action> actions;
   private int timer;
   private int currentActionI;

   public InventorySorter(AbstractContainerScreen<?> screen, Slot originSlot) {
      this.screen = screen;
      this.originInvPart = this.getInvPart(originSlot);
      if (this.originInvPart != InventorySorter.InvPart.Invalid && this.originInvPart != InventorySorter.InvPart.Hotbar && !(screen instanceof PeekScreen)) {
         this.actions = new ArrayList<>();
         this.generateActions();
      } else {
         this.invalid = true;
      }
   }

   public boolean tick(int delay) {
      if (this.invalid) {
         return true;
      } else if (this.currentActionI >= this.actions.size()) {
         return true;
      } else if (this.timer >= delay) {
         this.timer = 0;
         InventorySorter.Action action = this.actions.get(this.currentActionI);
         InvUtils.move().fromId(action.from).toId(action.to);
         this.currentActionI++;
         return false;
      } else {
         this.timer++;
         return false;
      }
   }

   private void generateActions() {
      List<InventorySorter.MySlot> slots = new ArrayList<>();

      for (Slot slot : this.screen.getMenu().slots) {
         if (this.getInvPart(slot) == this.originInvPart) {
            slots.add(new InventorySorter.MySlot(((ISlot)slot).meteor$getIndex(), slot.getItem()));
         }
      }

      slots.sort(Comparator.comparingInt(value -> value.id));
      this.generateStackingActions(slots);
      this.generateSortingActions(slots);
   }

   private void generateStackingActions(List<InventorySorter.MySlot> slots) {
      InventorySorter.SlotMap slotMap = new InventorySorter.SlotMap();

      for (InventorySorter.MySlot slot : slots) {
         if (!slot.itemStack.isEmpty() && slot.itemStack.isStackable() && slot.itemStack.getCount() < slot.itemStack.getMaxStackSize()) {
            slotMap.get(slot.itemStack).add(slot);
         }
      }

      for (Tuple<ItemStack, List<InventorySorter.MySlot>> entry : slotMap.map) {
         List<InventorySorter.MySlot> slotsToStack = entry.getB();
         InventorySorter.MySlot slotToStackTo = null;

         for (int i = 0; i < slotsToStack.size(); i++) {
            InventorySorter.MySlot slot = slotsToStack.get(i);
            if (slotToStackTo == null) {
               slotToStackTo = slot;
            } else {
               this.actions.add(new InventorySorter.Action(slot.id, slotToStackTo.id));
               if (slotToStackTo.itemStack.getCount() + slot.itemStack.getCount() <= slotToStackTo.itemStack.getMaxStackSize()) {
                  slotToStackTo.itemStack = new ItemStack(slotToStackTo.itemStack.getItem(), slotToStackTo.itemStack.getCount() + slot.itemStack.getCount());
                  slot.itemStack = ItemStack.EMPTY;
                  if (slotToStackTo.itemStack.getCount() >= slotToStackTo.itemStack.getMaxStackSize()) {
                     slotToStackTo = null;
                  }
               } else {
                  int needed = slotToStackTo.itemStack.getMaxStackSize() - slotToStackTo.itemStack.getCount();
                  slotToStackTo.itemStack = new ItemStack(slotToStackTo.itemStack.getItem(), slotToStackTo.itemStack.getMaxStackSize());
                  slot.itemStack = new ItemStack(slot.itemStack.getItem(), slot.itemStack.getCount() - needed);
                  slotToStackTo = null;
                  i--;
               }
            }
         }
      }
   }

   private void generateSortingActions(List<InventorySorter.MySlot> slots) {
      for (int i = 0; i < slots.size(); i++) {
         InventorySorter.MySlot bestSlot = null;

         for (int j = i; j < slots.size(); j++) {
            InventorySorter.MySlot slot = slots.get(j);
            if (bestSlot == null) {
               bestSlot = slot;
            } else if (this.isSlotBetter(bestSlot, slot)) {
               bestSlot = slot;
            }
         }

         if (!bestSlot.itemStack.isEmpty()) {
            InventorySorter.MySlot toSlot = slots.get(i);
            int from = bestSlot.id;
            int to = toSlot.id;
            if (from != to) {
               ItemStack temp = bestSlot.itemStack;
               bestSlot.itemStack = toSlot.itemStack;
               toSlot.itemStack = temp;
               this.actions.add(new InventorySorter.Action(from, to));
            }
         }
      }
   }

   private boolean isSlotBetter(InventorySorter.MySlot best, InventorySorter.MySlot slot) {
      ItemStack bestI = best.itemStack;
      ItemStack slotI = slot.itemStack;
      if (bestI.isEmpty() && !slotI.isEmpty()) {
         return true;
      }

      if (!bestI.isEmpty() && slotI.isEmpty()) {
         return false;
      }

      int c = BuiltInRegistries.ITEM.getKey(bestI.getItem()).compareTo(BuiltInRegistries.ITEM.getKey(slotI.getItem()));
      if (c == 0) {
         if (slotI.getCount() != bestI.getCount()) {
            return slotI.getCount() > bestI.getCount();
         }

         if (slotI.getDamageValue() != bestI.getDamageValue()) {
            return slotI.getDamageValue() > bestI.getDamageValue();
         }
      }

      return c > 0;
   }

   private InventorySorter.InvPart getInvPart(Slot slot) {
      int i = ((ISlot)slot).meteor$getSlot();
      if (!(slot.container instanceof Inventory) || this.screen instanceof CreativeModeInventoryScreen && ((ISlot)slot).meteor$getIndex() <= 8) {
         if ((this.screen instanceof ContainerScreen || this.screen instanceof ShulkerBoxScreen) && slot.container instanceof SimpleContainer) {
            return InventorySorter.InvPart.Main;
         }
      } else {
         if (SlotUtils.isHotbar(i)) {
            return InventorySorter.InvPart.Hotbar;
         }

         if (SlotUtils.isMain(i)) {
            return InventorySorter.InvPart.Player;
         }
      }

      return InventorySorter.InvPart.Invalid;
   }

   private record Action(int from, int to) {
   }

   private enum InvPart {
      Hotbar,
      Player,
      Main,
      Invalid;
   }

   private static class MySlot {
      public final int id;
      public ItemStack itemStack;

      public MySlot(int id, ItemStack itemStack) {
         this.id = id;
         this.itemStack = itemStack;
      }
   }

   private static class SlotMap {
      private final List<Tuple<ItemStack, List<InventorySorter.MySlot>>> map = new ArrayList<>();

      public List<InventorySorter.MySlot> get(ItemStack itemStack) {
         for (Tuple<ItemStack, List<InventorySorter.MySlot>> entry : this.map) {
            if (ItemStack.isSameItemSameComponents(itemStack, entry.getA())) {
               return entry.getB();
            }
         }

         List<InventorySorter.MySlot> list = new ArrayList<>();
         this.map.add(new Tuple<>(itemStack, list));
         return list;
      }
   }
}
