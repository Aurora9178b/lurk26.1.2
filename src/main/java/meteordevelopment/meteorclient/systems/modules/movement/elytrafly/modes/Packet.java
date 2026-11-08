package meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightMode;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class Packet extends ElytraFlightMode {
   private final Vec3 vec3d = new Vec3(0.0, 0.0, 0.0);

   public Packet() {
      super(ElytraFlightModes.Packet);
   }

   @Override
   public void onDeactivate() {
      this.mc.player.getAbilities().flying = false;
      this.mc.player.getAbilities().mayfly = false;
   }

   @Override
   public void onTick() {
      super.onTick();
      if (this.mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
         && !(this.mc.player.fallDistance <= 0.2)
         && !this.mc.options.keyShift.isDown()) {
         if (this.mc.options.keyUp.isDown()) {
            this.vec3d.add(0.0, 0.0, this.elytraFly.horizontalSpeed.get());
            this.vec3d.yRot(-((float)Math.toRadians(this.mc.player.getYRot())));
         } else if (this.mc.options.keyDown.isDown()) {
            this.vec3d.add(0.0, 0.0, this.elytraFly.horizontalSpeed.get());
            this.vec3d.yRot((float)Math.toRadians(this.mc.player.getYRot()));
         }

         if (this.mc.options.keyJump.isDown()) {
            this.vec3d.add(0.0, this.elytraFly.verticalSpeed.get(), 0.0);
         } else if (!this.mc.options.keyJump.isDown()) {
            this.vec3d.add(0.0, -this.elytraFly.verticalSpeed.get(), 0.0);
         }

         this.mc.player.setDeltaMovement(this.vec3d);
         this.mc.player.connection.send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_FALL_FLYING));
         this.mc.player.connection.send(new StatusOnly(true, this.mc.player.horizontalCollision));
      }
   }

   @Override
   public void onPacketSend(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundMovePlayerPacket) {
         this.mc.player.connection.send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_FALL_FLYING));
      }
   }

   @Override
   public void onPlayerMove() {
      this.mc.player.getAbilities().flying = true;
      this.mc.player.getAbilities().setFlyingSpeed(this.elytraFly.horizontalSpeed.get().floatValue() / 20.0F);
   }
}
