package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface ILevelRenderer {
   void meteor$pushEntityOutlineFramebuffer(RenderTarget var1);

   void meteor$popEntityOutlineFramebuffer();
}
