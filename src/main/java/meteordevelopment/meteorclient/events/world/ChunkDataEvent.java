package meteordevelopment.meteorclient.events.world;

import net.minecraft.world.level.chunk.LevelChunk;

public record ChunkDataEvent(LevelChunk chunk) {
}
