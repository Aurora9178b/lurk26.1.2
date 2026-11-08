package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class NoopImmediateVertexConsumerProvider extends BufferSource {
   public static final NoopImmediateVertexConsumerProvider INSTANCE = new NoopImmediateVertexConsumerProvider();

   private NoopImmediateVertexConsumerProvider() {
      super(null, null);
   }

   @Override
   public VertexConsumer getBuffer(RenderType layer) {
      return NoopVertexConsumer.INSTANCE;
   }

   @Override
   public void endBatch() {
   }

   @Override
   public void endBatch(RenderType layer) {
   }
}
