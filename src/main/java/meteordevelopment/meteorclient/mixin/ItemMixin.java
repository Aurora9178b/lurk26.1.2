package meteordevelopment.meteorclient.mixin;

import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.TooltipDataEvent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemMixin {
   @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
   private void onTooltipData(ItemStack itemStack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
      TooltipDataEvent event = (TooltipDataEvent)MeteorClient.EVENT_BUS.post(TooltipDataEvent.get(itemStack));
      if (event.tooltipData != null) {
         cir.setReturnValue(Optional.of(event.tooltipData));
      }
   }
}
