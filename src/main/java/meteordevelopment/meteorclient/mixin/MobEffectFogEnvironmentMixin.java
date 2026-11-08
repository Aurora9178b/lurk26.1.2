package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MobEffectFogEnvironmentMixin {
   @Shadow
   public abstract Holder<MobEffect> getMobEffect();

   @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
   private boolean modifyShouldApply(boolean original) {
      NoRender noRender = Modules.get().get(NoRender.class);
      if (this.getMobEffect() == MobEffects.BLINDNESS) {
         return original && !noRender.noBlindness();
      } else {
         return this.getMobEffect() != MobEffects.DARKNESS ? original : original && !noRender.noDarkness();
      }
   }
}
