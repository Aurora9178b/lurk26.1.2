package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public class SettingGroup implements ISerializable<SettingGroup>, Iterable<Setting<?>> {
   public final String name;
   public boolean sectionExpanded;
   final List<Setting<?>> settings = new ArrayList<>(1);

   SettingGroup(String name, boolean sectionExpanded) {
      this.name = name;
      this.sectionExpanded = sectionExpanded;
   }

   public Setting<?> get(String name) {
      for (Setting<?> setting : this) {
         if (setting.name.equals(name)) {
            return setting;
         }
      }

      return null;
   }

   public <T extends Setting<?>> T add(T setting) {
      this.settings.add(setting);
      return setting;
   }

   public Setting<?> getByIndex(int index) {
      return this.settings.get(index);
   }

   public boolean wasChanged() {
      for (Setting<?> setting : this.settings) {
         if (setting.wasChanged()) {
            return true;
         }
      }

      return false;
   }

   @NotNull
   @Override
   public Iterator<Setting<?>> iterator() {
      return this.settings.iterator();
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putString("name", this.name);
      tag.putBoolean("sectionExpanded", this.sectionExpanded);
      ListTag settingsTag = new ListTag();

      for (Setting<?> setting : this) {
         if (setting.wasChanged()) {
            settingsTag.add(setting.toTag());
         }
      }

      if (!settingsTag.isEmpty()) {
         tag.put("settings", settingsTag);
      }

      return tag;
   }

   public SettingGroup fromTag(CompoundTag tag) {
      this.sectionExpanded = tag.getBooleanOr("sectionExpanded", false);

      for (Tag t : tag.getListOrEmpty("settings")) {
         CompoundTag settingTag = (CompoundTag)t;
         Setting<?> setting = this.get(settingTag.getStringOr("name", ""));
         if (setting != null) {
            setting.fromTag(settingTag);
         }
      }

      return this;
   }
}
