package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public class TimeChanger extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> time = this.sgGeneral
      .add(new DoubleSetting.Builder().name("time").description("The specified time to be set.").defaultValue(0.0).sliderRange(-20000.0, 20000.0).build());
   long oldTime;

   public TimeChanger() {
      super(Categories.Render, "time-changer", "Makes you able to set a custom time.");
   }

   @Override
   public void onActivate() {
      this.oldTime = this.mc.level.getGameTime();
   }

   @Override
   public void onDeactivate() {
      this.mc.level.getLevelData().setGameTime(this.oldTime);
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundSetTimePacket packet) {
         this.oldTime = packet.gameTime();
         event.cancel();
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      this.mc.level.getLevelData().setGameTime(this.time.get().longValue());
   }
}
