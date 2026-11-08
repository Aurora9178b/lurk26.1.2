package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.FireworkParticles.OverlayParticle;
import net.minecraft.client.particle.FireworkParticles.SparkParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SparkParticle.class, OverlayParticle.class})
public abstract class FireworksSparkParticleSubMixin {
   @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
   private void buildExplosionGeometry(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime, CallbackInfo ci) {
      if (Modules.get().get(NoRender.class).noFireworkExplosions()) {
         ci.cancel();
      }
   }
}
