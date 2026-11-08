package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadViewMutable;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DefaultFluidRenderer.class, remap = false)
public abstract class SodiumDefaultFluidRendererMixin {
   @Final
   @Shadow
   private int[] quadColors;
   @Unique
   private int xrayAlpha;
   @Unique
   private boolean forceXrayFluidSides;
   @Unique
   private Xray xray;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.xray = Modules.get().get(Xray.class);
   }

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void onRender(
      LevelSlice level,
      BlockState blockState,
      FluidState fluidState,
      BlockPos blockPos,
      BlockPos offset,
      TranslucentGeometryCollector collector,
      ChunkModelBuilder meshBuilder,
      Material material,
      ColorProvider<FluidState> colorProvider,
      FluidModel sprites,
      CallbackInfo ci
   ) {
      this.xrayAlpha = Xray.getFluidAlpha(fluidState, blockPos);
      this.forceXrayFluidSides = this.xray.isActive();
      if (this.xrayAlpha == 0) {
         this.forceXrayFluidSides = false;
         ci.cancel();
      }
   }

   @ModifyReturnValue(method = "isFullBlockFluidSideVisible", at = @At("RETURN"))
   private boolean onIsFullBlockFluidSideVisible(
      boolean original, BlockGetter view, BlockPos selfPos, Direction facing, FluidState fluid, @Local(name = "otherState") BlockState otherState
   ) {
      if (!original && this.forceXrayFluidSides && !facing.getAxis().isVertical()) {
         return !this.xray.isBlocked(otherState.getBlock(), null) ? false : !otherState.getFluidState().getType().isSame(fluid.getType());
      } else {
         return original;
      }
   }

   @ModifyReturnValue(
      method = "isFluidSideExposed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;F)Z",
      at = @At("RETURN")
   )
   private boolean onIsFluidSideExposed(boolean original, BlockState ownBlockState, BlockState neighborBlockState, Direction facing, float height) {
      if (!original && this.forceXrayFluidSides && !facing.getAxis().isVertical()) {
         return !this.xray.isBlocked(neighborBlockState.getBlock(), null)
            ? false
            : !neighborBlockState.getFluidState().getType().isSame(ownBlockState.getFluidState().getType());
      } else {
         return original;
      }
   }

   @Inject(method = "getUpFaceExposureByNeighbors", at = @At("HEAD"), cancellable = true)
   private void onGetUpFaceExposureByNeighbors(BlockAndTintGetter level, BlockPos origin, FluidState fluidState, CallbackInfoReturnable<Integer> cir) {
      if (this.forceXrayFluidSides) {
         cir.setReturnValue(3);
      }
   }

   @Inject(method = "updateQuad", at = @At("TAIL"))
   private void onUpdateQuad(
      ModelQuadViewMutable quad,
      LevelSlice level,
      BlockPos pos,
      LightPipeline lighter,
      Direction dir,
      ModelQuadFacing facing,
      float brightness,
      ColorProvider<FluidState> colorProvider,
      FluidState fluidState,
      CallbackInfo ci
   ) {
      if (this.xrayAlpha != -1) {
         for (int i = 0; i < 4; i++) {
            this.quadColors[i] = this.quadColors[i] & 16777215 | this.xrayAlpha << 24;
         }
      }
   }

   @ModifyArg(
      method = "render",
      at = @At(
         value = "INVOKE",
         target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/DefaultFluidRenderer;writeQuad(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Lnet/caffeinemc/mods/sodium/client/render/chunk/translucent_sorting/TranslucentGeometryCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;Lnet/minecraft/core/BlockPos;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;Lnet/caffeinemc/mods/sodium/client/model/quad/properties/ModelQuadFacing;Z)V"
      ),
      index = 2
   )
   private Material modifyMaterial(Material material) {
      return this.xrayAlpha != -1 ? DefaultMaterials.TRANSLUCENT : material;
   }
}
