package meteordevelopment.meteorclient.systems.modules.misc.autologin;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;

public class AutoLoginAccount {
   public final Settings settings = new Settings();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<String> username = this.sgGeneral
      .add(new StringSetting.Builder().name("username").description("Account username.").defaultValue("").build());
   public final Setting<String> serverIp = this.sgGeneral.add(new StringSetting.Builder().name("server-ip").description("Server ip.").defaultValue("").build());
   public final Setting<String> password = this.sgGeneral
      .add(new StringSetting.Builder().name("password").description("Account password.").defaultValue("").build());
}
