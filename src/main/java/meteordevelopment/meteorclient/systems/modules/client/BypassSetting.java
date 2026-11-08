package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class BypassSetting extends Module {
   private static BypassSetting INSTANCE;
   private final SettingGroup sgSwitch = this.settings.createGroup("Switch");
   private final Setting<BypassSetting.SwitchBypass> switchBypass = this.sgSwitch
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("switch")).description("Switch bypass mode."))
               .defaultValue(BypassSetting.SwitchBypass.Grim))
            .build()
      );
   private final Setting<Integer> customDelay = this.sgSwitch
      .add(
         new IntSetting.Builder()
            .name("custom-delay")
            .description("Custom switch delay in milliseconds.")
            .defaultValue(100)
            .min(0)
            .sliderRange(0, 500)
            .visible(() -> this.switchBypass.get() == BypassSetting.SwitchBypass.Custom)
            .build()
      );

   public BypassSetting() {
      super(Categories.Client, "BypassSetting", "Global bypass settings.");
      INSTANCE = this;
   }

   public static long getSwitchDelay() {
      if (INSTANCE == null) {
         return 50L;
      }

      return switch ((BypassSetting.SwitchBypass)INSTANCE.switchBypass.get()) {
         case Grim -> 50L;
         case Custom -> INSTANCE.customDelay.get().intValue();
      };
   }

   public enum SwitchBypass {
      Grim,
      Custom;
   }
}
