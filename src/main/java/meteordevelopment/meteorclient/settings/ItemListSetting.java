package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public class ItemListSetting extends Setting<List<Item>> {
   public final Predicate<Item> filter;
   private final boolean bypassFilterWhenSavingAndLoading;

   public ItemListSetting(
      String name,
      String description,
      List<Item> defaultValue,
      Consumer<List<Item>> onChanged,
      Consumer<Setting<List<Item>>> onModuleActivated,
      IVisible visible,
      Predicate<Item> filter,
      boolean bypassFilterWhenSavingAndLoading
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
      this.filter = filter;
      this.bypassFilterWhenSavingAndLoading = bypassFilterWhenSavingAndLoading;
   }

   protected List<Item> parseImpl(String str) {
      String[] values = str.split(",");
      List<Item> items = new ArrayList<>(values.length);

      try {
         for (String value : values) {
            Item item = (Item)parseId(BuiltInRegistries.ITEM, value);
            if (item != null && (this.filter == null || this.filter.test(item))) {
               items.add(item);
            }
         }
      } catch (Exception var9) {
      }

      return items;
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   protected boolean isValueValid(List<Item> value) {
      return true;
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.ITEM.keySet();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (Item item : this.get()) {
         if (this.bypassFilterWhenSavingAndLoading || this.filter == null || this.filter.test(item)) {
            valueTag.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
         }
      }

      tag.put("value", valueTag);
      return tag;
   }

   public List<Item> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(tagI.asString().orElse("")));
         if (this.bypassFilterWhenSavingAndLoading || this.filter == null || this.filter.test(item)) {
            this.get().add(item);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<ItemListSetting.Builder, List<Item>, ItemListSetting> {
      private Predicate<Item> filter;
      private boolean bypassFilterWhenSavingAndLoading;

      public Builder() {
         super(new ArrayList<>(0));
      }

      public ItemListSetting.Builder defaultValue(Item... defaults) {
         return this.defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
      }

      public ItemListSetting.Builder filter(Predicate<Item> filter) {
         this.filter = filter;
         return this;
      }

      public ItemListSetting.Builder bypassFilterWhenSavingAndLoading() {
         this.bypassFilterWhenSavingAndLoading = true;
         return this;
      }

      public ItemListSetting build() {
         return new ItemListSetting(
            this.name,
            this.description,
            this.defaultValue,
            this.onChanged,
            this.onModuleActivated,
            this.visible,
            this.filter,
            this.bypassFilterWhenSavingAndLoading
         );
      }
   }
}
