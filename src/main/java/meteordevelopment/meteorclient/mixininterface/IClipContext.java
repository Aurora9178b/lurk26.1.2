package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.Vec3;

public interface IClipContext {
   void meteor$set(Vec3 var1, Vec3 var2, Block var3, Fluid var4, Entity var5);
}
