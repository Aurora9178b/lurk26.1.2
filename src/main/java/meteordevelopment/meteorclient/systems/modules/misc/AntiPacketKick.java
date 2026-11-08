package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AntiPacketKick extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> catchExceptions = this.sgGeneral
      .add(new BoolSetting.Builder().name("catch-exceptions").description("Drops corrupted packets.").defaultValue(false).build());
   public final Setting<Boolean> logExceptions = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("log-exceptions").description("Logs caught exceptions.").defaultValue(true).visible(this.catchExceptions::get).build()
      );

   public AntiPacketKick() {
      super(Categories.Misc, "anti-packet-kick", "Attempts to prevent you from being disconnected by large packets.");
   }

   public boolean catchExceptions() {
      return this.isActive() && this.catchExceptions.get();
   }
}
