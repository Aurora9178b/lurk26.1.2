package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Fullbright;
import net.minecraft.client.renderer.LevelRenderer.BrightnessGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BrightnessGetter.class)
public interface BrightnessGetterMixin {
   @ModifyVariable(method = "lambda$static$0", at = @At("STORE"), name = "sky")
   private static int getLightmapCoordinatesModifySkyLight(int sky) {
      return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightLayer.SKY), sky);
   }

   @ModifyVariable(method = "lambda$static$0", at = @At("STORE"), name = "block")
   private static int getLightmapCoordinatesModifyBlockLight(int block) {
      return Math.max(Modules.get().get(Fullbright.class).getLuminance(LightLayer.BLOCK), block);
   }
}
