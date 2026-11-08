package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

public class ParticleTypeListSetting extends Setting<List<ParticleType<?>>> {
   public ParticleTypeListSetting(
      String name,
      String description,
      List<ParticleType<?>> defaultValue,
      Consumer<List<ParticleType<?>>> onChanged,
      Consumer<Setting<List<ParticleType<?>>>> onModuleActivated,
      IVisible visible
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   protected List<ParticleType<?>> parseImpl(String str) {
      String[] values = str.split(",");
      List<ParticleType<?>> particleTypes = new ArrayList<>(values.length);

      try {
         for (String value : values) {
            ParticleType<?> particleType = (ParticleType<?>)parseId(BuiltInRegistries.PARTICLE_TYPE, value);
            if (particleType != null) {
               particleTypes.add(particleType);
            }
         }
      } catch (Exception var9) {
      }

      return particleTypes;
   }

   protected boolean isValueValid(List<ParticleType<?>> value) {
      return true;
   }

   @Override
   public Iterable<Identifier> getIdentifierSuggestions() {
      return BuiltInRegistries.PARTICLE_TYPE.keySet();
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (ParticleType<?> particleType : this.get()) {
         Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
         if (id != null) {
            valueTag.add(StringTag.valueOf(id.toString()));
         }
      }

      tag.put("value", valueTag);
      return tag;
   }

   public List<ParticleType<?>> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.getValue(Identifier.parse(tagI.asString().orElse("")));
         if (particleType != null) {
            this.get().add(particleType);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<ParticleTypeListSetting.Builder, List<ParticleType<?>>, ParticleTypeListSetting> {
      public Builder() {
         super(new ArrayList<>(0));
      }

      public ParticleTypeListSetting.Builder defaultValue(ParticleType<?>... defaults) {
         return this.defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
      }

      public ParticleTypeListSetting build() {
         return new ParticleTypeListSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }
}
