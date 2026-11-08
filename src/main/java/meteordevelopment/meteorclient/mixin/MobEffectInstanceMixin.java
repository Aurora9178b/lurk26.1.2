package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.PotionSaver;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin {
   @Inject(method = "tickDownDuration", at = @At("HEAD"), cancellable = true)
   private void tick(CallbackInfo ci) {
      if (Utils.canUpdate()) {
         if (Modules.get().get(PotionSaver.class).shouldFreeze(((MobEffectInstance)(Object)(Object)this).getEffect().value())) {
            ci.cancel();
         }
      }
   }
}
