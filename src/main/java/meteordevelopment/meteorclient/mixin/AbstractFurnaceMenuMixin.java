package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IAbstractFurnaceMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceMenu.class)
public abstract class AbstractFurnaceMenuMixin implements IAbstractFurnaceMenu {
   @Shadow
   protected abstract boolean canSmelt(ItemStack var1);

   @Override
   public boolean meteor$canSmelt(ItemStack itemStack) {
      return this.canSmelt(itemStack);
   }
}
