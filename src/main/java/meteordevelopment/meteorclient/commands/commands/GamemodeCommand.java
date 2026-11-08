package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.level.GameType;

public class GamemodeCommand extends Command {
   public GamemodeCommand() {
      super("gamemode", "Changes your gamemode client-side.", "gm");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      for (GameType gameMode : GameType.values()) {
         builder.then(literal(gameMode.getName()).executes(var1x -> {
            mc.gameMode.setLocalMode(gameMode);
            return 1;
         }));
      }
   }
}
