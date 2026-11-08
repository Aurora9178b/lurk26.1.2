package meteordevelopment.meteorclient.utils.leaves;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class LeavesBlockPos extends BlockPos {
   public LeavesBlockPos(double x, double y, double z) {
      super(Mth.floor(x), Mth.floor(y), Mth.floor(z));
   }

   public LeavesBlockPos(double x, double y, double z, boolean fix) {
      this(x, y + (fix ? 0.5 : 0.0), z);
   }

   public LeavesBlockPos(Vec3 vec) {
      this(vec.x, vec.y, vec.z);
   }

   public LeavesBlockPos(Vec3 vec, boolean fix) {
      this(vec.x, vec.y, vec.z, fix);
   }
}
