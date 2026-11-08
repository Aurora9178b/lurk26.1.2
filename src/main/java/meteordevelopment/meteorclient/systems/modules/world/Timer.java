package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Timer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> multiplier = this.sgGeneral
      .add(new DoubleSetting.Builder().name("multiplier").description("The timer multiplier amount.").defaultValue(1.0).min(0.1).sliderMin(0.1).build());
   public static final double OFF = 1.0;
   private double override = 1.0;

   public Timer() {
      super(Categories.World, "timer", "Changes the speed of everything in your game.");
   }

   public double getMultiplier() {
      return this.override != 1.0 ? this.override : (this.isActive() ? this.multiplier.get() : 1.0);
   }

   public void setOverride(double override) {
      this.override = override;
   }
}
