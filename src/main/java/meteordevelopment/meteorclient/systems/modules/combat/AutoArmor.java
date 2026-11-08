package meteordevelopment.meteorclient.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class AutoArmor extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoArmor.Protection> preferredProtection = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("preferred-protection"))
                  .description("Which type of protection to prefer."))
               .defaultValue(AutoArmor.Protection.Protection))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("swap-delay").description("The delay between equipping armor pieces.").defaultValue(1).min(0).sliderMax(5).build());
   private final Setting<Set<ResourceKey<Enchantment>>> avoidedEnchantments = this.sgGeneral
      .add(
         new EnchantmentListSetting.Builder()
            .name("avoided-enchantments")
            .description("Enchantments that should be avoided.")
            .defaultValue(Enchantments.BINDING_CURSE, Enchantments.FROST_WALKER)
            .build()
      );
   private final Setting<Boolean> blastLeggings = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("blast-prot-leggings")
            .description("Uses blast protection for leggings regardless of preferred protection.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> antiBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("anti-break").description("Takes off armor if it is about to break.").defaultValue(false).build());
   private final Setting<Boolean> ignoreElytra = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-elytra").description("Will not replace your elytra if you have it equipped.").defaultValue(true).build());
   private final Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
   private final AutoArmor.ArmorPiece[] armorPieces = new AutoArmor.ArmorPiece[4];
   private final AutoArmor.ArmorPiece helmet = new AutoArmor.ArmorPiece(EquipmentSlot.HEAD);
   private final AutoArmor.ArmorPiece chestplate = new AutoArmor.ArmorPiece(EquipmentSlot.CHEST);
   private final AutoArmor.ArmorPiece leggings = new AutoArmor.ArmorPiece(EquipmentSlot.LEGS);
   private final AutoArmor.ArmorPiece boots = new AutoArmor.ArmorPiece(EquipmentSlot.FEET);
   private int timer;

   public AutoArmor() {
      super(Categories.Combat, "auto-armor", "Automatically equips armor.");
      this.armorPieces[0] = this.helmet;
      this.armorPieces[1] = this.chestplate;
      this.armorPieces[2] = this.leggings;
      this.armorPieces[3] = this.boots;
   }

   @Override
   public void onActivate() {
      this.timer = 0;
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      if (this.timer > 0) {
         this.timer--;
      } else {
         for (AutoArmor.ArmorPiece armorPiece : this.armorPieces) {
            armorPiece.reset();
         }

         for (int i = 0; i < this.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
            ItemStack itemStack = this.mc.player.getInventory().getItem(i);
            if (!itemStack.isEmpty()
               && this.isArmor(itemStack)
               && (!this.antiBreak.get() || !itemStack.isDamageableItem() || itemStack.getMaxDamage() - itemStack.getDamageValue() > 10)) {
               Utils.getEnchantments(itemStack, this.enchantments);
               if (!this.hasAvoidedEnchantment()) {
                  switch (this.getItemSlotId(itemStack)) {
                     case 0:
                        this.boots.add(itemStack, i);
                        break;
                     case 1:
                        this.leggings.add(itemStack, i);
                        break;
                     case 2:
                        this.chestplate.add(itemStack, i);
                        break;
                     case 3:
                        this.helmet.add(itemStack, i);
                  }
               }
            }
         }

         for (AutoArmor.ArmorPiece armorPiece : this.armorPieces) {
            armorPiece.calculate();
         }

         Arrays.sort(this.armorPieces, Comparator.comparingInt(AutoArmor.ArmorPiece::getSortScore));

         for (AutoArmor.ArmorPiece armorPiece : this.armorPieces) {
            armorPiece.apply();
         }
      }
   }

   private boolean hasAvoidedEnchantment() {
      for (Holder<Enchantment> enchantment : this.enchantments.keySet()) {
         if (enchantment.is(this.avoidedEnchantments.get()::contains)) {
            return true;
         }
      }

      return false;
   }

   private int getItemSlotId(ItemStack itemStack) {
      return itemStack.has(DataComponents.GLIDER) ? 2 : itemStack.get(DataComponents.EQUIPPABLE).slot().getIndex();
   }

   private int getScore(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0;
      }

      int score = 0;
      ResourceKey<Enchantment> protection = this.preferredProtection.get().enchantment;
      if (this.isArmor(itemStack) && this.blastLeggings.get() && this.getItemSlotId(itemStack) == 1) {
         protection = Enchantments.BLAST_PROTECTION;
      }

      score += 3 * Utils.getEnchantmentLevel(this.enchantments, protection);
      score += Utils.getEnchantmentLevel(this.enchantments, Enchantments.PROTECTION);
      score += Utils.getEnchantmentLevel(this.enchantments, Enchantments.BLAST_PROTECTION);
      score += Utils.getEnchantmentLevel(this.enchantments, Enchantments.FIRE_PROTECTION);
      score += Utils.getEnchantmentLevel(this.enchantments, Enchantments.PROJECTILE_PROTECTION);
      score += Utils.getEnchantmentLevel(this.enchantments, Enchantments.UNBREAKING);
      score += 2 * Utils.getEnchantmentLevel(this.enchantments, Enchantments.MENDING);
      if (itemStack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
         ItemAttributeModifiers component = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);

         for (Entry modifier : component.modifiers()) {
            if (modifier.attribute() == Attributes.ARMOR || modifier.attribute() == Attributes.ARMOR_TOUGHNESS) {
               double e = modifier.modifier().amount();

               score += switch (modifier.modifier().operation()) {
                  case ADD_VALUE -> (int)e;
                  case ADD_MULTIPLIED_BASE -> (int)(e * this.mc.player.getAttributeBaseValue(modifier.attribute()));
                  case ADD_MULTIPLIED_TOTAL -> (int)(e * score);
               };
            }
         }
      }

      return score;
   }

   private boolean cannotSwap() {
      return this.timer > 0;
   }

   private void swap(int from, int armorSlotId) {
      InvUtils.move().from(from).toArmor(armorSlotId);
      this.timer = this.delay.get();
   }

   private void moveToEmpty(int armorSlotId) {
      for (int i = 0; i < this.mc.player.getInventory().getNonEquipmentItems().size(); i++) {
         if (this.mc.player.getInventory().getItem(i).isEmpty()) {
            InvUtils.move().fromArmor(armorSlotId).to(i);
            this.timer = this.delay.get();
            break;
         }
      }
   }

   private boolean isArmor(ItemStack itemStack) {
      return itemStack.is(ItemTags.FOOT_ARMOR) || itemStack.is(ItemTags.LEG_ARMOR) || itemStack.is(ItemTags.CHEST_ARMOR) || itemStack.is(ItemTags.HEAD_ARMOR);
   }

   private class ArmorPiece {
      private final EquipmentSlot slot;
      private int bestSlot;
      private int bestScore;
      private int score;
      private int durability;

      public ArmorPiece(EquipmentSlot slot) {
         this.slot = slot;
      }

      public void reset() {
         this.bestSlot = -1;
         this.bestScore = -1;
         this.score = -1;
         this.durability = Integer.MAX_VALUE;
      }

      public void add(ItemStack itemStack, int slot) {
         int score = AutoArmor.this.getScore(itemStack);
         if (score > this.bestScore) {
            this.bestScore = score;
            this.bestSlot = slot;
         }
      }

      public void calculate() {
         if (!AutoArmor.this.cannotSwap()) {
            ItemStack itemStack = AutoArmor.this.mc.player.getItemBySlot(this.slot);
            if ((AutoArmor.this.ignoreElytra.get() || Modules.get().isActive(ChestSwap.class)) && itemStack.getItem() == Items.ELYTRA) {
               this.score = Integer.MAX_VALUE;
            } else {
               Utils.getEnchantments(itemStack, AutoArmor.this.enchantments);
               if (AutoArmor.this.enchantments.containsKey(Enchantments.BINDING_CURSE)) {
                  this.score = Integer.MAX_VALUE;
               } else {
                  this.score = AutoArmor.this.getScore(itemStack);
                  this.score = this.decreaseScoreByAvoidedEnchantments(this.score);
                  this.score = this.applyAntiBreakScore(this.score, itemStack);
                  if (!itemStack.isEmpty()) {
                     this.durability = itemStack.getMaxDamage() - itemStack.getDamageValue();
                  }
               }
            }
         }
      }

      public int getSortScore() {
         return AutoArmor.this.antiBreak.get() && this.durability <= 10 ? -1 : this.bestScore;
      }

      public void apply() {
         if (!AutoArmor.this.cannotSwap() && this.score != Integer.MAX_VALUE) {
            if (this.bestScore > this.score) {
               AutoArmor.this.swap(this.bestSlot, this.slot.getIndex());
            } else if (AutoArmor.this.antiBreak.get() && this.durability <= 10) {
               AutoArmor.this.moveToEmpty(this.slot.getIndex());
            }
         }
      }

      private int decreaseScoreByAvoidedEnchantments(int score) {
         for (ResourceKey<Enchantment> enchantment : AutoArmor.this.avoidedEnchantments.get()) {
            score -= 2 * AutoArmor.this.enchantments.getInt(enchantment);
         }

         return score;
      }

      private int applyAntiBreakScore(int score, ItemStack itemStack) {
         return AutoArmor.this.antiBreak.get() && itemStack.isDamageableItem() && itemStack.getMaxDamage() - itemStack.getDamageValue() <= 10 ? -1 : score;
      }
   }

   public enum Protection {
      Protection(Enchantments.PROTECTION),
      BlastProtection(Enchantments.BLAST_PROTECTION),
      FireProtection(Enchantments.FIRE_PROTECTION),
      ProjectileProtection(Enchantments.PROJECTILE_PROTECTION);

      private final ResourceKey<Enchantment> enchantment;

      Protection(ResourceKey<Enchantment> enchantment) {
         this.enchantment = enchantment;
      }
   }
}
