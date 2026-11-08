package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderPassBackend;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GlDevice.class)
public abstract class GlDeviceMixin implements IGpuDevice {
   @Unique
   private int x;
   @Unique
   private int y;
   @Unique
   private int width;
   @Unique
   private int height;
   @Unique
   private boolean set;

   @Override
   public void meteor$pushScissor(int x, int y, int width, int height) {
      if (this.set) {
         throw new IllegalStateException("Currently there can only be one global scissor pushed");
      }

      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.set = true;
   }

   @Override
   public void meteor$popScissor() {
      if (!this.set) {
         throw new IllegalStateException("No scissor pushed");
      }

      this.set = false;
   }

   @Deprecated
   @Override
   public void meteor$onCreateRenderPass(RenderPassBackend backend) {
      if (this.set) {
         backend.enableScissor(this.x, this.y, this.width, this.height);
      }
   }
}
