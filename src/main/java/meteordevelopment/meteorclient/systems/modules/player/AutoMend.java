package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoMend extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> blacklist = this.sgGeneral
      .add(
         new ItemListSetting.Builder()
            .name("blacklist")
            .description("Item blacklist.")
            .filter(item -> item.components().get(DataComponents.DAMAGE) != null)
            .bypassFilterWhenSavingAndLoading()
            .build()
      );
   private final Setting<Boolean> force = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("force")
            .description("Replaces item in offhand even if there is some other non-repairable item.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-disable")
            .description("Automatically disables when there are no more items to repair.")
            .defaultValue(true)
            .build()
      );
   private boolean didMove;

   public AutoMend() {
      super(Categories.Player, "auto-mend", "Automatically replaces items in your offhand with mending when fully repaired.");
   }

   @Override
   public void onActivate() {
      this.didMove = false;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (!this.shouldWait()) {
         int slot = this.getSlot();
         if (slot == -1) {
            if (this.autoDisable.get()) {
               this.info("Repaired all items, disabling");
               if (this.didMove) {
                  int emptySlot = this.getEmptySlot();
                  InvUtils.move().fromOffhand().to(emptySlot);
               }

               this.toggle();
            }
         } else {
            InvUtils.move().from(slot).toOffhand();
            this.didMove = true;
         }
      }
   }

   private boolean shouldWait() {
      ItemStack itemStack = this.mc.player.getOffhandItem();
      if (itemStack.isEmpty()) {
         return false;
      } else {
         return Utils.hasEnchantments(itemStack, Enchantments.MENDING) ? itemStack.getDamageValue() != 0 : !this.force.get();
      }
   }

   private int getSlot() {
      for (int i = 0; i < this.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
         ItemStack itemStack = this.mc.player.getInventory().getItem(i);
         if (!this.blacklist.get().contains(itemStack.getItem()) && Utils.hasEnchantments(itemStack, Enchantments.MENDING) && itemStack.getDamageValue() > 0) {
            return i;
         }
      }

      return -1;
   }

   private int getEmptySlot() {
      for (int i = 0; i < this.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
         if (this.mc.player.getInventory().getItem(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }
}
