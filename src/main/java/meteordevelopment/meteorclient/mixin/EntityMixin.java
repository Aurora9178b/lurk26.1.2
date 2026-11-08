package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.JumpVelocityMultiplierEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.mixininterface.ICamera;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Hitboxes;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.Jesus;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.HighwayBuilder;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.movement.MovementFix;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Entity.class)
public abstract class EntityMixin {
   @Inject(method = {"isInWater", "isInLava"}, at = @At("HEAD"), cancellable = true)
   private void onIsInFluid(CallbackInfoReturnable<Boolean> cir) {
      if ((Object)this == MeteorClient.mc.player) {
         Flight flight = Modules.get().get(Flight.class);
         NoSlow noSlow = Modules.get().get(NoSlow.class);
         if (flight.isActive() || noSlow.fluidDrag()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(method = {"onAboveBubbleColumn", "onInsideBubbleColumn"}, at = @At("HEAD"))
   private void onBubbleColumn(CallbackInfo ci) {
      if ((Object)this == MeteorClient.mc.player) {
         Jesus jesus = Modules.get().get(Jesus.class);
         if (jesus.isActive()) {
            jesus.isInBubbleColumn = true;
         }
      }
   }

   @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
   private boolean isSubmergedInWater(boolean submerged) {
      if ((Object)this != MeteorClient.mc.player) {
         return submerged;
      } else if (Modules.get().get(NoSlow.class).fluidDrag()) {
         return false;
      } else {
         return Modules.get().get(Flight.class).isActive() ? false : submerged;
      }
   }

   @ModifyArgs(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
   private void onPushAwayFrom(Args args, Entity entity) {
      Velocity velocity = Modules.get().get(Velocity.class);
      if ((Object)this == MeteorClient.mc.player && velocity.isActive() && velocity.entityPush.get()) {
         double multiplier = velocity.entityPushAmount.get();
         args.set(0, args.<Double>get(0) * multiplier);
         args.set(2, args.<Double>get(2) * multiplier);
      } else if (entity instanceof FakePlayerEntity player && player.doNotPush) {
         args.set(0, 0.0);
         args.set(2, 0.0);
      }
   }

   @ModifyReturnValue(method = "getBlockJumpFactor", at = @At("RETURN"))
   private float onGetBlockJumpFactor(float original) {
      if ((Object)this == MeteorClient.mc.player) {
         JumpVelocityMultiplierEvent event = (JumpVelocityMultiplierEvent)MeteorClient.EVENT_BUS.post(JumpVelocityMultiplierEvent.get());
         return original * event.multiplier;
      } else {
         return original;
      }
   }

   @Inject(method = "move", at = @At("HEAD"))
   private void onMove(MoverType moverType, Vec3 delta, CallbackInfo ci) {
      if ((Object)this == MeteorClient.mc.player) {
         MeteorClient.EVENT_BUS.post(PlayerMoveEvent.get(moverType, delta));
      } else {
         MeteorClient.EVENT_BUS.post(EntityMoveEvent.get((Entity)(Object)this, delta));
      }
   }

   @WrapOperation(
      method = "getViewVector",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;")
   )
   private Vec3 onGetViewVector(Entity instance, float xRot, float yRot, Operation<Vec3> original) {
      RotationManager rotationManager = RotationManager.INSTANCE;
      return instance == MeteorClient.mc.player && rotationManager.isActive() && rotationManager.rotations != null
         ? (Vec3)original.call(new Object[]{instance, rotationManager.rotations.getPitch(), rotationManager.rotations.getYaw()})
         : (Vec3)original.call(new Object[]{instance, xRot, yRot});
   }

   @WrapOperation(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getYRot()F"))
   private float onMoveRelativeYaw(Entity instance, Operation<Float> original) {
      RotationManager rotationManager = RotationManager.INSTANCE;
      return instance == MeteorClient.mc.player && !MeteorClient.mc.player.isFallFlying() && MovementFix.shouldFix(rotationManager)
         ? rotationManager.rotations.getYaw()
         : (Float)original.call(new Object[]{instance});
   }

   @ModifyExpressionValue(
      method = "getBlockSpeedFactor",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
   )
   private Block modifyBlockSpeedFactor(Block original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      } else if (original == Blocks.SOUL_SAND && Modules.get().get(NoSlow.class).soulSand()) {
         return Blocks.STONE;
      } else {
         return original == Blocks.HONEY_BLOCK && Modules.get().get(NoSlow.class).honeyBlock() ? Blocks.STONE : original;
      }
   }

   @ModifyReturnValue(method = "isInvisibleTo", at = @At("RETURN"))
   private boolean isInvisibleToCanceller(boolean original) {
      if (!Utils.canUpdate()) {
         return original;
      }

      ESP esp = Modules.get().get(ESP.class);
      return !Modules.get().get(NoRender.class).noInvisibility() && (!esp.isActive() || esp.shouldSkip((Entity)(Object)(Object)this)) ? original : false;
   }

   @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
   private void isCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
      if (Modules.get().get(NoRender.class).noGlowing()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "getPickRadius", at = @At("HEAD"), cancellable = true)
   private void onGetPickRadius(CallbackInfoReturnable<Float> cir) {
      double v = Modules.get().get(Hitboxes.class).getEntityValue((Entity)(Object)(Object)this);
      if (v != 0.0) {
         cir.setReturnValue((float)v);
      }
   }

   @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
   private void onIsInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
      if (player == null) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "getPose", at = @At("HEAD"), cancellable = true)
   private void getPoseHook(CallbackInfoReturnable<Pose> cir) {
      if ((Object)this == MeteorClient.mc.player) {
         if (Modules.get().get(ElytraFly.class).canPacketEfly()) {
            cir.setReturnValue(Pose.FALL_FLYING);
         }
      }
   }

   @ModifyReturnValue(method = "getPose", at = @At("RETURN"))
   private Pose modifyGetPose(Pose original) {
      if ((Object)this != MeteorClient.mc.player) {
         return original;
      } else {
         return original == Pose.CROUCHING
               && !MeteorClient.mc.player.isShiftKeyDown()
               && ((PlayerAccessor)MeteorClient.mc.player).meteor$canChangeIntoPose(Pose.STANDING)
            ? Pose.STANDING
            : original;
      }
   }

   @ModifyReturnValue(method = "isSuppressingBounce", at = @At("RETURN"))
   private boolean cancelBounce(boolean original) {
      return Modules.get().get(NoFall.class).cancelBounce() || original;
   }

   @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
   private void updateTurn(double xo, double yo, CallbackInfo ci) {
      if ((Object)this == MeteorClient.mc.player) {
         Freecam freecam = Modules.get().get(Freecam.class);
         FreeLook freeLook = Modules.get().get(FreeLook.class);
         if (freecam.isActive()) {
            freecam.changeLookDirection(xo * 0.15, yo * 0.15);
            ci.cancel();
         } else if (Modules.get().isActive(HighwayBuilder.class)) {
            Camera camera = MeteorClient.mc.gameRenderer.getMainCamera();
            ((ICamera)camera).meteor$setRot(camera.yRot() + xo * 0.15, camera.xRot() + yo * 0.15);
            ci.cancel();
         } else if (freeLook.cameraMode()) {
            freeLook.cameraYaw = freeLook.cameraYaw + (float)(xo / freeLook.sensitivity.get().floatValue());
            freeLook.cameraPitch = freeLook.cameraPitch + (float)(yo / freeLook.sensitivity.get().floatValue());
            if (Math.abs(freeLook.cameraPitch) > 90.0F) {
               freeLook.cameraPitch = freeLook.cameraPitch > 0.0F ? 90.0F : -90.0F;
            }

            ci.cancel();
         }
      }
   }
}
