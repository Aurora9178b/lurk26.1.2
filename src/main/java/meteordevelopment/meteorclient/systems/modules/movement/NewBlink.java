package meteordevelopment.meteorclient.systems.modules.movement;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.phys.Vec3;

public class NewBlink extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> pulse = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("pulse")
            .description("Periodically sends stored packets instead of holding them until disable.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-disable").description("Disables when enough packets have been stored.").defaultValue(false).build());
   private final Setting<Boolean> disableOnVelocity = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-on-velocity").description("Disables when you receive velocity.").defaultValue(false).build());
   private final Setting<Boolean> disableOnAttack = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-on-attack").description("Disables before attacking.").defaultValue(false).build());
   private final Setting<Integer> disablePackets = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("disable-packets")
            .description("Stored packet count required to auto disable.")
            .defaultValue(17)
            .min(1)
            .sliderRange(1, 1000)
            .build()
      );
   private final Setting<Integer> pulsePackets = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("pulse-packets")
            .description("Stored packet count required to pulse.")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 1000)
            .build()
      );
   private final Setting<Keybind> cancel = this.sgGeneral
      .add(
         new KeybindSetting.Builder()
            .name("cancel")
            .description("Cancels stored packets and returns to the start position.")
            .defaultValue(Keybind.none())
            .build()
      );
   private Vec3 lastPos = Vec3.ZERO;
   private Vec3 prevVelocity = Vec3.ZERO;
   private float prevYaw;
   private boolean prevSprinting;
   private final Queue<Packet<?>> storedPackets = new LinkedList<>();
   private final Queue<Packet<?>> storedTransactions = new LinkedList<>();
   private final AtomicBoolean sending = new AtomicBoolean(false);
   private boolean isAttacking;

   public NewBlink() {
      super(Categories.Movement, "Blink+", "Stores movement packets and releases them later.");
   }

   @Override
   public void onActivate() {
      if (Utils.canUpdate() && !this.mc.isSingleplayer() && this.mc.getConnection() != null) {
         this.storedTransactions.clear();
         this.storedPackets.clear();
         this.sending.set(false);
         this.lastPos = this.mc.player.position();
         this.prevVelocity = this.mc.player.getDeltaMovement();
         this.prevYaw = this.mc.player.getYRot();
         this.prevSprinting = this.mc.player.isSprinting();
      } else {
         this.toggle();
      }
   }

   @Override
   public void onDeactivate() {
      if (Utils.canUpdate()) {
         this.sendPackets();
         this.storedTransactions.clear();
      }
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.storedPackets.size());
   }

   public int getStoredPacketCount() {
      return this.storedPackets.size();
   }

   public int getPulsePackets() {
      return this.pulsePackets.get();
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (Utils.canUpdate() && this.lastPos != null) {
         float hue = (float)(System.currentTimeMillis() % 7200L) / 7200.0F;
         double x = this.lastPos.x;
         double y = this.lastPos.y + 0.01;
         double z = this.lastPos.z;

         for (int i = 0; i < 360; i++) {
            double x1 = x + Math.sin(Math.toRadians(i)) * 0.5;
            double z1 = z + Math.cos(Math.toRadians(i)) * 0.5;
            double x2 = x + Math.sin(Math.toRadians(i + 1)) * 0.5;
            double z2 = z + Math.cos(Math.toRadians(i + 1)) * 0.5;
            int rgb = Color.getHSBColor((hue + i / 360.0F) % 1.0F, 0.0F, 1.0F).getRGB();
            event.renderer.line(x1, y, z1, x2, y, z2, new meteordevelopment.meteorclient.utils.render.color.Color(rgb));
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (this.disableOnVelocity.get()) {
         if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            if (Utils.canUpdate() && packet.id() == this.mc.player.getId()) {
               this.warning("Disabled due to velocity.");
               this.disable();
            }
         }
      }
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (this.disableOnAttack.get() && !this.isAttacking && Utils.canUpdate()) {
         event.cancel();
         this.warning("Disabled due to attack.");
         this.isAttacking = true;
         this.disable();
         this.mc.gameMode.attack(this.mc.player, event.entity);
         this.isAttacking = false;
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (Utils.canUpdate() && !this.sending.get()) {
         Packet<?> packet = event.packet;
         if (packet instanceof ServerboundPongPacket) {
            this.storedTransactions.add(packet);
         }

         if (this.pulse.get()) {
            if (packet instanceof ServerboundMovePlayerPacket) {
               event.cancel();
               this.storedPackets.add(packet);
            }
         } else if (!this.shouldIgnore(packet)) {
            event.cancel();
            this.storedPackets.add(packet);
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (Utils.canUpdate()) {
         if (this.cancel.get().isPressed()) {
            this.cancelBlink();
         } else {
            if (this.pulse.get() && this.storedPackets.size() >= this.pulsePackets.get()) {
               this.sendPackets();
            }

            if (this.autoDisable.get() && this.storedPackets.size() >= this.disablePackets.get()) {
               this.disable();
            }
         }
      }
   }

   private void cancelBlink() {
      this.storedPackets.clear();
      this.mc.player.setPos(this.lastPos.x, this.lastPos.y, this.lastPos.z);
      this.mc.player.setDeltaMovement(this.prevVelocity);
      this.mc.player.setYRot(this.prevYaw);
      this.mc.player.setSprinting(this.prevSprinting);
      this.mc.player.setShiftKeyDown(false);
      this.mc.options.keyShift.setDown(false);
      this.sending.set(true);

      while (!this.storedTransactions.isEmpty()) {
         this.mc.getConnection().send(this.storedTransactions.poll());
      }

      this.sending.set(false);
      this.info("Canceling...");
      this.disable();
   }

   private void sendPackets() {
      if (Utils.canUpdate() && this.mc.getConnection() != null) {
         this.sending.set(true);

         while (!this.storedPackets.isEmpty()) {
            Packet<?> packet = this.storedPackets.poll();
            this.mc.getConnection().send(packet);
            if (packet instanceof ServerboundMovePlayerPacket movePacket && !(packet instanceof Rot)) {
               this.lastPos = new Vec3(movePacket.getX(this.mc.player.getX()), movePacket.getY(this.mc.player.getY()), movePacket.getZ(this.mc.player.getZ()));
            }
         }

         this.sending.set(false);
      }
   }

   private boolean shouldIgnore(Packet<?> packet) {
      return packet instanceof ServerboundChatPacket
         || packet instanceof ServerboundChatCommandPacket
         || packet instanceof ServerboundAcceptTeleportationPacket
         || packet instanceof ServerboundKeepAlivePacket
         || packet instanceof ServerboundSeenAdvancementsPacket
         || packet instanceof ServerboundClientCommandPacket;
   }
}
