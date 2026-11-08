package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
   @Inject(
      method = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
      at = @At("TAIL")
   )
   private void meteor$reloadPipelines(CallbackInfo ci) {
      MeteorRenderPipelines.precompile((ShaderManager)(Object)(Object)this);
   }
}
