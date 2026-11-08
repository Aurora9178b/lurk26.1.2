package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform;

public class LiquidGlassUtil {
   private static final Color DEFAULT_TINT = new Color(255, 255, 255, 22);
   private static RenderTarget snapshot;
   private static RenderTarget blurredSnapshot;
   private static boolean snapshotCaptured;
   private static boolean blurPrepared;
   private static float preparedBlurStrength = Float.NaN;
   private static final int GLASS_UNIFORM_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();
   private static final int BLUR_UNIFORM_SIZE = new Std140SizeCalculator().putVec4().putVec4().get();
   private static final FixedUniformStorage<LiquidGlassUtil.GlassUniformData> GLASS_UNIFORM_STORAGE = new FixedUniformStorage<>(
      "Meteor - LiquidGlass UBO", GLASS_UNIFORM_SIZE, 128
   );
   private static final FixedUniformStorage<LiquidGlassUtil.BlurUniformData> BLUR_UNIFORM_STORAGE = new FixedUniformStorage<>(
      "Meteor - LiquidGlass Blur UBO", BLUR_UNIFORM_SIZE, 8
   );

   private LiquidGlassUtil() {
   }

   public static void beginFrame() {
      snapshotCaptured = false;
      blurPrepared = false;
      preparedBlurStrength = Float.NaN;
      GLASS_UNIFORM_STORAGE.clear();
      BLUR_UNIFORM_STORAGE.clear();
   }

   public static void beginFrame(GuiGraphicsExtractor graphics) {
      beginFrame();
   }

   public static void render(double x, double y, double width, double height, double radius) {
      render(x, y, width, height, radius, DEFAULT_TINT);
   }

   public static void render(HudRenderer renderer, double x, double y, double width, double height, double radius) {
      renderHud(
         x,
         y,
         width,
         height,
         radius,
         DEFAULT_TINT,
         LiquidGlassSetting.getBlur(),
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1(),
         true
      );
   }

   public static void renderGui(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double radius) {
      renderGui(graphics, x, y, width, height, radius, DEFAULT_TINT);
   }

   public static void renderGui(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double radius, Color tint) {
      renderGui(graphics, x, y, width, height, radius, tint, LiquidGlassSetting.getBlur());
   }

   public static void renderGui(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double radius, Color tint, float blurStrength) {
      GpuTexture sourceTexture = MeteorClient.mc.getMainRenderTarget().getColorTexture();
      if (sourceTexture != null && graphics.guiWidth() > 0 && graphics.guiHeight() > 0) {
         double scaleX = (double)sourceTexture.getWidth(0) / graphics.guiWidth();
         double scaleY = (double)sourceTexture.getHeight(0) / graphics.guiHeight();
         double radiusScale = Math.min(scaleX, scaleY);
         renderInternal(
            x * scaleX,
            y * scaleY,
            width * scaleX,
            height * scaleY,
            radius * radiusScale,
            tint,
            blurStrength,
            LiquidGlassSetting.getNoise(),
            LiquidGlassSetting.getRefraction(),
            LiquidGlassSetting.getGlow(),
            LiquidGlassSetting.getGlowBias(),
            LiquidGlassSetting.getGlowEdge0(),
            LiquidGlassSetting.getGlowEdge1(),
            true
         );
      }
   }

   public static void render(HudRenderer renderer, double x, double y, double width, double height, double radius, Color tint) {
      renderHud(
         x,
         y,
         width,
         height,
         radius,
         tint,
         LiquidGlassSetting.getBlur(),
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1(),
         true
      );
   }

   public static void render(HudRenderer renderer, double x, double y, double width, double height, double radius, Color tint, float blurStrength) {
      renderHud(
         x,
         y,
         width,
         height,
         radius,
         tint,
         blurStrength,
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1(),
         true
      );
   }

   public static void render(
      HudRenderer renderer,
      double x,
      double y,
      double width,
      double height,
      double radius,
      Color tint,
      float blurStrength,
      float noise,
      float refractionPower,
      float glowWeight,
      float glowBias,
      float glowEdge0,
      float glowEdge1
   ) {
      renderHud(x, y, width, height, radius, tint, blurStrength, noise, refractionPower, glowWeight, glowBias, glowEdge0, glowEdge1, true);
   }

