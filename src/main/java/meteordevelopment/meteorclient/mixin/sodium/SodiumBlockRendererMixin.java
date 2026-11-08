package meteordevelopment.meteorclient.mixin.sodium;

import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererMixin {
   @Unique
   private int xrayAlpha;

   @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
   private void onRenderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
      this.xrayAlpha = Xray.getAlpha(state, pos);
      if (this.xrayAlpha == 0) {
         ci.cancel();
      }
   }

   @Inject(method = "bufferQuad", at = @At("HEAD"))
   private void onBufferQuad(MutableQuadViewImpl quad, float[] brightnesses, Material material, CallbackInfo ci) {
      if (this.xrayAlpha != -1) {
         for (int i = 0; i < 4; i++) {
            int color = quad.baseColor(i);
            quad.setColor(i, (this.xrayAlpha & 0xFF) << 24 | color & 16777215);
         }
      }
   }

   @ModifyArg(
      method = "processQuad",
      at = @At(
         value = "INVOKE",
         target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V"
      ),
      index = 2
   )
   private Material modifyMaterial(Material material) {
      return this.xrayAlpha != -1 ? DefaultMaterials.TRANSLUCENT : material;
   }
}
