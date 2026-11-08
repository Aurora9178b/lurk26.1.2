package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundSpectateEntityPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FakeLag extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgFlush = this.settings.createGroup("Flush");
   private final Setting<FakeLag.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("How packets are queued."))
               .defaultValue(FakeLag.Mode.Dynamic))
            .build()
      );
   private final Setting<Double> minRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("min-range")
            .description("Minimum dynamic enemy range.")
            .defaultValue(2.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(() -> this.mode.get() == FakeLag.Mode.Dynamic)
            .build()
      );
   private final Setting<Double> maxRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("max-range")
            .description("Maximum dynamic enemy range.")
            .defaultValue(5.0)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(() -> this.mode.get() == FakeLag.Mode.Dynamic)
            .build()
      );
   private final Setting<Integer> minDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("min-delay")
            .description("Minimum delay before flushing queued packets in milliseconds.")
            .defaultValue(300)
            .range(0, 1000)
            .sliderRange(0, 1000)
            .build()
      );
   private final Setting<Integer> maxDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-delay")
            .description("Maximum delay before flushing queued packets in milliseconds.")
            .defaultValue(600)
            .range(0, 1000)
            .sliderRange(0, 1000)
            .build()
      );
   private final Setting<Integer> recoilTime = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("recoil-time")
            .description("Time after a flush before packets can be queued again in milliseconds.")
            .defaultValue(250)
            .range(0, 1000)
            .sliderRange(0, 1000)
            .build()
      );
   private final Setting<Boolean> flushOnEntityInteract = this.sgFlush
      .add(
         new BoolSetting.Builder()
            .name("entity-interact")
            .description("Flushes when attacking, interacting, spectating or swinging.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> flushOnBlockInteract = this.sgFlush
      .add(new BoolSetting.Builder().name("block-interact").description("Flushes when interacting with blocks or updating signs.").defaultValue(true).build());
   private final Setting<Boolean> flushOnAction = this.sgFlush
      .add(new BoolSetting.Builder().name("action").description("Flushes when sending player action packets.").defaultValue(true).build());
   private final Queue<FakeLag.QueuedPacket> packets = new ArrayDeque<>();
   private boolean sending;
   private boolean enemyNearby;
   private long nextDelay;
   private long lastFlush;
   private Vec3 serverPosition;

   public FakeLag() {
      super(Categories.Combat, "fake-lag", "Queues outgoing packets to simulate lag around enemies.");
   }

   @Override
   public void onActivate() {
      this.packets.clear();
      this.sending = false;
      this.enemyNearby = false;
      this.serverPosition = this.mc.player != null ? this.mc.player.position() : Vec3.ZERO;
      this.nextDelay = this.randomDelay();
      this.lastFlush = System.currentTimeMillis();
   }

   @Override
   public void onDeactivate() {
      this.flush();
      this.packets.clear();
      this.sending = false;
      this.enemyNearby = false;
      this.serverPosition = null;
   }

   @Override
   public String getInfoString() {
      return this.mode.get().toString();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         this.enemyNearby = this.hasEnemyInDynamicRange();
         if (!this.packets.isEmpty() && System.currentTimeMillis() - this.packets.peek().time >= this.nextDelay) {
            this.flush();
            this.nextDelay = this.randomDelay();
         }
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (Utils.canUpdate()
         && !this.sending
         && this.mc.player != null
         && !this.mc.player.isDeadOrDying()
         && !this.mc.player.isInWater()
         && this.mc.screen == null) {
         Packet<?> packet = event.packet;
         if (System.currentTimeMillis() - this.lastFlush < this.recoilTime.get().intValue()) {
            this.trackServerPosition(packet);
         } else if (this.shouldIgnore(packet)) {
            this.trackServerPosition(packet);
         } else if (packet instanceof ServerboundResourcePackPacket) {
            this.flush();
         } else if (this.shouldFlush(packet)) {
            this.flush();
            this.trackServerPosition(packet);
         } else if (this.mode.get() == FakeLag.Mode.Dynamic && !this.shouldLagDynamically()) {
            this.trackServerPosition(packet);
         } else {
            event.cancel();
            this.packets.add(new FakeLag.QueuedPacket(packet, event.connection, System.currentTimeMillis()));
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (Utils.canUpdate() && this.mc.player != null) {
         Packet<?> packet = event.packet;
         if (!(packet instanceof ClientboundPlayerPositionPacket) && !(packet instanceof ClientboundSetHealthPacket)) {
            if (packet instanceof ClientboundSetEntityMotionPacket velocity
               && velocity.id() == this.mc.player.getId()
               && !Vec3.ZERO.equals(velocity.movement())) {
               this.flush();
            } else {
               if (packet instanceof ClientboundExplodePacket explosion && !Vec3.ZERO.equals(explosion.playerKnockback().orElse(Vec3.ZERO))) {
                  this.flush();
               }
            }
         } else {
            this.flush();
         }
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.packets.clear();
      this.sending = false;
      this.enemyNearby = false;
      this.serverPosition = null;
   }

   private boolean shouldLagDynamically() {
      if (!this.enemyNearby) {
         return false;
      }

      if (this.serverPosition == null) {
         this.serverPosition = this.mc.player.position();
      }

      return this.shouldKeepLagging(this.serverPosition);
   }

   private boolean hasEnemyInDynamicRange() {
      double min = Math.min(this.minRange.get(), this.maxRange.get());
      double max = Math.max(this.minRange.get(), this.maxRange.get());
      double minSq = min * min;
      double maxSq = max * max;

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof Player player && this.isEnemy(player)) {
            double distanceSq = player.distanceToSqr(this.mc.player);
            if (distanceSq >= minSq && distanceSq <= maxSq) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean shouldKeepLagging(Vec3 oldPosition) {
      double max = Math.max(this.minRange.get(), this.maxRange.get());
      double maxSq = max * max;
      Vec3 clientPosition = this.mc.player.position();
      AABB oldPlayerBox = this.mc
         .player
         .getBoundingBox()
         .move(oldPosition.x - this.mc.player.getX(), oldPosition.y - this.mc.player.getY(), oldPosition.z - this.mc.player.getZ());
      boolean foundEnemy = false;
      double serverDistance = Double.MAX_VALUE;
      double clientDistance = Double.MAX_VALUE;

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof Player player && this.isEnemy(player) && !(player.position().distanceToSqr(oldPosition) > maxSq)) {
            foundEnemy = true;
            if (player.getBoundingBox().intersects(oldPlayerBox)) {
               return false;
            }

            serverDistance = Math.min(serverDistance, player.position().distanceTo(oldPosition));
            clientDistance = Math.min(clientDistance, player.position().distanceTo(clientPosition));
         }
      }

      return foundEnemy && serverDistance >= clientDistance;
   }

   private boolean isEnemy(Player player) {
      if (player == this.mc.player || player.isDeadOrDying() || player.getHealth() <= 0.0F) {
         return false;
      } else if (!Friends.get().shouldAttack(player)) {
         return false;
      } else {
         return player instanceof FakePlayerEntity fakePlayer ? !fakePlayer.noHit : EntityUtils.getGameMode(player) == GameType.SURVIVAL;
      }
   }

   private boolean shouldFlush(Packet<?> packet) {
      if (!this.flushOnEntityInteract.get()
         || !(packet instanceof ServerboundInteractPacket)
            && !(packet instanceof ServerboundAttackPacket)
            && !(packet instanceof ServerboundSpectateEntityPacket)
            && !(packet instanceof ServerboundSwingPacket)) {
         return !this.flushOnBlockInteract.get() || !(packet instanceof ServerboundUseItemOnPacket) && !(packet instanceof ServerboundSignUpdatePacket)
            ? this.flushOnAction.get() && packet instanceof ServerboundPlayerActionPacket
            : true;
      } else {
         return true;
      }
   }

   private boolean shouldIgnore(Packet<?> packet) {
      return packet instanceof ServerboundChatPacket
         || packet instanceof ServerboundChatCommandPacket
         || packet instanceof ServerboundKeepAlivePacket
         || packet instanceof ServerboundPongPacket
         || packet instanceof ServerboundAcceptTeleportationPacket
         || packet instanceof ServerboundClientCommandPacket
         || packet instanceof ServerboundSeenAdvancementsPacket
         || this.mc.player != null && this.mc.player.isUsingItem() && this.mc.player.getUseItem().get(DataComponents.CONSUMABLE) != null;
   }

   private void flush() {
      if (this.packets.isEmpty()) {
         this.lastFlush = System.currentTimeMillis();
      } else {
         this.sending = true;

         while (!this.packets.isEmpty()) {
            FakeLag.QueuedPacket queued = this.packets.poll();
            queued.connection.send(queued.packet, null, true);
            this.trackServerPosition(queued.packet);
         }

         this.sending = false;
         this.lastFlush = System.currentTimeMillis();
      }
   }

   private void trackServerPosition(Packet<?> packet) {
      if (this.mc.player != null && packet instanceof ServerboundMovePlayerPacket movePacket) {
         Vec3 fallback = this.serverPosition != null ? this.serverPosition : this.mc.player.position();
         this.serverPosition = new Vec3(movePacket.getX(fallback.x), movePacket.getY(fallback.y), movePacket.getZ(fallback.z));
      }
   }

   private long randomDelay() {
      int min = Math.min(this.minDelay.get(), this.maxDelay.get());
      int max = Math.max(this.minDelay.get(), this.maxDelay.get());
      return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
   }

   private enum Mode {
      Constant,
      Dynamic;
   }

   private record QueuedPacket(Packet<?> packet, Connection connection, long time) {
   }
}
