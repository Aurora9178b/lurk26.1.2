package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.renderer.text.MeteorTextRenderable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlyphRenderState.class)
public class GlyphRenderStateMixin {
   @Inject(method = "textureSetup", at = @At("HEAD"), cancellable = true)
   private void onTextureSetup(CallbackInfoReturnable<TextureSetup> cir) {
      TextRenderable renderable = ((GlyphRenderState)(Object)(Object)this).renderable();
      if (renderable instanceof MeteorTextRenderable meteorRenderable) {
         cir.setReturnValue(TextureSetup.singleTexture(renderable.textureView(), meteorRenderable.meteor$fontSampler()));
      }
   }
}
