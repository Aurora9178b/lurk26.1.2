package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements IEntityRenderState {
   @Unique
   private Entity entity;

   @Nullable("EntityCulling mod can prevent the code that sets the entity from running")
   @Override
   public Entity meteor$getEntity() {
      return this.entity;
   }

   @Override
   public void meteor$setEntity(Entity entity) {
      this.entity = entity;
   }
}
