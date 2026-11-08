package meteordevelopment.meteorclient.utils.render;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class DisplayItemUtils {
   private DisplayItemUtils() {
   }

   public static ItemStack toStack(Item item) {
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(directHolder(item));
   }

   public static ItemStack toStack(Block block) {
      return toStack(block.asItem());
   }

   public static ItemStack toStack(Item item, int count) {
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(directHolder(item), count);
   }

   private static Holder<Item> directHolder(Item item) {
      DataComponentMap components = DataComponentMap.builder()
         .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
         .set(DataComponents.ITEM_MODEL, item.builtInRegistryHolder().key().identifier())
         .set(DataComponents.ITEM_NAME, Component.translatable(item.getDescriptionId()))
         .build();
      return Holder.direct(item, components);
   }
}
