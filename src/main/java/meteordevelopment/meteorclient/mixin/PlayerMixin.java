package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.KeepSprint;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.player.Reach;
import meteordevelopment.meteorclient.systems.modules.player.SpeedMine;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
   @Shadow
   public abstract Abilities getAbilities();

   protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
      super(entityType, world);
   }

   @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
   protected void clipAtLedge(CallbackInfoReturnable<Boolean> cir) {
      if (this.level().isClientSide()) {
         ClipAtLedgeEvent event = (ClipAtLedgeEvent)MeteorClient.EVENT_BUS.post(ClipAtLedgeEvent.get());
         if (event.isSet()) {
            cir.setReturnValue(event.isClip());
         }
      }
   }

   @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
   private void onDropItem(ItemStack itemStack, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
      if (this.level().isClientSide() && !itemStack.isEmpty() && ((DropItemsEvent)MeteorClient.EVENT_BUS.post(DropItemsEvent.get(itemStack))).isCancelled()) {
         cir.setReturnValue(null);
      }
   }

   @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
   private void onIsSpectator(CallbackInfoReturnable<Boolean> cir) {
      if (MeteorClient.mc.getConnection() == null) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isCreative", at = @At("HEAD"), cancellable = true)
   private void onIsCreative(CallbackInfoReturnable<Boolean> cir) {
      if (MeteorClient.mc.getConnection() == null) {
         cir.setReturnValue(false);
      }
   }

   @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
   public float onGetBlockBreakingSpeed(float breakSpeed, BlockState state) {
      if (!this.level().isClientSide()) {
         return breakSpeed;
      }

      SpeedMine speedMine = Modules.get().get(SpeedMine.class);
      if (speedMine.isActive() && speedMine.mode.get() == SpeedMine.Mode.Normal && speedMine.filter(state.getBlock())) {
         float breakSpeedMod = (float)(breakSpeed * speedMine.modifier.get());
         if (MeteorClient.mc.hitResult instanceof BlockHitResult bhr) {
            BlockPos pos = bhr.getBlockPos();
            return !(speedMine.modifier.get() < 1.0) && BlockUtils.canInstaBreak(pos, breakSpeed) != BlockUtils.canInstaBreak(pos, breakSpeedMod)
               ? 0.9F / BlockUtils.calcBlockBreakingDelta2(pos, 1.0F)
               : breakSpeedMod;
         } else {
            return breakSpeed;
         }
      } else {
         return breakSpeed;
      }
   }

   @ModifyReturnValue(method = "getSpeed", at = @At("RETURN"))
   private float onGetSpeed(float original) {
      if (!this.level().isClientSide()) {
         return original;
      } else if (!Modules.get().get(NoSlow.class).slowness()) {
         return original;
      } else {
         float walkSpeed = this.getAbilities().getWalkingSpeed();
         if (original < walkSpeed) {
            return this.isSprinting() ? (float)(walkSpeed * 1.300000011920929) : walkSpeed;
         } else {
            return original;
         }
      }
   }

   @Inject(method = "getFlyingSpeed", at = @At("HEAD"), cancellable = true)
   private void onGetFlyingSpeed(CallbackInfoReturnable<Float> cir) {
      if (this.level().isClientSide()) {
         float speed = Modules.get().get(Flight.class).getFlyingSpeed();
         if (speed != -1.0F) {
            cir.setReturnValue(speed);
         }
      }
   }

   @WrapWithCondition(
      method = "causeExtraKnockback",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V")
   )
   private boolean keepSprint$setDeltaMovement(Player instance, Vec3 vec3d) {
      return Modules.get().get(Sprint.class).stopSprinting();
   }

   @WrapWithCondition(method = "causeExtraKnockback", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setSprinting(Z)V"))
   private boolean keepSprint$setSprinting(Player instance, boolean b) {
      return Modules.get().get(Sprint.class).stopSprinting();
   }

   @Inject(method = "attack", at = @At("RETURN"))
   private void keepSprint$afterAttack(Entity entity, CallbackInfo ci) {
      if ((Object)this == MeteorClient.mc.player) {
         KeepSprint keepSprint = Modules.get().get(KeepSprint.class);
         if (keepSprint != null && keepSprint.shouldKeepSprint()) {
            if (!MeteorClient.mc.player.isSprinting()) {
               MeteorClient.mc.player.setSprinting(true);
            }

            double slowdownPercent = keepSprint.slowdown.get().intValue() / 100.0;
            if (slowdownPercent > 0.0) {
               double customFactor = 0.6 + 0.4 * (1.0 - slowdownPercent);
               MeteorClient.mc.player.setDeltaMovement(MeteorClient.mc.player.getDeltaMovement().multiply(customFactor, 1.0, customFactor));
            }
         }
      }
   }

   @ModifyReturnValue(method = "blockInteractionRange", at = @At("RETURN"))
   private double modifyBlockInteractionRange(double original) {
      return Math.max(0.0, original + Modules.get().get(Reach.class).blockReach());
   }

   @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
   private double modifyEntityInteractionRange(double original) {
      return Math.max(0.0, original + Modules.get().get(Reach.class).entityReach());
   }
}
