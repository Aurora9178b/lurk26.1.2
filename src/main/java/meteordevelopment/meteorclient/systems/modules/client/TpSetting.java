package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class TpSetting extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<TpSetting.TpLib> tpLib = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("tp-lib"))
                  .description("Which teleportation library to use."))
               .defaultValue(TpSetting.TpLib.LTPUtils))
            .build()
      );
   public final Setting<Double> moveDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("move-distance")
            .description("Maximum distance per teleportation step.")
            .defaultValue(128.0)
            .min(0.0)
            .max(256.0)
            .sliderMax(256.0)
            .build()
      );
   public final Setting<Double> moveModeSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("move-mode-speed")
            .description("Speed used by TPBot move mode.")
            .defaultValue(5.0)
            .min(0.0)
            .max(100.0)
            .sliderRange(0.0, 100.0)
            .build()
      );
   public final Setting<Integer> maxPackets = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-packets")
            .description("Maximum packets to send per tick.")
            .defaultValue(20)
            .min(0)
            .max(1024)
            .sliderMax(1024)
            .build()
      );

   public TpSetting() {
      super(Categories.Client, "tp-setting", "Teleportation settings.");
   }

   public static TpSetting get() {
      return Modules.get().get(TpSetting.class);
   }

   public boolean isLTPUtils() {
      return this.tpLib.get() == TpSetting.TpLib.LTPUtils;
   }

   public boolean isQTPUtils() {
      return this.tpLib.get() == TpSetting.TpLib.QTPUtils;
   }

   public double getMoveDistance() {
      return this.moveDistance.get();
   }

   public double getMoveModeSpeed() {
      return this.moveModeSpeed.get();
   }

   public enum TpLib {
      LTPUtils,
      QTPUtils;
   }
}
