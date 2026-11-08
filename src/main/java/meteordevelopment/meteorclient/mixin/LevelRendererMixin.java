package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Function;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixininterface.IEntityRenderState;
import meteordevelopment.meteorclient.mixininterface.ILevelRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BlockSelection;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.systems.modules.render.ESP;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.Ambience;
import meteordevelopment.meteorclient.utils.OutlineRenderCommandQueue;
import meteordevelopment.meteorclient.utils.render.NoopImmediateVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.NoopOutlineVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.WrapperImmediateVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.postprocess.EntityShader;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.client.renderer.state.level.WorldBorderRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ILevelRenderer {
   @Unique
   private NoRender noRender;
   @Unique
   private ESP esp;
   @Unique
   private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();
   @Unique
   private MultiBufferSource provider;
   @Unique
   private FeatureRenderDispatcher renderDispatcher;
   @Shadow
   private RenderTarget entityOutlineTarget;
   @Shadow
   @Final
   private LevelTargetBundle targets;
   @Shadow
   @Final
   private EntityRenderDispatcher entityRenderDispatcher;
   @Shadow
   @Final
   private LevelRenderState levelRenderState;
   @Unique
   private Stack<RenderTarget> framebufferStack;
   @Unique
   private Stack<ResourceHandle<RenderTarget>> framebufferHandleStack;

   @Inject(method = "setLevel", at = @At("TAIL"))
   private void onSetLevel(ClientLevel level, CallbackInfo ci) {
      this.esp = Modules.get().get(ESP.class);
      this.noRender = Modules.get().get(NoRender.class);
   }

   @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true)
   private void onCheckPoseStack(PoseStack poseStack, CallbackInfo ci) {
      ci.cancel();
   }

   @Inject(method = "renderHitOutline", at = @At("HEAD"), cancellable = true)
   private void onDrawHighlightedHitOutline(
      PoseStack poseStack,
      VertexConsumer builder,
      double camX,
      double camY,
      double camZ,
      BlockOutlineRenderState state,
      int color,
      float width,
      CallbackInfo ci
   ) {
      if (Modules.get().isActive(BlockSelection.class)) {
         ci.cancel();
      }
   }

   @ModifyArg(
      method = "update",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V"
      )
   )
   private boolean update$cullTerraion$modifySpectator(boolean spectator) {
      return Modules.get().isActive(Freecam.class) || spectator;
   }

   @WrapWithCondition(
      method = "extractLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/WeatherEffectRenderer;extractRenderState(Lnet/minecraft/world/level/Level;IFLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;)V"
      )
   )
   private boolean extractLevel$noWeather(
      WeatherEffectRenderer instance, Level level, int ticks, float partialTicks, Vec3 cameraPos, WeatherRenderState renderState
   ) {
      if (this.noRender.noWeather()) {
         renderState.intensity = 0.0F;
         return false;
      } else {
         return true;
      }
   }

   @WrapWithCondition(
      method = "extractLevel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/WorldBorderRenderer;extract(Lnet/minecraft/world/level/border/WorldBorder;FLnet/minecraft/world/phys/Vec3;DLnet/minecraft/client/renderer/state/level/WorldBorderRenderState;)V"
      )
   )
   private boolean extractLevel$noWorldBorder(
      WorldBorderRenderer instance, WorldBorder border, float deltaPartialTick, Vec3 cameraPos, double renderDistance, WorldBorderRenderState state
   ) {
      if (this.noRender.noWorldBorder()) {
         state.alpha = 0.0;
         return false;
      } else {
         return true;
      }
   }

   @ModifyExpressionValue(
      method = "addSkyPass",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;doesMobEffectBlockSky:Z", opcode = 180)
   )
   private boolean modifyMobEffectBlocksSky(boolean original) {
      return !this.noRender.noBlindness() && !this.noRender.noDarkness() ? original : false;
   }

   @Inject(method = "renderLevel", at = @At("HEAD"))
   private void onRenderLevelHead(
      GraphicsResourceAllocator resourceAllocator,
      DeltaTracker deltaTracker,
      boolean renderOutline,
      CameraRenderState cameraState,
      Matrix4fc modelViewMatrix,
      GpuBufferSlice terrainFog,
      Vector4f fogColor,
      boolean shouldRenderSky,
      ChunkSectionsToRender chunkSectionsToRender,
      CallbackInfo ci
   ) {
      PostProcessShaders.beginRender();
   }

   @Inject(method = "submitEntities", at = @At("TAIL"))
   private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
      if (this.renderDispatcher == null) {
         this.renderDispatcher = new FeatureRenderDispatcher(
            this.outlineRenderCommandQueue,
            MeteorClient.mc.getModelManager(),
            new WrapperImmediateVertexConsumerProvider(() -> this.provider),
            MeteorClient.mc.getAtlasManager(),
            NoopOutlineVertexConsumerProvider.INSTANCE,
            NoopImmediateVertexConsumerProvider.INSTANCE,
            MeteorClient.mc.font,
            MeteorClient.mc.gameRenderer.getGameRenderState()
         );
      }

      this.draw(levelRenderState, poseStack, PostProcessShaders.CHAMS, var0 -> Color.WHITE);
      this.draw(levelRenderState, poseStack, PostProcessShaders.ENTITY_OUTLINE, entity -> this.esp.getColor(entity));
   }

   @Unique
   private void draw(LevelRenderState worldState, PoseStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
      Vec3 camera = worldState.cameraRenderState.pos;
      boolean empty = true;

      for (EntityRenderState state : worldState.entityRenderStates) {
         Entity entity = ((IEntityRenderState)state).meteor$getEntity();
         if (entity != null && shader.shouldDraw(entity)) {
            Color color = colorGetter.apply(entity);
            if (color != null) {
               this.outlineRenderCommandQueue.setColor(color);
               EntityRenderer renderer = (EntityRenderer)this.entityRenderDispatcher.getRenderer(state);
               Vec3 offset = renderer.getRenderOffset(state);
               matrices.pushPose();
               matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
               renderer.submit(state, matrices, this.outlineRenderCommandQueue, worldState.cameraRenderState);
               matrices.popPose();
               empty = false;
            }
         }
      }

      if (!empty) {
         this.meteor$pushEntityOutlineFramebuffer(shader.framebuffer);
         this.provider = shader.vertexConsumerProvider;
         this.renderDispatcher.renderAllFeatures();
         this.outlineRenderCommandQueue.endFrame();
         this.provider = null;
         this.meteor$popEntityOutlineFramebuffer();
      }
   }

   @ModifyExpressionValue(
      method = "extractVisibleEntities",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z")
   )
   boolean fillEntityRenderStatesIsRenderingReady(boolean original) {
      return this.esp.forceRender() ? true : original;
   }

   @Inject(
      method = "lambda$addMainPass$0",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V", shift = Shift.AFTER)
   )
   private void addMainPass$submitEntityVertices(CallbackInfo ci) {
      PostProcessShaders.submitEntityVertices();
   }

   @Inject(method = "resize", at = @At("HEAD"))
   private void onResize(int width, int height, CallbackInfo ci) {
      PostProcessShaders.onResized(width, height);
   }

   @Inject(
      method = "extractLevel",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/LevelRenderState;cloudColor:I", opcode = 181, shift = Shift.AFTER)
   )
   private void extractLevel$cloudColor(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
      Ambience ambience = Modules.get().get(Ambience.class);
      if (ambience.isActive() && ambience.customCloudColor.get()) {
         this.levelRenderState.cloudColor = ambience.cloudColor.get().getPacked();
      }
   }

   @Inject(method = "extractBlockDestroyAnimation", at = @At("HEAD"), cancellable = true)
   private void onExtractBlockDestroyAnimation(CallbackInfo ci) {
      if (Modules.get().isActive(BreakIndicators.class) || Modules.get().get(NoRender.class).noBlockBreakOverlay()) {
         ci.cancel();
      }
   }

   @Inject(method = "<init>", at = @At("TAIL"))
   private void init$IWorldRenderer(CallbackInfo ci) {
      this.framebufferStack = new ObjectArrayList<>();
      this.framebufferHandleStack = new ObjectArrayList<>();
   }

   @Override
   public void meteor$pushEntityOutlineFramebuffer(RenderTarget framebuffer) {
      this.framebufferStack.push(this.entityOutlineTarget);
      this.entityOutlineTarget = framebuffer;
      this.framebufferHandleStack.push(this.targets.entityOutline);
      this.targets.entityOutline = () -> framebuffer;
   }

   @Override
   public void meteor$popEntityOutlineFramebuffer() {
      this.entityOutlineTarget = this.framebufferStack.pop();
      this.targets.entityOutline = this.framebufferHandleStack.pop();
   }
}
