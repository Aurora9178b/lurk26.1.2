package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.rendertype.RenderType;

public class MeshBuilderVertexConsumerProvider implements IVertexConsumerProvider {
   private final MeshBuilderVertexConsumerProvider.MeshBuilderVertexConsumer vertexConsumer;

   public MeshBuilderVertexConsumerProvider(MeshBuilder mesh) {
      this.vertexConsumer = new MeshBuilderVertexConsumerProvider.MeshBuilderVertexConsumer(mesh);
   }

   @Override
   public VertexConsumer getBuffer(RenderType layer) {
      return new MeshBuilderVertexConsumerProvider.W(this.vertexConsumer);
   }

   public void setColor(Color color) {
      this.vertexConsumer.fixedColor(color.r, color.g, color.b, color.a);
   }

   @Override
   public void setOffset(int offsetX, int offsetY, int offsetZ) {
      this.vertexConsumer.setOffset(offsetX, offsetY, offsetZ);
   }

   public static class MeshBuilderVertexConsumer implements VertexConsumer {
      private final MeshBuilder mesh;
      private int offsetX;
      private int offsetY;
      private int offsetZ;
      private final double[] xs = new double[4];
      private final double[] ys = new double[4];
      private final double[] zs = new double[4];
      private final Color color = new Color();
      private int i;

      public MeshBuilderVertexConsumer(MeshBuilder mesh) {
         this.mesh = mesh;
      }

      public void setOffset(int offsetX, int offsetY, int offsetZ) {
         this.offsetX = offsetX;
         this.offsetY = offsetY;
         this.offsetZ = offsetZ;
      }

      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.xs[this.i] = (double)this.offsetX + x;
         this.ys[this.i] = (double)this.offsetY + y;
         this.zs[this.i] = (double)this.offsetZ + z;
         if (++this.i >= 4) {
            this.mesh.ensureQuadCapacity();
            this.mesh
               .quad(
                  this.mesh.vec3(this.xs[0], this.ys[0], this.zs[0]).color(this.color).next(),
                  this.mesh.vec3(this.xs[1], this.ys[1], this.zs[1]).color(this.color).next(),
                  this.mesh.vec3(this.xs[2], this.ys[2], this.zs[2]).color(this.color).next(),
                  this.mesh.vec3(this.xs[3], this.ys[3], this.zs[3]).color(this.color).next()
               );
            this.i = 0;
         }

         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         return this;
      }

      @Override
      public VertexConsumer setColor(int argb) {
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
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
         return null;
      }

      @Override
      public VertexConsumer setLineWidth(float width) {
         return this;
      }

      public void fixedColor(int red, int green, int blue, int alpha) {
         this.color.set(red, green, blue, alpha);
      }
   }

   private record W(MeshBuilderVertexConsumerProvider.MeshBuilderVertexConsumer d) implements VertexConsumer {
      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         this.d.addVertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this;
      }

      @Override
      public VertexConsumer setColor(int c) {
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
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
      public VertexConsumer setLineWidth(float w) {
         return this;
      }
   }
}
