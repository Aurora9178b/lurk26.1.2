package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class ModuleListSetting extends Setting<List<Module>> {
   private static List<String> suggestions;

   public ModuleListSetting(
      String name,
      String description,
      List<Module> defaultValue,
      Consumer<List<Module>> onChanged,
      Consumer<Setting<List<Module>>> onModuleActivated,
      IVisible visible
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   protected List<Module> parseImpl(String str) {
      String[] values = str.split(",");
      List<Module> modules = new ArrayList<>(values.length);

      try {
         for (String value : values) {
            Module module = Modules.get().get(value.trim());
            if (module != null) {
               modules.add(module);
            }
         }
      } catch (Exception var9) {
      }

      return modules;
   }

   protected boolean isValueValid(List<Module> value) {
      return true;
   }

   @Override
   public List<String> getSuggestions() {
      if (suggestions == null) {
         suggestions = new ArrayList<>(Modules.get().getAll().size());

         for (Module module : Modules.get().getAll()) {
            suggestions.add(module.name);
         }
      }

      return suggestions;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag modulesTag = new ListTag();

      for (Module module : this.get()) {
         modulesTag.add(StringTag.valueOf(module.name));
      }

      tag.put("modules", modulesTag);
      return tag;
   }

   public List<Module> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("modules")) {
         Module module = Modules.get().get(tagI.asString().orElse(""));
         if (module != null) {
            this.get().add(module);
         }
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<ModuleListSetting.Builder, List<Module>, ModuleListSetting> {
      public Builder() {
         super(new ArrayList<>(0));
      }

      @SafeVarargs
      public final ModuleListSetting.Builder defaultValue(Class<? extends Module>... defaults) {
         List<Module> modules = new ArrayList<>();

         for (Class<? extends Module> klass : defaults) {
            if (Modules.get().get(klass) != null) {
               modules.add(Modules.get().get(klass));
            }
         }

         return this.defaultValue(modules);
      }

      public ModuleListSetting build() {
         return new ModuleListSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }
}
