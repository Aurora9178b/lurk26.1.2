package meteordevelopment.meteorclient.settings;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class BlockSetting extends Setting<Block> {
   public final Predicate<Block> filter;

   public BlockSetting(
      String name,
      String description,
      Block defaultValue,
      Consumer<Block> onChanged,
      Consumer<Setting<Block>> onModuleActivated,
      IVisible visible,
      Predicate<Block> filter
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
      this.filter = filter;
   }

   protected Block parseImpl(String str) {
      return parseId(BuiltInRegistries.BLOCK, str);
   }

   protected boolean isValueValid(Block value) {
      return this.filter == null || this.filter.test(value);
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.BLOCK.keySet();
   }

   @Override
   protected CompoundTag save(CompoundTag tag) {
      tag.putString("value", BuiltInRegistries.BLOCK.getKey(this.get()).toString());
      return tag;
   }

   protected Block load(CompoundTag tag) {
      this.value = BuiltInRegistries.BLOCK.getValue(Identifier.parse(tag.getStringOr("value", "")));
      if (this.filter != null && !this.filter.test(this.value)) {
         for (Block block : BuiltInRegistries.BLOCK) {
            if (this.filter.test(block)) {
               this.value = block;
               break;
            }
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<BlockSetting.Builder, Block, BlockSetting> {
      private Predicate<Block> filter;

      public Builder() {
         super(null);
      }

      public BlockSetting.Builder filter(Predicate<Block> filter) {
         this.filter = filter;
         return this;
      }

      public BlockSetting build() {
         return new BlockSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible, this.filter);
      }
   }
}
