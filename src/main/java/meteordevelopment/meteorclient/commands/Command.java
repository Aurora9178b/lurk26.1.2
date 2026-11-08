package meteordevelopment.meteorclient.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;

public abstract class Command {
   protected static CommandBuildContext REGISTRY_ACCESS = net.minecraft.commands.Commands.createValidationContext(VanillaRegistries.createLookup());
   protected static final int SINGLE_SUCCESS = 1;
   protected static final Minecraft mc = MeteorClient.mc;
   private final String name;
   private final String title;
   private final String description;
   private final List<String> aliases;

   public Command(String name, String description, String... aliases) {
      this.name = name;
      this.title = Utils.nameToTitle(name);
      this.description = description;
      this.aliases = List.of(aliases);
   }

   protected static <T> RequiredArgumentBuilder<ClientSuggestionProvider, T> argument(String name, ArgumentType<T> type) {
      return RequiredArgumentBuilder.argument(name, type);
   }

   protected static LiteralArgumentBuilder<ClientSuggestionProvider> literal(String name) {
      return LiteralArgumentBuilder.literal(name);
   }

   public final void registerTo(CommandDispatcher<ClientSuggestionProvider> dispatcher) {
      this.register(dispatcher, this.name);

      for (String alias : this.aliases) {
         this.register(dispatcher, alias);
      }
   }

   public void register(CommandDispatcher<ClientSuggestionProvider> dispatcher, String name) {
      LiteralArgumentBuilder<ClientSuggestionProvider> builder = LiteralArgumentBuilder.literal(name);
      this.build(builder);
      dispatcher.register(builder);
   }

   public abstract void build(LiteralArgumentBuilder<ClientSuggestionProvider> var1);

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public List<String> getAliases() {
      return this.aliases;
   }

   @Override
   public String toString() {
      return Config.get().prefix.get() + this.name;
   }

   public String toString(String... args) {
      StringBuilder base = new StringBuilder(this.toString());

      for (String arg : args) {
         base.append(' ').append(arg);
      }

      return base.toString();
   }

   public void info(Component message) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.sendMsg(this.title, message);
   }

   public void info(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.infoPrefix(this.title, message, args);
   }

   public void warning(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.warningPrefix(this.title, message, args);
   }

   public void error(String message, Object... args) {
      ChatUtils.forceNextPrefixClass(this.getClass());
      ChatUtils.errorPrefix(this.title, message, args);
   }
}
