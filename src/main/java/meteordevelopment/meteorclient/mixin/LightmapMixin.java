package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import meteordevelopment.meteorclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Lightmap.class)
public abstract class LightmapMixin {
   @Shadow
   @Final
   private GpuTexture texture;

   @Inject(method = "render", at = @At("HEAD"), cancellable = true)
   private void render$fullbright(LightmapRenderState renderState, CallbackInfo ci) {
      if (Modules.get().get(Fullbright.class).getGamma() || Modules.get().isActive(Xray.class)) {
         ProfilerFiller profile = Profiler.get();
         profile.push("lightmap");
         RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.texture, ARGB.color(255, 255, 255, 255));
         profile.pop();
         ci.cancel();
      }
   }
}
