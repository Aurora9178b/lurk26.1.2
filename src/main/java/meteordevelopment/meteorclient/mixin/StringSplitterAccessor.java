package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.StringSplitter;
import net.minecraft.client.StringSplitter.WidthProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StringSplitter.class)
public interface StringSplitterAccessor {
   @Accessor("widthProvider")
   WidthProvider meteor$getWidthProvider();
}
