package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockStateBase.class)
public abstract class AbstractBlockStateMixin {
   @Inject(method = "getOffset", at = @At("HEAD"), cancellable = true)
   private void modifyPos(BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
      if (Modules.get() != null) {
         if (Modules.get().get(NoRender.class).noTextureRotations()) {
            cir.setReturnValue(Vec3.ZERO);
         }
      }
   }
}
