package meteordevelopment.meteorclient.utils.leaves;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LeavesCombatUtil {
   public static BlockPos modifyPos;
   public static BlockState modifyBlockState;

   private LeavesCombatUtil() {
   }

   public static List<Player> getEnemies(double range) {
      List<Player> list = new ArrayList<>();
      if (MeteorClient.mc.level == null) {
         return list;
      }

      for (Player player : MeteorClient.mc.level.players()) {
         if (isValid(player, range)) {
            list.add(player);
         }
      }

      return list;
   }

   public static boolean isValid(Entity entity, double range) {
      if (MeteorClient.mc.player != null && entity != null && entity.isAlive() && !entity.equals(MeteorClient.mc.player)) {
         return entity instanceof Player player && Friends.get().isFriend(player)
            ? false
            : MeteorClient.mc.player.position().distanceTo(entity.position()) <= range;
      } else {
         return false;
      }
   }

   public static Player getClosestEnemy(double distance) {
      Player closest = null;
      if (MeteorClient.mc.player == null) {
         return null;
      }

      for (Player player : getEnemies(distance)) {
         if (closest == null || MeteorClient.mc.player.distanceToSqr(player) < MeteorClient.mc.player.distanceToSqr(closest)) {
            closest = player;
         }
      }

      return closest;
   }

   public static void attackCrystal(BlockPos pos, boolean rotate, boolean eatingPause) {
      attackCrystal(new AABB(pos), rotate, eatingPause);
   }

   public static void attackCrystal(AABB box, boolean rotate, boolean eatingPause) {
      Iterator var3 = LeavesBlockUtil.getEndCrystals(box).iterator();
      if (var3.hasNext()) {
         EndCrystal crystal = (EndCrystal)var3.next();
         attackCrystal(crystal, rotate, eatingPause);
      }
   }

   public static void attackCrystal(Entity crystal, boolean rotate, boolean eatingPause) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null && MeteorClient.mc.getConnection() != null && crystal != null) {
         if (!eatingPause || !MeteorClient.mc.player.isUsingItem()) {
            if (rotate) {
               LeavesRotation.snapAt(new Vec3(crystal.getX(), crystal.getY() + 0.25, crystal.getZ()));
            }

            MeteorClient.mc.gameMode.attack(MeteorClient.mc.player, crystal);
            MeteorClient.mc.player.resetAttackStrengthTicker();
            LeavesEntityUtil.attackSwingHand();
            if (rotate) {
               LeavesRotation.snapBack();
            }
         }
      }
   }
}
