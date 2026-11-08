package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MutableComponent.class)
public abstract class MutableComponentMixin implements IComponent {
   @Shadow
   @Nullable
   private Language decomposedWith;

   @Override
   public void meteor$invalidateCache() {
      this.decomposedWith = null;
   }
}
