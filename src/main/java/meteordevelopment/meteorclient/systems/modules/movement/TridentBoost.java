package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class TridentBoost extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> multiplier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("boost")
            .description("How much your velocity is multiplied by when using riptide.")
            .defaultValue(2.0)
            .min(0.1)
            .sliderMin(1.0)
            .build()
      );
   private final Setting<Boolean> allowOutOfWater = this.sgGeneral
      .add(new BoolSetting.Builder().name("out-of-water").description("Whether riptide should work out of water").defaultValue(true).build());

   public TridentBoost() {
      super(Categories.Movement, "trident-boost", "Boosts you when using riptide with a trident.");
   }

   public double getMultiplier() {
      return this.isActive() ? this.multiplier.get() : 1.0;
   }

   public boolean allowOutOfWater() {
      return this.isActive() && this.allowOutOfWater.get();
   }
}
