package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class WrapperImmediateVertexConsumerProvider extends BufferSource {
   private final Supplier<MultiBufferSource> supplier;

   public WrapperImmediateVertexConsumerProvider(Supplier<MultiBufferSource> supplier) {
      super(null, null);
      this.supplier = supplier;
   }

   @Override
   public VertexConsumer getBuffer(RenderType layer) {
      return this.supplier.get().getBuffer(layer);
   }

   @Override
   public void endBatch() {
   }

   @Override
   public void endBatch(RenderType layer) {
   }
}
