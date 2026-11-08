package meteordevelopment.meteorclient.utils.notebot.instrumentdetect;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.NoteBlock;

public enum InstrumentDetectMode {
   BlockState((noteBlock, var1) -> noteBlock.getValue(NoteBlock.INSTRUMENT)),
   BelowBlock((var0, blockPos) -> Minecraft.getInstance().level.getBlockState(blockPos.below()).instrument());

   private final InstrumentDetectFunction instrumentDetectFunction;

   InstrumentDetectMode(InstrumentDetectFunction instrumentDetectFunction) {
      this.instrumentDetectFunction = instrumentDetectFunction;
   }

   public InstrumentDetectFunction getInstrumentDetectFunction() {
      return this.instrumentDetectFunction;
   }
}
