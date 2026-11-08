package meteordevelopment.meteorclient.utils.world;

import java.util.Iterator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.ClientChunkCacheAccessor;
import meteordevelopment.meteorclient.mixin.ClientChunkMapAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkIterator implements Iterator<ChunkAccess> {
   private final ClientChunkMapAccessor map = (ClientChunkMapAccessor)(Object)((ClientChunkCacheAccessor)MeteorClient.mc.level.getChunkSource()).meteor$getStorage();
   private final boolean onlyWithLoadedNeighbours;
   private int i = 0;
   private ChunkAccess chunk;

   public ChunkIterator(boolean onlyWithLoadedNeighbours) {
      this.onlyWithLoadedNeighbours = onlyWithLoadedNeighbours;
      this.getNext();
   }

   private ChunkAccess getNext() {
      ChunkAccess prev = this.chunk;
      this.chunk = null;

      while (this.i < this.map.meteor$getChunks().length()) {
         this.chunk = this.map.meteor$getChunks().get(this.i++);
         if (this.chunk != null && (!this.onlyWithLoadedNeighbours || this.isInRadius(this.chunk))) {
            break;
         }
      }

      return prev;
   }

   private boolean isInRadius(ChunkAccess chunk) {
      int x = chunk.getPos().x();
      int z = chunk.getPos().z();
      return MeteorClient.mc.level.getChunkSource().hasChunk(x + 1, z)
         && MeteorClient.mc.level.getChunkSource().hasChunk(x - 1, z)
         && MeteorClient.mc.level.getChunkSource().hasChunk(x, z + 1)
         && MeteorClient.mc.level.getChunkSource().hasChunk(x, z - 1);
   }

   @Override
   public boolean hasNext() {
      return this.chunk != null;
   }

   public ChunkAccess next() {
      return this.getNext();
   }
}
