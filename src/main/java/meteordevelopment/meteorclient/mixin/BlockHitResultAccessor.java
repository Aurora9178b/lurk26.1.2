package meteordevelopment.meteorclient.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockHitResult.class)
public interface BlockHitResultAccessor {
   @Mutable
   @Accessor("direction")
   void meteor$setDirection(Direction var1);
}
