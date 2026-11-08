package meteordevelopment.meteorclient.systems.modules;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public class Category {
   public final String name;
   public final Supplier<ItemStack> icon;
   private final int nameHash;

   public Category(String name, Supplier<ItemStack> icon) {
      this.name = name;
      this.nameHash = name.hashCode();
      this.icon = icon == null ? () -> ItemStack.EMPTY : icon;
   }

   public Category(String name) {
      this(name, null);
   }

   @Override
   public String toString() {
      return this.name;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Category category = (Category)o;
         return this.nameHash == category.nameHash;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.nameHash;
   }
}
