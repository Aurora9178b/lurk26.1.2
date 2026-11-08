package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.systems.RenderPassBackend;

public interface IGpuDevice {
   void meteor$pushScissor(int var1, int var2, int var3, int var4);

   void meteor$popScissor();

   @Deprecated
   void meteor$onCreateRenderPass(RenderPassBackend var1);
}
