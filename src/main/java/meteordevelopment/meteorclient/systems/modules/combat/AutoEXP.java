package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoEXP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoEXP.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Which items to repair."))
               .defaultValue(AutoEXP.Mode.Both))
            .build()
      );
   private final Setting<Boolean> replenish = this.sgGeneral
      .add(new BoolSetting.Builder().name("replenish").description("Automatically replenishes exp into a selected hotbar slot.").defaultValue(true).build());
   private final Setting<Boolean> onlyGround = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-on-ground").description("Only throw when the player is on the ground.").defaultValue(false).build());
   private final Setting<Integer> slot = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("exp-slot")
            .description("The slot to replenish exp into.")
            .visible(this.replenish::get)
            .defaultValue(6)
            .range(1, 9)
            .sliderRange(1, 9)
            .build()
      );
   private final Setting<Integer> minThreshold = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-threshold")
            .description("The minimum durability percentage that an item needs to fall to, to be repaired.")
            .defaultValue(30)
            .range(1, 100)
            .sliderRange(1, 100)
            .build()
      );
   private final Setting<Integer> maxThreshold = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-threshold")
            .description("The maximum durability percentage to repair items to.")
            .defaultValue(80)
            .range(1, 100)
            .sliderRange(1, 100)
            .build()
      );
   private int repairingI;

   public AutoEXP() {
      super(Categories.Combat, "auto-exp", "Automatically repairs your armor and tools in pvp.");
   }

   @Override
   public void onActivate() {
      this.repairingI = -1;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (!this.onlyGround.get() || this.mc.player.onGround()) {
         if (this.repairingI == -1) {
            if (this.mode.get() != AutoEXP.Mode.Hands) {
               for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
                  ItemStack stack = this.mc.player.getItemBySlot(slot);
                  if (this.needsRepair(stack, this.minThreshold.get().intValue())) {
                     this.repairingI = 36 + slot.getIndex();
                     break;
                  }
               }
            }

            if (this.mode.get() != AutoEXP.Mode.Armor && this.repairingI == -1) {
               for (InteractionHand hand : InteractionHand.values()) {
                  if (this.needsRepair(this.mc.player.getItemInHand(hand), this.minThreshold.get().intValue())) {
                     this.repairingI = hand == InteractionHand.MAIN_HAND ? this.mc.player.getInventory().getSelectedSlot() : 40;
                     break;
                  }
               }
            }
         }

         if (this.repairingI != -1) {
            if (!this.needsRepair(this.mc.player.getInventory().getItem(this.repairingI), this.maxThreshold.get().intValue())) {
               this.repairingI = -1;
               return;
            }

            FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);
            if (exp.found()) {
               if (!exp.isHotbar() && !exp.isOffhand()) {
                  if (!this.replenish.get()) {
                     return;
                  }

                  InvUtils.move().from(exp.slot()).toHotbar(this.slot.get() - 1);
               }

               Rotations.rotate(this.mc.player.getYRot(), 90.0, () -> {
                  if (exp.getHand() != null) {
                     this.mc.gameMode.useItem(this.mc.player, exp.getHand());
                  } else {
                     InvUtils.swap(exp.slot(), true);
                     this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
                     InvUtils.swapBack();
                  }
               });
            }
         }
      }
   }

   private boolean needsRepair(ItemStack itemStack, double threshold) {
      return !itemStack.isEmpty() && Utils.hasEnchantments(itemStack, Enchantments.MENDING)
         ? (double)(itemStack.getMaxDamage() - itemStack.getDamageValue()) / itemStack.getMaxDamage() * 100.0 <= threshold
         : false;
   }

   public enum Mode {
      Armor,
      Hands,
      Both;
   }
}
