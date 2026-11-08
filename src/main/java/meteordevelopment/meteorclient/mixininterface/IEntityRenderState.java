package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IEntityRenderState {
   @Nullable("EntityCulling mod can prevent the code that sets the entity from running")
   Entity meteor$getEntity();

   void meteor$setEntity(Entity var1);
}
