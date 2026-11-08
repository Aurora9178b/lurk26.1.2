package meteordevelopment.meteorclient.utils.leaves;

import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class LeavesRotation {
   private LeavesRotation() {
   }

   public static void snapAt(Vec3 vec) {
      RotationManager.INSTANCE.setRotations(RotationUtil.calculate(vec), 100.0, Priority.High);
   }

   public static void snapAt(BlockPos pos, Direction side) {
      double x = pos.getX() + 0.5 + side.getStepX() * 0.5;
      double y = pos.getY() + 0.5 + side.getStepY() * 0.5;
      double z = pos.getZ() + 0.5 + side.getStepZ() * 0.5;
      snapAt(new Vec3(x, y, z));
   }

   public static void snapAt(float yaw, float pitch) {
      RotationManager.INSTANCE.setRotations(new Rotation(yaw, pitch), 100.0, Priority.High);
   }

   public static void snapBack() {
      RotationManager.INSTANCE.reset();
   }
}
