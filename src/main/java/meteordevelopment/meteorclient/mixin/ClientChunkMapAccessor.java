package meteordevelopment.meteorclient.mixin;

import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.client.multiplayer.ClientChunkCache.Storage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Storage.class)
public interface ClientChunkMapAccessor {
   @Accessor("chunks")
   AtomicReferenceArray<LevelChunk> meteor$getChunks();
}
