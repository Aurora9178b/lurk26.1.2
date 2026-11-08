package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.ILevelRenderer;
import meteordevelopment.meteorclient.utils.render.CustomOutlineVertexConsumerProvider;
import net.minecraft.world.entity.Entity;

public abstract class EntityShader extends PostProcessShader {
   public final CustomOutlineVertexConsumerProvider vertexConsumerProvider = new CustomOutlineVertexConsumerProvider();

   protected EntityShader(RenderPipeline pipeline) {
      super(pipeline);
   }

   public abstract boolean shouldDraw(Entity var1);

   @Override
   protected void preDraw() {
      ((ILevelRenderer)MeteorClient.mc.levelRenderer).meteor$pushEntityOutlineFramebuffer(this.framebuffer);
   }

   @Override
   protected void postDraw() {
      ((ILevelRenderer)MeteorClient.mc.levelRenderer).meteor$popEntityOutlineFramebuffer();
   }

   public void submitVertices() {
      this.submitVertices(this.vertexConsumerProvider::draw);
   }
}