   public static void render(double x, double y, double width, double height, double radius, Color tint) {
      renderHud(
         x,
         y,
         width,
         height,
         radius,
         tint,
         LiquidGlassSetting.getBlur(),
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1(),
         true
      );
   }

   public static void render(double x, double y, double width, double height, double radius, Color tint, float blurStrength) {
      renderHud(
         x,
         y,
         width,
         height,
         radius,
         tint,
         blurStrength,
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1(),
         true
      );
   }

   public static void render(
      double x,
      double y,
      double width,
      double height,
      double radius,
      Color tint,
      float blurStrength,
      float noise,
      float refractionPower,
      float glowWeight,
      float glowBias,
      float glowEdge0,
      float glowEdge1
   ) {
      renderHud(x, y, width, height, radius, tint, blurStrength, noise, refractionPower, glowWeight, glowBias, glowEdge0, glowEdge1, true);
   }

   public static void renderClear(double x, double y, double width, double height, double radius) {
      renderClear(x, y, width, height, radius, LiquidGlassSetting.getBlur());
   }

   public static void renderClear(double x, double y, double width, double height, double radius, float blurStrength) {
      renderClear(
         x,
         y,
         width,
         height,
         radius,
         blurStrength,
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1()
      );
   }

   public static void renderClear(
      double x,
      double y,
      double width,
      double height,
      double radius,
      float blurStrength,
      float noise,
      float refractionPower,
      float glowWeight,
      float glowBias,
      float glowEdge0,
      float glowEdge1
   ) {
      renderHud(x, y, width, height, radius, null, blurStrength, noise, refractionPower, glowWeight, glowBias, glowEdge0, glowEdge1, false);
   }

   public static void renderClear(HudRenderer renderer, double x, double y, double width, double height, double radius) {
      renderClear(renderer, x, y, width, height, radius, LiquidGlassSetting.getBlur());
   }

   public static void renderClear(HudRenderer renderer, double x, double y, double width, double height, double radius, float blurStrength) {
      renderHud(
         x,
         y,
         width,
         height,
         radius,
         null,
         blurStrength,
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         LiquidGlassSetting.getGlow(),
         LiquidGlassSetting.getGlowBias(),
         LiquidGlassSetting.getGlowEdge0(),
         LiquidGlassSetting.getGlowEdge1(),
         false
      );
   }

   private static void renderHud(
      double x,
      double y,
      double width,
      double height,
      double radius,
      Color tint,
      float blurStrength,
      float noise,
      float refractionPower,
      float glowWeight,
      float glowBias,
      float glowEdge0,
      float glowEdge1,
      boolean tinted
   ) {
      GpuTexture sourceTexture = MeteorClient.mc.getMainRenderTarget().getColorTexture();
      if (sourceTexture != null) {
         if (MeteorClient.mc.getWindow().getWidth() > 0 && MeteorClient.mc.getWindow().getHeight() > 0) {
            double scaleX = (double)sourceTexture.getWidth(0) / MeteorClient.mc.getWindow().getWidth();
            double scaleY = (double)sourceTexture.getHeight(0) / MeteorClient.mc.getWindow().getHeight();
            double radiusScale = Math.min(scaleX, scaleY);
            renderInternal(
               x * scaleX,
               y * scaleY,
               width * scaleX,
               height * scaleY,
               radius * radiusScale,
               tint,
               blurStrength,
               noise,
               refractionPower,
               glowWeight,
               glowBias,
               glowEdge0,
               glowEdge1,
               tinted
            );
         }
      }
   }

