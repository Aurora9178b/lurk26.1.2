package meteordevelopment.meteorclient.mixin;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Direction.class)
public interface DirectionAccessor {
   @Accessor("BY_2D_DATA")
   static Direction[] meteor$getHorizontal() {
      throw new AssertionError();
   }
}
