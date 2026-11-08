package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

public class PredictSetting extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> dataSize = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("data-size")
            .description("The amount of data required for prediction.")
            .range(2, 4)
            .sliderRange(2, 4)
            .defaultValue(4)
            .build()
      );
   private final Setting<Double> resultMul = this.sgGeneral
      .add(new DoubleSetting.Builder().name("result-multiply").description("Result * Result Multiply").min(0.0).sliderRange(0.5, 3.0).defaultValue(3.0).build());
   private final Setting<Boolean> pingSync = this.sgGeneral
      .add(new BoolSetting.Builder().name("ping-sync").description("Predict extra ticks from your current ping.").defaultValue(false).build());

   public PredictSetting() {
      super(Categories.Client, "predict-setting", "Predict setting configuration.");
   }

   public int getDataSize() {
      return this.dataSize.get();
   }

   public double getResultMul() {
      return this.resultMul.get();
   }

   public boolean getPingSync() {
      return this.pingSync.get();
   }
}
