package meteordevelopment.meteorclient.mixin.sodium;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import meteordevelopment.meteorclient.utils.render.MeshBuilderVertexConsumerProvider;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = MeshBuilderVertexConsumerProvider.MeshBuilderVertexConsumer.class, remap = false)
public abstract class MeshVertexConsumerMixin implements VertexConsumer, VertexBufferWriter {
   public void push(MemoryStack stack, long ptr, int count, VertexFormat format) {
      int positionOffset = format.getOffset(VertexFormatElement.POSITION);
      if (positionOffset != -1) {
         for (int i = 0; i < count; i++) {
            long positionPtr = ptr + (long)format.getVertexSize() * i + positionOffset;
            float x = MemoryUtil.memGetFloat(positionPtr);
            float y = MemoryUtil.memGetFloat(positionPtr + 4L);
            float z = MemoryUtil.memGetFloat(positionPtr + 8L);
            this.addVertex(x, y, z);
         }
      }
   }
}
