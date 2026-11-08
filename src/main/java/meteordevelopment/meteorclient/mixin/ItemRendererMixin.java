package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemRendererMixin {
   @ModifyVariable(method = "renderItem", at = @At("STORE"), name = "foilType")
   private FoilType modifyEnchant(FoilType foilType) {
      return Modules.get().get(NoRender.class).noEnchantGlint() ? FoilType.NONE : foilType;
   }
}
