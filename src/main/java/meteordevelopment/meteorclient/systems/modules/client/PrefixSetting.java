package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class PrefixSetting extends Module {
   private static PrefixSetting INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<String> clientName = this.sgGeneral
      .add(new StringSetting.Builder().name("client-name").description("The text shown in the chat prefix.").defaultValue("Lurk").build());
   private final Setting<Boolean> flowing = this.sgGeneral
      .add(new BoolSetting.Builder().name("flowing").description("Animates the chat prefix gradient.").defaultValue(true).build());
   private final Setting<Integer> speed = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("speed")
            .description("Flowing gradient speed.")
            .defaultValue(20)
            .min(1)
            .max(50)
            .sliderRange(1, 50)
            .visible(this.flowing::get)
            .build()
      );
   private final Setting<SettingColor> startColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("start-color").description("The first prefix color.").defaultValue(new SettingColor(232, 232, 232)).build());
   private final Setting<SettingColor> endColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("end-color").description("The second prefix color.").defaultValue(new SettingColor(142, 142, 142)).build());

   public PrefixSetting() {
      super(Categories.Client, "prefix-setting", "Chat prefix settings.");
      INSTANCE = this;
   }

   public static String getClientName() {
      return INSTANCE == null ? "Lurk" : INSTANCE.clientName.get();
   }

   public static boolean isFlowing() {
      return INSTANCE == null || INSTANCE.flowing.get();
   }

   public static int getSpeed() {
      return INSTANCE == null ? 20 : INSTANCE.speed.get();
   }

   public static SettingColor getStartColor() {
      return INSTANCE == null ? new SettingColor(232, 232, 232) : INSTANCE.startColor.get();
   }

   public static SettingColor getEndColor() {
      return INSTANCE == null ? new SettingColor(142, 142, 142) : INSTANCE.endColor.get();
   }
}
