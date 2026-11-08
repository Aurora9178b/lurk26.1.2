package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import meteordevelopment.meteorclient.MeteorClient;

public class RegionRenderer {
   private GpuBuffer vbo;
   private GpuBuffer ibo;

   public RegionRenderer(float x, float y, float width, float height) {
      MeshBuilder mesh = new MeshBuilder(MeteorVertexFormats.POS2, Mode.TRIANGLES, 4, 6);
      float screenW = MeteorClient.mc.getWindow().getWidth() / MeteorClient.mc.getWindow().getGuiScale();
      float screenH = MeteorClient.mc.getWindow().getHeight() / MeteorClient.mc.getWindow().getGuiScale();
      float x1 = x / screenW * 2.0F - 1.0F;
      float y1 = 1.0F - y / screenH * 2.0F;
      float x2 = (x + width) / screenW * 2.0F - 1.0F;
      float y2 = 1.0F - (y + height) / screenH * 2.0F;
      mesh.begin();
      mesh.quad(mesh.vec2(x1, y1).next(), mesh.vec2(x1, y2).next(), mesh.vec2(x2, y2).next(), mesh.vec2(x2, y1).next());
      mesh.end();
      this.vbo = mesh.getVertexBuffer();
      this.ibo = mesh.getIndexBuffer();
   }

   public GpuBuffer getVbo() {
      return this.vbo;
   }

   public GpuBuffer getIbo() {
      return this.ibo;
   }
}
