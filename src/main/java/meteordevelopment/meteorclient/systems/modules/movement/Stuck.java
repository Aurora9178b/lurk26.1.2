package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.TravelEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LocalPlayerAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.phys.Vec3;

public class Stuck extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Stuck.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("How to keep you stuck in place."))
               .defaultValue(Stuck.Mode.NoPacket))
            .build()
      );
   private final Setting<Boolean> freezeLook = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("freeze-look")
            .description("Freezes your pitch and yaw.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == Stuck.Mode.NoPacket)
            .build()
      );
   private final Setting<Boolean> freezeLookSilent = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("freeze-look-silent")
            .description("Freezes your pitch and yaw silently.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == Stuck.Mode.NoPacket && this.freezeLook.get())
            .build()
      );
   private final Setting<Boolean> freezeLookPlace = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("freeze-look-place")
            .description("Unfreezes your yaw and pitch when placing.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == Stuck.Mode.NoPacket && this.freezeLookSilent.get())
            .build()
      );
   private float yaw;
   private float pitch;
   private boolean rotate;
   private Vec3 position = Vec3.ZERO;

   public Stuck() {
      super(Categories.Movement, "stuck", "Keeps you stuck in place.");
   }

   @Override
   public void onActivate() {
      if (Utils.canUpdate()) {
         this.yaw = this.mc.player.getYRot();
         this.pitch = this.mc.player.getXRot();
         this.position = this.mc.player.position();
         this.rotate = false;
      }
   }

   @EventHandler
   private void onMoveInput(MoveInputEvent event) {
      event.setForward(0.0F);
      event.setStrafe(0.0F);
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (this.mode.get() == Stuck.Mode.NoPacket && event.packet instanceof ServerboundMovePlayerPacket playerMove) {
         this.freezeNoPacket(event, playerMove);
      }
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      if (this.mode.get() == Stuck.Mode.CancelMove && event.packet instanceof ClientboundPlayerPositionPacket) {
         this.toggle();
      }
   }

   @EventHandler
   private void onTravel(TravelEvent event) {
      if (Utils.canUpdate()) {
         if (this.mode.get() == Stuck.Mode.CancelMove && ((LocalPlayerAccessor)this.mc.player).meteor$getPositionReminder() < 19) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (Utils.canUpdate() && this.mode.get() == Stuck.Mode.NoPacket && this.freezeLookPlace.get()) {
         Rot packet = new Rot(this.mc.player.getYRot(), this.mc.player.getXRot(), this.mc.player.onGround(), this.mc.player.horizontalCollision);
         this.rotate = true;
         this.mc.getConnection().send(packet);
         this.rotate = false;
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate() && this.mode.get() == Stuck.Mode.NoPacket) {
         this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         this.mc.player.setPosRaw(this.position.x, this.position.y, this.position.z);
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.mode.get() == Stuck.Mode.NoPacket) {
         event.movement = Vec3.ZERO;
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      if (this.isActive()) {
         this.toggle();
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      if (event.entity == this.mc.player && this.isActive()) {
         this.toggle();
      }
   }

   private void freezeNoPacket(PacketEvent.Send event, ServerboundMovePlayerPacket playerMove) {
      if (playerMove.hasRotation() && this.freezeLook.get() && this.freezeLookSilent.get() && !this.rotate) {
         event.cancel();
      } else if (this.mc.player != null && playerMove.hasRotation() && this.freezeLook.get() && !this.freezeLookSilent.get()) {
         event.cancel();
         this.mc.player.setYRot(this.yaw);
         this.mc.player.setXRot(this.pitch);
      }

      if (this.mc.player != null && playerMove.hasPosition()) {
         this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
         this.mc.player.setPosRaw(this.position.x, this.position.y, this.position.z);
         event.cancel();
      }
   }

   public enum Mode {
      NoPacket,
      CancelMove;
   }
}
