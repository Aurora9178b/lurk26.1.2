package meteordevelopment.meteorclient.systems.modules.misc;

import io.netty.channel.Channel;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ConnectionAccessor;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public class PacketDeBug extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> packetsPerTick = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("packets-per-tick")
            .description("The amount of invalid item switch packets to send per tick.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 1000)
            .build()
      );
   private final Setting<Integer> badSlot = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("bad-slot")
            .description("The invalid carried item slot sent to the server.")
            .defaultValue(-1)
            .range(-128, 127)
            .sliderRange(-16, 16)
            .build()
      );

   public PacketDeBug() {
      super(Categories.Misc, "packet-de-bug", "Sends invalid item switch packets with zero delay.");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.player.connection != null) {
         Channel channel = ((ConnectionAccessor)this.mc.player.connection.getConnection()).meteor$getChannel();

         for (int i = 0; i < this.packetsPerTick.get(); i++) {
            channel.write(new ServerboundSetCarriedItemPacket(this.badSlot.get()));
         }

         channel.flush();
      }
   }
}
