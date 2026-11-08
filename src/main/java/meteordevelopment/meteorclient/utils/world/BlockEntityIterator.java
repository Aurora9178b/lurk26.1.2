package meteordevelopment.meteorclient.utils.world;

import java.util.Iterator;
import java.util.Map;
import meteordevelopment.meteorclient.mixin.ChunkAccessAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;

public class BlockEntityIterator implements Iterator<BlockEntity> {
   private final Iterator<ChunkAccess> chunks = new ChunkIterator(false);
   private Iterator<BlockEntity> blockEntities;

   public BlockEntityIterator() {
      this.nextChunk();
   }

   private void nextChunk() {
      while (this.chunks.hasNext()) {
         Map<BlockPos, BlockEntity> blockEntityMap = ((ChunkAccessAccessor)this.chunks.next()).getBlockEntities();
         if (!blockEntityMap.isEmpty()) {
            this.blockEntities = blockEntityMap.values().iterator();
            break;
         }
      }
   }

   @Override
   public boolean hasNext() {
      if (this.blockEntities == null) {
         return false;
      }

      if (this.blockEntities.hasNext()) {
         return true;
      }

      this.nextChunk();
      return this.blockEntities.hasNext();
   }

   public BlockEntity next() {
      return this.blockEntities.next();
   }
}
