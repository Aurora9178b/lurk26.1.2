package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.RotationAnimationEvent;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.lwjgl.opengl.GL11C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private LivingEntity meteor$rotationEntity;
   @Unique
   private float meteor$originalYaw;
   @Unique
   private float meteor$originalLastYaw;
   @Unique
   private float meteor$originalHeadYaw;
   @Unique
   private float meteor$originalLastHeadYaw;
   @Unique
   private float meteor$originalBodyYaw;
   @Unique
   private float meteor$originalLastBodyYaw;
   @Unique
   private float meteor$originalPitch;
   @Unique
   private float meteor$originalLastPitch;
   @Unique
   private Chams chams;

   @ModifyExpressionValue(
      method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;")
   )
   private Entity hasLabelGetCameraEntityProxy(Entity cameraEntity) {
      return Modules.get().isActive(Freecam.class) ? null : cameraEntity;
   }

   @ModifyExpressionValue(
      method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getTeam()Lnet/minecraft/world/scores/PlayerTeam;")
   )
   private PlayerTeam hasLabelClientPlayerEntityGetScoreboardTeamProxy(PlayerTeam team) {
      return MeteorClient.mc.player == null ? null : team;
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("HEAD")
   )
   private void meteor$applyRotationAnimation(T entity, S state, float partialTicks, CallbackInfo ci) {
      if (entity == MeteorClient.mc.player) {
         RotationAnimationEvent bodyEvent = (RotationAnimationEvent)MeteorClient.EVENT_BUS
            .post(RotationAnimationEvent.get(RotationAnimationEvent.Part.Body, entity.yBodyRot, entity.yBodyRotO, 0.0F, 0.0F));
         boolean bodyModified = bodyEvent.isModified();
         float bodyYaw = bodyEvent.getYaw();
         float lastBodyYaw = bodyEvent.getLastYaw();
         RotationAnimationEvent headEvent = (RotationAnimationEvent)MeteorClient.EVENT_BUS
            .post(RotationAnimationEvent.get(RotationAnimationEvent.Part.Head, entity.yHeadRot, entity.yHeadRotO, 0.0F, 0.0F));
         boolean headModified = headEvent.isModified();
         float headYaw = headEvent.getYaw();
         float lastHeadYaw = headEvent.getLastYaw();
         RotationAnimationEvent pitchEvent = (RotationAnimationEvent)MeteorClient.EVENT_BUS
            .post(RotationAnimationEvent.get(RotationAnimationEvent.Part.Pitch, 0.0F, 0.0F, entity.getXRot(), entity.xRotO));
         boolean pitchModified = pitchEvent.isModified();
         float pitch = pitchEvent.getPitch();
         float lastPitch = pitchEvent.getLastPitch();
         if (bodyModified || headModified || pitchModified) {
            this.meteor$rotationEntity = entity;
            this.meteor$originalYaw = entity.getYRot();
            this.meteor$originalLastYaw = entity.yRotO;
            this.meteor$originalHeadYaw = entity.yHeadRot;
            this.meteor$originalLastHeadYaw = entity.yHeadRotO;
            this.meteor$originalBodyYaw = entity.yBodyRot;
            this.meteor$originalLastBodyYaw = entity.yBodyRotO;
            this.meteor$originalPitch = entity.getXRot();
            this.meteor$originalLastPitch = entity.xRotO;
            if (bodyModified) {
               entity.setYRot(bodyYaw);
               entity.yRotO = lastBodyYaw;
               entity.yBodyRot = bodyYaw;
               entity.yBodyRotO = lastBodyYaw;
            }

            if (headModified) {
               entity.yHeadRot = headYaw;
               entity.yHeadRotO = lastHeadYaw;
            }

            if (pitchModified) {
               entity.setXRot(pitch);
               entity.xRotO = lastPitch;
            }
         }
      }
   }

   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void meteor$restoreRotationAnimation(T entity, S state, float partialTicks, CallbackInfo ci) {
      if (this.meteor$rotationEntity == entity) {
         entity.setYRot(this.meteor$originalYaw);
         entity.yRotO = this.meteor$originalLastYaw;
         entity.yHeadRot = this.meteor$originalHeadYaw;
         entity.yHeadRotO = this.meteor$originalLastHeadYaw;
         entity.yBodyRot = this.meteor$originalBodyYaw;
         entity.yBodyRotO = this.meteor$originalLastBodyYaw;
         entity.setXRot(this.meteor$originalPitch);
         entity.xRotO = this.meteor$originalLastPitch;
         this.meteor$rotationEntity = null;
      }
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   private void init$chams(CallbackInfo ci) {
      this.chams = Modules.get().get(Chams.class);
   }

   @WrapWithCondition(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
      )
   )
   private <TState> boolean render$render(
      SubmitNodeCollector instance,
      Model<? super TState> model,
      TState state,
      PoseStack matrixStack,
      RenderType renderLayer,
      int light,
      int overlay,
      int mixColor,
      TextureAtlasSprite sprite,
      int outlineColor,
      CrumblingOverlay crumblingOverlayCommand
   ) {
      if (!(this.chams.isActive() && this.chams.players.get() && ((IEntityRenderState)state).meteor$getEntity() instanceof Player player)) {
         return true;
      }

      if (this.chams.ignoreSelf.get() && player == MeteorClient.mc.player) {
         return true;
      }

      instance.submitModel(
         model,
         state,
         matrixStack,
         renderLayer,
         light,
         overlay,
         PlayerUtils.getPlayerColor(player, this.chams.playersColor.get()).getPacked(),
         sprite,
         outlineColor,
         null
      );
      return false;
   }

   @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
   private RenderType getRenderPlayer(RenderType original, S state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
      if (!(this.chams.isActive() && ((IEntityRenderState)state).meteor$getEntity() instanceof Player player)) {
         return original;
      } else if (!this.chams.players.get() || this.chams.playersTexture.get()) {
         return original;
      } else {
         return this.chams.ignoreSelf.get() && player == MeteorClient.mc.player ? original : RenderTypes.itemTranslucent(Chams.BLANK);
      }
   }

   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void render$Head(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
      Entity entity = ((IEntityRenderState)state).meteor$getEntity();
      if (entity instanceof LivingEntity livingEntity) {
         if (Modules.get().get(NoRender.class).noDeadEntities() && livingEntity.isDeadOrDying()) {
            ci.cancel();
         }

         if (this.chams.shouldRender(entity)) {
            GL11C.glEnable(32823);
            GL11C.glPolygonOffset(1.0F, -1100000.0F);
         }
      }
   }

   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At("TAIL")
   )
   private void render$Tail(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
      if (((IEntityRenderState)state).meteor$getEntity() instanceof LivingEntity livingEntity) {
         if (this.chams.shouldRender(livingEntity)) {
            GL11C.glPolygonOffset(1.0F, 1100000.0F);
            GL11C.glDisable(32823);
         }
      }
   }
}
