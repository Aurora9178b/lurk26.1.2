package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public interface IVec3 {
   Vec3 meteor$set(double var1, double var3, double var5);

   default Vec3 meteor$set(Vec3i vec) {
      return this.meteor$set(vec.getX(), vec.getY(), vec.getZ());
   }

   default Vec3 meteor$set(Vector3d vec) {
      return this.meteor$set(vec.x, vec.y, vec.z);
   }

   default Vec3 meteor$set(Vec3 pos) {
      return this.meteor$set(pos.x, pos.y, pos.z);
   }

   Vec3 meteor$setXZ(double var1, double var3);

   Vec3 meteor$setY(double var1);
}
