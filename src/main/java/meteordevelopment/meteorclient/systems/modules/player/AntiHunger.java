package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;

public class AntiHunger extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> sprint = this.sgGeneral
      .add(new BoolSetting.Builder().name("sprint").description("Spoofs sprinting packets.").defaultValue(true).build());
   private final Setting<Boolean> onGround = this.sgGeneral
      .add(new BoolSetting.Builder().name("on-ground").description("Spoofs the onGround flag.").defaultValue(true).build());
   private boolean lastOnGround;
   private boolean ignorePacket;

   public AntiHunger() {
      super(Categories.Player, "anti-hunger", "Reduces (does NOT remove) hunger consumption.");
   }

   @Override
   public void onActivate() {
      this.lastOnGround = this.mc.player.onGround();
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.ignorePacket && event.packet instanceof ServerboundMovePlayerPacket) {
         this.ignorePacket = false;
      } else if (!this.mc.player.isPassenger() && !this.mc.player.isInWater() && !this.mc.player.isUnderWater()) {
         if (event.packet instanceof ServerboundPlayerCommandPacket packet && this.sprint.get() && packet.getAction() == Action.START_SPRINTING) {
            event.cancel();
         }

         if (event.packet instanceof ServerboundMovePlayerPacket packet
            && this.onGround.get()
            && this.mc.player.onGround()
            && this.mc.player.fallDistance <= 0.0
            && !this.mc.gameMode.isDestroying()) {
            ((ServerboundMovePlayerPacketAccessor)packet).meteor$setOnGround(false);
         }
      }
   }

   @EventHandler
   private void onTick(SendMovementPacketsEvent.Pre event) {
      if (this.mc.player.onGround() && !this.lastOnGround && this.onGround.get()) {
         this.ignorePacket = true;
      }

      this.lastOnGround = this.mc.player.onGround();
   }
}
