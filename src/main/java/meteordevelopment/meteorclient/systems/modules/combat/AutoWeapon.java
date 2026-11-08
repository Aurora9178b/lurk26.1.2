package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

public class AutoWeapon extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoWeapon.Weapon> weapon = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("weapon")).description("What type of weapon to use."))
               .defaultValue(AutoWeapon.Weapon.Sword))
            .build()
      );
   private final Setting<Integer> threshold = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("threshold")
            .description("If the non-preferred weapon produces this much damage this will favor it over your preferred weapon.")
            .defaultValue(4)
            .build()
      );
   private final Setting<Boolean> antiBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-break").description("Prevents you from breaking your weapon.").defaultValue(false).build());

   public AutoWeapon() {
      super(Categories.Combat, "auto-weapon", "Finds the best weapon to use in your hotbar.");
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (event.entity instanceof LivingEntity livingEntity) {
         InvUtils.swap(this.getBestWeapon(livingEntity), false);
      }
   }

   private int getBestWeapon(LivingEntity target) {
      int slotS = this.mc.player.getInventory().getSelectedSlot();
      int slotA = this.mc.player.getInventory().getSelectedSlot();
      double damageS = 0.0;
      double damageA = 0.0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (!stack.is(ItemTags.SWORDS) || this.antiBreak.get() && stack.getMaxDamage() - stack.getDamageValue() <= 10) {
            if (stack.getItem() instanceof AxeItem && (!this.antiBreak.get() || stack.getMaxDamage() - stack.getDamageValue() > 10)) {
               double currentDamageA = DamageUtils.getAttackDamage(this.mc.player, target, stack);
               if (currentDamageA > damageA) {
                  damageA = currentDamageA;
                  slotA = i;
               }
            }
         } else {
            double currentDamageS = DamageUtils.getAttackDamage(this.mc.player, target, stack);
            if (currentDamageS > damageS) {
               damageS = currentDamageS;
               slotS = i;
            }
         }
      }

      if (this.weapon.get() == AutoWeapon.Weapon.Sword && this.threshold.get().intValue() > damageA - damageS) {
         return slotS;
      } else if (this.weapon.get() == AutoWeapon.Weapon.Axe && this.threshold.get().intValue() > damageS - damageA) {
         return slotA;
      } else if (this.weapon.get() == AutoWeapon.Weapon.Sword && this.threshold.get().intValue() < damageA - damageS) {
         return slotA;
      } else {
         return this.weapon.get() == AutoWeapon.Weapon.Axe && this.threshold.get().intValue() < damageS - damageA
            ? slotS
            : this.mc.player.getInventory().getSelectedSlot();
      }
   }

   public enum Weapon {
      Sword,
      Axe;
   }
}
