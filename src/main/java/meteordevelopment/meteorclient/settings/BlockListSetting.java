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
import net.minecraft.world.level.block.Block;

public class BlockListSetting extends Setting<List<Block>> {
   public final Predicate<Block> filter;

   public BlockListSetting(
      String name,
      String description,
      List<Block> defaultValue,
      Consumer<List<Block>> onChanged,
      Consumer<Setting<List<Block>>> onModuleActivated,
      Predicate<Block> filter,
      IVisible visible
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
      this.filter = filter;
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   protected List<Block> parseImpl(String str) {
      String[] values = str.split(",");
      List<Block> blocks = new ArrayList<>(values.length);

      try {
         for (String value : values) {
            Block block = (Block)parseId(BuiltInRegistries.BLOCK, value);
            if (block != null && (this.filter == null || this.filter.test(block))) {
               blocks.add(block);
            }
         }
      } catch (Exception var9) {
      }

      return blocks;
   }

   protected boolean isValueValid(List<Block> value) {
      return true;
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.BLOCK.keySet();
   }

   @Override
   protected CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (Block block : this.get()) {
         valueTag.add(StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(block).toString()));
      }

      tag.put("value", valueTag);
      return tag;
   }

   protected List<Block> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(tagI.asString().orElse("")));
         if (this.filter == null || this.filter.test(block)) {
            this.get().add(block);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<BlockListSetting.Builder, List<Block>, BlockListSetting> {
      private Predicate<Block> filter;

      public Builder() {
         super(new ArrayList<>(0));
      }

      public BlockListSetting.Builder defaultValue(Block... defaults) {
         return this.defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
      }

      public BlockListSetting.Builder filter(Predicate<Block> filter) {
         this.filter = filter;
         return this;
      }

      public BlockListSetting build() {
         return new BlockListSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.filter, this.visible);
      }
   }
}
