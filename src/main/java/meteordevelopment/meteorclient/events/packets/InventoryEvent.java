package meteordevelopment.meteorclient.events.packets;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;

public class InventoryEvent {
   private static final InventoryEvent INSTANCE = new InventoryEvent();
   public ClientboundContainerSetContentPacket packet;

   public static InventoryEvent get(ClientboundContainerSetContentPacket packet) {
      INSTANCE.packet = packet;
      return INSTANCE;
   }
}
