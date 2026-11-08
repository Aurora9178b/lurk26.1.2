package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Component.class)
public interface ComponentMixin extends IComponent {
   @Override
   default void meteor$invalidateCache() {
   }
}
