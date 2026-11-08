package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform;
import org.lwjgl.glfw.GLFW;

public abstract class PostProcessShader {
   protected final RenderPipeline pipeline;
   public final RenderTarget framebuffer;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec2().putFloat().get();
   private static final DynamicUniformStorage<PostProcessShader.UniformData> UNIFORM_STORAGE = new DynamicUniformStorage<>(
      "Meteor - Post UBO", UNIFORM_SIZE, 16
   );

   protected PostProcessShader(RenderPipeline pipeline) {
      this.pipeline = pipeline;
      this.framebuffer = new TextureTarget(
         MeteorClient.NAME + " PostProcessShader " + this.getClass().getSimpleName(),
         MeteorClient.mc.getWindow().getWidth(),
         MeteorClient.mc.getWindow().getHeight(),
         true
      );
   }

   protected abstract boolean shouldDraw();

   protected void preDraw() {
   }

   protected void postDraw() {
   }

   protected abstract void setupPass(MeshRenderer var1);

   public void clearTexture() {
      if (this.shouldDraw()) {
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.framebuffer.getColorTexture(), 0);
      }
   }

   public void submitVertices(Runnable draw) {
      if (this.shouldDraw()) {
         this.preDraw();
         draw.run();
         this.postDraw();
      }
   }

   public void render() {
      if (this.shouldDraw()) {
         MeshRenderer renderer = MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(this.pipeline)
            .fullscreen()
            .uniform(
               "PostData",
               UNIFORM_STORAGE.writeUniform(
                  new PostProcessShader.UniformData(MeteorClient.mc.getWindow().getWidth(), MeteorClient.mc.getWindow().getHeight(), (float)GLFW.glfwGetTime())
               )
            )
            .sampler("u_Texture", this.framebuffer.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
         this.setupPass(renderer);
         renderer.end();
      }
   }

   public void onResized(int width, int height) {
      if (this.framebuffer != null) {
         this.framebuffer.resize(width, height);
      }
   }

   public static void flipFrame() {
      UNIFORM_STORAGE.endFrame();
   }

   private record UniformData(float sizeX, float sizeY, float time) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer).putVec2(this.sizeX, this.sizeY).putFloat(this.time);
      }
   }
}
