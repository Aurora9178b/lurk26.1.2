package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.commands.arguments.SettingArgumentType;
import meteordevelopment.meteorclient.commands.arguments.SettingValueArgumentType;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.screens.LurkClickGuiScreen;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class SettingCommand extends Command {
   public SettingCommand() {
      super("settings", "Allows you to view and change module settings.", "s");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("hud").executes(var0 -> {
         LurkClickGuiScreen screen = new LurkClickGuiScreen(GuiThemes.get(), LurkClickGuiScreen.Page.HUD);
         screen.parent = null;
         Utils.screenToOpen = screen;
         return 1;
      }));
      builder.then(literal("config").executes(var0 -> {
         LurkClickGuiScreen screen = new LurkClickGuiScreen(GuiThemes.get(), LurkClickGuiScreen.Page.CONFIG);
         screen.parent = null;
         Utils.screenToOpen = screen;
         return 1;
      }));
      builder.then(
         literal("config")
            .then(
               argument("setting", SettingArgumentType.create())
                  .executes(context -> {
                     Setting<?> setting = SettingArgumentType.get(context, Config.get().settings);
                     ChatUtils.infoPrefix("Config", "Setting (highlight)%s(default) is (highlight)%s(default).", setting.title, setting.get());
                     return 1;
                  })
                  .suggests((var0, suggestionsBuilder) -> SettingArgumentType.listSuggestions(suggestionsBuilder, Config.get().settings))
                  .then(argument("value", SettingValueArgumentType.create()).executes(context -> {
                     Setting<?> setting = SettingArgumentType.get(context, Config.get().settings);
                     String value = SettingValueArgumentType.get(context);
                     if (setting.parse(value)) {
                        ChatUtils.infoPrefix("Config", "Setting (highlight)%s(default) changed to (highlight)%s(default).", setting.title, value);
                     }

                     return 1;
                  }).suggests((context, suggestionsBuilder) -> SettingValueArgumentType.listSuggestions(context, suggestionsBuilder, Config.get().settings)))
            )
      );
      builder.then(argument("module", ModuleArgumentType.create()).executes(context -> {
         Module module = context.getArgument("module", Module.class);
         WidgetScreen screen = GuiThemes.get().moduleScreen(module);
         screen.parent = null;
         Utils.screenToOpen = screen;
         return 1;
      }));
      builder.then(argument("module", ModuleArgumentType.create()).then(argument("setting", SettingArgumentType.create()).executes(context -> {
         Setting<?> setting = SettingArgumentType.get(context);
         ModuleArgumentType.get(context).info("Setting (highlight)%s(default) is (highlight)%s(default).", setting.title, setting.get());
         return 1;
      }).then(argument("value", SettingValueArgumentType.create()).executes(context -> {
         Setting<?> setting = SettingArgumentType.get(context);
         String value = SettingValueArgumentType.get(context);
         if (setting.parse(value)) {
            ModuleArgumentType.get(context).info("Setting (highlight)%s(default) changed to (highlight)%s(default).", setting.title, value);
         }

         return 1;
      }))));
   }
}
