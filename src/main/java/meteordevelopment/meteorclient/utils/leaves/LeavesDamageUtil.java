package meteordevelopment.meteorclient.utils.leaves;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.function.BiFunction;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class LeavesDamageUtil {
   public static final LeavesDamageUtil.RaycastFactory HIT_FACTORY = (context, blockPos) -> {
      BlockState blockState = LeavesCombatUtil.modifyPos != null && LeavesCombatUtil.modifyPos.equals(blockPos)
         ? LeavesCombatUtil.modifyBlockState
         : MeteorClient.mc.level.getBlockState(blockPos);
      return blockState.getBlock().getExplosionResistance() < 600.0F
         ? null
         : blockState.getCollisionShape(MeteorClient.mc.level, blockPos).clip(context.start(), context.end(), blockPos);
   };

   private LeavesDamageUtil() {
   }

   public static float calculateDamage(BlockPos pos, LivingEntity entity) {
      return explosionDamage(entity, null, new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5), 12.0F);
   }

   public static float calculateDamage(Vec3 pos, LivingEntity entity) {
      return explosionDamage(entity, null, pos, 12.0F);
   }

   public static float anchorDamage(LivingEntity target, Vec3 anchor) {
      return overridingExplosionDamage(target, null, anchor, 10.0F, BlockPos.containing(anchor), Blocks.AIR.defaultBlockState());
   }

   public static float overridingExplosionDamage(
      LivingEntity target, LivingEntity predict, Vec3 explosionPos, float power, BlockPos overridePos, BlockState overrideState
   ) {
      return explosionDamage(target, predict, explosionPos, power, getOverridingHitFactory(overridePos, overrideState));
   }

   private static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3 explosionPos, float power) {
      return explosionDamage(target, predict, explosionPos, power, HIT_FACTORY);
   }

   private static float explosionDamage(
      LivingEntity target, LivingEntity predict, Vec3 explosionPos, float power, LeavesDamageUtil.RaycastFactory raycastFactory
   ) {
      if (MeteorClient.mc.level != null && target != null) {
         if (target instanceof Player player && getGameMode(player) == GameType.CREATIVE) {
            return 0.0F;
         } else {
            Vec3 targetPos = predict != null ? predict.position() : target.position();
            AABB targetBox = predict != null ? predict.getBoundingBox() : target.getBoundingBox();
            return explosionDamage(target, targetPos, targetBox, explosionPos, power, raycastFactory);
         }
      } else {
         return 0.0F;
      }
   }

   public static float explosionDamage(
      LivingEntity target, Vec3 targetPos, AABB targetBox, Vec3 explosionPos, float power, LeavesDamageUtil.RaycastFactory raycastFactory
   ) {
      double distance = distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
      if (distance > power) {
         return 0.0F;
      }

      double exposure = getExposure(explosionPos, targetBox, raycastFactory);
      double impact = (1.0 - distance / power) * exposure;
      float damage = (int)((impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0);
      return calculateReductions(damage, target, MeteorClient.mc.level.damageSources().explosion(null));
   }

   public static LeavesDamageUtil.RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
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

   public static float calculateReductions(float damage, LivingEntity entity, DamageSource source) {
      if (source.scalesWithDifficulty()) {
         switch (MeteorClient.mc.level.getDifficulty()) {
            case EASY:
               damage = Math.min(damage / 2.0F + 1.0F, damage);
               break;
            case HARD:
               damage *= 1.5F;
         }
      }

      damage = CombatRules.getDamageAfterAbsorb(entity, damage, source, getArmor(entity), (float)entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      damage = resistanceReduction(entity, damage);
      damage = protectionReduction(entity, damage, source);
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
         int level = resistance.getAmplifier() + 1;
         damage *= 1.0F - level * 0.2F;
      }

      return Math.max(damage, 0.0F);
   }

   private static float getExposure(Vec3 source, AABB box, LeavesDamageUtil.RaycastFactory raycastFactory) {
      double xDiff = box.maxX - box.minX;
      double yDiff = box.maxY - box.minY;
      double zDiff = box.maxZ - box.minZ;
      double xStep = 1.0 / (xDiff * 2.0 + 1.0);
      double yStep = 1.0 / (yDiff * 2.0 + 1.0);
      double zStep = 1.0 / (zDiff * 2.0 + 1.0);
      if (!(xStep <= 0.0) && !(yStep <= 0.0) && !(zStep <= 0.0)) {
         int misses = 0;
         int hits = 0;
         double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) * 0.5;
         double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) * 0.5;
         xStep *= xDiff;
         yStep *= yDiff;
         zStep *= zDiff;

         for (double x = box.minX + xOffset; x <= box.maxX + xOffset; x += xStep) {
            for (double y = box.minY; y <= box.maxY; y += yStep) {
               for (double z = box.minZ + zOffset; z <= box.maxZ + zOffset; z += zStep) {
                  Vec3 position = new Vec3(x, y, z);
                  if (raycast(new LeavesDamageUtil.ExposureRaycastContext(position, source), raycastFactory) == null) {
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

   private static BlockHitResult raycast(LeavesDamageUtil.ExposureRaycastContext context, LeavesDamageUtil.RaycastFactory raycastFactory) {
      return BlockGetter.traverseBlocks(context.start, context.end, context, raycastFactory, var0x -> null);
   }

   public static GameType getGameMode(Player player) {
      if (MeteorClient.mc.getConnection() != null && player != null) {
         PlayerInfo playerListEntry = MeteorClient.mc.getConnection().getPlayerInfo(player.getUUID());
         return playerListEntry == null ? null : playerListEntry.getGameMode();
      } else {
         return null;
      }
   }

   public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double x = x1 - x2;
      double y = y1 - y2;
      double z = z1 - z2;
      return Math.sqrt(x * x + y * y + z * z);
   }

   public record ExposureRaycastContext(Vec3 start, Vec3 end) {
   }

   @FunctionalInterface
   public interface RaycastFactory extends BiFunction<LeavesDamageUtil.ExposureRaycastContext, BlockPos, BlockHitResult> {
   }
}
