package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowFeatureRenderer.class)
public abstract class ShadowFeatureRendererMixin {
   @Inject(method = "renderTranslucent", at = @At("HEAD"), cancellable = true)
   private void meteor$onRenderTranslucent(SubmitNodeCollection nodeCollection, BufferSource bufferSource, CallbackInfo ci) {
      if (nodeCollection.getShadowSubmits().isEmpty()) {
         ci.cancel();
      }
   }
}
