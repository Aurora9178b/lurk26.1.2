package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.DoAttackEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AttributeSwap extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSwappingOptions = this.settings.createGroup("Swapping Options");
   private final SettingGroup sgSwordEnchants = this.settings.createGroup("Sword Enchants");
   private final SettingGroup sgMaceEnchants = this.settings.createGroup("Mace Enchants");
   private final SettingGroup sgSpearEnchants = this.settings.createGroup("Spear Enchants");
   private final SettingGroup sgOtherEnchants = this.settings.createGroup("Other Enchants");
   private final SettingGroup sgWeapon = this.settings.createGroup("Weapon Options");
   private final Setting<AttributeSwap.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The mode to use."))
               .defaultValue(AttributeSwap.Mode.Simple))
            .build()
      );
   private final Setting<Integer> targetSlot = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("target-slot")
            .description("Hotbar slot to swap to (1-9).")
            .defaultValue(1)
            .range(1, 9)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Simple)
            .build()
      );
   private final Setting<Boolean> swapOnMiss = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("swap-on-miss")
            .description("Whether to swap on a missed attack. Useful for quickly lunging with spears.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Simple)
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(new BoolSetting.Builder().name("swap-back").description("Swap back to the original slot after a delay.").defaultValue(true).build());
   private final Setting<Integer> swapBackDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("swap-back-delay")
            .description("Delay in ticks before swapping back.")
            .defaultValue(2)
            .min(0)
            .max(100)
            .sliderRange(0, 20)
            .visible(this.swapBack::get)
            .build()
      );
   private final Setting<Boolean> smartShieldBreak = this.sgSwappingOptions
      .add(
         new BoolSetting.Builder()
            .name("shield-breaker")
            .description("Automatically swaps to an axe if the target is blocking.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> smartDurability = this.sgSwappingOptions
      .add(
         new BoolSetting.Builder()
            .name("durability-saver")
            .description("Swaps to a non-damageable item to save durability on the main weapon.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> swordSwapping = this.sgSwappingOptions
      .add(
         new BoolSetting.Builder()
            .name("sword-swapping")
            .description("Enables smart swapping for sword enchantments.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> maceSwapping = this.sgSwappingOptions
      .add(
         new BoolSetting.Builder()
            .name("mace-swapping")
            .description("Enables smart swapping for mace enchantments.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> spearSwapping = this.sgSwappingOptions
      .add(
         new BoolSetting.Builder()
            .name("spear-swapping")
            .description("Enables smart swapping for spear enchantments.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> otherSwapping = this.sgSwappingOptions
      .add(
         new BoolSetting.Builder()
            .name("other-swapping")
            .description("Enables smart swapping for other enchantments like Impaling.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> enchantFireAspect = this.sgSwordEnchants
      .add(
         new BoolSetting.Builder()
            .name("fire-aspect")
            .description("Swaps to an item with Fire Aspect to set the target on fire, if target isn't already on fire")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.swordSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantLooting = this.sgSwordEnchants
      .add(
         new BoolSetting.Builder()
            .name("looting")
            .description("Swaps to an item with Looting for better drops or more experience. Only prefers for mobs (but fire aspect is priority)")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.swordSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantSharpness = this.sgSwordEnchants
      .add(
         new BoolSetting.Builder()
            .name("sharpness")
            .description("Swaps to an item with Sharpness for increased damage against all entities.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.swordSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantSmite = this.sgSwordEnchants
      .add(
         new BoolSetting.Builder()
            .name("smite")
            .description("Swaps to an item with Smite for increased damage against undead mobs.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.swordSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantBaneOfArthropods = this.sgSwordEnchants
      .add(
         new BoolSetting.Builder()
            .name("bane-of-arthropods")
            .description("Swaps to an item with Bane of Arthropods for increased damage against arthropods.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.swordSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantSweepingEdge = this.sgSwordEnchants
      .add(
         new BoolSetting.Builder()
            .name("sweeping-edge")
            .description("Swaps to an item with Sweeping Edge for increased sweeping attack damage.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.swordSwapping.get())
            .build()
      );
   private final Setting<Boolean> regularMace = this.sgMaceEnchants
      .add(
         new BoolSetting.Builder()
            .name("regular-mace")
            .description("Swaps to a regular Mace when falling if no better option is available.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.maceSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantDensity = this.sgMaceEnchants
      .add(
         new BoolSetting.Builder()
            .name("density")
            .description("Swaps to a Mace with Density to deal increased damage when falling.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.maceSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantBreach = this.sgMaceEnchants
      .add(
         new BoolSetting.Builder()
            .name("breach")
            .description("Swaps to a Mace with Breach to reduce the target's armor effectiveness.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.maceSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantWindBurst = this.sgMaceEnchants
      .add(
         new BoolSetting.Builder()
            .name("wind-burst")
            .description("Swaps to a Mace with Wind Burst to launch up when hitting while falling.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.maceSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantImpaling = this.sgOtherEnchants
      .add(
         new BoolSetting.Builder()
            .name("impaling")
            .description("Swaps to an item with Impaling for increased damage against aquatic mobs.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.otherSwapping.get())
            .build()
      );
   private final Setting<Boolean> enchantLunge = this.sgSpearEnchants
      .add(
         new BoolSetting.Builder()
            .name("lunge")
            .description("Swaps to a spear with Lunge for traveling.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.spearSwapping.get())
            .build()
      );
   private final Setting<Boolean> spearHitbox = this.sgSpearEnchants
      .add(
         new BoolSetting.Builder()
            .name("hitbox")
            .description("Swaps to a spear for extended reach when target is far.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.spearSwapping.get())
            .build()
      );
   private final Setting<Boolean> excludeLungeFromHitbox = this.sgSpearEnchants
      .add(
         new BoolSetting.Builder()
            .name("exclude-lunge-from-hitbox")
            .description("Don't use lunge-enchanted spears for hitbox extension.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AttributeSwap.Mode.Smart && this.spearSwapping.get() && this.spearHitbox.get())
            .build()
      );
   private final Setting<Boolean> onlyOnWeapon = this.sgWeapon
      .add(new BoolSetting.Builder().name("only-on-weapon").description("Only swaps when holding a selected weapon in hand.").defaultValue(false).build());
   private final Setting<Boolean> sword = this.sgWeapon
      .add(new BoolSetting.Builder().name("sword").description("Works while holding a sword.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> axe = this.sgWeapon
      .add(new BoolSetting.Builder().name("axe").description("Works while holding an axe.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> pickaxe = this.sgWeapon
      .add(new BoolSetting.Builder().name("pickaxe").description("Works while holding a pickaxe.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> shovel = this.sgWeapon
      .add(new BoolSetting.Builder().name("shovel").description("Works while holding a shovel.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> hoe = this.sgWeapon
      .add(new BoolSetting.Builder().name("hoe").description("Works while holding a hoe.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> mace = this.sgWeapon
      .add(new BoolSetting.Builder().name("mace").description("Works while holding a mace.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private final Setting<Boolean> trident = this.sgWeapon
      .add(new BoolSetting.Builder().name("trident").description("Works while holding a trident.").defaultValue(true).visible(this.onlyOnWeapon::get).build());
   private int backTimer;
   private boolean awaitingBack;

   public AttributeSwap() {
      super(Categories.Combat, "attribute-swap", "Swaps to a target slot when you attack.");
   }

   @Override
   public void onDeactivate() {
      this.backTimer = 0;
      this.awaitingBack = false;
   }

   @EventHandler
   private void onAttack(DoAttackEvent event) {
      if (this.mc.hitResult.getType() != Type.BLOCK && this.canSwapByWeapon()) {
         if (this.mode.get() == AttributeSwap.Mode.Smart && this.spearSwapping.get()) {
            if (this.spearHitbox.get()) {
               Entity target = this.getTargetEntity();
               if (target != null) {
                  if (this.mc.player.distanceTo(target) <= this.mc.player.entityInteractionRange() + 0.5) {
                     return;
                  }

                  int spearSlot = this.getSmartSpearSlot(false);
                  if (spearSlot != -1) {
                     this.doSwap(spearSlot);
                     return;
                  }
               }
            }

            if (this.enchantLunge.get()) {
               int lungeSlot = this.getSmartSpearSlot(true);
               if (lungeSlot != -1) {
                  this.doSwap(lungeSlot);
                  return;
               }
            }
         }

         if (this.mode.get() != AttributeSwap.Mode.Smart && this.swapOnMiss.get()) {
            this.doSwap(this.targetSlot.get() - 1);
         }
      }
   }

   @EventHandler
   private void onAttackEntity(AttackEntityEvent event) {
      if (this.canSwapByWeapon() && (this.mode.get() != AttributeSwap.Mode.Simple || !this.swapOnMiss.get())) {
         this.performSwap(event.entity);
      }
   }

   private void performSwap(Entity target) {
      if (!this.awaitingBack) {
         if (this.mode.get() == AttributeSwap.Mode.Simple) {
            this.doSwap(this.targetSlot.get() - 1);
         } else {
            this.doSwap(this.getSmartSlot(target));
         }
      }
   }

   private void doSwap(int slotIndex) {
      if (!this.awaitingBack) {
         if (slotIndex >= 0 && slotIndex <= 8) {
            if (slotIndex != this.mc.player.getInventory().getSelectedSlot()) {
               if (InvUtils.swap(slotIndex, this.swapBack.get())) {
                  this.awaitingBack = this.swapBack.get();
                  if (this.awaitingBack) {
                     this.backTimer = this.swapBackDelay.get();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.awaitingBack) {
         if (this.backTimer-- <= 0) {
            InvUtils.swapBack();
            this.awaitingBack = false;
         }
      }
   }

   private boolean canSwapByWeapon() {
      return !this.onlyOnWeapon.get()
         ? true
         : InvUtils.testInMainHand(
            item -> this.sword.get() && item.is(ItemTags.SWORDS)
               || this.axe.get() && item.is(ItemTags.AXES)
               || this.pickaxe.get() && item.is(ItemTags.PICKAXES)
               || this.shovel.get() && item.is(ItemTags.SHOVELS)
               || this.hoe.get() && item.is(ItemTags.HOES)
               || this.mace.get() && item.getItem() instanceof MaceItem
               || this.trident.get() && item.getItem() instanceof TridentItem
         );
   }

   private int getSmartSlot(Entity target) {
      ItemStack currentStack = this.mc.player.getMainHandItem();
      if (target != null && this.smartShieldBreak.get() && target instanceof LivingEntity living && living.isBlocking()) {
         if (currentStack.getItem() instanceof AxeItem) {
            return -1;
         }

         int axeSlot = InvUtils.findInHotbar(item -> item.getItem() instanceof AxeItem).slot();
         if (axeSlot != -1) {
            return axeSlot;
         }
      }

      boolean isFalling = this.mc.player.fallDistance > 1.5;
      boolean durability = this.smartDurability.get();
      boolean isLiving = target instanceof LivingEntity;
      boolean isPlayer = target instanceof Player;
      boolean isOnFire = target != null && target.isOnFire();
      boolean isUndead = target != null && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE);
      boolean isArthropod = target != null && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS);
      boolean isAquatic = target != null && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_IMPALING);
      boolean hasFireResistance = isLiving
         && (((LivingEntity)target).hasEffect(MobEffects.FIRE_RESISTANCE) || this.hasFireProtectionArmor((LivingEntity)target));
      double armor = isLiving ? ((LivingEntity)target).getAttributeValue(Attributes.ARMOR) : 0.0;
      float health = isLiving ? ((LivingEntity)target).getHealth() : 0.0F;
      int bestSlot = -1;
      double bestScore = this.getItemScore(
         currentStack, isFalling, durability, isLiving, isPlayer, isOnFire, hasFireResistance, isUndead, isArthropod, isAquatic, armor, health
      );

      for (int i = 0; i < 9; i++) {
         if (i != this.mc.player.getInventory().getSelectedSlot()) {
            ItemStack stack = this.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() || durability) {
               double score = this.getItemScore(
                  stack, isFalling, durability, isLiving, isPlayer, isOnFire, hasFireResistance, isUndead, isArthropod, isAquatic, armor, health
               );
               if (score > bestScore) {
                  bestScore = score;
                  bestSlot = i;
               }
            }
         }
      }

      return bestSlot;
   }

   private int getSmartSpearSlot(boolean requireLunge) {
      for (int i = 0; i < 9; i++) {
         if (i != this.mc.player.getInventory().getSelectedSlot()) {
            ItemStack stack = this.mc.player.getInventory().getItem(i);
            if (stack.is(ItemTags.SPEARS)) {
               boolean hasLunge = Utils.getEnchantmentLevel(stack, Enchantments.LUNGE) > 0;
               if ((!requireLunge || hasLunge) && (requireLunge || !this.excludeLungeFromHitbox.get() || !hasLunge)) {
                  return i;
               }
            }
         }
      }

      return -1;
   }

   private Entity getTargetEntity() {
      double maxDistance = 7.0;
      Vec3 start = this.mc.player.getEyePosition(1.0F);
      Vec3 look = this.mc.player.getViewVector(1.0F);
      Vec3 end = start.add(look.scale(maxDistance));
      AABB box = this.mc.player.getBoundingBox().expandTowards(look.scale(maxDistance)).inflate(1.0);
      Entity target = null;
      double closestDistance = maxDistance * maxDistance;

      for (Entity entity : this.mc.level.getEntities(this.mc.player, box, e -> !e.isSpectator() && e.isPickable())) {
         AABB expandedBox = entity.getBoundingBox().inflate(0.15);
         if (expandedBox.clip(start, end).isPresent()) {
            double distSq = start.distanceToSqr(entity.getX(), entity.getY(), entity.getZ());
            if (distSq < closestDistance) {
               closestDistance = distSq;
               target = entity;
            }
         }
      }

      return target;
   }

   private double getItemScore(
      ItemStack stack,
      boolean isFalling,
      boolean durability,
      boolean isLiving,
      boolean isPlayer,
      boolean isOnFire,
      boolean hasFireResistance,
      boolean isUndead,
      boolean isArthropod,
      boolean isAquatic,
      double armor,
      float health
   ) {
      double score = 0.0;
      if (durability) {
         score += this.getDurabilityScore(stack);
      }

      return stack.isEmpty()
         ? score
         : score + this.getCombatScore(stack, isFalling, isLiving, isPlayer, isOnFire, hasFireResistance, isUndead, isArthropod, isAquatic, armor, health);
   }

   private double getDurabilityScore(ItemStack stack) {
      if (!stack.isDamageableItem()) {
         return 4.0;
      }

      int unbreaking = Utils.getEnchantmentLevel(stack, Enchantments.UNBREAKING);
      return unbreaking > 0 ? unbreaking * 0.05 : 0.0;
   }

   private double getCombatScore(
      ItemStack stack,
      boolean isFalling,
      boolean isLiving,
      boolean isPlayer,
      boolean isOnFire,
      boolean hasFireResistance,
      boolean isUndead,
      boolean isArthropod,
      boolean isAquatic,
      double armor,
      float health
   ) {
      double score = 0.0;
      if (this.swordSwapping.get()) {
         score += this.getFireAspectScore(stack, isOnFire, hasFireResistance);
         score += this.getLootingScore(stack, isPlayer, isLiving, isOnFire, health);
         score += this.getSharpnessScore(stack, isOnFire);
         score += this.getSmiteScore(stack, isUndead, isOnFire);
         score += this.getBaneOfArthropodsScore(stack, isArthropod, isOnFire);
         score += this.getSweepingEdgeScore(stack);
      }

      if (this.maceSwapping.get()) {
         score += this.getBreachScore(stack, isLiving, armor);
         score += this.getDensityScore(stack, isFalling);
         score += this.getWindBurstScore(stack, isFalling);
         score += this.getMaceScore(stack, isFalling);
      }

      if (this.otherSwapping.get()) {
         score += this.getImpalingScore(stack, isAquatic);
      }

      return score;
   }

   private double getFireAspectScore(ItemStack stack, boolean isOnFire, boolean hasFireResistance) {
      if (this.enchantFireAspect.get() && !isOnFire && !hasFireResistance) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.FIRE_ASPECT);
         return level > 0 ? 30.0 : 0.0;
      } else {
         return 0.0;
      }
   }

   private double getLootingScore(ItemStack stack, boolean isPlayer, boolean isLiving, boolean isOnFire, float health) {
      if (this.enchantLooting.get() && !isPlayer) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.LOOTING);
         if (level <= 0) {
            return 0.0;
         }

         boolean execute = isLiving && health < 20.0F || isOnFire;
         return level * (execute ? 10 : 5);
      } else {
         return 0.0;
      }
   }

   private double getSharpnessScore(ItemStack stack, boolean isOnFire) {
      if (!this.enchantSharpness.get()) {
         return 0.0;
      } else {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.SHARPNESS);
         if (level > 0) {
            double baseScore = (1.0 + 0.5 * (level - 1)) * 3.0;
            return isOnFire ? baseScore * 1.5 : baseScore;
         } else {
            return 0.0;
         }
      }
   }

   private double getSmiteScore(ItemStack stack, boolean isUndead, boolean isOnFire) {
      if (this.enchantSmite.get() && isUndead) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.SMITE);
         if (level > 0) {
            double baseScore = level * 5;
            return isOnFire ? baseScore * 1.5 : baseScore;
         } else {
            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   private double getBaneOfArthropodsScore(ItemStack stack, boolean isArthropod, boolean isOnFire) {
      if (this.enchantBaneOfArthropods.get() && isArthropod) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS);
         if (level > 0) {
            double baseScore = level * 5;
            return isOnFire ? baseScore * 1.5 : baseScore;
         } else {
            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   private double getSweepingEdgeScore(ItemStack stack) {
      if (!this.enchantSweepingEdge.get()) {
         return 0.0;
      }

      int level = Utils.getEnchantmentLevel(stack, Enchantments.SWEEPING_EDGE);
      return level > 0 ? level * 3 : 0.0;
   }

   private double getImpalingScore(ItemStack stack, boolean isAquatic) {
      if (this.enchantImpaling.get() && isAquatic) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.IMPALING);
         return level > 0 ? level * 5 : 0.0;
      } else {
         return 0.0;
      }
   }

   private double getBreachScore(ItemStack stack, boolean isLiving, double armor) {
      if (this.enchantBreach.get() && isLiving && !(armor <= 0.0)) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.BREACH);
         return level > 0 ? level * armor * 0.3 : 0.0;
      } else {
         return 0.0;
      }
   }

   private double getDensityScore(ItemStack stack, boolean isFalling) {
      if (this.enchantDensity.get() && isFalling) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.DENSITY);
         return level > 0 ? 50.0 + level * this.mc.player.fallDistance * 2.0 : 0.0;
      } else {
         return 0.0;
      }
   }

   private double getWindBurstScore(ItemStack stack, boolean isFalling) {
      if (this.enchantWindBurst.get() && isFalling) {
         int level = Utils.getEnchantmentLevel(stack, Enchantments.WIND_BURST);
         return level > 0 ? level * 20 : 0.0;
      } else {
         return 0.0;
      }
   }

   private double getMaceScore(ItemStack stack, boolean isFalling) {
      if (!this.regularMace.get() || !isFalling) {
         return 0.0;
      } else {
         return stack.getItem() instanceof MaceItem ? 40.0 : 0.0;
      }
   }

   private boolean hasFireProtectionArmor(LivingEntity entity) {
      for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
         ItemStack stack = entity.getItemBySlot(slot);
         if (!stack.isEmpty()) {
            int fireProtection = Utils.getEnchantmentLevel(stack, Enchantments.FIRE_PROTECTION);
            if (fireProtection > 0) {
               return true;
            }
         }
      }

      return false;
   }

   public enum Mode {
      Simple,
      Smart;
   }
}
