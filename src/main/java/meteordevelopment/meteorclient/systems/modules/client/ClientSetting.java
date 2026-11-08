package meteordevelopment.meteorclient.systems.modules.client;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ClientSetting extends Module {
   private static ClientSetting INSTANCE;
   private final SettingGroup sgFont = this.settings.createGroup("Font");
   private final SettingGroup sgChat = this.settings.createGroup("Chat");
   private final SettingGroup sgHotBar = this.settings.createGroup("HotBar");
   private final Setting<Boolean> replaceMinecraftFont = this.sgFont
      .add(
         new BoolSetting.Builder()
            .name("replace-minecraft-font")
            .description("Replaces Minecraft's global font with the font selected in Config.")
            .defaultValue(false)
            .build()
      );
   private final Setting<SettingColor> chatBackgroundColor = this.sgChat
      .add(
         new ColorSetting.Builder()
            .name("chat-background-color")
            .description("Color of the custom chat input background.")
            .defaultValue(new SettingColor(0, 0, 0, 100))
            .build()
      );
   private final Setting<SettingColor> hotbarBackgroundColor = this.sgHotBar
      .add(
         new ColorSetting.Builder()
            .name("hotbar-background-color")
            .description("Color of the custom hotbar background.")
            .defaultValue(new SettingColor(0, 0, 0, 76))
            .build()
      );
   private final Setting<SettingColor> hotbarSelectedColor = this.sgHotBar
      .add(
         new ColorSetting.Builder()
            .name("hotbar-selected-color")
            .description("Color of the selected hotbar slot.")
            .defaultValue(new SettingColor(255, 255, 255, 42))
            .build()
      );

   public ClientSetting() {
      super(Categories.Client, "client-setting", "Global client settings.");
      INSTANCE = this;
   }

   public static boolean shouldReplaceMinecraftFont() {
      return INSTANCE != null && INSTANCE.replaceMinecraftFont.get();
   }

   public static SettingColor getChatBackgroundColor() {
      return INSTANCE == null ? new SettingColor(0, 0, 0, 100) : INSTANCE.chatBackgroundColor.get();
   }

   public static SettingColor getHotbarBackgroundColor() {
      return INSTANCE == null ? new SettingColor(0, 0, 0, 76) : INSTANCE.hotbarBackgroundColor.get();
   }

   public static SettingColor getHotbarSelectedColor() {
      return INSTANCE == null ? new SettingColor(255, 255, 255, 42) : INSTANCE.hotbarSelectedColor.get();
   }
}
