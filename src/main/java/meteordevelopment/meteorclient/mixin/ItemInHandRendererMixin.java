package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import meteordevelopment.meteorclient.systems.modules.render.HandsViewPlus;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
   @Unique
   private boolean meteor$handsViewPlusBlocking;
   @Unique
   private boolean meteor$handsViewPlusBlockingApplied;
   @Shadow
   private float mainHandHeight;
   @Shadow
   private float offHandHeight;
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   private ItemStack offHandItem;

   @Shadow
   protected abstract boolean shouldInstantlyReplaceVisibleItem(ItemStack var1, ItemStack var2);

   @Shadow
   protected abstract void applyItemArmAttackTransform(PoseStack var1, HumanoidArm var2, float var3);

   @ModifyExpressionValue(
      method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getAttackAnim(F)F")
   )
   private float modifySwing(float attackValue) {
      HandView module = Modules.get().get(HandView.class);
      InteractionHand hand = Objects.requireNonNullElse(MeteorClient.mc.player.swingingArm, InteractionHand.MAIN_HAND);
      ItemStack displayMainHand = SlotHelper.INSTANCE.getMainHandStack(MeteorClient.mc.player.getMainHandItem());
      if (module.isActive()) {
         if (module.swordSlash() && hand == InteractionHand.MAIN_HAND && displayMainHand.is(ItemTags.SWORDS)) {
            return 0.0F;
         }

         if (hand == InteractionHand.OFF_HAND && !this.offHandItem.isEmpty()) {
            return attackValue + module.offSwing.get().floatValue();
         }

         if (hand == InteractionHand.MAIN_HAND && !displayMainHand.isEmpty()) {
            return attackValue + module.mainSwing.get().floatValue();
         }
      }

      return attackValue;
   }

   @ModifyReturnValue(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"))
   private boolean modifySkipSwapAnimation(boolean original) {
      return original || Modules.get().get(HandView.class).skipSwapping();
   }

   @ModifyExpressionValue(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack useSilentSlotMainHand(ItemStack original) {
      return SlotHelper.INSTANCE.getMainHandStack(original);
   }

   @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 2), index = 0)
   private float modifyEquipProgressMainhand(float value) {
      HandView handView = Modules.get().get(HandView.class);
      ItemStack displayMainHand = SlotHelper.INSTANCE.getMainHandStack(MeteorClient.mc.player.getMainHandItem());
      if (handView.swordSlash() && displayMainHand.is(ItemTags.SWORDS)) {
         return value;
      }

      float f = MeteorClient.mc.player.getItemSwapScale(1.0F);
      float modified = handView.oldAnimations() ? 1.0F : f * f * f;
      return (this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, displayMainHand) ? modified : 0.0F) - this.mainHandHeight;
   }

   @ModifyArg(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F", ordinal = 3), index = 0)
   private float modifyEquipProgressOffhand(float value) {
      return (this.shouldInstantlyReplaceVisibleItem(this.offHandItem, MeteorClient.mc.player.getOffhandItem()) ? 1 : 0) - this.offHandHeight;
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void hideHandsViewPlusSwapAnimation(CallbackInfo ci) {
      HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
      if (handsViewPlus != null && MeteorClient.mc.player != null) {
         ItemStack displayMainHand = SlotHelper.INSTANCE.getMainHandStack(MeteorClient.mc.player.getMainHandItem());
         if (handsViewPlus.shouldDisableSwapMain()) {
            this.mainHandHeight = 1.0F;
            this.mainHandItem = displayMainHand;
         }

         if (handsViewPlus.shouldDisableSwapOff()) {
            this.offHandHeight = 1.0F;
            this.offHandItem = MeteorClient.mc.player.getOffhandItem();
         }
      }
   }

   @Inject(method = "renderArmWithItem", at = @At("HEAD"))
   private void cacheHandsViewPlusBlockingState(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
      this.meteor$handsViewPlusBlocking = handsViewPlus != null && handsViewPlus.shouldApplyBlockingAnimation(hand, itemStack);
      this.meteor$handsViewPlusBlockingApplied = false;
   }

   @Inject(method = "renderArmWithItem", at = @At("RETURN"))
   private void clearHandsViewPlusBlockingState(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      this.meteor$handsViewPlusBlocking = false;
      this.meteor$handsViewPlusBlockingApplied = false;
   }

   @Inject(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
         ordinal = 2,
         shift = Shift.AFTER
      )
   )
   private void addHandsViewPlusSwingToUsing(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
      if (handsViewPlus != null && handsViewPlus.shouldVisualSwingOnUse() && attack > 0.0F) {
         HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
         this.applyItemArmAttackTransform(poseStack, arm, attack);
      }
   }

   @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
   private void cancelHandsViewPlusSwingForBlocking(float attack, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
      if (this.meteor$handsViewPlusBlocking) {
         ci.cancel();
      }
   }

   @Inject(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
         shift = Shift.BEFORE
      )
   )
   private void applyHandsViewPlusBlockingAnimation(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      if (this.meteor$handsViewPlusBlocking && !this.meteor$handsViewPlusBlockingApplied) {
         HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
         if (handsViewPlus != null) {
            HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
            handsViewPlus.applyBlockingTransform(poseStack, arm, attack);
            this.meteor$handsViewPlusBlockingApplied = true;
         }
      }
   }

   @Inject(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
         shift = Shift.BEFORE
      )
   )
   private void onRenderItem(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      MeteorClient.EVENT_BUS.post(HeldItemRendererEvent.get(hand, poseStack));
   }

   @Inject(
      method = "renderArmWithItem",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;)V"
      )
   )
   private void onRenderArm(
      AbstractClientPlayer player,
      float frameInterp,
      float xRot,
      InteractionHand hand,
      float attack,
      ItemStack itemStack,
      float inverseArmHeight,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      CallbackInfo ci
   ) {
      MeteorClient.EVENT_BUS.post(ArmRenderEvent.get(hand, poseStack));
   }

   @Inject(method = "applyEatTransform", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D", shift = Shift.BEFORE), cancellable = true)
   private void cancelTransformations(PoseStack poseStack, float frameInterp, HumanoidArm arm, ItemStack itemStack, Player player, CallbackInfo ci) {
      if (Modules.get().get(HandView.class).disableFoodAnimation()) {
         ci.cancel();
      }
   }

   @WrapOperation(method = "swingArm", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
   private void wrapHandsViewPlusSwingArmTranslate(PoseStack poseStack, float x, float y, float z, Operation<Void> original) {
      HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
      if (handsViewPlus == null
         || MeteorClient.mc.player == null
         || !handsViewPlus.shouldApplyFluxSwing(SlotHelper.INSTANCE.getMainHandStack(MeteorClient.mc.player.getMainHandItem()))) {
         original.call(new Object[]{poseStack, x, y, z});
      }
   }
}
