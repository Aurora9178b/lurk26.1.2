package meteordevelopment.meteorclient.events.packets;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

public class ContainerSlotUpdateEvent {
   private static final ContainerSlotUpdateEvent INSTANCE = new ContainerSlotUpdateEvent();
   public ClientboundContainerSetSlotPacket packet;

   public static ContainerSlotUpdateEvent get(ClientboundContainerSetSlotPacket packet) {
      INSTANCE.packet = packet;
      return INSTANCE;
   }
}
