package meteordevelopment.meteorclient.settings;

import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StorageBlockListSetting extends Setting<List<BlockEntityType<?>>> {
   public static final BlockEntityType<?>[] STORAGE_BLOCKS = new BlockEntityType[]{
      BlockEntityType.BARREL,
      BlockEntityType.BLAST_FURNACE,
      BlockEntityType.BREWING_STAND,
      BlockEntityType.CAMPFIRE,
      BlockEntityType.CHEST,
      BlockEntityType.CHISELED_BOOKSHELF,
      BlockEntityType.CRAFTER,
      BlockEntityType.DISPENSER,
      BlockEntityType.DECORATED_POT,
      BlockEntityType.DROPPER,
      BlockEntityType.ENDER_CHEST,
      BlockEntityType.FURNACE,
      BlockEntityType.HOPPER,
      BlockEntityType.SHULKER_BOX,
      BlockEntityType.SMOKER,
      BlockEntityType.TRAPPED_CHEST
   };
   public static final Registry<BlockEntityType<?>> REGISTRY = new StorageBlockListSetting.SRegistry();

   public StorageBlockListSetting(
      String name,
      String description,
      List<BlockEntityType<?>> defaultValue,
      Consumer<List<BlockEntityType<?>>> onChanged,
      Consumer<Setting<List<BlockEntityType<?>>>> onModuleActivated,
      IVisible visible
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   protected List<BlockEntityType<?>> parseImpl(String str) {
      String[] values = str.split(",");
      List<BlockEntityType<?>> blocks = new ArrayList<>(values.length);

      try {
         for (String value : values) {
            BlockEntityType<?> block = (BlockEntityType<?>)parseId(BuiltInRegistries.BLOCK_ENTITY_TYPE, value);
            if (block != null) {
               blocks.add(block);
            }
         }
      } catch (Exception var9) {
      }

      return blocks;
   }

   protected boolean isValueValid(List<BlockEntityType<?>> value) {
      return true;
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (BlockEntityType<?> type : this.get()) {
         Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
         if (id != null) {
            valueTag.add(StringTag.valueOf(id.toString()));
         }
      }

      tag.put("value", valueTag);
      return tag;
   }

   public List<BlockEntityType<?>> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.parse(tagI.asString().orElse("")));
         if (type != null) {
            this.get().add(type);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<StorageBlockListSetting.Builder, List<BlockEntityType<?>>, StorageBlockListSetting> {
      public Builder() {
         super(new ArrayList<>(0));
      }

      public StorageBlockListSetting.Builder defaultValue(BlockEntityType<?>... defaults) {
         return this.defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
      }

      public StorageBlockListSetting build() {
         return new StorageBlockListSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }

   private static class SRegistry extends MappedRegistry<BlockEntityType<?>> {
      public SRegistry() {
         super(ResourceKey.createRegistryKey(MeteorClient.identifier("storage-blocks")), Lifecycle.stable());
      }

      @Override
      public int size() {
         return StorageBlockListSetting.STORAGE_BLOCKS.length;
      }

      @Nullable
      public Identifier getKey(BlockEntityType<?> entry) {
         return null;
      }

      public Optional<ResourceKey<BlockEntityType<?>>> getResourceKey(BlockEntityType<?> entry) {
         return Optional.empty();
      }

      public int getId(@Nullable BlockEntityType<?> entry) {
         return 0;
      }

      @Nullable
      public BlockEntityType<?> getValue(@Nullable ResourceKey<BlockEntityType<?>> key) {
         return null;
      }

      @Nullable
      public BlockEntityType<?> getValue(@Nullable Identifier id) {
         return null;
      }

      @Override
      public Lifecycle registryLifecycle() {
         return null;
      }

      @Override
      public Set<Identifier> keySet() {
         return null;
      }

      public BlockEntityType<?> byIdOrThrow(int index) {
         return (BlockEntityType<?>)super.byIdOrThrow(index);
      }

      @Override
      public boolean containsKey(Identifier id) {
         return false;
      }

      @Nullable
      public BlockEntityType<?> byId(int index) {
         return null;
      }

      @NotNull
      @Override
      public Iterator<BlockEntityType<?>> iterator() {
         return ObjectIterators.wrap(StorageBlockListSetting.STORAGE_BLOCKS);
      }

      @Override
      public boolean containsKey(ResourceKey<BlockEntityType<?>> key) {
         return false;
      }

      @Override
      public Set<Entry<ResourceKey<BlockEntityType<?>>, BlockEntityType<?>>> entrySet() {
         return null;
      }

      @Override
      public Optional<Reference<BlockEntityType<?>>> getRandom(RandomSource random) {
         return Optional.empty();
      }

      @Override
      public Registry<BlockEntityType<?>> freeze() {
         return null;
      }

      public Reference<BlockEntityType<?>> createIntrusiveHolder(BlockEntityType<?> value) {
         return null;
      }

      @Override
      public Optional<Reference<BlockEntityType<?>>> get(int rawId) {
         return Optional.empty();
      }

      @Override
      public Optional<Reference<BlockEntityType<?>>> get(Identifier id) {
         return Optional.empty();
      }

      @Override
      public Stream<Reference<BlockEntityType<?>>> listElements() {
         return null;
      }

      @Override
      public Stream<Named<BlockEntityType<?>>> getTags() {
         return null;
      }

      @Override
      public Set<ResourceKey<BlockEntityType<?>>> registryKeySet() {
         return null;
      }
   }
}
