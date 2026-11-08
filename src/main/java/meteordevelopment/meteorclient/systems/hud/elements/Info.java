package meteordevelopment.meteorclient.systems.hud.elements;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.client.multiplayer.PlayerInfo;

public class Info extends HudElement {
   public static final HudElementInfo<Info> INFO = new HudElementInfo<>(Hud.GROUP, "info", "Displays information with blurred rounded background.", Info::new);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> hiddenUsername = this.sgGeneral
      .add(new BoolSetting.Builder().name("hidden-username").description("Hide username display.").defaultValue(false).build());
   private final Setting<Boolean> hiddenServer = this.sgGeneral
      .add(new BoolSetting.Builder().name("hidden-server").description("Hide server display.").defaultValue(false).build());
   private final Setting<Boolean> hiddenPos = this.sgGeneral
      .add(new BoolSetting.Builder().name("hidden-pos").description("Hide coordinates display.").defaultValue(false).build());
   private final Setting<Double> scale = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the info HUD.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 5.0)
            .decimalPlaces(2)
            .build()
      );
   private final Setting<Boolean> enableBlur = this.sgGeneral
      .add(new BoolSetting.Builder().name("enable-blur").description("Enable blur effect.").defaultValue(true).build());
   private final Setting<SettingColor> backgroundColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("background-color").description("Color of the background.").defaultValue(new SettingColor(25, 25, 25, 150)).build());
   private static final float CORNER_RADIUS = 5.0F;
   private static final double PADDING = 3.0;
   private double currentWidth = 378.0;
   private double targetWidth = 378.0;
   private double currentHeight = 131.0;
   private double targetHeight = 131.0;

   public Info() {
      super(INFO);
   }

   @Override
   public void render(HudRenderer renderer) {
      String username;
      if (this.hiddenUsername.get()) {
         username = "UserName: Hidden";
      } else if (MeteorClient.mc.player != null) {
         NameProtect nameProtect = Modules.get().get(NameProtect.class);
         String playerName;
         if (nameProtect != null && nameProtect.isActive()) {
            playerName = nameProtect.getName(MeteorClient.mc.player.getName().getString());
         } else {
            playerName = MeteorClient.mc.player.getName().getString();
         }

         username = "UserName: " + playerName;
      } else {
         username = "UserName: Unknown";
      }

      String server;
      if (this.hiddenServer.get()) {
         server = "Server: Hidden";
      } else if (MeteorClient.mc.getCurrentServer() != null) {
         server = "Server: " + MeteorClient.mc.getCurrentServer().ip;
      } else {
         server = "Server: Singleplayer";
      }

      String ping = "Ping: ";
      if (MeteorClient.mc.player != null && MeteorClient.mc.getConnection() != null) {
         PlayerInfo entry = MeteorClient.mc.getConnection().getPlayerInfo(MeteorClient.mc.player.getUUID());
         int latency = entry != null ? entry.getLatency() : 0;
         ping = ping + latency + "ms";
      } else {
         ping = ping + "0ms";
      }

      String tps = "TPS: ";
      if (MeteorClient.mc.level != null) {
         float currentTps = TickRate.INSTANCE.getTickRate();
         tps = tps + String.format("%.1f", currentTps);
      } else {
         tps = tps + "20.0";
      }

      LocalTime time = LocalTime.now();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
      String timeStr = "Time: " + time.format(formatter);
      String coords;
      if (this.hiddenPos.get()) {
         coords = "Pos: Hidden";
      } else if (MeteorClient.mc.player != null) {
         int x = (int)MeteorClient.mc.player.getX();
         int y = (int)MeteorClient.mc.player.getY();
         int z = (int)MeteorClient.mc.player.getZ();
         coords = String.format("Pos: %d, %d, %d", x, y, z);
      } else {
         coords = "Pos: Hidden";
      }

      double hudScale = this.scale.get();
      double textScale = Hud.get().getTextScale() * hudScale;
      double padding = 3.0 * hudScale;
      double cornerRadius = 5.0 * hudScale;
      double usernameWidth = renderer.textWidth(username, true, textScale);
      double serverWidth = renderer.textWidth(server, true, textScale);
      double pingWidth = renderer.textWidth(ping, true, textScale);
      double tpsWidth = renderer.textWidth(tps, true, textScale);
      double timeWidth = renderer.textWidth(timeStr, true, textScale);
      double coordsWidth = renderer.textWidth(coords, true, textScale);
      double textHeight = renderer.textHeight(true, textScale);
      int lineCount = 6;
      double maxWidth = Math.max(Math.max(usernameWidth, serverWidth), Math.max(pingWidth, Math.max(tpsWidth, Math.max(timeWidth, coordsWidth))));
      this.targetWidth = maxWidth + padding * 2.0;
      this.targetHeight = textHeight * lineCount + padding * (lineCount + 1);
      this.currentWidth = this.currentWidth + (this.targetWidth - this.currentWidth) * 0.15;
      this.currentHeight = this.currentHeight + (this.targetHeight - this.currentHeight) * 0.15;
      this.setSize(this.currentWidth, this.currentHeight);
      LiquidGlassUtil.render(renderer, this.x, this.y, this.currentWidth, this.currentHeight, cornerRadius, this.backgroundColor.get());
      double textStartY = this.y + padding;
      renderer.text(username, this.x + padding, textStartY, SettingColor.WHITE, true, textScale);
      renderer.text(server, this.x + padding, textStartY + (textHeight + padding), SettingColor.WHITE, true, textScale);
      renderer.text(ping, this.x + padding, textStartY + (textHeight + padding) * 2.0, SettingColor.WHITE, true, textScale);
      renderer.text(tps, this.x + padding, textStartY + (textHeight + padding) * 3.0, SettingColor.WHITE, true, textScale);
      renderer.text(timeStr, this.x + padding, textStartY + (textHeight + padding) * 4.0, SettingColor.WHITE, true, textScale);
      renderer.text(coords, this.x + padding, textStartY + (textHeight + padding) * 5.0, SettingColor.WHITE, true, textScale);
   }
}
