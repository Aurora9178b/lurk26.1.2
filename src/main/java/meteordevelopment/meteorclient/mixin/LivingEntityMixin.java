package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.events.entity.player.TravelEvent;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.HighJump;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFlightModes;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.modes.Bounce;
import meteordevelopment.meteorclient.systems.modules.player.NoStatusEffects;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.systems.modules.render.HandsViewPlus;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.movement.MovementFix;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
   @Unique
   private boolean previousElytra = false;

   public LivingEntityMixin(EntityType<?> type, Level world) {
      super(type, world);
   }

   @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
   private void onTravel(Vec3 input, CallbackInfo ci) {
      if ((Object)this == MeteorClient.mc.player) {
         TravelEvent event = (TravelEvent)MeteorClient.EVENT_BUS.post(TravelEvent.get());
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @ModifyReturnValue(method = "canStandOnFluid", at = @At("RETURN"))
   private boolean onCanWalkOnFluid(boolean original, FluidState fluid) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      }

      CanWalkOnFluidEvent event = (CanWalkOnFluidEvent)MeteorClient.EVENT_BUS.post(CanWalkOnFluidEvent.get(fluid));
      return event.walkOnFluid;
   }

   @Inject(method = "spawnItemParticles", at = @At("HEAD"), cancellable = true)
   private void spawnItemParticles(ItemStack itemStack, int count, CallbackInfo ci) {
      NoRender noRender = Modules.get().get(NoRender.class);
      if (noRender.noEatParticles() && itemStack.getComponents().has(DataComponents.FOOD)) {
         ci.cancel();
      }
   }

   @ModifyVariable(method = "swing(Lnet/minecraft/world/InteractionHand;)V", at = @At("HEAD"), argsOnly = true, name = "hand")
   private InteractionHand setHand(InteractionHand hand) {
      if ((Object)this != MeteorClient.mc.player) {
         return hand;
      }

      HandView handView = Modules.get().get(HandView.class);
      if (handView.isActive()) {
         if (handView.swingMode.get() == HandView.SwingMode.None) {
            return hand;
         } else {
            return handView.swingMode.get() == HandView.SwingMode.Offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         }
      } else {
         return hand;
      }
   }

   @ModifyExpressionValue(
      method = "getCurrentSwingDuration",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/SwingAnimation;duration()I")
   )
   private int getHandSwingDuration(int original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      } else {
         HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
         if (handsViewPlus != null && handsViewPlus.isActive()) {
            return handsViewPlus.swingSpeed.get();
         } else {
            return Modules.get().get(HandView.class).isActive() && MeteorClient.mc.options.getCameraType().isFirstPerson()
               ? Modules.get().get(HandView.class).swingSpeed.get()
               : original;
         }
      }
   }

   @ModifyReturnValue(method = "isFallFlying", at = @At("RETURN"))
   private boolean isGlidingHook(boolean original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      } else {
         return Modules.get().get(ElytraFly.class).canPacketEfly() ? true : original;
      }
   }

   @Inject(method = "isFallFlying", at = @At("TAIL"), cancellable = true)
   public void recastOnLand(CallbackInfoReturnable<Boolean> cir) {
      boolean elytra = cir.getReturnValue();
      ElytraFly elytraFly = Modules.get().get(ElytraFly.class);
      if (this.previousElytra && !elytra && elytraFly.isActive() && elytraFly.flightMode.get() == ElytraFlightModes.Bounce) {
         cir.setReturnValue(Bounce.recastElytra(MeteorClient.mc.player));
      }

      this.previousElytra = elytra;
   }

   @ModifyReturnValue(method = "hasEffect", at = @At("RETURN"))
   private boolean hasEffect(boolean original, Holder<MobEffect> effect) {
      if (effect != null && effect.value() != null) {
         return Modules.get().get(NoStatusEffects.class).shouldBlock(effect.value()) ? false : original;
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
   private float modifyGetYaw(float original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      }

      RotationManager rotationManager = RotationManager.INSTANCE;
      if (!MeteorClient.mc.player.isFallFlying() && MovementFix.shouldFix(rotationManager)) {
         original = rotationManager.rotations.getYaw();
      }

      if (!Modules.get().get(Sprint.class).rageSprint()) {
         return original;
      }

      float forward = Math.signum(MeteorClient.mc.player.zza);
      float strafe = 90.0F * Math.signum(MeteorClient.mc.player.xxa);
      if (forward != 0.0F) {
         strafe *= forward * 0.5F;
      }

      original -= strafe;
      if (forward < 0.0F) {
         original -= 180.0F;
      }

      return original;
   }

   @ModifyConstant(method = "jumpFromGround", constant = @Constant(floatValue = 1.0E-5F))
   private float modifyJumpConstant(float original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      } else {
         return !Modules.get().isActive(HighJump.class) ? original : -1.0F;
      }
   }

   @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSprinting()Z"))
   private boolean modifyIsSprinting(boolean original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      } else {
         return !Modules.get().get(Sprint.class).rageSprint()
            ? original
            : original && (Math.abs(MeteorClient.mc.player.zza) > 1.0E-5F || Math.abs(MeteorClient.mc.player.xxa) > 1.0E-5F);
      }
   }
}
