package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.InventoryTweaks;
import net.minecraft.client.gui.BundleMouseActions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BundleMouseActions.class)
public abstract class BundleMouseActionsMixin {
   @ModifyExpressionValue(
      method = "toggleSelectedBundleItem",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BundleItem;getNumberOfItemsToShow(Lnet/minecraft/world/item/ItemStack;)I")
   )
   private int uncapBundleScrolling1(int original, ItemStack bundleItem, int slotIndex, int selectedItem) {
      return Modules.get().get(InventoryTweaks.class).uncapBundleScrolling()
         ? bundleItem.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size()
         : original;
   }

   @ModifyExpressionValue(
      method = "onMouseScrolled",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BundleItem;getNumberOfItemsToShow(Lnet/minecraft/world/item/ItemStack;)I")
   )
   private int uncapBundleScrolling2(int original, double scrollX, double scrollY, int slotIndex, ItemStack itemStack) {
      return Modules.get().get(InventoryTweaks.class).uncapBundleScrolling()
         ? itemStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).size()
         : original;
   }
}
