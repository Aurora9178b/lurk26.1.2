package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

public class StatusEffectListSetting extends Setting<List<MobEffect>> {
   public StatusEffectListSetting(
      String name,
      String description,
      List<MobEffect> defaultValue,
      Consumer<List<MobEffect>> onChanged,
      Consumer<Setting<List<MobEffect>>> onModuleActivated,
      IVisible visible
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   protected List<MobEffect> parseImpl(String str) {
      String[] values = str.split(",");
      List<MobEffect> effects = new ArrayList<>(values.length);

      try {
         for (String value : values) {
            MobEffect effect = (MobEffect)parseId(BuiltInRegistries.MOB_EFFECT, value);
            if (effect != null) {
               effects.add(effect);
            }
         }
      } catch (Exception var9) {
      }

      return effects;
   }

   protected boolean isValueValid(List<MobEffect> value) {
      return true;
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.MOB_EFFECT.keySet();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (MobEffect effect : this.get()) {
         Identifier id = BuiltInRegistries.MOB_EFFECT.getKey(effect);
         if (id != null) {
            valueTag.add(StringTag.valueOf(id.toString()));
         }
      }

      tag.put("value", valueTag);
      return tag;
   }

   public List<MobEffect> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(tagI.asString().orElse("")));
         if (effect != null) {
            this.get().add(effect);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<StatusEffectListSetting.Builder, List<MobEffect>, StatusEffectListSetting> {
      public Builder() {
         super(new ArrayList<>(0));
      }

      public StatusEffectListSetting.Builder defaultValue(MobEffect... defaults) {
         return this.defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
      }

      public StatusEffectListSetting build() {
         return new StatusEffectListSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }
}
