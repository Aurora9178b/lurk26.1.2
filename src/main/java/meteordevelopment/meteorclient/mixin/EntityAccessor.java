package meteordevelopment.meteorclient.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
   @Accessor("wasTouchingWater")
   void meteor$setInWater(boolean var1);
}
