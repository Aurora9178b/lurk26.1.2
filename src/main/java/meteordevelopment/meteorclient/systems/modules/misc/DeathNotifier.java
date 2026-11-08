package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class DeathNotifier extends Module {
   private boolean sent;

   public DeathNotifier() {
      super(Categories.Misc, "DeathNotifier", "Shows your death coordinates in chat.");
   }

   @Override
   public void onActivate() {
      this.sent = false;
   }

   @Override
   public void onDeactivate() {
      this.sent = false;
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.sent = false;
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.sent = false;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null) {
         boolean dead = this.mc.player.isDeadOrDying() || !this.mc.player.isAlive() || this.mc.player.getHealth() <= 0.0F;
         if (!dead) {
            this.sent = false;
         } else if (!this.sent) {
            BlockPos pos = this.mc.player.blockPosition();
            ChatUtils.sendMsg(Component.literal("Death:" + pos.getX() + "," + pos.getY() + "," + pos.getZ()));
            this.sent = true;
         }
      }
   }
}
