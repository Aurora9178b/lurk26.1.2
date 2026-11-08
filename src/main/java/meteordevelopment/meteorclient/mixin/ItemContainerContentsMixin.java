package meteordevelopment.meteorclient.mixin;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemContainerContents.class)
public abstract class ItemContainerContentsMixin {
   @Shadow
   @Final
   private List<Optional<ItemStackTemplate>> items;

   @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
   private void onAddToTooltip(TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components, CallbackInfo ci) {
      if (Modules.get() != null) {
         BetterTooltips tooltips = Modules.get().get(BetterTooltips.class);
         if (tooltips.isActive()) {
            if (tooltips.previewShulkers()) {
               ci.cancel();
            } else if (tooltips.shulkerCompactTooltip()) {
               ci.cancel();
               tooltips.applyCompactShulkerTooltip(this.items, consumer);
            }
         }
      }
   }
}
