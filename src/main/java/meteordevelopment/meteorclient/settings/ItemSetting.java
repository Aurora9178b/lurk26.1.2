package meteordevelopment.meteorclient.settings;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ItemSetting extends Setting<Item> {
   public final Predicate<Item> filter;

   public ItemSetting(
      String name,
      String description,
      Item defaultValue,
      Consumer<Item> onChanged,
      Consumer<Setting<Item>> onModuleActivated,
      IVisible visible,
      Predicate<Item> filter
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
      this.filter = filter;
   }

   protected Item parseImpl(String str) {
      return parseId(BuiltInRegistries.ITEM, str);
   }

   protected boolean isValueValid(Item value) {
      return this.filter == null || this.filter.test(value);
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.ITEM.keySet();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putString("value", BuiltInRegistries.ITEM.getKey(this.get()).toString());
      return tag;
   }

   public Item load(CompoundTag tag) {
      this.value = BuiltInRegistries.ITEM.getValue(Identifier.parse(tag.getStringOr("value", "")));
      if (this.filter != null && !this.filter.test(this.value)) {
         for (Item item : BuiltInRegistries.ITEM) {
            if (this.filter.test(item)) {
               this.value = item;
               break;
            }
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<ItemSetting.Builder, Item, ItemSetting> {
      private Predicate<Item> filter;

      public Builder() {
         super(null);
      }

      public ItemSetting.Builder filter(Predicate<Item> filter) {
         this.filter = filter;
         return this;
      }

      public ItemSetting build() {
         return new ItemSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible, this.filter);
      }
   }
}
