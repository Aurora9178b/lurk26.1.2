package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

public class FlagLogger extends Module {
   int count;

   public FlagLogger() {
      super(Categories.Player, "FlagLogger", "FlagLogger");
   }

   @EventHandler(priority = 201)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerPositionPacket) {
         this.count++;
         ChatUtils.sendMsg(
            Component.literal(
               ChatFormatting.WHITE + "Flag detected: " + ChatFormatting.GRAY + "[" + ChatFormatting.RED + this.count + ChatFormatting.GRAY + "]"
            )
         );
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.count = 0;
   }

   @Override
   public void onDeactivate() {
      this.count = 0;
   }
}
