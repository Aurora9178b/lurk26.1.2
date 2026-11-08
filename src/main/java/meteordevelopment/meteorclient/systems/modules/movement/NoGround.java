package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoGround extends Module {
   public NoGround() {
      super(Categories.Movement, "NoGround", "Skid from CCBlueX");
   }

   @EventHandler
   private void onSentPacket(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundMovePlayerPacket) {
         ((ServerboundMovePlayerPacketAccessor)event.packet).meteor$setOnGround(false);
      }
   }
}
