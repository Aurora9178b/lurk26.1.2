package meteordevelopment.meteorclient.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.notebot.decoder.SongDecoders;
import net.minecraft.commands.SharedSuggestionProvider;

public class NotebotSongArgumentType implements ArgumentType<Path> {
   private static final NotebotSongArgumentType INSTANCE = new NotebotSongArgumentType();

   public static NotebotSongArgumentType create() {
      return INSTANCE;
   }

   private NotebotSongArgumentType() {
   }

   public Path parse(StringReader reader) throws CommandSyntaxException {
      String text = reader.getRemaining();
      reader.setCursor(reader.getTotalLength());
      return MeteorClient.FOLDER.toPath().resolve("notebot/" + text);
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      try (Stream<Path> suggestions = Files.list(MeteorClient.FOLDER.toPath().resolve("notebot"))) {
         return SharedSuggestionProvider.suggest(suggestions.filter(SongDecoders::hasDecoder).map(path -> path.getFileName().toString()), builder);
      } catch (IOException var8) {
         return Suggestions.empty();
      }
   }
}
