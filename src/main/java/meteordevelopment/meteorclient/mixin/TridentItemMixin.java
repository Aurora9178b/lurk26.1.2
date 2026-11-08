package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.TridentBoost;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
   @Inject(method = "releaseUsing", at = @At("HEAD"))
   private void onReleaseUsingHead(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
      if (entity == MeteorClient.mc.player) {
         Utils.isReleasingTrident = true;
      }
   }

   @Inject(method = "releaseUsing", at = @At("TAIL"))
   private void onReleaseUsingTail(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
      if (entity == MeteorClient.mc.player) {
         Utils.isReleasingTrident = false;
      }
   }

   @ModifyArgs(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V"))
   private void modifyVelocity(Args args) {
      TridentBoost tridentBoost = Modules.get().get(TridentBoost.class);
      args.set(0, args.<Double>get(0) * tridentBoost.getMultiplier());
      args.set(1, args.<Double>get(1) * tridentBoost.getMultiplier());
      args.set(2, args.<Double>get(2) * tridentBoost.getMultiplier());
   }

   @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
   private boolean isInWaterUse(boolean original) {
      TridentBoost tridentBoost = Modules.get().get(TridentBoost.class);
      return tridentBoost.allowOutOfWater() || original;
   }

   @ModifyExpressionValue(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
   private boolean isInWaterPostUse(boolean original) {
      TridentBoost tridentBoost = Modules.get().get(TridentBoost.class);
      return tridentBoost.allowOutOfWater() || original;
   }
}
