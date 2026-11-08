package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ResetCommand extends Command {
   public ResetCommand() {
      super("reset", "Resets specified settings.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("settings").then(argument("module", ModuleArgumentType.create()).executes(context -> {
         Module module = context.getArgument("module", Module.class);
         module.settings.forEach(group -> group.forEach(Setting::reset));
         module.info("Reset all settings.");
         return 1;
      })).then(literal("all").executes(var0 -> {
         Modules.get().getAll().forEach(module -> module.settings.forEach(group -> group.forEach(Setting::reset)));
         ChatUtils.infoPrefix("Modules", "Reset all module settings");
         return 1;
      }))).then(literal("gui").executes(var0 -> {
         GuiThemes.get().clearWindowConfigs();
         GuiThemes.get().settings.reset();
         ChatUtils.info("Reset all GUI settings.");
         return 1;
      })).then(literal("bind").then(argument("module", ModuleArgumentType.create()).executes(context -> {
         Module module = context.getArgument("module", Module.class);
         module.keybind.reset();
         module.info("Reset bind.");
         return 1;
      })).then(literal("all").executes(var0 -> {
         Modules.get().getAll().forEach(module -> module.keybind.reset());
         ChatUtils.infoPrefix("Modules", "Reset all binds.");
         return 1;
      }))).then(literal("hud").executes(var0 -> {
         Hud.get().resetToDefaultElements();
         ChatUtils.infoPrefix("HUD", "Reset all elements.");
         return 1;
      }));
   }
}
