package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class GoToSetting extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("Teleport delay in milliseconds.")
            .defaultValue(40)
            .min(1)
            .max(2000)
            .sliderMin(1)
            .sliderMax(2000)
            .build()
      );

   public GoToSetting() {
      super(Categories.Client, "GoToSetting", "Settings for the goto command.");
   }

   public static GoToSetting get() {
      return Modules.get().get(GoToSetting.class);
   }

   public int getDelay() {
      return this.delay.get();
   }
}
