package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class CustomOutlineVertexConsumerProvider implements MultiBufferSource {
   private final BufferSource immediate = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

   @Override
   public VertexConsumer getBuffer(RenderType layer) {
      if (layer.isOutline()) {
         return new CustomOutlineVertexConsumerProvider.CustomVertexConsumer(this.immediate.getBuffer(layer));
      }

      Optional<RenderType> optional = layer.outline();
      return optional.isPresent()
         ? new CustomOutlineVertexConsumerProvider.CustomVertexConsumer(this.immediate.getBuffer(optional.get()))
         : NoopVertexConsumer.INSTANCE;
   }

   public void draw() {
      this.immediate.endBatch();
   }

   private record CustomVertexConsumer(VertexConsumer consumer) implements VertexConsumer {
      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.consumer.addVertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         this.consumer.setColor(red, green, blue, alpha);
         return this;
      }

      @Override
      public VertexConsumer setColor(int argb) {
         this.consumer.setColor(argb);
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         this.consumer.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float width) {
         return this;
      }
   }
}
