package meteordevelopment.meteorclient.commands.arguments;

import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class ProfileArgumentType implements ArgumentType<String> {
   private static final ProfileArgumentType INSTANCE = new ProfileArgumentType();
   private static final DynamicCommandExceptionType NO_SUCH_PROFILE = new DynamicCommandExceptionType(
      name -> Component.literal("Profile with name " + name + " doesn't exist.")
   );
   private static final Collection<String> EXAMPLES = List.of("pvp.meteorclient.com", "anarchy");

   public static ProfileArgumentType create() {
      return INSTANCE;
   }

   public static Profile get(CommandContext<?> context) {
      return Profiles.get().get(context.getArgument("profile", String.class));
   }

   private ProfileArgumentType() {
   }

   public String parse(StringReader reader) throws CommandSyntaxException {
      String argument = reader.getRemaining();
      reader.setCursor(reader.getTotalLength());
      if (Profiles.get().get(argument) == null) {
         throw NO_SUCH_PROFILE.create(argument);
      } else {
         return argument;
      }
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return SharedSuggestionProvider.suggest(Streams.stream(Profiles.get()).map(profile -> profile.name.get()), builder);
   }

   @Override
   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
