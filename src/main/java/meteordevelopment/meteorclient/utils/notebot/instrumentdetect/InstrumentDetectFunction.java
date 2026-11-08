package meteordevelopment.meteorclient.utils.notebot.instrumentdetect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

public interface InstrumentDetectFunction {
   NoteBlockInstrument detectInstrument(BlockState var1, BlockPos var2);
}
