package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ToggleCommand extends Command {
   public ToggleCommand() {
      super("toggle", "Toggles a module.", "t");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("all").then(literal("on").executes(var0 -> {
         new ArrayList<>(Modules.get().getAll()).forEach(Module::enable);
         Hud.get().active = true;
         return 1;
      })).then(literal("off").executes(var0 -> {
         new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);
         Hud.get().active = false;
         return 1;
      }))).then(argument("module", ModuleArgumentType.create()).executes(context -> {
         Module m = ModuleArgumentType.get(context);
         m.toggle();
         m.sendToggledMsg();
         return 1;
      }).then(literal("on").executes(context -> {
         Module m = ModuleArgumentType.get(context);
         m.enable();
         return 1;
      })).then(literal("off").executes(context -> {
         Module m = ModuleArgumentType.get(context);
         m.disable();
         return 1;
      }))).then(literal("hud").executes(var0 -> {
         Hud.get().active = !Hud.get().active;
         return 1;
      }).then(literal("on").executes(var0 -> {
         Hud.get().active = true;
         return 1;
      })).then(literal("off").executes(var0 -> {
         Hud.get().active = false;
         return 1;
      })));
   }
}
