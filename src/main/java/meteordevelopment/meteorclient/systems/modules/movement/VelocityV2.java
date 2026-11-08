package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class VelocityV2 extends Module {
   private static final int MAX_FREEZE_TICKS = 20;
   private boolean cancelNextVelocity;
   private boolean needClick;
   private boolean waitForUpdate;
   private boolean shouldSkip;
   private int freezeTicks;

   public VelocityV2() {
      super(Categories.Movement, "VelocityV2", "Grim interaction velocity bypass.");
   }

   @Override
   public void onActivate() {
      this.cancelNextVelocity = false;
      this.needClick = false;
      this.waitForUpdate = false;
      this.shouldSkip = false;
      this.freezeTicks = 0;
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (Utils.canUpdate()) {
         if (!(event.packet instanceof ServerboundInteractPacket) && !(event.packet instanceof ServerboundUseItemOnPacket)) {
            if (event.packet instanceof ServerboundMovePlayerPacket packet && packet.hasPosition() && this.waitForUpdate) {
               event.cancel();
            }
         } else {
            this.shouldSkip = true;
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (Utils.canUpdate()) {
         if (event.packet instanceof ClientboundBlockUpdatePacket packet) {
            if (this.waitForUpdate && packet.getPos().equals(this.mc.player.blockPosition())) {
               this.waitForUpdate = false;
               this.needClick = false;
            }
         } else if (event.packet instanceof ClientboundDamageEventPacket packet) {
            if (packet.entityId() == this.mc.player.getId()) {
               this.cancelNextVelocity = true;
            }
         } else if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            if (packet.id() == this.mc.player.getId() && this.cancelNextVelocity) {
               event.cancel();
               this.cancelNextVelocity = false;
               this.needClick = true;
            }
         } else if (event.packet instanceof ClientboundExplodePacket && this.cancelNextVelocity) {
            event.cancel();
            this.cancelNextVelocity = false;
            this.needClick = true;
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         if (this.needClick && !this.shouldSkip && !this.mc.player.isUsingItem()) {
            BlockPos pos = this.mc.player.blockPosition();
            Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);
            if (this.mc.getConnection() != null) {
               this.mc.getConnection().send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hitResult, 0));
               this.mc.player.swing(InteractionHand.MAIN_HAND);
               this.mc.getConnection().send(new Rot(this.mc.player.getYRot(), 90.0F, this.mc.player.onGround(), this.mc.player.horizontalCollision));
            }

            this.freezeTicks = 0;
            this.waitForUpdate = true;
            this.needClick = false;
         }

         if (this.waitForUpdate) {
            this.mc.player.setDeltaMovement(0.0, this.mc.player.getDeltaMovement().y, 0.0);
            this.freezeTicks++;
            if (this.freezeTicks > 20) {
               this.waitForUpdate = false;
               this.needClick = false;
            }
         }

         this.shouldSkip = false;
      }
   }
}
