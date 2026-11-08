package meteordevelopment.meteorclient.events.entity;

import net.minecraft.world.entity.Entity;

public class EntityAddedEvent {
   private static final EntityAddedEvent INSTANCE = new EntityAddedEvent();
   public Entity entity;

   public static EntityAddedEvent get(Entity entity) {
      INSTANCE.entity = entity;
      return INSTANCE;
   }
}
