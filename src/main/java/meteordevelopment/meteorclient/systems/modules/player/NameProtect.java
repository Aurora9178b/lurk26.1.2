package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class NameProtect extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> nameProtect = this.sgGeneral
      .add(new BoolSetting.Builder().name("name-protect").description("Hides your name client-side.").defaultValue(true).build());
   private final Setting<String> name = this.sgGeneral
      .add(new StringSetting.Builder().name("name").description("Name to be replaced with.").defaultValue("seasnail").visible(this.nameProtect::get).build());
   private final Setting<Boolean> skinProtect = this.sgGeneral
      .add(new BoolSetting.Builder().name("skin-protect").description("Make players become Steves.").defaultValue(true).build());
   private String username = "If you see this, something is wrong.";

   public NameProtect() {
      super(Categories.Player, "name-protect", "Hide player names and skins.");
   }

   @Override
   public void onActivate() {
      this.username = this.mc.getUser().getName();
   }

   public String replaceName(String string) {
      return string != null && this.isActive() ? string.replace(this.username, this.name.get()) : string;
   }

   public String getName(String original) {
      return !this.name.get().isEmpty() && this.isActive() ? this.name.get() : original;
   }

   public boolean skinProtect() {
      return this.isActive() && this.skinProtect.get();
   }
}
