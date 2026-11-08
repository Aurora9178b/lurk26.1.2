package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.network.Capes;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ReloadCommand extends Command {
   public ReloadCommand() {
      super("reload", "Reloads many systems.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.executes(var1x -> {
         this.warning("Reloading systems, this may take a while.");
         Systems.load();
         Capes.init();
         Fonts.refresh();
         MeteorExecutor.execute(() -> Friends.get().forEach(Friend::updateInfo));
         return 1;
      });
   }
}
