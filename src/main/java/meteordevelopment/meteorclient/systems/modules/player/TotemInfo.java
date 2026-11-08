package meteordevelopment.meteorclient.systems.modules.player;

import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.player.Player;

public class TotemInfo extends Module {
   private final Map<String, Integer> totemPops = new HashMap<>();

   public TotemInfo() {
      super(Categories.Player, "TotemPops", "Counts totem pops for all players.");
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.totemPops.clear();
   }

   @EventHandler(priority = 100)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (this.mc.level != null) {
         if (event.packet instanceof ClientboundEntityEventPacket packet
            && packet.getEventId() == 35
            && packet.getEntity(this.mc.level) instanceof Player entity) {
            String entityName = entity.getName().getString();
            int count = this.totemPops.getOrDefault(entityName, 0) + 1;
            this.totemPops.put(entityName, count);
            MutableComponent countText = Component.literal(String.valueOf(count)).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(9418239)));
            this.mc.execute(() -> {
               MutableComponent msg = Component.literal(entityName + " popped ").append(countText).append(Component.literal(" totem"));
               ChatUtils.sendMsg(msg);
            });
         }
      }
   }

   @Override
   public void onDeactivate() {
      this.totemPops.clear();
   }
}
