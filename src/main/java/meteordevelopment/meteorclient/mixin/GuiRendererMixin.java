package meteordevelopment.meteorclient.mixin;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.ButtonRenderQueue;
import meteordevelopment.meteorclient.utils.render.ChatBackgroundRenderer;
import meteordevelopment.meteorclient.utils.render.GlobalBackgroundRenderer;
import meteordevelopment.meteorclient.utils.render.MeteorMcGuiRenderer;
import meteordevelopment.meteorclient.utils.render.ReGlassBackgroundQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogRenderer.FogMode;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
   @Shadow
   @Final
   private GuiRenderState renderState;
   @Unique
   private GuiRenderState meteor$renderState;
   @Unique
   private MeteorMcGuiRenderer meteor$guiRenderer;

   @Inject(method = "<init>", at = @At("RETURN"))
   private void init$meteor(
      GuiRenderState renderState,
      BufferSource bufferSource,
      SubmitNodeCollector submitNodeCollector,
      FeatureRenderDispatcher featureRenderDispatcher,
      List<PictureInPictureRenderer<?>> pictureInPictureRenderers,
      CallbackInfo ci
   ) {
      if (!((GuiRenderer)(Object)(Object)this instanceof MeteorMcGuiRenderer)) {
         this.meteor$renderState = new GuiRenderState();
         this.meteor$guiRenderer = new MeteorMcGuiRenderer(
            this.meteor$renderState, bufferSource, submitNodeCollector, featureRenderDispatcher, pictureInPictureRenderers
         );
      }
   }

   @Inject(method = "render", at = @At("HEAD"))
   private void render$preGui(CallbackInfo ci) {
      if (!((GuiRenderer)(Object)(Object)this instanceof MeteorMcGuiRenderer)) {
         Minecraft mc = Minecraft.getInstance();
         GlobalBackgroundRenderer.renderPending();
         ChatBackgroundRenderer.renderPending();
         this.meteor$renderReGlassBackgrounds(mc);
         this.meteor$renderQueuedButtons(mc);
         if (mc.screen != null && !(mc.screen instanceof WidgetScreen)) {
            this.meteor$render2D(mc);
         }
      }
   }

   @Inject(method = "render", at = @At("TAIL"))
   private void render$postGui(CallbackInfo ci) {
      if (!((GuiRenderer)(Object)(Object)this instanceof MeteorMcGuiRenderer)) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.screen == null || mc.screen instanceof WidgetScreen) {
            this.meteor$render2D(mc);
         }

         this.meteor$guiRenderer.endFrame();
      }
   }

   @Unique
   private void meteor$renderReGlassBackgrounds(Minecraft mc) {
      if (!ReGlassBackgroundQueue.isEmpty()) {
         int mouseX = (int)mc.mouseHandler.getScaledXPos(mc.getWindow());
         int mouseY = (int)mc.mouseHandler.getScaledYPos(mc.getWindow());
         GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, this.renderState, mouseX, mouseY);
         ReGlassBackgroundQueue.flush(graphics);
      }
   }

   @Unique
   private void meteor$renderQueuedButtons(Minecraft mc) {
      if (!ButtonRenderQueue.isEmpty()) {
         int mouseX = (int)mc.mouseHandler.getScaledXPos(mc.getWindow());
         int mouseY = (int)mc.mouseHandler.getScaledYPos(mc.getWindow());
         GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, this.renderState, mouseX, mouseY);
         ButtonRenderQueue.flush(graphics);
      }
   }

   @Unique
   private void meteor$render2D(Minecraft mc) {
      int mouseX = (int)mc.mouseHandler.getScaledXPos(mc.getWindow());
      int mouseY = (int)mc.mouseHandler.getScaledYPos(mc.getWindow());
      FogRenderer fogRenderer = ((GameRendererAccessor)mc.gameRenderer).meteor$fogRenderer();
      if (Utils.canUpdate() || HudEditorScreen.isOpen()) {
         Profiler.get().push("meteor-client_render_2d");
         Utils.unscaledProjection();
         GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, this.meteor$renderState, mouseX, mouseY);
         float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         MeteorClient.EVENT_BUS.post(Render2DEvent.get(graphics, graphics.guiWidth(), graphics.guiHeight(), tickDelta));
         this.meteor$guiRenderer.render(fogRenderer.getBuffer(FogMode.NONE));
         Utils.scaledProjection();
         Profiler.get().pop();
      }

      if (mc.screen instanceof WidgetScreen widgetScreen) {
         GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, this.meteor$renderState, mouseX, mouseY);
         float guiDelta = mc.getDeltaTracker().getGameTimeDeltaTicks();
         widgetScreen.renderCustom(graphics, mouseX, mouseY, guiDelta);
         this.meteor$guiRenderer.render(fogRenderer.getBuffer(FogMode.NONE));
      }
   }
}
