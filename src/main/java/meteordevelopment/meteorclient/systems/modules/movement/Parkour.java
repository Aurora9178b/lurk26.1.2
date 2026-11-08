package meteordevelopment.meteorclient.systems.modules.movement;

import com.google.common.collect.Streams;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Parkour extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> edgeDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder().name("edge-distance").description("How far from the edge should you jump.").range(0.001, 0.1).defaultValue(0.001).build()
      );

   public Parkour() {
      super(Categories.Movement, "parkour", "Automatically jumps at the edges of blocks.");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player.onGround() && !this.mc.options.keyJump.isDown()) {
         if (!this.mc.player.isShiftKeyDown() && !this.mc.options.keyShift.isDown()) {
            AABB box = this.mc.player.getBoundingBox();
            AABB adjustedBox = box.move(0.0, -0.5, 0.0).inflate(-this.edgeDistance.get(), 0.0, -this.edgeDistance.get());
            Stream<VoxelShape> blockCollisions = Streams.stream(this.mc.level.getBlockCollisions(this.mc.player, adjustedBox));
            if (!blockCollisions.findAny().isPresent()) {
               this.mc.player.jumpFromGround();
            }
         }
      }
   }
}
