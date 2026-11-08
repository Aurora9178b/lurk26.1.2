package meteordevelopment.meteorclient.utils.entity;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiFunction;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class DamageUtils {
   public static final DamageUtils.RaycastFactory HIT_FACTORY = (context, blockPos) -> {
      BlockState blockState = MeteorClient.mc.level.getBlockState(blockPos);
      return blockState.getBlock().getExplosionResistance() < 600.0F
         ? null
         : blockState.getCollisionShape(MeteorClient.mc.level, blockPos).clip(context.start(), context.end(), blockPos);
   };

   private DamageUtils() {
   }

   public static float crystalDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, DamageUtils.RaycastFactory raycastFactory) {
      return explosionDamage(target, targetPos, targetBox, explosionPos, 12.0F, raycastFactory);
   }

   public static float bedDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, DamageUtils.RaycastFactory raycastFactory) {
      return explosionDamage(target, targetPos, targetBox, explosionPos, 10.0F, raycastFactory);
   }

   public static float anchorDamage(LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, DamageUtils.RaycastFactory raycastFactory) {
      return explosionDamage(target, targetPos, targetBox, explosionPos, 10.0F, raycastFactory);
   }

   public static float explosionDamage(
      LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, float power, DamageUtils.RaycastFactory raycastFactory
   ) {
      double modDistance = PlayerUtils.distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
      if (modDistance > power) {
         return 0.0F;
      }

      double exposure = getExposure(explosionPos, targetBox, raycastFactory);
      double impact = (1.0 - modDistance / power) * exposure;
      float damage = (int)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
      return calculateReductions(damage, target, MeteorClient.mc.level.damageSources().explosion(null));
   }

   public static float crystalDamage(LivingEntity target, Vec3 crystal, boolean predictMovement, BlockPos obsidianPos) {
      return overridingExplosionDamage(target, crystal, 12.0F, predictMovement, obsidianPos, Blocks.OBSIDIAN.defaultBlockState());
   }

   public static float crystalDamage(LivingEntity target, Vec3 crystal) {
      return explosionDamage(target, crystal, 12.0F, false);
   }

   public static float bedDamage(LivingEntity target, Vec3 bed) {
      return explosionDamage(target, bed, 10.0F, false);
   }

   public static float anchorDamage(LivingEntity target, Vec3 anchor) {
      return overridingExplosionDamage(target, anchor, 10.0F, false, BlockPos.containing(anchor), Blocks.AIR.defaultBlockState());
   }

   private static float overridingExplosionDamage(
      LivingEntity target, Vec3 explosionPos, float power, boolean predictMovement, BlockPos overridePos, BlockState overrideState
   ) {
      return explosionDamage(target, explosionPos, power, predictMovement, getOverridingHitFactory(overridePos, overrideState));
   }

   private static float explosionDamage(LivingEntity target, Vec3 explosionPos, float power, boolean predictMovement) {
      return explosionDamage(target, explosionPos, power, predictMovement, HIT_FACTORY);
   }

   private static float explosionDamage(LivingEntity target, Vec3 explosionPos, float power, boolean predictMovement, DamageUtils.RaycastFactory raycastFactory) {
      if (target == null) {
         return 0.0F;
      } else if (target instanceof Player player && EntityUtils.getGameMode(player) == GameType.CREATIVE && !(player instanceof FakePlayerEntity)) {
         return 0.0F;
      } else {
         Vec3 position = predictMovement ? target.position().add(target.getDeltaMovement()) : target.position();
         AABB box = target.getBoundingBox();
         if (predictMovement) {
            box = box.move(target.getDeltaMovement());
         }

         return explosionDamage(target, position, box, explosionPos, power, raycastFactory);
      }
   }

   public static DamageUtils.RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
      return (context, blockPos) -> {
         BlockState blockState;
         if (blockPos.equals(overridePos)) {
            blockState = overrideState;
         } else {
            blockState = MeteorClient.mc.level.getBlockState(blockPos);
            if (blockState.getBlock().getExplosionResistance() < 600.0F) {
               return null;
            }
         }

         return blockState.getCollisionShape(MeteorClient.mc.level, blockPos).clip(context.start(), context.end(), blockPos);
      };
   }

   public static float getAttackDamage(LivingEntity attacker, Entity target) {
      float itemDamage = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
      DamageSource damageSource = attacker instanceof Player player
         ? MeteorClient.mc.level.damageSources().playerAttack(player)
         : MeteorClient.mc.level.damageSources().mobAttack(attacker);
      float damage = modifyAttackDamage(attacker, target, attacker.getWeaponItem(), damageSource, itemDamage);
      return calculateReductions(damage, target, damageSource);
   }

   public static float getAttackDamage(LivingEntity attacker, Entity target, ItemStack weapon) {
      AttributeInstance original = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
      AttributeInstance copy = new AttributeInstance(Attributes.ATTACK_DAMAGE, var0x -> {});
      copy.setBaseValue(original.getBaseValue());

      for (AttributeModifier modifier : original.getModifiers()) {
         copy.addTransientModifier(modifier);
      }

      copy.removeModifier(Item.BASE_ATTACK_DAMAGE_ID);
      ItemAttributeModifiers attributeModifiers = weapon.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (attributeModifiers != null) {
         attributeModifiers.forEach(EquipmentSlot.MAINHAND, (entry, modifier) -> {
            if (entry == Attributes.ATTACK_DAMAGE) {
               copy.addOrUpdateTransientModifier(modifier);
            }
         });
      }

      float itemDamage = (float)copy.getValue();
      DamageSource damageSource = attacker instanceof Player player
         ? MeteorClient.mc.level.damageSources().playerAttack(player)
         : MeteorClient.mc.level.damageSources().mobAttack(attacker);
      float damage = modifyAttackDamage(attacker, target, weapon, damageSource, itemDamage);
      return calculateReductions(damage, target, damageSource);
   }

   private static float modifyAttackDamage(LivingEntity attacker, Entity target, ItemStack weapon, DamageSource damageSource, float damage) {
      Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
      Utils.getEnchantments(weapon, enchantments);
      float enchantDamage = 0.0F;
      int sharpness = Utils.getEnchantmentLevel(enchantments, Enchantments.SHARPNESS);
      if (sharpness > 0) {
         enchantDamage += 1.0F + 0.5F * (sharpness - 1);
      }

      int baneOfArthropods = Utils.getEnchantmentLevel(enchantments, Enchantments.BANE_OF_ARTHROPODS);
      if (baneOfArthropods > 0 && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
         enchantDamage += 2.5F * baneOfArthropods;
      }

      int impaling = Utils.getEnchantmentLevel(enchantments, Enchantments.IMPALING);
      if (impaling > 0 && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
         enchantDamage += 2.5F * impaling;
      }

      int smite = Utils.getEnchantmentLevel(enchantments, Enchantments.SMITE);
      if (smite > 0 && target.typeHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) {
         enchantDamage += 2.5F * smite;
      }

      if (attacker instanceof Player playerEntity) {
         float charge = playerEntity.getAttackStrengthScale(0.5F);
         damage *= 0.2F + charge * charge * 0.8F;
         enchantDamage *= charge;
         if (weapon.getItem() instanceof MaceItem item) {
            float bonusDamage = item.getAttackDamageBonus(target, damage, damageSource);
            if (bonusDamage > 0.0F) {
               int density = Utils.getEnchantmentLevel(weapon, Enchantments.DENSITY);
               if (density > 0) {
                  bonusDamage += (float)(0.5 * attacker.fallDistance);
               }

               damage += bonusDamage;
            }
         }

         if (charge > 0.9F
            && attacker.fallDistance > 0.0
            && !attacker.onGround()
            && !attacker.onClimbable()
            && !attacker.isInWater()
            && !attacker.hasEffect(MobEffects.BLINDNESS)
            && !attacker.isPassenger()) {
            damage *= 1.5F;
         }
      }

      return damage + enchantDamage;
   }

   public static float fallDamage(LivingEntity entity) {
      if (entity instanceof Player player && player.getAbilities().flying) {
         return 0.0F;
      } else if (!entity.hasEffect(MobEffects.SLOW_FALLING) && !entity.hasEffect(MobEffects.LEVITATION)) {
         int surface = MeteorClient.mc
            .level
            .getChunkAt(entity.blockPosition())
            .getOrCreateHeightmapUnprimed(Types.MOTION_BLOCKING)
            .getFirstAvailable(entity.getBlockX() & 15, entity.getBlockZ() & 15);
         if (entity.getBlockY() >= surface) {
            return fallDamageReductions(entity, surface);
         }

         BlockHitResult raycastResult = MeteorClient.mc
            .level
            .clip(
               new ClipContext(entity.position(), new Vec3(entity.getX(), MeteorClient.mc.level.getMinY(), entity.getZ()), Block.COLLIDER, Fluid.WATER, entity)
            );
         return raycastResult.getType() == Type.MISS ? 0.0F : fallDamageReductions(entity, raycastResult.getBlockPos().getY());
      } else {
         return 0.0F;
      }
   }

   private static float fallDamageReductions(LivingEntity entity, int surface) {
      int fallHeight = (int)(entity.getY() - surface + entity.fallDistance - 3.0);
      MobEffectInstance jumpBoostInstance = entity.getEffect(MobEffects.JUMP_BOOST);
      if (jumpBoostInstance != null) {
         fallHeight -= jumpBoostInstance.getAmplifier() + 1;
      }

      return calculateReductions(fallHeight, entity, MeteorClient.mc.level.damageSources().fall());
   }

   public static float calculateReductions(float damage, Entity entity, DamageSource damageSource) {
      if (damageSource.scalesWithDifficulty()) {
         switch (MeteorClient.mc.level.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0F + 1.0F, damage);
               break;
            case HARD:
               damage *= 1.5F;
         }
      }

      if (entity instanceof LivingEntity livingEntity) {
         damage = CombatRules.getDamageAfterAbsorb(
            livingEntity, damage, damageSource, getArmor(livingEntity), (float)livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
         );
         damage = resistanceReduction(livingEntity, damage);
         damage = protectionReduction(livingEntity, damage, damageSource);
      }

      return Math.max(damage, 0.0F);
   }

   private static float getArmor(LivingEntity entity) {
      return (float)Math.floor(entity.getAttributeValue(Attributes.ARMOR));
   }

   private static float protectionReduction(LivingEntity player, float damage, DamageSource source) {
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return damage;
      }

      int damageProtection = 0;

      for (EquipmentSlot slot : EquipmentSlotGroup.ARMOR) {
         ItemStack stack = player.getItemBySlot(slot);
         Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
         Utils.getEnchantments(stack, enchantments);
         int protection = Utils.getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
         if (protection > 0) {
            damageProtection += protection;
         }

         int fireProtection = Utils.getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION);
         if (fireProtection > 0 && source.is(DamageTypeTags.IS_FIRE)) {
            damageProtection += 2 * fireProtection;
         }

         int blastProtection = Utils.getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION);
         if (blastProtection > 0 && source.is(DamageTypeTags.IS_EXPLOSION)) {
            damageProtection += 2 * blastProtection;
         }

         int projectileProtection = Utils.getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION);
         if (projectileProtection > 0 && source.is(DamageTypeTags.IS_PROJECTILE)) {
            damageProtection += 2 * projectileProtection;
         }

         int featherFalling = Utils.getEnchantmentLevel(enchantments, Enchantments.FEATHER_FALLING);
         if (featherFalling > 0 && source.is(DamageTypeTags.IS_FALL)) {
            damageProtection += 3 * featherFalling;
         }
      }

      return CombatRules.getDamageAfterMagicAbsorb(damage, damageProtection);
   }

   private static float resistanceReduction(LivingEntity player, float damage) {
      MobEffectInstance resistance = player.getEffect(MobEffects.RESISTANCE);
      if (resistance != null) {
         int lvl = resistance.getAmplifier() + 1;
         damage *= 1.0F - lvl * 0.2F;
      }

      return Math.max(damage, 0.0F);
   }

   private static float getExposure(Vec3 source, AABB box, DamageUtils.RaycastFactory raycastFactory) {
      double xDiff = box.maxX - box.minX;
      double yDiff = box.maxY - box.minY;
      double zDiff = box.maxZ - box.minZ;
      double xStep = 1.0 / (xDiff * 2.0 + 1.0);
      double yStep = 1.0 / (yDiff * 2.0 + 1.0);
      double zStep = 1.0 / (zDiff * 2.0 + 1.0);
      if (xStep > 0.0 && yStep > 0.0 && zStep > 0.0) {
         int misses = 0;
         int hits = 0;
         double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
         double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
         xStep *= xDiff;
         yStep *= yDiff;
         zStep *= zDiff;
         double startX = box.minX + xOffset;
         double startY = box.minY;
         double startZ = box.minZ + zOffset;
         double endX = box.maxX + xOffset;
         double endY = box.maxY;
         double endZ = box.maxZ + zOffset;

         for (double x = startX; x <= endX; x += xStep) {
            for (double y = startY; y <= endY; y += yStep) {
               for (double z = startZ; z <= endZ; z += zStep) {
                  Vec3 position = new Vec3(x, y, z);
                  if (raycast(new DamageUtils.ExposureRaycastContext(position, source), raycastFactory) == null) {
                     misses++;
                  }

                  hits++;
               }
            }
         }

         return (float)misses / hits;
      } else {
         return 0.0F;
      }
   }

   private static BlockHitResult raycast(DamageUtils.ExposureRaycastContext context, DamageUtils.RaycastFactory raycastFactory) {
      return BlockGetter.traverseBlocks(context.start, context.end, context, raycastFactory, var0x -> null);
   }

   public record ExposureRaycastContext(Vec3 start, Vec3 end) {
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<DamageUtils.ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
