package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
   @ModifyExpressionValue(
      method = "controlBoat",
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;inputLeft:Z", opcode = 180)
   )
   private boolean modifyPressingLeft(boolean original) {
      return Modules.get().isActive(EntityControl.class) && Modules.get().get(EntityControl.class).lockYaw.get() ? false : original;
   }

   @ModifyExpressionValue(
      method = "controlBoat",
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;inputRight:Z", opcode = 180)
   )
   private boolean modifyPressingRight(boolean original) {
      return Modules.get().isActive(EntityControl.class) && Modules.get().get(EntityControl.class).lockYaw.get() ? false : original;
   }
}
