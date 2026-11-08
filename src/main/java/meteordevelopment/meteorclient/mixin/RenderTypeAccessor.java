package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderType.class)
public interface RenderTypeAccessor {
   @Accessor("state")
   RenderSetup getState();

   @Invoker("create")
   static RenderType create(String name, RenderSetup state) {
      throw new AssertionError();
   }
}
