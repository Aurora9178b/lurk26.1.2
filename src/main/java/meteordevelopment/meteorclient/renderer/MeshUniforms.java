package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform;
import org.joml.Matrix4f;

public class MeshUniforms {
   public static final int SIZE = new Std140SizeCalculator().putMat4f().putMat4f().get();
   private static final MeshUniforms.Data DATA = new MeshUniforms.Data();
   private static final DynamicUniformStorage<MeshUniforms.Data> STORAGE = new DynamicUniformStorage<>("Meteor - Mesh UBO", SIZE, 16);

   public static void flipFrame() {
      STORAGE.endFrame();
   }

   public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
      DATA.proj = proj;
      DATA.modelView = modelView;
      return STORAGE.writeUniform(DATA);
   }

   private static final class Data implements DynamicUniform {
      private Matrix4f proj;
      private Matrix4f modelView;

      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer).putMat4f(this.proj).putMat4f(this.modelView);
      }

      @Override
      public boolean equals(Object o) {
         return false;
      }
   }
}
