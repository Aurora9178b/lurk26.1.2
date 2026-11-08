package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.modules.client.BlurSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform;

public class BlurUtil {
   private static GpuTexture snapshotTexture;
   private static GpuTextureView snapshotView;
   private static boolean snapshotCaptured;
   private static int lastWidth = -1;
   private static int lastHeight = -1;
   private static final int UNIFORM_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().get();
   private static final FixedUniformStorage<BlurUtil.BlurRectData> UNIFORM_STORAGE = new FixedUniformStorage<>("Meteor - Arc Blur UBO", UNIFORM_SIZE, 1024);

   private BlurUtil() {
   }

   public static void beginFrame() {
      snapshotCaptured = false;
      UNIFORM_STORAGE.clear();
   }

   public static void beginFrame(GuiGraphicsExtractor graphics) {
      beginFrame();
   }

   public static void render(int x, int y, int width, int height, float radius) {
      render(x, y, width, height, radius, BlurSetting.getBlurStrengthDirect());
   }

   public static void render(int x, int y, int width, int height, float radius, float blurStrength) {
      render(x, y, width, height, radius, radius, radius, radius, blurStrength);
   }

   public static void render(int x, int y, int width, int height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft) {
      render(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, BlurSetting.getBlurStrengthDirect());
   }

   public static void render(
      int x, int y, int width, int height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, float blurStrength
   ) {
      if (!(blurStrength <= 0.0F) && width > 0 && height > 0) {
         GpuTexture sourceTexture = MeteorClient.mc.getMainRenderTarget().getColorTexture();
         if (sourceTexture != null) {
            int sourceWidth = sourceTexture.getWidth(0);
            int sourceHeight = sourceTexture.getHeight(0);
            if (sourceWidth > 0 && sourceHeight > 0) {
               if (MeteorClient.mc.getWindow().getWidth() > 0 && MeteorClient.mc.getWindow().getHeight() > 0) {
                  initOrResize(sourceWidth, sourceHeight);
                  captureSnapshot(sourceTexture, sourceWidth, sourceHeight);
                  if (snapshotView != null) {
                     float scaleX = (float)sourceWidth / MeteorClient.mc.getWindow().getWidth();
                     float scaleY = (float)sourceHeight / MeteorClient.mc.getWindow().getHeight();
                     float radiusScale = Math.min(scaleX, scaleY);
                     float rx = x * scaleX;
                     float ry = y * scaleY;
                     float rw = width * scaleX;
                     float rh = height * scaleY;
                     float rtl = Math.max(0.0F, radiusTopLeft * radiusScale);
                     float rtr = Math.max(0.0F, radiusTopRight * radiusScale);
                     float rbr = Math.max(0.0F, radiusBottomRight * radiusScale);
                     float rbl = Math.max(0.0F, radiusBottomLeft * radiusScale);
                     GpuBufferSlice blurData = UNIFORM_STORAGE.write(
                        new BlurUtil.BlurRectData(
                           sourceWidth, sourceHeight, Math.max(0.0F, blurStrength), 0.0F, rx, ry, rw, rh, 0.0F, 0.0F, 0.0F, 0.0F, rtl, rtr, rbr, rbl
                        )
                     );
                     MeshRenderer.begin()
                        .attachments(MeteorClient.mc.getMainRenderTarget())
                        .pipeline(MeteorRenderPipelines.BLUR_ARC)
                        .fullscreen()
                        .uniform("BlurRectData", blurData)
                        .sampler("u_Texture", snapshotView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                        .end();
                  }
               }
            }
         }
      }
   }

   private static void initOrResize(int width, int height) {
      if (lastWidth != width || lastHeight != height || snapshotTexture == null) {
         closeSnapshot();
         lastWidth = width;
         lastHeight = height;
         snapshotTexture = RenderSystem.getDevice().createTexture("Arc Blur Snapshot", 15, TextureFormat.RGBA8, width, height, 1, 1);
         snapshotView = RenderSystem.getDevice().createTextureView(snapshotTexture);
         snapshotCaptured = false;
      }
   }

   private static void captureSnapshot(GpuTexture sourceTexture, int width, int height) {
      if (!snapshotCaptured && snapshotTexture != null) {
         RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(sourceTexture, snapshotTexture, 0, 0, 0, 0, 0, width, height);
         snapshotCaptured = true;
      }
   }

   public static void close() {
      closeSnapshot();
      lastWidth = -1;
      lastHeight = -1;
      snapshotCaptured = false;
   }

   private static void closeSnapshot() {
      if (snapshotView != null) {
         snapshotView.close();
         snapshotView = null;
      }

      if (snapshotTexture != null) {
         snapshotTexture.close();
         snapshotTexture = null;
      }
   }

   private record BlurRectData(
      float resolutionX,
      float resolutionY,
      float quality,
      float padding,
      float x,
      float y,
      float width,
      float height,
      float colorR,
      float colorG,
      float colorB,
      float colorA,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft
   ) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer)
            .putVec4(this.resolutionX, this.resolutionY, this.quality, this.padding)
            .putVec4(this.x, this.y, this.width, this.height)
            .putVec4(this.colorR, this.colorG, this.colorB, this.colorA)
            .putVec4(this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft);
      }
   }
}
