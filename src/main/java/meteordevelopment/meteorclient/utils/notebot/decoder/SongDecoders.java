package meteordevelopment.meteorclient.utils.notebot.decoder;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.Notebot;
import meteordevelopment.meteorclient.utils.notebot.NotebotUtils;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import meteordevelopment.meteorclient.utils.notebot.song.Song;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

public class SongDecoders {
   private static final Map<String, SongDecoder> decoders = new HashMap<>();

   public static void registerDecoder(String extension, SongDecoder songDecoder) {
      decoders.put(extension, songDecoder);
   }

   public static SongDecoder getDecoder(File file) {
      return decoders.get(FilenameUtils.getExtension(file.getName()));
   }

   public static boolean hasDecoder(File file) {
      return decoders.containsKey(FilenameUtils.getExtension(file.getName()));
   }

   public static boolean hasDecoder(Path path) {
      return hasDecoder(path.toFile());
   }

   @NotNull
   public static Song parse(File file) throws Exception {
      if (!hasDecoder(file)) {
         throw new IllegalStateException("Decoder for this file does not exists!");
      }

      SongDecoder decoder = getDecoder(file);
      Song song = decoder.parse(file);
      fixSong(song);
      song.finishLoading();
      return song;
   }

   private static void fixSong(Song song) {
      Notebot notebot = Modules.get().get(Notebot.class);
      Iterator<Entry<Integer, Note>> iterator = song.getNotesMap().entries().iterator();

      while (iterator.hasNext()) {
         Entry<Integer, Note> entry = iterator.next();
         int tick = entry.getKey();
         Note note = entry.getValue();
         int n = note.getNoteLevel();
         if (n < 0 || n > 24) {
            if (!notebot.roundOutOfRange.get()) {
               notebot.warning("Note at tick %d out of range.", tick);
               iterator.remove();
               continue;
            }

            note.setNoteLevel(n < 0 ? 0 : 24);
         }

         if (notebot.mode.get() == NotebotUtils.NotebotMode.ExactInstruments) {
            NoteBlockInstrument newInstrument = notebot.getMappedInstrument(note.getInstrument());
            if (newInstrument != null) {
               note.setInstrument(newInstrument);
            }
         } else {
            note.setInstrument(null);
         }
      }
   }

   static {
      registerDecoder("nbs", new NBSSongDecoder());
      registerDecoder("txt", new TextSongDecoder());
   }
}
