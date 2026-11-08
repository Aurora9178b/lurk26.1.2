package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LayerRenderState.class)
public interface LayerRenderStateAccessor {
   @Accessor("itemTransform")
   ItemTransform meteor$getTransform();
}
