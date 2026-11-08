package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerTickMovementEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.EntityControl;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Scaffold;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import meteordevelopment.meteorclient.systems.modules.movement.Sprint;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.meteorclient.systems.modules.player.LiquidInteract;
import meteordevelopment.meteorclient.systems.modules.player.NoMiningTrace;
import meteordevelopment.meteorclient.systems.modules.player.Portals;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
   @Shadow
   public ClientInput input;
   @Unique
   private SendMovementPacketsEvent.Pre meteor$sendMovementPacketsEvent;

   public LocalPlayerMixin(ClientLevel world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
   private void onDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
      if (((DropItemsEvent)MeteorClient.EVENT_BUS.post(DropItemsEvent.get(this.getMainHandItem()))).isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @ModifyExpressionValue(
      method = "handlePortalTransitionEffect",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = 180)
   )
   private Screen modifyPortalTransitionEffect(Screen original) {
      return Modules.get().isActive(Portals.class) ? null : original;
   }

   @ModifyExpressionValue(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
   private boolean redirectUsingItem(boolean isUsingItem) {
      return Modules.get().get(NoSlow.class).items() ? false : isUsingItem;
   }

   @Inject(method = "isShiftKeyDown", at = @At("HEAD"), cancellable = true)
   private void onIsShiftKeyDown(CallbackInfoReturnable<Boolean> cir) {
      if (Modules.get().get(Scaffold.class).scaffolding()) {
         cir.setReturnValue(false);
      }

      if (Modules.get().get(Flight.class).noSneak()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
   private void onIsMovingSlowly(CallbackInfoReturnable<Boolean> cir) {
      if (Modules.get().get(NoSlow.class).sneaking()) {
         cir.setReturnValue(this.isVisuallyCrawling());
      }
   }

   @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
   private void onMoveTowardsClosestSpace(double x, double z, CallbackInfo ci) {
      Velocity velocity = Modules.get().get(Velocity.class);
      if (velocity.isActive() && velocity.blocks.get()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(
      method = "tick",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/ClientInput;keyPresses:Lnet/minecraft/world/entity/player/Input;", opcode = 180)
   )
   private Input isSneaking(Input original) {
      return !Modules.get().get(Sneak.class).doPacket() && !Modules.get().get(NoSlow.class).airStrict()
         ? original
         : new Input(original.forward(), original.backward(), original.left(), original.right(), original.jump(), true, original.sprint());
   }

   @Inject(method = "aiStep", at = @At("HEAD"))
   private void preAiStep(CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(PlayerTickMovementEvent.get());
   }

   @ModifyReturnValue(method = "getJumpRidingScale", at = @At("RETURN"))
   private float modifyJumpRidingScale(float original) {
      return Modules.get().get(EntityControl.class).maxJump() ? 1.0F : original;
   }

   @Inject(method = "jumpableVehicle", at = @At("RETURN"), cancellable = true)
   private void changeJumpableVehicle(CallbackInfoReturnable<PlayerRideableJumping> cir) {
      if (Modules.get().get(EntityControl.class).cancelJump()) {
         cir.setReturnValue(null);
      }
   }

   @ModifyReturnValue(method = "pick", at = @At("RETURN"))
   private static HitResult onUpdateTargetedEntity(HitResult original, @Local(name = "blockHitResult") HitResult blockHitResult) {
      if (original instanceof EntityHitResult ehr) {
         if (Modules.get().get(NoMiningTrace.class).canWork(ehr.getEntity()) && blockHitResult.getType() == Type.BLOCK) {
            return blockHitResult;
         }

         if (ehr.getEntity() instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) {
            return blockHitResult;
         }
      }

      return original;
   }

   @ModifyExpressionValue(
      method = "pick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;")
   )
   private static HitResult modifyPick(
      HitResult original,
      @Local(argsOnly = true, name = "cameraEntity") Entity cameraEntity,
      @Local(argsOnly = true, name = "partialTicks") float partialTicks,
      @Local(name = "maxDistance") double maxDistance
   ) {
      if (!Modules.get().isActive(LiquidInteract.class)) {
         return original;
      } else {
         return original.getType() != Type.MISS ? original : cameraEntity.pick(maxDistance, partialTicks, true);
      }
   }

   @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
   private boolean modifyIsWalking(boolean original) {
      if (!Modules.get().get(Sprint.class).rageSprint()) {
         return original;
      }

      float forwards = Math.abs(this.zza);
      float sideways = Math.abs(this.xxa);
      return this.isUnderWater() ? forwards > 1.0E-5F || sideways > 1.0E-5F : forwards > 0.8 || sideways > 0.8;
   }

   @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"))
   private boolean modifyAiStep(boolean original) {
      return !Modules.get().get(Sprint.class).rageSprint() ? original : Math.abs(this.xxa) > 1.0E-5F || Math.abs(this.zza) > 1.0E-5F;
   }

   @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 3))
   private boolean wrapSetSprinting(LocalPlayer instance, boolean b) {
      Sprint s = Modules.get().get(Sprint.class);
      return !s.rageSprint() || s.unsprintInWater() && this.isInWater();
   }

   @Inject(method = "sendPosition", at = @At("HEAD"))
   private void onSendPositionHead(CallbackInfo ci) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      this.meteor$sendMovementPacketsEvent = SendMovementPacketsEvent.Pre.get(
         player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround()
      );
      MeteorClient.EVENT_BUS.post(this.meteor$sendMovementPacketsEvent);
      this.meteor$sendMovementPacketsEvent.updateUnmodified(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround());
   }

   @Inject(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V", ordinal = 1)
   )
   private void onTickHasVehicleBeforeSendPackets(CallbackInfo ci) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      SendMovementPacketsEvent.Pre event = SendMovementPacketsEvent.Pre.get(
         player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround()
      );
      MeteorClient.EVENT_BUS.post(event);
      event.updateUnmodified(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.onGround());
   }

   @Inject(method = "sendPosition", at = @At("TAIL"))
   private void onSendPositionTail(CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
      this.meteor$sendMovementPacketsEvent = null;
   }

   @Inject(
      method = "tick",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V",
         ordinal = 1,
         shift = Shift.AFTER
      )
   )
   private void onTickHasVehicleAfterSendPackets(CallbackInfo ci) {
      MeteorClient.EVENT_BUS.post(SendMovementPacketsEvent.Post.get());
   }

   @WrapOperation(
      method = "sendPosition",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;position()Lnet/minecraft/world/phys/Vec3;")
   )
   private Vec3 redirectPosition(LocalPlayer instance, Operation<Vec3> original) {
      return this.meteor$sendMovementPacketsEvent == null
         ? (Vec3)original.call(new Object[]{instance})
         : new Vec3(this.meteor$sendMovementPacketsEvent.getX(), this.meteor$sendMovementPacketsEvent.getY(), this.meteor$sendMovementPacketsEvent.getZ());
   }

   @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
   private double redirectGetX(LocalPlayer instance, Operation<Double> original) {
      return this.meteor$sendMovementPacketsEvent != null ? this.meteor$sendMovementPacketsEvent.getX() : (Double)original.call(new Object[]{instance});
   }

   @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
   private double redirectGetY(LocalPlayer instance, Operation<Double> original) {
      return this.meteor$sendMovementPacketsEvent != null ? this.meteor$sendMovementPacketsEvent.getY() : (Double)original.call(new Object[]{instance});
   }

   @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
   private double redirectGetZ(LocalPlayer instance, Operation<Double> original) {
      return this.meteor$sendMovementPacketsEvent != null ? this.meteor$sendMovementPacketsEvent.getZ() : (Double)original.call(new Object[]{instance});
   }

   @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
   private float redirectGetYRot(LocalPlayer instance, Operation<Float> original) {
      return this.meteor$sendMovementPacketsEvent != null ? this.meteor$sendMovementPacketsEvent.getYaw() : (Float)original.call(new Object[]{instance});
   }

   @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
   private float redirectGetXRot(LocalPlayer instance, Operation<Float> original) {
      return this.meteor$sendMovementPacketsEvent != null ? this.meteor$sendMovementPacketsEvent.getPitch() : (Float)original.call(new Object[]{instance});
   }

   @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;onGround()Z"))
   private boolean redirectOnGround(LocalPlayer instance, Operation<Boolean> original) {
      return this.meteor$sendMovementPacketsEvent != null ? this.meteor$sendMovementPacketsEvent.isOnGround() : (Boolean)original.call(new Object[]{instance});
   }
}
