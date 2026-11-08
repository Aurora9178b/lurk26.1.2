package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;

public class BlinkFakeLag extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> maxPackets = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("MaxPackets")
            .description("Maximum packets to queue before flushing.")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 100)
            .build()
      );
   private final Setting<Boolean> pauseOnAttack = this.sgGeneral
      .add(new BoolSetting.Builder().name("Pause on Attack").description("Stop blinking when attacking.").defaultValue(false).build());
   private final Queue<Packet<?>> storedPackets = new LinkedList<>();
   private final AtomicBoolean sending = new AtomicBoolean(false);

   public BlinkFakeLag() {
      super(Categories.Combat, "blink-fake-lag", "Delay packets to create fake lag.");
   }

   @Override
   public void onActivate() {
      if (this.mc.player != null && this.mc.level != null) {
         this.storedPackets.clear();
         this.sending.set(false);
      } else {
         this.toggle();
      }
   }

   @Override
   public void onDeactivate() {
      this.flushPackets();
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.storedPackets.size());
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.isActive() && this.mc.player != null && this.mc.level != null && !this.sending.get()) {
         if (!this.shouldIgnore(event.packet)) {
            event.cancel();
            this.storedPackets.add(event.packet);
            if (this.storedPackets.size() >= this.maxPackets.get()) {
               this.flushPackets();
            }
         }
      }
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (this.pauseOnAttack.get()) {
         this.flushPackets();
      }
   }

   private void flushPackets() {
      if (this.mc.player != null && this.mc.getConnection() != null) {
         this.sending.set(true);

         while (!this.storedPackets.isEmpty()) {
            this.mc.getConnection().send(this.storedPackets.poll());
         }

         this.sending.set(false);
      }
   }

   private boolean shouldIgnore(Packet<?> packet) {
      return packet instanceof ServerboundChatPacket
         || packet instanceof ServerboundChatCommandPacket
         || packet instanceof ServerboundKeepAlivePacket
         || packet instanceof ServerboundAcceptTeleportationPacket
         || packet instanceof ServerboundSeenAdvancementsPacket
         || packet instanceof ServerboundClientCommandPacket;
   }
}
