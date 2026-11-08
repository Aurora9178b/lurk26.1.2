package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Backtrack extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPause = this.settings.createGroup("Pause");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> minRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("min-range").description("Minimum target range.").defaultValue(1.0).range(0.0, 10.0).sliderRange(0.0, 10.0).build());
   private final Setting<Double> maxRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("max-range").description("Maximum target range.").defaultValue(3.0).range(0.0, 10.0).sliderRange(0.0, 10.0).build());
   private final Setting<Backtrack.PacketMode> packetMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("packet-mode"))
                  .description("How queued packets are released while backtracking."))
               .defaultValue(Backtrack.PacketMode.Freeze))
            .build()
      );
   private final Setting<Integer> minDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-delay")
            .description("Minimum packet delay in milliseconds.")
            .defaultValue(100)
            .range(0, 1000)
            .sliderRange(0, 1000)
            .visible(() -> this.packetMode.get() == Backtrack.PacketMode.Delay)
            .build()
      );
   private final Setting<Integer> maxDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-delay")
            .description("Maximum packet delay in milliseconds.")
            .defaultValue(150)
            .range(0, 1000)
            .sliderRange(0, 1000)
            .visible(() -> this.packetMode.get() == Backtrack.PacketMode.Delay)
            .build()
      );
   private final Setting<Integer> minNextBacktrackDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-next-delay")
            .description("Minimum delay before starting another backtrack in milliseconds.")
            .defaultValue(0)
            .range(0, 2000)
            .sliderRange(0, 2000)
            .build()
      );
   private final Setting<Integer> maxNextBacktrackDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-next-delay")
            .description("Maximum delay before starting another backtrack in milliseconds.")
            .defaultValue(10)
            .range(0, 2000)
            .sliderRange(0, 2000)
            .build()
      );
   private final Setting<Integer> trackingBuffer = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("tracking-buffer")
            .description("How long to keep backtracking after the target leaves range.")
            .defaultValue(500)
            .range(0, 2000)
            .sliderRange(0, 2000)
            .build()
      );
   private final Setting<Double> chance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("chance")
            .description("Chance to start backtracking.")
            .defaultValue(50.0)
            .range(0.0, 100.0)
            .sliderRange(0.0, 100.0)
            .build()
      );
   private final Setting<Backtrack.Mode> targetMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-mode"))
                  .description("How targets are selected."))
               .defaultValue(Backtrack.Mode.Attack))
            .build()
      );
   private final Setting<Integer> lastAttackTimeToWork = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("last-attack-time")
            .description("How long backtrack can work after attacking.")
            .defaultValue(1000)
            .range(0, 5000)
            .sliderRange(0, 5000)
            .build()
      );
   private final Setting<Boolean> pauseOnHurtTime = this.sgPause
      .add(
         new BoolSetting.Builder()
            .name("pause-on-hurt-time")
            .description("Pauses backtrack while the target has enough hurt time.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Integer> hurtTime = this.sgPause
      .add(
         new IntSetting.Builder()
            .name("hurt-time")
            .description("Target hurt time required to pause.")
            .defaultValue(3)
            .range(0, 10)
            .sliderRange(0, 10)
            .visible(this.pauseOnHurtTime::get)
            .build()
      );
   private final Setting<Backtrack.EspMode> esp = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("esp"))
                  .description("How to render the tracked server-side target position."))
               .defaultValue(Backtrack.EspMode.Box))
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How to render the tracked position."))
                  .defaultValue(ShapeMode.Both))
               .visible(() -> this.esp.get() == Backtrack.EspMode.Box))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("Side color.")
            .defaultValue(new SettingColor(80, 160, 255, 35))
            .visible(() -> this.esp.get() == Backtrack.EspMode.Box && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("Line color.")
            .defaultValue(new SettingColor(80, 160, 255, 220))
            .visible(() -> this.esp.get() == Backtrack.EspMode.Box && this.shapeMode.get().lines())
            .build()
      );
   private final Queue<Backtrack.QueuedPacket> queuedPackets = new ConcurrentLinkedQueue<>();
   private final Backtrack.TrackedPosition trackedPosition = new Backtrack.TrackedPosition();
   private Entity target;
   private boolean flushing;
   private boolean shouldPause;
   private long currentDelay;
   private long nextAllowedBacktrackTime;
   private long lastAttackTime;
   private long lastInRangeTime;
   private int currentChance;

   public Backtrack() {
      super(Categories.Combat, "backtrack", "Delays incoming target packets to attack previous positions.");
   }

   @Override
   public void onActivate() {
      this.clear(false, true);
      this.currentDelay = this.randomDelay();
      this.currentChance = this.randomChance();
      this.lastAttackTime = 0L;
      this.lastInRangeTime = 0L;
   }

   @Override
   public void onDeactivate() {
      this.clear(true, false);
   }

   @Override
   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      this.lastAttackTime = System.currentTimeMillis();
      this.currentChance = this.randomChance();
      if (this.targetMode.get() == Backtrack.Mode.Attack) {
         this.processTarget(event.entity);
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.getConnection() != null) {
         if (this.targetMode.get() == Backtrack.Mode.Range) {
            Entity enemy = this.findRangeTarget();
            if (enemy == null) {
               this.clear(true, false);
            } else {
               this.processTarget(enemy);
            }
         }

         if (this.shouldCancelPackets()) {
            if (this.packetMode.get() == Backtrack.PacketMode.Delay) {
               this.flushOlderThan(System.currentTimeMillis() - this.currentDelay);
            }
         } else if (!this.queuedPackets.isEmpty()) {
            this.clear(true, false);
         }

         if (this.queuedPackets.isEmpty()) {
            this.currentDelay = this.randomDelay();
         }
      } else {
         this.clear(false, true);
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (!this.flushing && this.mc.player != null && this.mc.level != null && this.mc.getConnection() != null) {
         Packet<?> packet = event.packet;
         if (!(packet instanceof ClientboundDisconnectPacket) && !(packet instanceof ClientboundPlayerPositionPacket)) {
            if (packet instanceof ClientboundSetHealthPacket health && health.getHealth() <= 0.0F) {
               this.clear(true, false);
            } else if (!(packet instanceof ClientboundSoundPacket sound && sound.getSound().value() == SoundEvents.PLAYER_HURT)) {
               if (!this.shouldPass(packet)) {
                  Entity currentTarget = this.target;
                  if (currentTarget != null
                     && this.updateTrackedPosition(packet, currentTarget)
                     && this.packetMode.get() == Backtrack.PacketMode.Delay
                     && this.boxedDistanceTo(this.mc.player.getBoundingBox(), this.trackedPosition.box(currentTarget))
                        < this.boxedDistanceTo(this.mc.player.getBoundingBox(), currentTarget.getBoundingBox())) {
                     this.clear(true, false);
                  } else if (this.shouldCancelPackets() || !this.queuedPackets.isEmpty()) {
                     event.cancel();
                     this.queuedPackets.add(new Backtrack.QueuedPacket(packet, event.connection.getPacketListener(), System.currentTimeMillis()));
                  }
               }
            }
         } else {
            this.clear(true, false);
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.esp.get() != Backtrack.EspMode.None
         && this.target != null
         && this.trackedPosition.position != null
         && this.trackedPosition.position != Vec3.ZERO) {
         AABB box = this.trackedPosition.box(this.target);
         event.renderer.box(box, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.clear(false, true);
   }

   private void processTarget(Entity enemy) {
      this.shouldPause = enemy instanceof LivingEntity living && living.hurtTime >= this.hurtTime.get();
      if (this.shouldBacktrack(enemy)) {
         if (enemy != this.target) {
            this.clear(true, false, false);
            this.trackedPosition.setBaseFrom(enemy);
         }

         this.target = enemy;
      }
   }

   private boolean shouldBacktrack(Entity entity) {
      boolean inRange = this.inRange(entity);
      long now = System.currentTimeMillis();
      if (inRange) {
         this.lastInRangeTime = now;
      }

      return (inRange || now - this.lastInRangeTime <= this.trackingBuffer.get().intValue())
         && this.shouldAttack(entity)
         && this.mc.player.tickCount > 10
         && this.currentChance < this.chance.get()
         && now >= this.nextAllowedBacktrackTime
         && !this.shouldPause()
         && now - this.lastAttackTime <= this.lastAttackTimeToWork.get().intValue();
   }

   private boolean shouldCancelPackets() {
      return this.target != null && this.target.isAlive() && this.shouldBacktrack(this.target);
   }

   private boolean shouldPass(Packet<?> packet) {
      return packet instanceof ClientboundSystemChatPacket
         || packet instanceof ClientboundPlayerChatPacket
         || packet instanceof ClientboundDisguisedChatPacket
         || packet instanceof ClientboundKeepAlivePacket
         || packet instanceof ClientboundPingPacket
         || packet instanceof ClientboundResourcePackPopPacket
         || packet instanceof ClientboundResourcePackPushPacket;
   }

   private boolean shouldPause() {
      return this.pauseOnHurtTime.get() && this.shouldPause;
   }

   private boolean inRange(Entity entity) {
      double min = Math.min(this.minRange.get(), this.maxRange.get());
      double max = Math.max(this.minRange.get(), this.maxRange.get());
      double distance = this.boxedDistanceTo(this.mc.player.getBoundingBox(), entity.getBoundingBox());
      return distance >= min && distance <= max;
   }

   private Entity findRangeTarget() {
      return TargetUtils.get(entity -> this.shouldAttack(entity) && this.inRange(entity), SortPriority.LowestDistance);
   }

   private boolean shouldAttack(Entity entity) {
      if (entity != null && entity != this.mc.player && entity.isAlive()) {
         if (!(entity instanceof LivingEntity living && !living.isDeadOrDying() && !(living.getHealth() <= 0.0F))) {
            return false;
         } else if (!(entity instanceof Player player)) {
            return entity.getType() != EntityType.ARMOR_STAND;
         } else if (!Friends.get().shouldAttack(player)) {
            return false;
         } else if (player.isCreative() || player.isSpectator()) {
            return false;
         } else {
            return player instanceof FakePlayerEntity fakePlayer ? !fakePlayer.noHit : EntityUtils.getGameMode(player) == GameType.SURVIVAL;
         }
      } else {
         return false;
      }
   }

   private void clear(boolean handlePackets, boolean clearOnly) {
      this.clear(handlePackets, clearOnly, true);
   }

   private void clear(boolean handlePackets, boolean clearOnly, boolean resetNextDelay) {
      if (handlePackets && !clearOnly) {
         this.flushAll();
      } else {
         this.queuedPackets.clear();
      }

      if (this.target != null && resetNextDelay) {
         this.nextAllowedBacktrackTime = System.currentTimeMillis() + this.randomNextDelay();
      }

      this.target = null;
      this.shouldPause = false;
      this.trackedPosition.clear();
   }

   private void flushOlderThan(long time) {
      if (!this.queuedPackets.isEmpty()) {
         this.flushing = true;

         try {
            while (!this.queuedPackets.isEmpty() && this.queuedPackets.peek().time <= time) {
               Backtrack.QueuedPacket queued = this.queuedPackets.poll();
               this.handleQueuedPacket(queued);
            }
         } finally {
            this.flushing = false;
         }
      }
   }

   private void flushAll() {
      if (!this.queuedPackets.isEmpty()) {
         this.flushing = true;

         try {
            while (!this.queuedPackets.isEmpty()) {
               this.handleQueuedPacket(this.queuedPackets.poll());
            }
         } finally {
            this.flushing = false;
         }
      }
   }

   private void handleQueuedPacket(Backtrack.QueuedPacket queued) {
      PacketListener listener = queued.listener;
      if (listener == null && this.mc.getConnection() != null) {
         listener = this.mc.getConnection();
      }

      if (listener != null) {
         ((Packet<PacketListener>)queued.packet).handle(listener);
      }
   }

   private boolean updateTrackedPosition(Packet<?> packet, Entity entity) {
      if (packet instanceof ClientboundMoveEntityPacket move && move.getEntity(this.mc.level) == entity) {
         if (move.hasPosition()) {
            Vec3 pos = this.trackedPosition.codec.decode(move.getXa(), move.getYa(), move.getZa());
            this.trackedPosition.position = pos;
            this.trackedPosition.codec.setBase(pos);
         }

         if (move.hasRotation()) {
            this.trackedPosition.yaw = move.getYRot();
            this.trackedPosition.pitch = move.getXRot();
         }

         return true;
      } else if (packet instanceof ClientboundTeleportEntityPacket teleport && teleport.id() == entity.getId()) {
         PositionMoveRotation old = new PositionMoveRotation(
            this.trackedPosition.position, entity.getDeltaMovement(), this.trackedPosition.yaw, this.trackedPosition.pitch
         );
         PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(old, teleport.change(), teleport.relatives());
         this.trackedPosition.position = absolute.position();
         this.trackedPosition.yaw = absolute.yRot();
         this.trackedPosition.pitch = absolute.xRot();
         this.trackedPosition.codec.setBase(absolute.position());
         return true;
      } else if (packet instanceof ClientboundRotateHeadPacket rotate && rotate.getEntity(this.mc.level) == entity) {
         this.trackedPosition.headYaw = rotate.getYHeadRot();
         return true;
      } else if (packet instanceof ClientboundSetEntityMotionPacket motion && motion.id() == entity.getId()) {
         this.trackedPosition.velocity = motion.movement();
         return true;
      } else if (packet instanceof ClientboundRemoveEntitiesPacket remove && remove.getEntityIds().contains(entity.getId())) {
         this.clear(true, false);
         return false;
      } else {
         return false;
      }
   }

   private double boxedDistanceTo(AABB first, AABB second) {
      if (first != null && second != null) {
         double x = Math.max(0.0, Math.max(first.minX - second.maxX, second.minX - first.maxX));
         double y = Math.max(0.0, Math.max(first.minY - second.maxY, second.minY - first.maxY));
         double z = Math.max(0.0, Math.max(first.minZ - second.maxZ, second.minZ - first.maxZ));
         return Math.sqrt(x * x + y * y + z * z);
      } else {
         return Double.MAX_VALUE;
      }
   }

   private long randomDelay() {
      return this.randomBetween(this.minDelay.get(), this.maxDelay.get());
   }

   private long randomNextDelay() {
      return this.randomBetween(this.minNextBacktrackDelay.get(), this.maxNextBacktrackDelay.get());
   }

   private long randomBetween(int first, int second) {
      int min = Math.min(first, second);
      int max = Math.max(first, second);
      return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
   }

   private int randomChance() {
      return ThreadLocalRandom.current().nextInt(0, 101);
   }

   public enum EspMode {
      Box,
      None;
   }

   public enum Mode {
      Attack,
      Range;
   }

   public enum PacketMode {
      Freeze,
      Delay;
   }

   private record QueuedPacket(Packet<?> packet, PacketListener listener, long time) {
   }

   private static class TrackedPosition {
      private final VecDeltaCodec codec = new VecDeltaCodec();
      private Vec3 position = Vec3.ZERO;
      private Vec3 velocity = Vec3.ZERO;
      private float yaw;
      private float pitch;
      private float headYaw;

      private void setBaseFrom(Entity entity) {
         this.position = entity.position();
         this.velocity = entity.getDeltaMovement();
         this.yaw = entity.getYRot();
         this.pitch = entity.getXRot();
         this.headYaw = entity.getYHeadRot();
         this.codec.setBase(this.position);
      }

      private AABB box(Entity entity) {
         return entity.getBoundingBox().move(this.position.subtract(entity.position()));
      }

      private void clear() {
         this.position = Vec3.ZERO;
         this.velocity = Vec3.ZERO;
         this.yaw = 0.0F;
         this.pitch = 0.0F;
         this.headYaw = 0.0F;
         this.codec.setBase(Vec3.ZERO);
      }
   }
}
