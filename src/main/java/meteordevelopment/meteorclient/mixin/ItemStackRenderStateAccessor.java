package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
   @Accessor("activeLayerCount")
   int meteor$getActiveLayerCount();

   @Accessor("layers")
   LayerRenderState[] meteor$getLayers();
}
