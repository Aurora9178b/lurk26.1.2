package meteordevelopment.meteorclient.utils.movement;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.math.BlockUtil;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.listeners.ConsumerListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class LTpUtils {
   private static final AtomicLong lastPacketTime = new AtomicLong(0L);
   private static final Map<Long, Long> packetCountWindow = new ConcurrentHashMap<>();
   private static int tickCounter = 0;
   public static double shortDistanceThreshold = 20.0;
   public static double splitDistanceThreshold = 160.0;
   public static double defaultMaxMove = 10.0;

   private static void addPacket() {
      long now = System.nanoTime();
      long window = (long)(getInterval() * 1.0E9);
      packetCountWindow.entrySet().removeIf(entry -> now - entry.getKey() > window);
      packetCountWindow.put(now, packetCountWindow.getOrDefault(now, 0L) + 1L);
   }

   private static int getPacketRate() {
      long now = System.nanoTime();
      long window = (long)(getInterval() * 1.0E9);
      return (int)packetCountWindow.entrySet().stream().filter(e -> now - e.getKey() <= window).mapToLong(Entry::getValue).sum();
   }

   private static boolean hasRateOut() {
      return (getAction() == LTpUtils.Action.Both || getAction() == LTpUtils.Action.Drop) && getPacketRate() >= getMaxPacketRate();
   }

   private static boolean hasTickPacketOut() {
      double inOneTick = getInOneTick();
      return (getAction() == LTpUtils.Action.Both || getAction() == LTpUtils.Action.Drop) && inOneTick > 0.0 && tickCounter > inOneTick;
   }

   private static void sendLimitMsg() {
      if (getAction() == LTpUtils.Action.Both || getAction() == LTpUtils.Action.Message) {
         ChatUtils.info("Limited packet.");
      }
   }

   private static double getInterval() {
      return 1.0;
   }

   private static int getMaxPacketRate() {
      return TpSetting.get().maxPackets.get();
   }

   private static double getInOneTick() {
      return 0.0;
   }

   private static LTpUtils.Action getAction() {
      return LTpUtils.Action.Both;
   }

   private static double getMoveDistance() {
      return defaultMaxMove;
   }

   private static int getSteps(Vec3 from, Vec3 to) {
      double distance = from.distanceTo(to);
      double md = getMoveDistance();
      int steps = md <= 0.0 ? 1 : (int)Math.ceil(distance / md);
      return Math.max(0, steps - 1 - tickCounter);
   }

   public static void sendMovePacket(double x, double y, double z, boolean onGround) {
      if (!hasRateOut() && !hasTickPacketOut()) {
         ClientPacketListener connection = MeteorClient.mc.player.connection;
         connection.send(new Pos(x, y, z, onGround, MeteorClient.mc.player.horizontalCollision));
         tickCounter++;
      } else {
         sendLimitMsg();
      }
   }

   public static void sendMovePacket(Vec3 vec3, boolean onGround) {
      sendMovePacket(vec3.x, vec3.y, vec3.z, onGround);
   }

   public static void sendClientMovePacket(double x, double y, double z, boolean onGround) {
      MeteorClient.mc.player.setPos(x, y, z);
      MeteorClient.mc.player.setOnGround(onGround);
   }

   public static final void send(@NotNull Packet<?> packet) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientPacketListener clientPacketListener = minecraft.getConnection();
      clientPacketListener.send(packet);
   }

   public static void warp(double x1, double y1, double z1, double x2, double y2, double z2) {
      double distance = calculateDistance(x1, y1, z1, x2, y2, z2);
      int packetsRequired = (int)Math.ceil(Math.abs(distance / 10.0));

      for (int packetNumber = 0; packetNumber < packetsRequired - 1; packetNumber++) {
         MeteorClient.mc.player.connection.send(new StatusOnly(true, MeteorClient.mc.player.horizontalCollision));
      }

      MeteorClient.mc.player.connection.send(new Pos(x2, y2, z2, true, MeteorClient.mc.player.horizontalCollision));
   }

   public static void doTp(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double maxMove, boolean onGround) {
      if (hasRateOut()) {
         sendLimitMsg();
      } else {
         Vec3 from = new Vec3(fromX, fromY, fromZ);
         Vec3 to = new Vec3(toX, toY, toZ);
         int steps = getSteps(from, to);

         for (int i = 0; i < steps; i++) {
            sendMovePacket(from, onGround);
         }

         sendMovePacket(to, onGround);
      }
   }

   public static void doTp(Vec3 from, Vec3 to, double maxMove, boolean onGround) {
      doTp(from.x, from.y, from.z, to.x, to.y, to.z, maxMove, onGround);
   }

   public static void doTp(Vec3 from, Vec3 to, double maxMove) {
      doTp(from, to, maxMove, true);
   }

   public static void doTp(Vec3 from, Vec3 to) {
      doTp(from, to, defaultMaxMove, true);
   }

   public static void doTp(Vec3 vec3d, Vec3 vec3d2, double d, boolean bl, int n, Object object) {
      if ((n & 8) != 0) {
         bl = true;
      }

      doTp(vec3d, vec3d2, d, bl);
   }

   public static void doTpInterpolated(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double maxMove, boolean onGround) {
      if (hasRateOut()) {
         sendLimitMsg();
      } else {
         Vec3 from = new Vec3(fromX, fromY, fromZ);
         Vec3 to = new Vec3(toX, toY, toZ);
         double dist = from.distanceTo(to);
         int steps = (int)Math.ceil(dist / maxMove);
         if (steps <= 1) {
            sendMovePacket(to, onGround);
         } else {
            double stepX = (toX - fromX) / steps;
            double stepY = (toY - fromY) / steps;
            double stepZ = (toZ - fromZ) / steps;

            for (int i = 1; i < steps; i++) {
               sendMovePacket(fromX + stepX * i, fromY + stepY * i, fromZ + stepZ * i, onGround);
            }

            sendMovePacket(to, onGround);
         }
      }
   }

   public static void doTpInterpolated(Vec3 from, Vec3 to, double maxMove, boolean onGround) {
      doTpInterpolated(from.x, from.y, from.z, to.x, to.y, to.z, maxMove, onGround);
   }

   public static void doTpInterpolated(Vec3 from, Vec3 to, double maxMove) {
      doTpInterpolated(from, to, maxMove, true);
   }

   public static void doTpSplit(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double maxMove, boolean onGround) {
      if (hasRateOut()) {
         sendLimitMsg();
      } else {
         double midX = (fromX + toX) / 2.0;
         double midY = (fromY + toY) / 2.0;
         double midZ = (fromZ + toZ) / 2.0;
         doTpInterpolated(fromX, fromY, fromZ, midX, midY, midZ, maxMove, onGround);

         try {
            Thread.sleep(1L);
         } catch (InterruptedException var22) {
         }

         doTpInterpolated(midX, midY, midZ, toX, toY, toZ, maxMove, onGround);
      }
   }

   public static void doTpSplit(Vec3 from, Vec3 to, double maxMove, boolean onGround) {
      doTpSplit(from.x, from.y, from.z, to.x, to.y, to.z, maxMove, onGround);
   }

   public static void doTpSplit(Vec3 from, Vec3 to, double maxMove) {
      doTpSplit(from, to, maxMove, true);
   }

   public static void doTpBurst(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double maxMove, boolean onGround) {
      if (hasRateOut()) {
         sendLimitMsg();
      } else {
         double squareDis = squaredDistance(fromX, fromY, fromZ, toX, toY, toZ);
         int step = (int)Math.ceil(Math.sqrt(squareDis) / maxMove);

         for (int i = 1; i <= step; i++) {
            sendMovePacket(fromX, fromY, fromZ, onGround);
         }

         sendMovePacket(toX, toY, toZ, onGround);
      }
   }

   public static void doTpBurst(Vec3 from, Vec3 to, double maxMove, boolean onGround) {
      doTpBurst(from.x, from.y, from.z, to.x, to.y, to.z, maxMove, onGround);
   }

   public static void doTpBurst(Vec3 from, Vec3 to, double maxMove) {
      doTpBurst(from, to, maxMove, true);
   }

   public static void doTpXY(double fromX, double fromZ, double toX, double toZ, double moveDistance, boolean onGround) {
      doTp(fromX, MeteorClient.mc.player.getY(), fromZ, toX, MeteorClient.mc.player.getY(), toZ, moveDistance, onGround);
   }

   public static void doTpClient(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, double moveDistance, boolean onGround) {
      if (hasRateOut()) {
         sendLimitMsg();
      } else {
         double squareDis = squaredDistance(fromX, fromY, fromZ, toX, toY, toZ);
         int step = (int)Math.ceil(Math.sqrt(squareDis) / moveDistance);

         for (int i = 1; i <= step; i++) {
            sendClientMovePacket(fromX, fromY, fromZ, onGround);
         }

         sendClientMovePacket(toX, toY, toZ, onGround);
      }
   }

   public static void doTpClient(Vec3 fromVec, Vec3 toVec, double moveDistance, boolean onGround) {
      doTpClient(fromVec.x, fromVec.y, fromVec.z, toVec.x, toVec.y, toVec.z, moveDistance, onGround);
   }

   public static void doTpClient(Vec3 vec3d, Vec3 vec3d2, double d, boolean bl, int n, Object object) {
      if ((n & 8) != 0) {
         bl = true;
      }

      doTpClient(vec3d, vec3d2, d, bl);
   }

   public static Vec3 findVClipVecToMove(Vec3 fromVec, Vec3 toVec, double searchStep, boolean allowVoid) {
      return findVClipVecToMove(fromVec, toVec, searchStep, allowVoid, null);
   }

   public static Vec3 findVClipVecToMove(Vec3 fromVec, Vec3 toVec, double searchStep, boolean allowVoid, Map<BlockPos, BlockState> extraPosMap) {
      Vec3 findVec = null;
      double step = 0.0;
      int voidY = MeteorClient.mc.level.dimensionType().minY();

      while (step <= 200.0) {
         Vec3 vec = new Vec3(fromVec.x, fromVec.y + step, fromVec.z);
         if (allowVoid && fromVec.y <= voidY) {
            break;
         }

         if (isSafeBlockWithExtra(BlockPos.containing(vec), extraPosMap)) {
            return vec;
         }

         vec = new Vec3(fromVec.x, fromVec.y - step, fromVec.z);
         if (allowVoid && fromVec.y <= voidY) {
            break;
         }

         if (isSafeBlockWithExtra(BlockPos.containing(vec), extraPosMap)) {
            return vec;
         }

         step += searchStep;
      }

      return findVec;
   }

   private static boolean isSafeBlockWithExtra(BlockPos pos, Map<BlockPos, BlockState> extraPosMap) {
      return extraPosMap != null && extraPosMap.containsKey(pos) ? true : BlockUtil.isSafeBlock(pos);
   }

   private static double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = x2 - x1;
      double dy = y2 - y1;
      double dz = z2 - z1;
      return dx * dx + dy * dy + dz * dz;
   }

   static {
      MeteorClient.EVENT_BUS.subscribe(new ConsumerListener(PacketEvent.Send.class, -1199, event -> addPacket()));
      MeteorClient.EVENT_BUS.subscribe(new ConsumerListener(PacketEvent.Receive.class, 1199, event -> {
         PacketEvent.Receive packetEvent = (PacketEvent.Receive)(Object)event;
         if (packetEvent.packet instanceof ClientboundPlayerPositionPacket) {
            tickCounter--;
         }
      }));
      MeteorClient.EVENT_BUS.subscribe(new ConsumerListener(TickEvent.Pre.class, 1199, event -> tickCounter = 0));
   }

   enum Action {
      None,
      Drop,
      Message,
      Both;
   }
}
