package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClickTP extends Module {
   public ClickTP() {
      super(Categories.Movement, "click-tp", "Teleports you to the block you click on.");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player.getInventory().getSelectedItem().getUseAnimation() == ItemUseAnimation.NONE) {
         if (this.mc.options.keyUse.isDown()) {
            if (this.mc.hitResult != null) {
               if (this.mc.hitResult.getType() == Type.ENTITY
                  && this.mc.player.interactOn(((EntityHitResult)this.mc.hitResult).getEntity(), InteractionHand.MAIN_HAND, this.mc.hitResult.getLocation())
                     != InteractionResult.PASS) {
                  return;
               }

               if (this.mc.hitResult.getType() == Type.BLOCK && this.mc.player.getMainHandItem().getItem() instanceof BlockItem) {
                  return;
               }
            }

            Camera camera = this.mc.gameRenderer.getMainCamera();
            Vec3 cameraPos = camera.position();
            Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(210.0);
            Vec3 targetPos = cameraPos.add(direction);
            ClipContext context = new ClipContext(cameraPos, targetPos, Block.OUTLINE, Fluid.NONE, this.mc.player);
            BlockHitResult hitResult = this.mc.level.clip(context);
            if (hitResult.getType() == Type.BLOCK) {
               BlockPos pos = hitResult.getBlockPos();
               Direction side = hitResult.getDirection();
               if (this.mc.level.getBlockState(pos).useWithoutItem(this.mc.level, this.mc.player, hitResult) != InteractionResult.PASS) {
                  return;
               }

               BlockState state = this.mc.level.getBlockState(pos);
               VoxelShape shape = state.getCollisionShape(this.mc.level, pos);
               if (shape.isEmpty()) {
                  shape = state.getShape(this.mc.level, pos);
               }

               double height = shape.isEmpty() ? 1.0 : shape.max(Axis.Y);
               Vec3 newPos = new Vec3(pos.getX() + 0.5 + side.getStepX(), pos.getY() + height, pos.getZ() + 0.5 + side.getStepZ());
               int packetsRequired = (int)Math.ceil(this.mc.player.position().distanceTo(newPos) / 10.0) - 1;
               if (packetsRequired > 19) {
                  packetsRequired = 0;
               }

               for (int packetNumber = 0; packetNumber < packetsRequired; packetNumber++) {
                  this.mc.player.connection.send(new StatusOnly(true, true));
               }

               this.mc.player.connection.send(new Pos(newPos.x, newPos.y, newPos.z, true, true));
               this.mc.player.setPos(newPos);
            }
         }
      }
   }
}
