package meteordevelopment.meteorclient.systems.modules.player;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.DoItemUseEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Fail;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GhostHand extends Module {
   private final Set<BlockPos> posList = new ObjectOpenHashSet<>();

   public GhostHand() {
      super(Categories.Player, "ghost-hand", "Opens containers through walls.");
   }

   @EventHandler
   private void onTick(DoItemUseEvent event) {
      if (this.mc.options.keyUse.isDown() && !this.mc.player.isShiftKeyDown()) {
         if (!this.mc
            .level
            .getBlockState(
               BlockPos.containing(
                  this.mc.player.pick(this.mc.player.blockInteractionRange(), this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true), false).getLocation()
               )
            )
            .hasBlockEntity()) {
            Vec3 direction = new Vec3(0.0, 0.0, 0.1)
               .xRot(-((float)Math.toRadians(this.mc.player.getXRot())))
               .yRot(-((float)Math.toRadians(this.mc.player.getYRot())));
            this.posList.clear();

            for (int i = 1; i < this.mc.player.blockInteractionRange() * 10.0; i++) {
               BlockPos pos = BlockPos.containing(
                  this.mc.player.getEyePosition(this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)).add(direction.scale(i))
               );
               if (!this.posList.contains(pos)) {
                  this.posList.add(pos);
                  if (this.mc.level.getBlockState(pos).hasBlockEntity()) {
                     for (InteractionHand hand : InteractionHand.values()) {
                        InteractionResult result = this.mc
                           .gameMode
                           .useItemOn(
                              this.mc.player, hand, new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true)
                           );
                        if (result instanceof Success || result instanceof Fail) {
                           this.mc.player.swing(hand);
                           event.cancel();
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
