package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Jesus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
   @ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
   private static boolean onCanWalkOnPowderSnow(boolean original, Entity entity) {
      return entity == MeteorClient.mc.player && Modules.get().get(Jesus.class).canWalkOnPowderSnow() ? true : original;
   }
}
