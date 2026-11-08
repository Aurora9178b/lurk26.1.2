package meteordevelopment.meteorclient.events.entity;

import net.minecraft.world.entity.Entity;

public class EntityRemovedEvent {
   private static final EntityRemovedEvent INSTANCE = new EntityRemovedEvent();
   public Entity entity;

   public static EntityRemovedEvent get(Entity entity) {
      INSTANCE.entity = entity;
      return INSTANCE;
   }
}
