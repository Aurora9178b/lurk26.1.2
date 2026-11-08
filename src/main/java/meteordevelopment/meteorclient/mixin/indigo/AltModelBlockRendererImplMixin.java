package meteordevelopment.meteorclient.mixin.indigo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AltModelBlockRendererImpl.class)
public abstract class AltModelBlockRendererImplMixin {
   @Shadow
   private BlockState blockState;
   @Shadow
   private BlockPos pos;
   @Shadow
   private BlockAndTintGetter level;

   @ModifyReturnValue(method = "shouldCullFace", at = @At("RETURN"))
   private boolean shouldCullFace$xray(boolean original, Direction direction) {
      if (direction == null) {
         return original;
      }

      Xray xray = Modules.get().get(Xray.class);
      return xray.isActive() ? !xray.modifyDrawSide(this.blockState, this.level, this.pos, direction, !original) : original;
   }

   @Inject(method = "transform", at = @At("RETURN"), cancellable = true)
   private void transform$xray(MutableQuadView quad, CallbackInfoReturnable<Boolean> cir) {
      int alpha = Xray.getAlpha(this.blockState, this.pos);
      if (alpha == 0) {
         cir.setReturnValue(false);
      } else if (alpha != -1) {
         if (alpha > 0 && alpha < 255) {
            quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
         }

         for (int i = 0; i < 4; i++) {
            quad.color(i, rewriteQuadAlpha(quad.color(i), alpha));
         }
      }
   }

   @Unique
   private static int rewriteQuadAlpha(int color, int alpha) {
      return (alpha & 0xFF) << 24 | color & 16777215;
   }
}
