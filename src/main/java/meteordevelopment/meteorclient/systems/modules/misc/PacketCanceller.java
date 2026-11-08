package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;

public class PacketCanceller extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = this.sgGeneral
      .add(
         new PacketListSetting.Builder()
            .name("S2C-packets")
            .description("Server-to-client packets to cancel.")
            .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
            .build()
      );
   private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = this.sgGeneral
      .add(
         new PacketListSetting.Builder()
            .name("C2S-packets")
            .description("Client-to-server packets to cancel.")
            .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
            .build()
      );

   public PacketCanceller() {
      super(Categories.Misc, "packet-canceller", "Allows you to cancel certain packets.");
      this.runInMainMenu = true;
   }

   @EventHandler(priority = 201)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (this.s2cPackets.get().contains(event.packet.getClass())) {
         event.cancel();
      }
   }

   @EventHandler(priority = 201)
   private void onSendPacket(PacketEvent.Send event) {
      if (this.c2sPackets.get().contains(event.packet.getClass())) {
         event.cancel();
      }
   }
}
