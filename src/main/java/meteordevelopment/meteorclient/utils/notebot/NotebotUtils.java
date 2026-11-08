package meteordevelopment.meteorclient.utils.notebot;

import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.utils.notebot.instrumentdetect.InstrumentDetectFunction;
import meteordevelopment.meteorclient.utils.notebot.song.Note;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import org.jetbrains.annotations.Nullable;

public class NotebotUtils {
   public static Note getNoteFromNoteBlock(
      BlockState noteBlock, BlockPos blockPos, NotebotUtils.NotebotMode mode, InstrumentDetectFunction instrumentDetectFunction
   ) {
      NoteBlockInstrument instrument = null;
      int level = noteBlock.getValue(NoteBlock.NOTE);
      if (mode == NotebotUtils.NotebotMode.ExactInstruments) {
         instrument = instrumentDetectFunction.detectInstrument(noteBlock, blockPos);
      }

      return new Note(instrument, level);
   }

   public enum NotebotMode {
      AnyInstrument,
      ExactInstruments;
   }

   public enum OptionalInstrument {
      None(null),
      Harp(NoteBlockInstrument.HARP),
      Basedrum(NoteBlockInstrument.BASEDRUM),
      Snare(NoteBlockInstrument.SNARE),
      Hat(NoteBlockInstrument.HAT),
      Bass(NoteBlockInstrument.BASS),
      Flute(NoteBlockInstrument.FLUTE),
      Bell(NoteBlockInstrument.BELL),
      Guitar(NoteBlockInstrument.GUITAR),
      Chime(NoteBlockInstrument.CHIME),
      Xylophone(NoteBlockInstrument.XYLOPHONE),
      IronXylophone(NoteBlockInstrument.IRON_XYLOPHONE),
      CowBell(NoteBlockInstrument.COW_BELL),
      Didgeridoo(NoteBlockInstrument.DIDGERIDOO),
      Bit(NoteBlockInstrument.BIT),
      Banjo(NoteBlockInstrument.BANJO),
      Pling(NoteBlockInstrument.PLING);

      public static final Map<NoteBlockInstrument, NotebotUtils.OptionalInstrument> BY_MINECRAFT_INSTRUMENT = new HashMap<>();
      private final NoteBlockInstrument minecraftInstrument;

      OptionalInstrument(@Nullable NoteBlockInstrument minecraftInstrument) {
         this.minecraftInstrument = minecraftInstrument;
      }

      public NoteBlockInstrument toMinecraftInstrument() {
         return this.minecraftInstrument;
      }

      public static NotebotUtils.OptionalInstrument fromMinecraftInstrument(NoteBlockInstrument instrument) {
         return instrument != null ? BY_MINECRAFT_INSTRUMENT.get(instrument) : null;
      }

      static {
         for (NotebotUtils.OptionalInstrument optionalInstrument : values()) {
            BY_MINECRAFT_INSTRUMENT.put(optionalInstrument.minecraftInstrument, optionalInstrument);
         }
      }
   }
}
