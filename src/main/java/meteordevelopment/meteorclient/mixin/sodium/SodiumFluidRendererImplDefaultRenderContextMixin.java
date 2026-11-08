package meteordevelopment.meteorclient.mixin.sodium;

import java.util.Arrays;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.caffeinemc.mods.sodium.fabric.render.FluidRendererImpl$DefaultRenderContext", remap = false)
public abstract class SodiumFluidRendererImplDefaultRenderContextMixin {
   @Unique
   private Ambience ambience;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.ambience = Modules.get().get(Ambience.class);
   }

   @Inject(method = "getColorProvider", at = @At("HEAD"), cancellable = true)
   private void onGetColorProvider(Fluid fluid, @Nullable BlockTintSource blockTintSource, CallbackInfoReturnable<ColorProvider<FluidState>> cir) {
      if (this.ambience.isActive() && this.ambience.customLavaColor.get() && fluid.defaultFluidState().is(FluidTags.LAVA)) {
         cir.setReturnValue(this::lavaColorProvider);
      }
   }

   @Unique
   private void lavaColorProvider(
      LevelSlice slice, BlockPos pos, MutableBlockPos scratchPos, FluidState state, ModelQuadView quad, int[] output, boolean smooth
   ) {
      Color c = this.ambience.lavaColor.get();
      int alpha = Xray.getFluidAlpha(state, pos);
      Arrays.fill(output, Color.fromRGBA(c.r, c.g, c.b, alpha != -1 ? alpha : c.a));
   }
}
