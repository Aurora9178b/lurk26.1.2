package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
   @WrapOperation(
      method = "handleEntityEvent",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;pullEntity(Lnet/minecraft/world/entity/Entity;)V")
   )
   private void preventFishingRodPull(FishingHook instance, Entity entity, Operation<Void> original) {
      if (!instance.level().isClientSide() || entity != MeteorClient.mc.player) {
         original.call(new Object[]{instance, entity});
      }

      Velocity velocity = Modules.get().get(Velocity.class);
      if (!velocity.isActive() || !velocity.fishing.get()) {
         original.call(new Object[]{instance, entity});
      }
   }
}
