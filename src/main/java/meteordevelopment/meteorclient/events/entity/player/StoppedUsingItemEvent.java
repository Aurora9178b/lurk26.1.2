package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.world.item.ItemStack;

public class StoppedUsingItemEvent {
   private static final StoppedUsingItemEvent INSTANCE = new StoppedUsingItemEvent();
   public ItemStack itemStack;

   public static StoppedUsingItemEvent get(ItemStack itemStack) {
      INSTANCE.itemStack = itemStack;
      return INSTANCE;
   }
}
