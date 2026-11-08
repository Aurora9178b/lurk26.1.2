package meteordevelopment.meteorclient.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractBlockRenderContext.class, remap = false)
public abstract class AbstractBlockRenderContextMixin {
   @Shadow
   protected BlockState state;
   @Shadow
   protected BlockAndTintGetter level;
   @Shadow
   protected BlockPos pos;
   @Unique
   private Xray xray;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.xray = Modules.get().get(Xray.class);
   }

   @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
   private void meteor$forceXrayFace(Direction facing, CallbackInfoReturnable<Boolean> cir) {
      if (this.xray != null && this.xray.isActive() && !this.xray.isBlocked(this.state.getBlock(), null)) {
         cir.setReturnValue(true);
      }
   }

   @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
   private boolean shouldDrawSide(boolean original, Direction facing) {
      return this.xray.isActive() ? this.xray.modifyDrawSide(this.state, this.level, this.pos, facing, original) : original;
   }
}
