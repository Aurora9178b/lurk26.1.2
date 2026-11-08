package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Map.Entry;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.ARGB;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class MeshRenderer {
   private static final MeshRenderer INSTANCE = new MeshRenderer();
   private static boolean taken;
   private GpuTextureView colorAttachment;
   private GpuTextureView depthAttachment;
   private Color clearColor;
   private RenderPipeline pipeline;
   @Nullable
   private MeshBuilder mesh;
   @Nullable
   private GpuBuffer vertexBuffer;
   @Nullable
   private GpuBuffer indexBuffer;
   private Matrix4f matrix;
   private boolean scissorEnabled;
   private int scissorX;
   private int scissorY;
   private int scissorWidth;
   private int scissorHeight;
   private final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
   private final HashMap<String, Tuple<GpuTextureView, GpuSampler>> samplers = new HashMap<>();

   private MeshRenderer() {
   }

   public static MeshRenderer begin() {
      if (taken) {
         throw new IllegalStateException("Previous instance of MeshRenderer was not ended");
      }

      taken = true;
      return INSTANCE;
   }

   public MeshRenderer attachments(GpuTextureView color, GpuTextureView depth) {
      this.colorAttachment = color;
      this.depthAttachment = depth;
      return this;
   }

   public MeshRenderer attachments(RenderTarget framebuffer) {
      this.colorAttachment = framebuffer.getColorTextureView();
      this.depthAttachment = framebuffer.getDepthTextureView();
      return this;
   }

   public MeshRenderer scissor(int x, int y, int width, int height) {
      this.scissorEnabled = true;
      this.scissorX = x;
      this.scissorY = y;
      this.scissorWidth = Math.max(0, width);
      this.scissorHeight = Math.max(0, height);
      return this;
   }

   public MeshRenderer clearColor(Color color) {
      this.clearColor = color;
      return this;
   }

   public MeshRenderer pipeline(RenderPipeline pipeline) {
      this.pipeline = pipeline;
      return this;
   }

   public MeshRenderer mesh(GpuBuffer vertices, GpuBuffer indices) {
      this.vertexBuffer = vertices;
      this.indexBuffer = indices;
      return this;
   }

   public MeshRenderer mesh(MeshBuilder mesh) {
      this.mesh = mesh;
      return this;
   }

   public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
      this.mesh = mesh;
      return this.transform(matrix);
   }

   public MeshRenderer mesh(MeshBuilder mesh, PoseStack matrices) {
      this.mesh = mesh;
      return this.transform(matrices);
   }

   public MeshRenderer transform(Matrix4f matrix) {
      this.matrix = matrix;
      return this;
   }

   public MeshRenderer transform(PoseStack matrices) {
      this.matrix = matrices.last().pose();
      return this;
   }

   public MeshRenderer fullscreen() {
      return this.mesh(FullScreenRenderer.vbo, FullScreenRenderer.ibo);
   }

   public MeshRenderer uniform(String name, GpuBufferSlice slice) {
      this.uniforms.put(name, slice);
      return this;
   }

   public MeshRenderer sampler(String name, GpuTextureView view, GpuSampler sampler) {
      if (name != null && view != null && sampler != null) {
         this.samplers.put(name, new Tuple<>(view, sampler));
      }

      return this;
   }

   public void end() {
      if (this.mesh != null && this.mesh.isBuilding()) {
         this.mesh.end();
      }

      int indexCount = this.mesh != null ? this.mesh.getIndicesCount() : (int)(this.indexBuffer != null ? this.indexBuffer.size() / 4L : -1L);
      if (indexCount > 0) {
         if (Utils.rendering3D || this.matrix != null) {
            RenderSystem.getModelViewStack().pushMatrix();
         }

         if (this.matrix != null) {
            RenderSystem.getModelViewStack().mul(this.matrix);
         }

         if (Utils.rendering3D) {
            applyCameraPos();
         }

         GpuBuffer vertexBuffer = this.mesh != null ? this.mesh.getVertexBuffer() : this.vertexBuffer;
         GpuBuffer indexBuffer = this.mesh != null ? this.mesh.getIndexBuffer() : this.indexBuffer;
         OptionalInt clearColor = this.clearColor != null
            ? OptionalInt.of(ARGB.color(this.clearColor.a, this.clearColor.r, this.clearColor.g, this.clearColor.b))
            : OptionalInt.empty();
         GpuBufferSlice meshData = MeshUniforms.write(RenderUtils.projection, RenderSystem.getModelViewStack());
         RenderPass pass = this.depthAttachment != null && this.pipeline.wantsDepthTexture()
            ? RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Meteor MeshRenderer", this.colorAttachment, clearColor, this.depthAttachment, OptionalDouble.empty())
            : RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Meteor MeshRenderer", this.colorAttachment, clearColor);
         pass.setPipeline(this.pipeline);
         pass.setUniform("MeshData", meshData);
         if (this.scissorEnabled) {
            pass.enableScissor(this.scissorX, this.scissorY, this.scissorWidth, this.scissorHeight);
         }

         for (Entry<String, GpuBufferSlice> entry : this.uniforms.entrySet()) {
            pass.setUniform(entry.getKey(), entry.getValue());
         }

         for (Entry<String, Tuple<GpuTextureView, GpuSampler>> entry : this.samplers.entrySet()) {
            pass.bindTexture(entry.getKey(), entry.getValue().getA(), entry.getValue().getB());
         }

         pass.setVertexBuffer(0, vertexBuffer);
         pass.setIndexBuffer(indexBuffer, IndexType.INT);
         pass.drawIndexed(0, 0, indexCount, 1);
         pass.close();
         if (Utils.rendering3D || this.matrix != null) {
            RenderSystem.getModelViewStack().popMatrix();
         }
      }

      this.colorAttachment = null;
      this.depthAttachment = null;
      this.clearColor = null;
      this.pipeline = null;
      this.mesh = null;
      this.vertexBuffer = null;
      this.indexBuffer = null;
      this.matrix = null;
      this.scissorEnabled = false;
      this.scissorX = this.scissorY = this.scissorWidth = this.scissorHeight = 0;
      this.uniforms.clear();
      this.samplers.clear();
      taken = false;
   }

   private static void applyCameraPos() {
      Vec3 cameraPos = MeteorClient.mc.gameRenderer.getMainCamera().position();
      RenderSystem.getModelViewStack().translate(0.0F, (float)(-cameraPos.y), 0.0F);
   }
}
