package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.concurrent.atomic.LongAdder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class PacketStatsHud extends HudElement {
   public static final HudElementInfo<PacketStatsHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "packet-stats", "Packet Stats", "Displays sent and received packets per second.", PacketStatsHud::new
   );
   private static final double PADDING = 4.0;
   private static final double LINE_GAP = 2.0;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<SettingColor> labelColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("label-color").description("Color of the label text.").defaultValue(new SettingColor(255, 255, 255)).build());
   private final Setting<SettingColor> valueColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("value-color").description("Color of the value text.").defaultValue(new SettingColor(200, 200, 200)).build());
   private final Setting<Boolean> shadow = this.sgGeneral
      .add(new BoolSetting.Builder().name("shadow").description("Draws shadow behind text.").defaultValue(true).build());
   private final Setting<Double> scale = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the packet stats HUD.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 5.0)
            .decimalPlaces(2)
            .build()
      );
   private final Setting<PacketStatsHud.BackgroundMode> backgroundMode = this.sgBackground
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for the packet stats background."))
               .defaultValue(PacketStatsHud.BackgroundMode.LiquidGlass))
            .build()
      );
   private final Setting<Double> backgroundRadius = this.sgBackground
      .add(
         new DoubleSetting.Builder()
            .name("background-radius")
            .description("Radius of the background corners.")
            .defaultValue(5.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(new ColorSetting.Builder().name("background-color").description("Color of the background.").defaultValue(new SettingColor(0, 0, 0, 100)).build());
   private final Setting<Boolean> enableGlow = this.sgBackground
      .add(new BoolSetting.Builder().name("enable-glow").description("Renders glow around the packet stats background.").defaultValue(true).build());
   private final LongAdder sendCountRaw = new LongAdder();
   private final LongAdder recvCountRaw = new LongAdder();
   private double sendPerSec;
   private double recvPerSec;
   private long lastSampleTime = System.nanoTime();

   private PacketStatsHud() {
      super(INFO);
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   @EventHandler
   private void onSend(PacketEvent.Send event) {
      this.sendCountRaw.increment();
   }

   @EventHandler
   private void onReceive(PacketEvent.Receive event) {
      this.recvCountRaw.increment();
   }

   @Override
   public void render(HudRenderer renderer) {
      this.updateRates();
      String label1 = "Send/s: ";
      String label2 = "Recv/s: ";
      String value1 = formatRate(this.sendPerSec);
      String value2 = formatRate(this.recvPerSec);
      double hudScale = this.scale.get();
      double textScale = Hud.get().getTextScale() * hudScale;
      double padding = 4.0 * hudScale;
      double lineGap = 2.0 * hudScale;
      double textHeight = renderer.textHeight(this.shadow.get(), textScale);
      double lineHeight = textHeight + lineGap;
      double labelWidth = Math.max(renderer.textWidth(label1, this.shadow.get(), textScale), renderer.textWidth(label2, this.shadow.get(), textScale));
      double valueWidth = Math.max(renderer.textWidth(value1, this.shadow.get(), textScale), renderer.textWidth(value2, this.shadow.get(), textScale));
      double contentWidth = labelWidth + valueWidth;
      double contentHeight = lineHeight * 2.0 - lineGap;
      double width = contentWidth + padding * 2.0;
      double height = contentHeight + padding * 2.0;
      this.setSize(width, height);
      this.drawBackground(renderer, width, height, hudScale);
      double textX = this.x + padding;
      double textY = this.y + padding;
      renderer.text(label1, textX, textY, this.labelColor.get(), this.shadow.get(), textScale);
      renderer.text(value1, textX + labelWidth, textY, this.valueColor.get(), this.shadow.get(), textScale);
      textY += lineHeight;
      renderer.text(label2, textX, textY, this.labelColor.get(), this.shadow.get(), textScale);
      renderer.text(value2, textX + labelWidth, textY, this.valueColor.get(), this.shadow.get(), textScale);
   }

   private void updateRates() {
      long now = System.nanoTime();
      long elapsed = now - this.lastSampleTime;
      if (elapsed >= 1000000000L) {
         double seconds = elapsed / 1.0E9;
         this.sendPerSec = this.sendCountRaw.sumThenReset() / seconds;
         this.recvPerSec = this.recvCountRaw.sumThenReset() / seconds;
         this.lastSampleTime = now;
      }
   }

   private void drawBackground(HudRenderer renderer, double width, double height, double hudScale) {
      double radius = this.backgroundRadius.get() * hudScale;
      Color color = this.backgroundColor.get();
      if (this.backgroundMode.get() == PacketStatsHud.BackgroundMode.LiquidGlass && LiquidGlassSetting.isEnabled()) {
         LiquidGlassUtil.render(renderer, this.x, this.y, width, height, radius, color);
      } else {
         BlurUtil.render(Math.round(this.x), Math.round(this.y), (int)Math.ceil(width), (int)Math.ceil(height), (float)radius);
         Render2DUtil.drawRoundedRect(renderer, this.x, this.y, width, height, radius, color);
      }

      if (this.enableGlow.get()) {
         Render2DUtil.drawRoundedGlow(this.x, this.y, width, height, radius);
      }
   }

   private static String formatRate(double value) {
      return Long.toString(Math.round(value));
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }
}
