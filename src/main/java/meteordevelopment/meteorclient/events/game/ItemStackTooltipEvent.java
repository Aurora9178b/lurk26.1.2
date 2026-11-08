package meteordevelopment.meteorclient.events.game;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemStackTooltipEvent {
   private final ItemStack itemStack;
   private List<Component> list;

   public ItemStackTooltipEvent(ItemStack itemStack, List<Component> list) {
      this.itemStack = itemStack;
      this.list = list;
   }

   public List<Component> list() {
      return this.list;
   }

   public ItemStack itemStack() {
      return this.itemStack;
   }

   public void appendStart(Component text) {
      this.copyIfImmutable();
      int index = this.list.isEmpty() ? 0 : 1;
      this.list.add(index, text);
   }

   public void appendEnd(Component text) {
      this.copyIfImmutable();
      this.list.add(text);
   }

   public void append(int index, Component text) {
      this.copyIfImmutable();
      this.list.add(index, text);
   }

   public void set(int index, Component text) {
      this.copyIfImmutable();
      this.list.set(index, text);
   }

   private void copyIfImmutable() {
      if (List.of().getClass().getSuperclass().isInstance(this.list)) {
         this.list = new ObjectArrayList<>(this.list);
      }
   }
}
