package meteordevelopment.meteorclient.settings;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityTypeListSetting extends Setting<Set<EntityType<?>>> {
   public final Predicate<EntityType<?>> filter;
   private List<String> suggestions;
   private static final List<String> groups = List.of("animal", "wateranimal", "monster", "ambient", "misc");

   public EntityTypeListSetting(
      String name,
      String description,
      Set<EntityType<?>> defaultValue,
      Consumer<Set<EntityType<?>>> onChanged,
      Consumer<Setting<Set<EntityType<?>>>> onModuleActivated,
      IVisible visible,
      Predicate<EntityType<?>> filter
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
      this.filter = filter;
   }

   @Override
   public void resetImpl() {
      this.value = new ObjectOpenHashSet<>(this.defaultValue);
   }

   protected Set<EntityType<?>> parseImpl(String str) {
      String[] values = str.split(",");
      Set<EntityType<?>> entities = new ObjectOpenHashSet<>(values.length);

      try {
         for (String value : values) {
            EntityType<?> entity = (EntityType<?>)parseId(BuiltInRegistries.ENTITY_TYPE, value);
            if (entity != null) {
               entities.add(entity);
            } else {
               String lowerValue = value.trim().toLowerCase();
               if (groups.contains(lowerValue)) {
                  for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                     if (this.filter == null || this.filter.test(entityType)) {
                        switch (lowerValue) {
                           case "animal":
                              if (entityType.getCategory() == MobCategory.CREATURE) {
                                 entities.add(entityType);
                              }
                              break;
                           case "wateranimal":
                              if (entityType.getCategory() == MobCategory.WATER_AMBIENT
                                 || entityType.getCategory() == MobCategory.WATER_CREATURE
                                 || entityType.getCategory() == MobCategory.UNDERGROUND_WATER_CREATURE
                                 || entityType.getCategory() == MobCategory.AXOLOTLS) {
                                 entities.add(entityType);
                              }
                              break;
                           case "monster":
                              if (entityType.getCategory() == MobCategory.MONSTER) {
                                 entities.add(entityType);
                              }
                              break;
                           case "ambient":
                              if (entityType.getCategory() == MobCategory.AMBIENT) {
                                 entities.add(entityType);
                              }
                              break;
                           case "misc":
                              if (entityType.getCategory() == MobCategory.MISC) {
                                 entities.add(entityType);
                              }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var14) {
      }

      return entities;
   }

   protected boolean isValueValid(Set<EntityType<?>> value) {
      return true;
   }

   @Override
   public List<String> getSuggestions() {
      if (this.suggestions == null) {
         this.suggestions = new ArrayList<>(groups);

         for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            if (this.filter == null || this.filter.test(entityType)) {
               this.suggestions.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString());
            }
         }
      }

      return this.suggestions;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (EntityType<?> entityType : this.get()) {
         valueTag.add(StringTag.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString()));
      }

      tag.put("value", valueTag);
      return tag;
   }

   public Set<EntityType<?>> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(tagI.asString().orElse("")));
         if (this.filter == null || this.filter.test(type)) {
            this.get().add(type);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<EntityTypeListSetting.Builder, Set<EntityType<?>>, EntityTypeListSetting> {
      private Predicate<EntityType<?>> filter;

      public Builder() {
         super(new ObjectOpenHashSet<>(0));
      }

      public EntityTypeListSetting.Builder defaultValue(EntityType<?>... defaults) {
         return this.defaultValue(defaults != null ? new ObjectOpenHashSet<>(defaults) : new ObjectOpenHashSet<>(0));
      }

      public EntityTypeListSetting.Builder onlyAttackable() {
         this.filter = EntityUtils::isAttackable;
         return this;
      }

      public EntityTypeListSetting.Builder filter(Predicate<EntityType<?>> filter) {
         this.filter = filter;
         return this;
      }

      public EntityTypeListSetting build() {
         return new EntityTypeListSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible, this.filter);
      }
   }
}
