package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.mixininterface.IOptionInstance;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class FovCommand extends Command {
   public FovCommand() {
      super("fov", "Changes your fov.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(argument("fov", IntegerArgumentType.integer(1, 180)).executes(context -> {
         ((IOptionInstance)(Object)mc.options.fov()).meteor$set(context.getArgument("fov", Integer.class));
         return 1;
      }));
   }
}
