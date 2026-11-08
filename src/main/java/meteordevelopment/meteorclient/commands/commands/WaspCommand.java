package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWasp;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class WaspCommand extends Command {
   private static final SimpleCommandExceptionType CANT_WASP_SELF = new SimpleCommandExceptionType(Component.literal("You cannot target yourself!"));

   public WaspCommand() {
      super("wasp", "Sets the auto wasp target.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      AutoWasp wasp = Modules.get().get(AutoWasp.class);
      builder.then(literal("reset").executes(var1x -> {
         wasp.disable();
         return 1;
      }));
      builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
         Player player = PlayerArgumentType.get(context);
         if (player == mc.player) {
            throw CANT_WASP_SELF.create();
         }

         wasp.target = player;
         wasp.enable();
         this.info(player.getName().getString() + " set as target.");
         return 1;
      }));
   }
}
