package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

public class MBlockPos {
   private static final MutableBlockPos POS = new MutableBlockPos();
   public int x;
   public int y;
   public int z;

   public MBlockPos() {
   }

   public MBlockPos(Entity entity) {
      this.set(entity);
   }

   public MBlockPos set(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return this;
   }

   public MBlockPos set(MBlockPos pos) {
      return this.set(pos.x, pos.y, pos.z);
   }

   public MBlockPos set(Entity entity) {
      return this.set(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ());
   }

   public MBlockPos coerceBlockLevel(Entity entity) {
      return this.set(entity.getBlockX(), (int)Math.round(entity.getY()), entity.getBlockZ());
   }

   public MBlockPos offset(HorizontalDirection dir, int amount) {
      this.x = this.x + dir.offsetX * amount;
      this.z = this.z + dir.offsetZ * amount;
      return this;
   }

   public MBlockPos offset(HorizontalDirection dir) {
      return this.offset(dir, 1);
   }

   public MBlockPos add(int x, int y, int z) {
      this.x += x;
      this.y += y;
      this.z += z;
      return this;
   }

   public BlockPos getBlockPos() {
      return POS.set(this.x, this.y, this.z);
   }

   public BlockState getState() {
      return MeteorClient.mc.level.getBlockState(this.getBlockPos());
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         MBlockPos mBlockPos = (MBlockPos)o;
         if (this.x != mBlockPos.x) {
            return false;
         } else {
            return this.y != mBlockPos.y ? false : this.z == mBlockPos.z;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.x;
      result = 31 * result + this.y;
      return 31 * result + this.z;
   }

   @Override
   public String toString() {
      return this.x + ", " + this.y + ", " + this.z;
   }
}
