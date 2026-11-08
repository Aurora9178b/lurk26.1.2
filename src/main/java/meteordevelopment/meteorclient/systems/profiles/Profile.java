package meteordevelopment.meteorclient.systems.profiles;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.apache.commons.io.FileUtils;

public class Profile implements ISerializable<Profile> {
   public final Settings settings = new Settings();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSave = this.settings.createGroup("Save");
   public Setting<String> name = this.sgGeneral
      .add(new StringSetting.Builder().name("name").description("The name of the profile.").filter(Utils::nameFilter).build());
   public Setting<List<String>> loadOnJoin = this.sgGeneral
      .add(
         new StringListSetting.Builder()
            .name("load-on-join")
            .description("Which servers to set this profile as active when joining.")
            .filter(Utils::ipFilter)
            .build()
      );
   public Setting<Boolean> hud = this.sgSave
      .add(new BoolSetting.Builder().name("hud").description("Whether the profile should save hud.").defaultValue(false).build());
   public Setting<Boolean> macros = this.sgSave
      .add(new BoolSetting.Builder().name("macros").description("Whether the profile should save macros.").defaultValue(false).build());
   public Setting<Boolean> modules = this.sgSave
      .add(new BoolSetting.Builder().name("modules").description("Whether the profile should save modules.").defaultValue(false).build());
   public Setting<Boolean> waypoints = this.sgSave
      .add(new BoolSetting.Builder().name("waypoints").description("Whether the profile should save waypoints.").defaultValue(false).build());

   public Profile() {
   }

   public Profile(Tag tag) {
      this.fromTag((CompoundTag)tag);
   }

   public void load() {
      File folder = this.getFile();
      if (this.hud.get()) {
         Hud.get().load(folder);
      }

      if (this.macros.get()) {
         Macros.get().load(folder);
      }

      if (this.modules.get()) {
         Modules.get().load(folder);
      }

      if (this.waypoints.get()) {
         Waypoints.get().load(folder);
      }
   }

   public void save() {
      File folder = this.getFile();
      if (this.hud.get()) {
         Hud.get().save(folder);
      }

      if (this.macros.get()) {
         Macros.get().save(folder);
      }

      if (this.modules.get()) {
         Modules.get().save(folder);
      }

      if (this.waypoints.get()) {
         Waypoints.get().save(folder);
      }
   }

   public void delete() {
      try {
         FileUtils.deleteDirectory(this.getFile());
      } catch (IOException e) {
         MeteorClient.LOG.error("Error deleting profile {}", this.name.get(), e);
      }
   }

   public File getFile() {
      return new File(Profiles.FOLDER, this.name.get());
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.put("settings", this.settings.toTag());
      return tag;
   }

   public Profile fromTag(CompoundTag tag) {
      if (tag.contains("settings")) {
         this.settings.fromTag(tag.getCompoundOrEmpty("settings"));
      }

      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Profile profile = (Profile)o;
         return Objects.equals(profile.name.get(), this.name.get());
      } else {
         return false;
      }
   }
}
