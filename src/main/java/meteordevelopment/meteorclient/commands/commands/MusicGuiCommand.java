package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.MusicPlayerScreen;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.MusicPlayer;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class MusicGuiCommand extends Command {
   public MusicGuiCommand() {
      super("music-gui", "Opens the music player screen.", "musicgui", "MusicGUI", "music");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.executes(var1x -> {
         MusicPlayer musicPlayer = Modules.get().get(MusicPlayer.class);
         if (musicPlayer == null) {
            this.error("MusicPlayer module was not found.");
            return 1;
         } else {
            mc.setScreen(new MusicPlayerScreen(GuiThemes.get(), musicPlayer));
            return 1;
         }
      });
   }
}
