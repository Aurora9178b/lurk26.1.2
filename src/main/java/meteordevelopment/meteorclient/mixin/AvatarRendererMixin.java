package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.HandsViewPlus;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
   @Unique
   private Chams chams;

   public AvatarRendererMixin(Context ctx, PlayerModel model, float shadowRadius) {
      super(ctx, model, shadowRadius);
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   private void init$chams(CallbackInfo ci) {
      this.chams = Modules.get().get(Chams.class);
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("RETURN")
   )
   private void updateRenderState$scale(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
      if (this.chams.isActive() && this.chams.players.get()) {
         if (!this.chams.ignoreSelf.get() || entity != MeteorClient.mc.player) {
            float v = this.chams.playersScale.get().floatValue();
            state.scale *= v;
            if (state.nameTagAttachment != null) {
               ((IVec3)state.nameTagAttachment).meteor$setY(state.nameTagAttachment.y + (entity.getBbHeight() * v - entity.getBbHeight()));
            }
         }
      }
   }

   @ModifyExpressionValue(
      method = "renderHand",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityTranslucent(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"
      )
   )
   private RenderType renderArm$texture(
      RenderType original,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int lightCoords,
      Identifier skinTexture,
      ModelPart arm,
      boolean hasSleeve
   ) {
      if (this.chams.isActive() && this.chams.hand.get()) {
         Identifier texture = this.chams.handTexture.get() ? skinTexture : Chams.BLANK;
         return RenderTypes.entityTranslucent(texture);
      } else {
         return original;
      }
   }

   @WrapWithCondition(
      method = "renderHand",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
      )
   )
   private boolean renderArm$color(
      SubmitNodeCollector instance, ModelPart modelPart, PoseStack matrixStack, RenderType renderLayer, int light, int uv, TextureAtlasSprite sprite
   ) {
      if (this.chams.isActive() && this.chams.hand.get()) {
         instance.submitModelPart(modelPart, matrixStack, renderLayer, light, uv, null, this.chams.handColor.get().getPacked(), null);
         return false;
      } else {
         return true;
      }
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("RETURN")
   )
   private void extractRenderState$handsViewPlusBlocking(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
      HandsViewPlus handsViewPlus = Modules.get().get(HandsViewPlus.class);
      if (handsViewPlus != null) {
         if (handsViewPlus.shouldApplyThirdPersonBlockingAnimation(entity, HumanoidArm.RIGHT)) {
            state.rightArmPose = ArmPose.BLOCK;
         }

         if (handsViewPlus.shouldApplyThirdPersonBlockingAnimation(entity, HumanoidArm.LEFT)) {
            state.leftArmPose = ArmPose.BLOCK;
         }
      }
   }
}
