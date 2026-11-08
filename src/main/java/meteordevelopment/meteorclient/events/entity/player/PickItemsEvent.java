package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.world.item.ItemStack;

public class PickItemsEvent {
   private static final PickItemsEvent INSTANCE = new PickItemsEvent();
   public ItemStack itemStack;
   public int count;

   public static PickItemsEvent get(ItemStack itemStack, int count) {
      INSTANCE.itemStack = itemStack;
      INSTANCE.count = count;
      return INSTANCE;
   }
}
