package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndCrystalRenderer.class)
public abstract class EndCrystalRendererMixin {
   @Unique
   private Chams chams;
   @Shadow
   @Final
   private static Identifier END_CRYSTAL_LOCATION;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void onInit(CallbackInfo ci) {
      this.chams = Modules.get().get(Chams.class);
   }

   @Inject(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V")
   )
   private void render$scale(
      EndCrystalRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci
   ) {
      if (this.chams.isActive() && this.chams.crystals.get()) {
         float v = this.chams.crystalsScale.get().floatValue();
         poseStack.scale(v, v, v);
      }
   }

   @WrapWithCondition(
      method = "submit(Lnet/minecraft/client/renderer/entity/state/EndCrystalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"
      )
   )
   private <S> boolean render$color(
      SubmitNodeCollector instance,
      Model<? super S> model,
      S state,
      PoseStack poseStack,
      Identifier identifier,
      int lightCoords,
      int overlayCoords,
      int outlineColor,
      CrumblingOverlay crumblingOverlay
   ) {
      if (this.chams.isActive() && this.chams.crystals.get()) {
         RenderType renderType = RenderTypes.entityTranslucent(
            this.chams.isActive() && this.chams.crystals.get() && !this.chams.crystalsTexture.get() ? Chams.BLANK : END_CRYSTAL_LOCATION
         );
         instance.submitModel(
            model, state, poseStack, renderType, lightCoords, overlayCoords, this.chams.crystalsColor.get().getPacked(), null, outlineColor, null
         );
         return false;
      } else {
         return true;
      }
   }
}
