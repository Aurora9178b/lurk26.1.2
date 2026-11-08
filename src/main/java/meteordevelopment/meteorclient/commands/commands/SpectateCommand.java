package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;

public class SpectateCommand extends Command {
   private final SpectateCommand.StaticListener shiftListener = new SpectateCommand.StaticListener();

   public SpectateCommand() {
      super("spectate", "Allows you to spectate nearby players");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("reset").executes(var0 -> {
         mc.setCameraEntity(mc.player);
         return 1;
      }));
      builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
         mc.setCameraEntity(PlayerArgumentType.get(context));
         mc.player.sendSystemMessage(Component.literal("Sneak to un-spectate."));
         MeteorClient.EVENT_BUS.subscribe(this.shiftListener);
         return 1;
      }));
   }

   private static class StaticListener {
      @EventHandler
      private void onKey(KeyInputEvent event) {
         if (Input.isPressed(SpectateCommand.mc.options.keyShift)) {
            SpectateCommand.mc.setCameraEntity(SpectateCommand.mc.player);
            event.cancel();
            MeteorClient.EVENT_BUS.unsubscribe(this);
         }
      }

      @EventHandler
      private void onMouse(MouseClickEvent event) {
         if (Input.isPressed(SpectateCommand.mc.options.keyShift)) {
            SpectateCommand.mc.setCameraEntity(SpectateCommand.mc.player);
            event.cancel();
            MeteorClient.EVENT_BUS.unsubscribe(this);
         }
      }
   }
}
