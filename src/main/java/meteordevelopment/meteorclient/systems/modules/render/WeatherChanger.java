package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket.Type;

public class WeatherChanger extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> rainLevel = this.sgGeneral
      .add(new DoubleSetting.Builder().name("rain-level").description("The specified rain level to be set.").defaultValue(0.0).sliderRange(0.0, 1.0).build());
   private final Setting<Double> thunderLevel = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("thunder-level")
            .description("The specified thunder level to be set.")
            .defaultValue(0.0)
            .sliderRange(0.0, 1.0)
            .build()
      );
   private float oldThunderLevel;
   private float oldRainLevel;

   public WeatherChanger() {
      super(Categories.Render, "weather-changer", "Allows you to override the world's current weather.");
   }

   @Override
   public void onActivate() {
      if (this.mc.level != null) {
         this.oldThunderLevel = this.mc.level.getThunderLevel(1.0F);
         this.oldRainLevel = this.mc.level.getRainLevel(1.0F);
      }
   }

   @Override
   public void onDeactivate() {
      if (this.mc.level != null) {
         this.mc.level.setRainLevel(this.oldRainLevel);
         this.mc.level.setThunderLevel(this.oldThunderLevel);
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundGameEventPacket packet) {
         Type var4 = packet.getEvent();
         if (this.isWeatherPacket(var4)) {
            if (var4 == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
               this.oldThunderLevel = packet.getParam();
            } else if (var4 == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
               this.oldRainLevel = packet.getParam();
            }

            event.cancel();
         }
      }
   }

   private boolean isWeatherPacket(Type type) {
      return type == ClientboundGameEventPacket.START_RAINING
         || type == ClientboundGameEventPacket.STOP_RAINING
         || type == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
         || type == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.level != null) {
         this.mc.level.setRainLevel(this.rainLevel.get().floatValue());
         this.mc.level.setThunderLevel(this.thunderLevel.get().floatValue());
      }
   }
}