   private static void renderInternal(
      double x,
      double y,
      double width,
      double height,
      double radius,
      Color tint,
      float blurStrength,
      float noise,
      float refractionPower,
      float glowWeight,
      float glowBias,
      float glowEdge0,
      float glowEdge1,
      boolean tinted
   ) {
      if (LiquidGlassSetting.isEnabled() && !(width <= 0.0) && !(height <= 0.0)) {
         GpuTexture sourceTexture = MeteorClient.mc.getMainRenderTarget().getColorTexture();
         if (sourceTexture != null) {
            int sourceWidth = sourceTexture.getWidth(0);
            int sourceHeight = sourceTexture.getHeight(0);
            if (sourceWidth > 0 && sourceHeight > 0) {
               RenderTarget snapshotTarget = getSnapshotTarget(sourceWidth, sourceHeight);
               RenderTarget blurredSnapshotTarget = getBlurredSnapshotTarget(sourceWidth, sourceHeight);
               if (snapshotTarget != null && snapshotTarget.getColorTexture() != null && snapshotTarget.getColorTextureView() != null) {
                  if (blurredSnapshotTarget != null && blurredSnapshotTarget.getColorTexture() != null && blurredSnapshotTarget.getColorTextureView() != null) {
                     captureSnapshot(sourceTexture, snapshotTarget, sourceWidth, sourceHeight);
                     prepareBlur(sourceWidth, sourceHeight, blurStrength);
                     if (blurredSnapshotTarget.getColorTextureView() != null) {
                        float cx = (float)(x + width / 2.0);
                        float cy = (float)(y + height / 2.0);
                        float hw = (float)(width / 2.0);
                        float hh = (float)(height / 2.0);
                        float sr = (float)Math.max(0.0, radius);
                        Color safeTint = tint == null ? DEFAULT_TINT : tint;
                        GpuBufferSlice glassData = GLASS_UNIFORM_STORAGE.write(
                           new LiquidGlassUtil.GlassUniformData(
                              sourceWidth,
                              sourceHeight,
                              1.0F / sourceWidth,
                              1.0F / sourceHeight,
                              cx,
                              cy,
                              hw,
                              hh,
                              sr,
                              sr,
                              sr,
                              sr,
                              Math.max(0.0F, noise),
                              refractionPower,
                              glowWeight,
                              glowBias,
                              glowEdge0,
                              glowEdge1,
                              0.0F,
                              0.0F,
                              0.0F,
                              0.0F,
                              0.0F,
                              0.0F,
                              safeTint.r / 255.0F,
                              safeTint.g / 255.0F,
                              safeTint.b / 255.0F,
                              safeTint.a / 255.0F
                           )
                        );
                        LiquidGlassUtil.ScissorRect scissor = toScissor(x, y, width, height);
                        if (scissor.width() > 0 && scissor.height() > 0) {
                           MeshRenderer.begin()
                              .attachments(MeteorClient.mc.getMainRenderTarget())
                              .pipeline(tinted ? MeteorRenderPipelines.LIQUID_GLASS_TINTED : MeteorRenderPipelines.LIQUID_GLASS_CLEAR)
                              .fullscreen()
                              .scissor(scissor.x(), scissor.y(), scissor.width(), scissor.height())
                              .uniform("LiquidGlassData", glassData)
                              .sampler(
                                 "u_Texture", blurredSnapshotTarget.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
                              )
                              .end();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static RenderTarget getSnapshotTarget(int width, int height) {
      if (snapshot == null) {
         snapshot = new TextureTarget("Meteor LiquidGlass Snapshot", width, height, false);
      } else if (isWrongSize(snapshot, width, height)) {
         snapshot.resize(width, height);
         snapshotCaptured = false;
         blurPrepared = false;
         preparedBlurStrength = Float.NaN;
      }

      return snapshot;
   }

   private static RenderTarget getBlurredSnapshotTarget(int width, int height) {
      if (blurredSnapshot == null) {
         blurredSnapshot = new TextureTarget("Meteor LiquidGlass Blurred", width, height, false);
      } else if (isWrongSize(blurredSnapshot, width, height)) {
         blurredSnapshot.resize(width, height);
         blurPrepared = false;
         preparedBlurStrength = Float.NaN;
      }

      return blurredSnapshot;
   }

   private static boolean isWrongSize(RenderTarget target, int width, int height) {
      GpuTexture texture = target.getColorTexture();
      return texture == null || texture.getWidth(0) != width || texture.getHeight(0) != height;
   }

   private static void captureSnapshot(GpuTexture sourceTexture, RenderTarget snapshotTarget, int width, int height) {
      if (!snapshotCaptured && snapshotTarget != null && snapshotTarget.getColorTexture() != null && sourceTexture != null) {
         RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(sourceTexture, snapshotTarget.getColorTexture(), 0, 0, 0, 0, 0, width, height);
         snapshotCaptured = true;
         blurPrepared = false;
         preparedBlurStrength = Float.NaN;
      }
   }

   private static void prepareBlur(int width, int height, float blurStrength) {
      if (snapshot != null && blurredSnapshot != null) {
         if (snapshot.getColorTexture() != null && snapshot.getColorTextureView() != null) {
            if (blurredSnapshot.getColorTexture() != null && blurredSnapshot.getColorTextureView() != null) {
               float safeBlurStrength = Math.max(0.0F, blurStrength);
               if (!blurPrepared || !(Math.abs(preparedBlurStrength - safeBlurStrength) < 0.01F)) {
                  if (safeBlurStrength <= 0.01F) {
                     RenderSystem.getDevice()
                        .createCommandEncoder()
                        .copyTextureToTexture(snapshot.getColorTexture(), blurredSnapshot.getColorTexture(), 0, 0, 0, 0, 0, width, height);
                     blurPrepared = true;
                     preparedBlurStrength = safeBlurStrength;
                  } else {
                     float radius = safeBlurStrength <= 0.01F ? 0.0F : Math.max(0.6F, safeBlurStrength * 0.22F);
                     GpuBufferSlice blurData = BLUR_UNIFORM_STORAGE.write(
                        new LiquidGlassUtil.BlurUniformData(width, height, 1.0F / width, 1.0F / height, 1.0F, 1.0F, radius, 0.0F)
                     );
                     MeshRenderer.begin()
                        .attachments(blurredSnapshot.getColorTextureView(), null)
                        .pipeline(MeteorRenderPipelines.LIQUID_GLASS_BLUR)
                        .fullscreen()
                        .uniform("LiquidGlassBlurData", blurData)
                        .sampler("u_Texture", snapshot.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                        .end();
                     blurPrepared = true;
                     preparedBlurStrength = safeBlurStrength;
                  }
               }
            }
         }
      }
   }

   public static void close() {
      snapshotCaptured = false;
      blurPrepared = false;
      preparedBlurStrength = Float.NaN;
   }

   private static LiquidGlassUtil.ScissorRect toScissor(double x, double y, double width, double height) {
      int framebufferWidth = MeteorClient.mc.getWindow().getWidth();
      int framebufferHeight = MeteorClient.mc.getWindow().getHeight();
      int x1 = (int)Math.round(x);
      int y1 = (int)Math.round(framebufferHeight - y - height);
      int x2 = (int)Math.round(x + width);
      int y2 = (int)Math.round(framebufferHeight - y);
      x1 = Math.max(0, Math.min(framebufferWidth, x1));
      y1 = Math.max(0, Math.min(framebufferHeight, y1));
      x2 = Math.max(0, Math.min(framebufferWidth, x2));
      y2 = Math.max(0, Math.min(framebufferHeight, y2));
      return new LiquidGlassUtil.ScissorRect(x1, y1, Math.max(0, x2 - x1), Math.max(0, y2 - y1));
   }

   private record BlurUniformData(
      float resolutionX, float resolutionY, float inverseResolutionX, float inverseResolutionY, float radiusX, float radiusY, float quality, float padding
   ) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer)
            .putVec4(this.resolutionX, this.resolutionY, this.inverseResolutionX, this.inverseResolutionY)
            .putVec4(this.radiusX, this.radiusY, this.quality, this.padding);
      }
   }

   private record GlassUniformData(
      float resolutionX,
      float resolutionY,
      float inverseResolutionX,
      float inverseResolutionY,
      float centerX,
      float centerY,
      float halfWidth,
      float halfHeight,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float noise,
      float refractionPower,
      float glowWeight,
      float glowBias,
      float glowEdge0,
      float glowEdge1,
      float glowPadding0,
      float glowPadding1,
      float refractionPadding0,
      float refractionPadding1,
      float refractionPadding2,
      float refractionPadding3,
      float tintR,
      float tintG,
      float tintB,
      float tintA
   ) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer)
            .putVec4(this.resolutionX, this.resolutionY, this.inverseResolutionX, this.inverseResolutionY)
            .putVec4(this.centerX, this.centerY, this.halfWidth, this.halfHeight)
            .putVec4(this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft)
            .putVec4(this.noise, this.refractionPower, this.glowWeight, this.glowBias)
            .putVec4(this.glowEdge0, this.glowEdge1, this.glowPadding0, this.glowPadding1)
            .putVec4(this.refractionPadding0, this.refractionPadding1, this.refractionPadding2, this.refractionPadding3)
            .putVec4(this.tintR, this.tintG, this.tintB, this.tintA);
      }
   }

   private record ScissorRect(int x, int y, int width, int height) {
   }
}
