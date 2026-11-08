package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.MacroArgumentType;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;

public class MacroCommand extends Command {
   List<ScheduledMacro> scheduleQueue = new ArrayList<>();
   List<ScheduledMacro> scheduledMacros = new ArrayList<>();

   public MacroCommand() {
      super("macro", "Allows you to execute macros.");
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("clear").executes(var1x -> {
         if (this.scheduleQueue.isEmpty() && this.scheduledMacros.isEmpty()) {
            this.error("No macros are currently scheduled.");
            return 1;
         } else {
            this.clearAll();
            this.info("Cleared all scheduled macros.");
            return 1;
         }
      }).then(argument("macro", MacroArgumentType.create()).executes(context -> {
         Macro macro = MacroArgumentType.get(context);
         if (!this.isScheduled(macro)) {
            this.error("This macro is not currently scheduled.");
            return 1;
         } else {
            this.clear(macro);
            this.info("Cleared scheduled macro.");
            return 1;
         }
      }))).then(argument("macro", MacroArgumentType.create()).executes(context -> {
         Macro macro = MacroArgumentType.get(context);
         this.scheduleQueue.add(new ScheduledMacro(0, macro));
         return 1;
      }).then(argument("delay", TimeArgument.time()).executes(context -> {
         Macro macro = MacroArgumentType.get(context);
         this.scheduleQueue.add(new ScheduledMacro(IntegerArgumentType.getInteger(context, "delay"), macro));
         return 1;
      })));
   }

   public void clearAll() {
      this.scheduleQueue.clear();
      this.scheduledMacros.clear();
   }

   public boolean isScheduled(Macro macro) {
      return this.scheduleQueue.stream().anyMatch(element -> element.macro == macro)
         || this.scheduledMacros.stream().anyMatch(element -> element.macro == macro);
   }

   public void clear(Macro macro) {
      this.scheduleQueue.removeIf(scheduledMacro -> scheduledMacro.macro == macro);
      this.scheduledMacros.removeIf(scheduledMacro -> scheduledMacro.macro == macro);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (!this.scheduleQueue.isEmpty()) {
         this.scheduledMacros.addAll(this.scheduleQueue);
         this.scheduleQueue.clear();
      }

      if (!this.scheduledMacros.isEmpty()) {
         this.runMacros();
      }

      this.scheduledMacros.forEach(ScheduledMacro::tick);
   }

   private void runMacros() {
      this.scheduledMacros.removeIf(ScheduledMacro::run);
   }
}
