package meteordevelopment.meteorclient.utils.notebot.song;

import java.util.Objects;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public class Note {
   private NoteBlockInstrument instrument;
   private int noteLevel;

   public Note(NoteBlockInstrument instrument, int noteLevel) {
      this.instrument = instrument;
      this.noteLevel = noteLevel;
   }

   public NoteBlockInstrument getInstrument() {
      return this.instrument;
   }

   public void setInstrument(NoteBlockInstrument instrument) {
      this.instrument = instrument;
   }

   public int getNoteLevel() {
      return this.noteLevel;
   }

   public void setNoteLevel(int noteLevel) {
      this.noteLevel = noteLevel;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Note note = (Note)o;
         return this.instrument == note.instrument && this.noteLevel == note.noteLevel;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.instrument, this.noteLevel);
   }

   @Override
   public String toString() {
      return "Note{instrument=" + this.getInstrument() + ", noteLevel=" + this.getNoteLevel() + "}";
   }
}
