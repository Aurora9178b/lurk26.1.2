package meteordevelopment.meteorclient.utils.movement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.math.BlockUtil;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class QTPUtils {
   private static final ExecutorService threadPool = Executors.newFixedThreadPool(Math.max(3, Runtime.getRuntime().availableProcessors() / 2));

   public static boolean allowSendP() {
      return MeteorClient.mc != null && MeteorClient.mc.getConnection() != null;
   }

   public static void sendMovePacket(double x, double y, double z, boolean onGround) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
         MeteorClient.mc.getConnection().send(new Pos(x, y, z, onGround, MeteorClient.mc.player.horizontalCollision));
      }
   }

   public static void sendMovePacket(Vec3 pos, boolean onGround) {
      sendMovePacket(pos.x, pos.y, pos.z, onGround);
   }

   public static void boatMove(Vec3 pos) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
         Entity vehicle = MeteorClient.mc.player.getVehicle();
         if (vehicle instanceof AbstractBoat) {
            vehicle.setPos(pos.x, pos.y, pos.z);
            MeteorClient.mc
               .getConnection()
               .send(new ServerboundMoveVehiclePacket(new Vec3(vehicle.getX(), vehicle.getY(), vehicle.getZ()), vehicle.getYRot(), vehicle.getXRot(), false));
         }
      }
   }

   public static void boatMove$noSync(Vec3 pos) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
         Entity vehicle = MeteorClient.mc.player.getVehicle();
         if (vehicle instanceof AbstractBoat) {
            vehicle.setPos(pos.x, pos.y, pos.z);
            MeteorClient.mc
               .getConnection()
               .send(new ServerboundMoveVehiclePacket(new Vec3(vehicle.getX(), vehicle.getY(), vehicle.getZ()), vehicle.getYRot(), vehicle.getXRot(), false));
         }
      }
   }

   public static void doLagBack(Vec3 pos) {
      sendMovePacket(pos, true);
   }

   public static void doTp(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double maxMove, boolean onGround) {
      double dist = Math.sqrt(squaredDistance(fromX, fromY, fromZ, toX, toY, toZ));
      int steps = (int)Math.ceil(dist / maxMove);

      for (int i = 1; i <= steps; i++) {
         sendMovePacket(fromX, fromY, fromZ, onGround);
      }

      sendMovePacket(toX, toY, toZ, onGround);
   }

   public static void doTp(Vec3 from, Vec3 to, double maxMove) {
      doTp(from, to, maxMove, true);
   }

   public static void doTp(Vec3 from, Vec3 to, double maxMove, boolean onGround) {
      doTp(from.x, from.y, from.z, to.x, to.y, to.z, maxMove, onGround);
   }

   public static void doTpMove(
      Vec3 from,
      Vec3 to,
      double maxMove,
      boolean extraMove,
      int limitPacket,
      QTPUtils.TpVClipFindType findType,
      boolean extraMoveFlag,
      Consumer<Integer> action
   ) {
      if (MeteorClient.mc.getConnection() != null && MeteorClient.mc.player != null) {
         int maxPackets = TpSetting.get().maxPackets.get();
         if (BlockUtil.isBlinkVec(from) && BlockUtil.isBlinkVec(to)) {
            Vec3 goVec = findVClipVecToMove(from, to, findType);
            if (goVec != null) {
               Vec3 backVec = findVClipVecToMove(to, from, findType);
               if (backVec != null) {
                  int stepsToGo = (int)Math.ceil(from.distanceTo(goVec) / maxMove);
                  int stepsToBack = (int)Math.ceil(from.distanceTo(backVec) / maxMove);
                  int stepsToTarget = (int)Math.ceil(from.distanceTo(to) / maxMove);
                  int totalSteps = Math.max(Math.max(stepsToGo, stepsToBack), stepsToTarget) - 1 + 4;
                  QTPUtils.MovePacketCounter counter = new QTPUtils.MovePacketCounter();

                  for (int i = 1; i < totalSteps; i++) {
                     QTPUtils.MovePacketCounter.sendC04(counter, from.x, from.y, from.z);
                     if (counter.getPacketCount() >= maxPackets) {
                        ChatUtils.info("Packet limit reached: " + maxPackets);
                        return;
                     }
                  }

                  QTPUtils.MovePacketCounter.sendC04(counter, goVec.x, goVec.y, goVec.z);
                  QTPUtils.MovePacketCounter.sendC04(counter, to.x, to.y, to.z);
                  int extra = extraMove ? (extraMoveFlag ? totalSteps / 2 + 2 : 2) : 0;
                  action.accept(counter.getPacketCount() + extra);
                  if (extraMove) {
                     if (extraMoveFlag) {
                        for (int i = 1; i < totalSteps / 2; i++) {
                           QTPUtils.MovePacketCounter.sendC04(counter, to.x, to.y, to.z);
                           if (counter.getPacketCount() >= maxPackets) {
                              ChatUtils.info("Packet limit reached: " + maxPackets);
                              return;
                           }
                        }
                     }

                     QTPUtils.MovePacketCounter.sendC04(counter, backVec.x, backVec.y, backVec.z);
                     QTPUtils.MovePacketCounter.sendC04(counter, from.x, from.y, from.z);
                  } else {
                     MeteorClient.mc.player.setPos(to.x, to.y, to.z);
                  }
               }
            }
         }
      }
   }

   public static void doTpMove(Vec3 from, Vec3 to, double maxMove, boolean extraMove, Consumer<Integer> action) {
      doTpMove(from, to, maxMove, extraMove, 100, QTPUtils.TpVClipFindType.All, true, action);
   }

   public static Vec3 findVClipVecToMove(Vec3 from, Vec3 target, QTPUtils.TpVClipFindType type) {
      int minY = MeteorClient.mc.level == null ? -64 : MeteorClient.mc.level.dimensionType().minY();
      double step = 1.0;

      for (double yOffset = 0.0; yOffset <= 200.0; yOffset += step) {
         switch (type) {
            case All:
               Vec3 up = new Vec3(from.x, from.y + yOffset, from.z);
               if (up.y > minY && canSee(up, target)) {
                  return up;
               }

               Vec3 down = new Vec3(from.x, from.y - yOffset, from.z);
               if (down.y > minY && canSee(down, target)) {
                  return down;
               }
               break;
            case OnlyUp:
               Vec3 onlyUp = new Vec3(from.x, from.y + yOffset, from.z);
               if (onlyUp.y > minY && canSee(onlyUp, target)) {
                  return onlyUp;
               }
               break;
            case OnlyDown:
               Vec3 onlyDown = new Vec3(from.x, from.y - yOffset, from.z);
               if (onlyDown.y > minY && canSee(onlyDown, target)) {
                  return onlyDown;
               }
         }
      }

      return from;
   }

   public static boolean canSee(Vec3 from, Vec3 to) {
      if (MeteorClient.mc.level == null) {
         return false;
      }

      ClipContext context = new ClipContext(from, to, Block.OUTLINE, Fluid.NONE, MeteorClient.mc.player);
      HitResult result = MeteorClient.mc.level.clip(context);
      return result.getType() == Type.MISS;
   }

   public static List<BlockPos> aStarPath(
      BlockPos start, BlockPos goal, int searchRange, int maxIterations, QTPUtils.DistanceMetric metric, double gWeight, double hWeight
   ) {
      PriorityQueue<QTPUtils.Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(QTPUtils.Node::getFCost));
      Set<BlockPos> closedSet = new HashSet<>();
      Map<BlockPos, QTPUtils.Node> nodeMap = new HashMap<>();
      QTPUtils.Node startNode = new QTPUtils.Node(start, gWeight, hWeight);
      startNode.gCost = 0.0;
      QTPUtils.Node goalNode = new QTPUtils.Node(goal, gWeight, hWeight);
      nodeMap.put(start, startNode);
      nodeMap.put(goal, goalNode);
      openSet.add(startNode);

      while (!openSet.isEmpty() && closedSet.size() < maxIterations) {
         QTPUtils.Node current = openSet.poll();
         if (current.position.equals(goal)) {
            return constructPath(current);
         }

         closedSet.add(current.position);

         for (int dx = -searchRange; dx <= searchRange; dx++) {
            for (int dy = -searchRange; dy <= searchRange; dy++) {
               for (int dz = -searchRange; dz <= searchRange; dz++) {
                  if (dx != 0 || dy != 0 || dz != 0) {
                     BlockPos neighborPos = current.position.offset(dx, dy, dz);
                     if (!closedSet.contains(neighborPos) && isPassable(neighborPos)) {
                        QTPUtils.Node neighbor = nodeMap.computeIfAbsent(neighborPos, p -> new QTPUtils.Node(p, gWeight, hWeight));
                        double tentativeG = current.gCost + getDistance(current.position, neighborPos);
                        if (tentativeG < neighbor.gCost) {
                           neighbor.parent = current;
                           neighbor.gCost = tentativeG;
                           neighbor.hCost = getHeuristicCost(neighborPos, goal, metric);
                           neighbor.fCost = neighbor.gCost + neighbor.hCost;
                           if (!openSet.contains(neighbor)) {
                              openSet.add(neighbor);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return Collections.emptyList();
   }

   private static double getDistance(BlockPos a, BlockPos b) {
      return Math.sqrt(a.distSqr(b));
   }

   private static double getHeuristicCost(BlockPos from, BlockPos to, QTPUtils.DistanceMetric metric) {
      switch (metric) {
         case EUCLIDEAN:
            return getDistance(from, to);
         case MANHATTAN:
            return Math.abs(to.getX() - from.getX()) + Math.abs(to.getY() - from.getY()) + Math.abs(to.getZ() - from.getZ());
         case JPS:
            return getJPSHeuristic(from, to);
         default:
            return 0.0;
      }
   }

   private static double getJPSHeuristic(BlockPos from, BlockPos to) {
      int dx = Math.abs(to.getX() - from.getX());
      int dy = Math.abs(to.getY() - from.getY());
      int dz = Math.abs(to.getZ() - from.getZ());
      return Math.sqrt(dx * dx + dy * dy + dz * dz) + (dx + dy + dz);
   }

   private static boolean isPassable(BlockPos pos) {
      return BlockUtil.checkNoPosCollide(pos) && BlockUtil.checkNoPosCollide(pos.above());
   }

   private static List<BlockPos> constructPath(QTPUtils.Node node) {
      List<BlockPos> path = new ArrayList<>();

      while (node != null) {
         path.add(node.position);
         node = node.parent;
      }

      Collections.reverse(path);
      return path;
   }

   public static List<BlockPos> simplifyPath(List<BlockPos> path, double tolerance) {
      if (path.isEmpty()) {
         return new ArrayList<>();
      }

      if (path.size() == 1) {
         return new ArrayList<>(path);
      }

      List<BlockPos> simplified = new ArrayList<>();
      simplified.add(path.get(0));
      int i = 0;

      while (i < path.size() - 1) {
         BlockPos dir = path.get(i + 1).subtract(path.get(i));
         int lastGood = i + 1;

         for (int j = i + 2; j < path.size(); lastGood = j++) {
            BlockPos currentDir = path.get(j).subtract(path.get(j - 1));
            if (!currentDir.equals(dir) || path.get(i).distSqr(path.get(j)) > tolerance * tolerance) {
               break;
            }
         }

         simplified.add(path.get(lastGood));
         i = lastGood;
      }

      return simplified;
   }

   public static List<BlockPos> simplifyPathE(List<BlockPos> path) {
      if (path != null && path.size() >= 2) {
         List<BlockPos> result = new ArrayList<>();
         result.add(path.get(0));

         for (int i = 1; i < path.size() - 1; i++) {
            BlockPos prev = path.get(i - 1);
            BlockPos curr = path.get(i);
            BlockPos next = path.get(i + 1);
            Vec3 v1 = new Vec3(curr.getX() - prev.getX(), curr.getY() - prev.getY(), curr.getZ() - prev.getZ());
            Vec3 v2 = new Vec3(next.getX() - curr.getX(), next.getY() - curr.getY(), next.getZ() - curr.getZ());
            if (!v1.equals(v2)) {
               result.add(curr);
            }
         }

         result.add(path.get(path.size() - 1));
         return result;
      } else {
         return path;
      }
   }

   public static void findPathAsync(
      BlockPos start,
      BlockPos goal,
      int searchRange,
      int maxIterations,
      QTPUtils.DistanceMetric metric,
      double gWeight,
      double hWeight,
      Consumer<List<BlockPos>> callback
   ) {
      threadPool.submit(() -> {
         List<BlockPos> path = aStarPath(start, goal, searchRange, maxIterations, metric, gWeight, hWeight);
         callback.accept(path);
      });
   }

   public static double calculatePathLength(List<BlockPos> path) {
      double length = 0.0;

      for (int i = 1; i < path.size(); i++) {
         length += Vec3.atCenterOf(path.get(i - 1)).distanceTo(Vec3.atCenterOf(path.get(i)));
      }

      return length;
   }

   public static Vec3 getPrevPos(Entity entity, float ticks) {
      return new Vec3(
         entity.getX() + (entity.getX() - entity.xOld) * ticks,
         entity.getY() + (entity.getY() - entity.yOld) * ticks,
         entity.getZ() + (entity.getZ() - entity.zOld) * ticks
      );
   }

   private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      return dx * dx + dy * dy + dz * dz;
   }

   public enum DistanceMetric {
      EUCLIDEAN,
      MANHATTAN,
      JPS;
   }

   public static class MovePacketCounter {
      private int packetCount = 0;

      public int getPacketCount() {
         return this.packetCount;
      }

      public void setPacketCount(int packetCount) {
         this.packetCount = packetCount;
      }

      public void sendC04(double x, double y, double z, boolean onGround) {
         if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
            this.packetCount++;
            MeteorClient.mc.getConnection().send(new Pos(x, y, z, onGround, MeteorClient.mc.player.horizontalCollision));
         }
      }

      public static void sendC04(QTPUtils.MovePacketCounter counter, double x, double y, double z) {
         counter.sendC04(x, y, z, true);
      }

      public static void sendC04(QTPUtils.MovePacketCounter counter, Vec3 pos) {
         counter.sendC04(pos.x, pos.y, pos.z, true);
      }
   }

   private static class Node {
      BlockPos position;
      double gCost = Double.MAX_VALUE;
      double hCost = 0.0;
      double fCost = 0.0;
      QTPUtils.Node parent;
      double gWeight;
      double hWeight;

      Node(BlockPos position, double gWeight, double hWeight) {
         this.position = position;
         this.gWeight = gWeight;
         this.hWeight = hWeight;
      }

      double getFCost() {
         this.fCost = this.gCost * this.gWeight + this.hCost * this.hWeight;
         return this.fCost;
      }
   }

   public enum TpVClipFindType {
      All,
      OnlyUp,
      OnlyDown;
   }
}
