package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {
   @ModifyExpressionValue(method = "submitSignText", at = @At(value = "CONSTANT", args = {"intValue=4", "ordinal=1"}))
   private int loopTextLengthProxy(int i) {
      return Modules.get().get(NoRender.class).noSignText() ? 0 : i;
   }
}
