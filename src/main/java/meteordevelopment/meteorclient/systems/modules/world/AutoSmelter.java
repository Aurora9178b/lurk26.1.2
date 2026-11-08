package meteordevelopment.meteorclient.systems.modules.world;

import java.util.List;
import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceMenu;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipePropertySet;

public class AutoSmelter extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> fuelItems = this.sgGeneral
      .add(
         new ItemListSetting.Builder()
            .name("fuel-items")
            .description("Items to use as fuel")
            .defaultValue(Items.COAL, Items.CHARCOAL)
            .filter(this::fuelItemFilter)
            .bypassFilterWhenSavingAndLoading()
            .build()
      );
   private final Setting<Integer> fuelItemsPerRefill = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("fuel-items-per-refill")
            .description("How many fuel items to put into the furnace each time it refills")
            .defaultValue(64)
            .range(1, 64)
            .sliderRange(1, 16)
            .build()
      );
   private final Setting<List<Item>> smeltableItems = this.sgGeneral
      .add(
         new ItemListSetting.Builder()
            .name("smeltable-items")
            .description("Items to smelt")
            .defaultValue(Items.IRON_ORE, Items.GOLD_ORE, Items.COPPER_ORE, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD)
            .filter(this::smeltableItemFilter)
            .bypassFilterWhenSavingAndLoading()
            .build()
      );
   private final Setting<Boolean> disableWhenOutOfItems = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-when-out-of-items").description("Disable the module when you run out of items").defaultValue(true).build());
   private final Setting<Boolean> autoClose = this.sgGeneral.add(new BoolSetting.Builder().name("auto-close").defaultValue(false).build());

   public AutoSmelter() {
      super(Categories.World, "auto-smelter", "Automatically smelts items from your inventory");
   }

   private boolean fuelItemFilter(Item item) {
      return !Utils.canUpdate() ? false : this.mc.getConnection().fuelValues().fuelItems().contains(item);
   }

   private boolean smeltableItemFilter(Item item) {
      return this.mc.level != null && this.mc.level.recipeAccess().propertySet(RecipePropertySet.FURNACE_INPUT).test(item.getDefaultInstance());
   }

   public void tick(AbstractFurnaceMenu c) {
      if (this.mc.player.tickCount % 10 != 0) {
         this.checkFuel(c);
         this.takeResults(c);
         this.insertItems(c);
         if (this.autoClose.get()) {
            this.mc.setScreen(null);
         }
      }
   }

   private void insertItems(AbstractFurnaceMenu c) {
      ItemStack inputItemStack = c.slots.getFirst().getItem();
      if (inputItemStack.isEmpty()) {
         int slot = -1;

         for (int i = 3; i < c.slots.size(); i++) {
            ItemStack item = c.slots.get(i).getItem();
            if (((IAbstractFurnaceMenu)c).meteor$canSmelt(item)
               && this.smeltableItems.get().contains(item.getItem())
               && this.smeltableItemFilter(item.getItem())) {
               slot = i;
               break;
            }
         }

         if (this.disableWhenOutOfItems.get() && slot == -1) {
            this.error("You do not have any items in your inventory that can be smelted. Disabling.");
            this.toggle();
         } else if (slot != -1) {
            InvUtils.move().fromId(slot).toId(0);
            c.slots.getFirst().getItem().isEmpty();
         }
      }
   }

   private void checkFuel(AbstractFurnaceMenu c) {
      ItemStack fuelStack = c.slots.get(1).getItem();
      if (!(c.getLitProgress() > 0.0F)) {
         if (fuelStack.isEmpty()) {
            int slot = -1;

            for (int i = 3; i < c.slots.size(); i++) {
               ItemStack item = c.slots.get(i).getItem();
               if (this.fuelItems.get().contains(item.getItem()) && this.fuelItemFilter(item.getItem())) {
                  slot = i;
                  break;
               }
            }

            if (this.disableWhenOutOfItems.get() && slot == -1) {
               this.error("You do not have any fuel in your inventory. Disabling.");
               this.toggle();
            } else if (slot != -1) {
               ItemStack sourceStack = c.slots.get(slot).getItem();
               int moveCount = Math.min(this.fuelItemsPerRefill.get(), Math.min(sourceStack.getCount(), c.slots.get(1).getMaxStackSize(sourceStack)));
               if (moveCount > 0) {
                  this.moveFuelItems(c, slot, moveCount);
               }
            }
         }
      }
   }

   private void moveFuelItems(AbstractFurnaceMenu c, int fromId, int amount) {
      if (amount > 0 && this.mc.player != null && this.mc.gameMode != null) {
         if (this.mc.player.containerMenu.getCarried().isEmpty()) {
            this.mc.gameMode.handleContainerInput(c.containerId, fromId, 0, ContainerInput.PICKUP, this.mc.player);

            for (int i = 0; i < amount && !this.mc.player.containerMenu.getCarried().isEmpty(); i++) {
               this.mc.gameMode.handleContainerInput(c.containerId, 1, 1, ContainerInput.PICKUP, this.mc.player);
            }

            if (!this.mc.player.containerMenu.getCarried().isEmpty()) {
               this.mc.gameMode.handleContainerInput(c.containerId, fromId, 0, ContainerInput.PICKUP, this.mc.player);
            }

            c.slots.get(1).getItem().isEmpty();
         }
      }
   }

   private void takeResults(AbstractFurnaceMenu c) {
      ItemStack resultStack = c.slots.get(2).getItem();
      if (!resultStack.isEmpty()) {
         InvUtils.shiftClick().slotId(2);
         if (!resultStack.isEmpty()) {
            this.error("Your inventory is full. Disabling.");
            this.toggle();
         }
      }
   }
}
