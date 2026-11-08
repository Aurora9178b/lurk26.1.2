package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.function.ToDoubleFunction;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttackRange.class)
public abstract class AttackRangeMixin {
   @ModifyExpressionValue(
      method = "isInRange(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/ToDoubleFunction;D)Z",
      at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/component/AttackRange;hitboxMargin:F", opcode = 180)
   )
   private float modifyHitboxMargin(float original, LivingEntity attacker, ToDoubleFunction<Vec3> distanceFunction, double extraBuffer) {
      float v = (float)Modules.get().get(Hitboxes.class).getEntityValue(attacker);
      return original + v;
   }
}
