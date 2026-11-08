package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.renderer.FixedUniformStorage;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.client.GlowSetting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform;
import net.minecraft.resources.Identifier;

public class Render2DUtil {
   private static final int RECT_UNIFORM_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().get();
   private static final int RECT_CORNERS_UNIFORM_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().get();
   private static final int ACID_GLOW_UNIFORM_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();
   private static final FixedUniformStorage<Render2DUtil.RectUniformData> RECT_UNIFORM_STORAGE = new FixedUniformStorage<>(
      "Lead - RectRounded UBO", RECT_UNIFORM_SIZE, 1
   );
   private static final FixedUniformStorage<Render2DUtil.RectCornersUniformData> RECT_CORNERS_UNIFORM_STORAGE = new FixedUniformStorage<>(
      "Lead - RectRoundedCorners UBO", RECT_CORNERS_UNIFORM_SIZE, 1
   );
   private static final FixedUniformStorage<Render2DUtil.AcidGlowUniformData> ACID_GLOW_UNIFORM_STORAGE = new FixedUniformStorage<>(
      "Lead - AcidGlow UBO", ACID_GLOW_UNIFORM_SIZE, 1
   );

   public static void drawRect(HudRenderer renderer, double x, double y, double width, double height, Color color) {
      renderer.quad(x, y, width, height, color);
   }

   public static void drawRoundedRect(HudRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
      if (radius <= 0.0) {
         drawRect(renderer, x, y, width, height, color);
      } else {
         double windowScale = MeteorClient.mc.getWindow().getGuiScale();
         double sx = x / windowScale;
         double sy = y / windowScale;
         double sw = width / windowScale;
         double sh = height / windowScale;
         double sr = radius / windowScale;
         float fbH = MeteorClient.mc.getWindow().getHeight();
         float scale = (float)windowScale;
         float cx = (float)(sx + sw / 2.0);
         float cy = (float)(sy + sh / 2.0);
         float hw = (float)(sw / 2.0);
         float hh = (float)(sh / 2.0);
         RECT_UNIFORM_STORAGE.clear();
         GpuBufferSlice[] rectUbos = RECT_UNIFORM_STORAGE.writeAll(
            new Render2DUtil.RectUniformData[]{
               new Render2DUtil.RectUniformData(
                  cx, cy, hw, hh, fbH, scale, (float)sr, 0.0F, color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, color.a / 255.0F
               )
            }
         );
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.RECT_ROUNDED)
            .fullscreen()
            .uniform("RectData", rectUbos[0])
            .end();
      }
   }

   public static void drawRoundedRectCorners(
      HudRenderer renderer,
      double x,
      double y,
      double width,
      double height,
      double radiusTopLeft,
      double radiusTopRight,
      double radiusBottomRight,
      double radiusBottomLeft,
      Color color
   ) {
      if (radiusTopLeft <= 0.0 && radiusTopRight <= 0.0 && radiusBottomRight <= 0.0 && radiusBottomLeft <= 0.0) {
         drawRect(renderer, x, y, width, height, color);
      } else {
         drawRoundedRectCorners(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color);
      }
   }

   public static void drawRoundedTexture(
      HudRenderer renderer,
      Identifier texture,
      double x,
      double y,
      double width,
      double height,
      double radius,
      float u,
      float v,
      float regionWidth,
      float regionHeight,
      float textureWidth,
      float textureHeight
   ) {
      double windowScale = MeteorClient.mc.getWindow().getGuiScale();
      double sx = x / windowScale;
      double sy = y / windowScale;
      double sw = width / windowScale;
      double sh = height / windowScale;
      double sr = radius / windowScale;
      if (radius <= 0.0) {
         renderer.graphics
            .blit(
               RenderPipelines.GUI_TEXTURED,
               texture,
               (int)sx,
               (int)sy,
               u,
               v,
               (int)sw,
               (int)sh,
               (int)regionWidth,
               (int)regionHeight,
               (int)textureWidth,
               (int)textureHeight
            );
      } else {
         float fbH = MeteorClient.mc.getWindow().getHeight();
         float scale = (float)windowScale;
         float cx = (float)(sx + sw / 2.0);
         float cy = (float)(sy + sh / 2.0);
         float hw = (float)(sw / 2.0);
         float hh = (float)(sh / 2.0);
         RECT_UNIFORM_STORAGE.clear();
         GpuBufferSlice[] rectUbos = RECT_UNIFORM_STORAGE.writeAll(
            new Render2DUtil.RectUniformData[]{
               new Render2DUtil.RectUniformData(
                  cx, cy, hw, hh, fbH, scale, (float)sr, 0.0F, u / textureWidth, v / textureHeight, regionWidth / textureWidth, regionHeight / textureHeight
               )
            }
         );
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.TEXTURE_ROUNDED)
            .fullscreen()
            .uniform("RectData", rectUbos[0])
            .sampler(
               "u_Texture",
               MeteorClient.mc.getTextureManager().getTexture(texture).getTextureView(),
               MeteorClient.mc.getTextureManager().getTexture(texture).getSampler()
            )
            .end();
      }
   }

   public static void drawTexture(HudRenderer renderer, Identifier icon, int x, int y, int iconWeight, int iconHeight, int iconsize, int color) {
      double windowScale = MeteorClient.mc.getWindow().getGuiScale();
      renderer.graphics
         .blit(
            RenderPipelines.GUI_TEXTURED,
            icon,
            (int)(x / windowScale),
            (int)(y / windowScale),
            0.0F,
            0.0F,
            iconsize,
            iconsize,
            iconWeight,
            iconHeight,
            iconWeight,
            iconHeight,
            color
         );
   }

   public static double interpolate(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
      return (float)interpolate(oldValue, newValue, interpolationValue);
   }

   public static void drawRoundedRect(double x, double y, double width, double height, double radius, Color color) {
      if (!(radius <= 0.0)) {
         double windowScale = MeteorClient.mc.getWindow().getGuiScale();
         double sx = x / windowScale;
         double sy = y / windowScale;
         double sw = width / windowScale;
         double sh = height / windowScale;
         double sr = radius / windowScale;
         float fbH = MeteorClient.mc.getWindow().getHeight();
         float scale = (float)windowScale;
         float cx = (float)(sx + sw / 2.0);
         float cy = (float)(sy + sh / 2.0);
         float hw = (float)(sw / 2.0);
         float hh = (float)(sh / 2.0);
         RECT_UNIFORM_STORAGE.clear();
         GpuBufferSlice[] rectUbos = RECT_UNIFORM_STORAGE.writeAll(
            new Render2DUtil.RectUniformData[]{
               new Render2DUtil.RectUniformData(
                  cx, cy, hw, hh, fbH, scale, (float)sr, 0.0F, color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, color.a / 255.0F
               )
            }
         );
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.RECT_ROUNDED)
            .fullscreen()
            .uniform("RectData", rectUbos[0])
            .end();
      }
   }

   public static void drawRoundedRectCorners(
      double x,
      double y,
      double width,
      double height,
      double radiusTopLeft,
      double radiusTopRight,
      double radiusBottomRight,
      double radiusBottomLeft,
      Color color
   ) {
      if (!(width <= 0.0) && !(height <= 0.0) && color.a > 0) {
         if (!(radiusTopLeft <= 0.0) || !(radiusTopRight <= 0.0) || !(radiusBottomRight <= 0.0) || !(radiusBottomLeft <= 0.0)) {
            drawRoundedRectCornersInternal(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color);
         }
      }
   }

   public static void drawRect(GuiRenderer renderer, double x, double y, double width, double height, Color color) {
      renderer.quad(x, y, width, height, color);
   }

   public static void drawRoundedRect(GuiRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
      if (radius <= 0.0) {
         drawRect(renderer, x, y, width, height, color);
      } else {
         double windowScale = MeteorClient.mc.getWindow().getGuiScale();
         double sx = x / windowScale;
         double sy = y / windowScale;
         double sw = width / windowScale;
         double sh = height / windowScale;
         double sr = radius / windowScale;
         float fbH = MeteorClient.mc.getWindow().getHeight();
         float scale = (float)windowScale;
         float cx = (float)(sx + sw / 2.0);
         float cy = (float)(sy + sh / 2.0);
         float hw = (float)(sw / 2.0);
         float hh = (float)(sh / 2.0);
         RECT_UNIFORM_STORAGE.clear();
         GpuBufferSlice[] rectUbos = RECT_UNIFORM_STORAGE.writeAll(
            new Render2DUtil.RectUniformData[]{
               new Render2DUtil.RectUniformData(
                  cx, cy, hw, hh, fbH, scale, (float)sr, 0.0F, color.r / 255.0F, color.g / 255.0F, color.b / 255.0F, color.a / 255.0F
               )
            }
         );
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.RECT_ROUNDED)
            .fullscreen()
            .uniform("RectData", rectUbos[0])
            .end();
      }
   }

   public static void drawRoundedRectCorners(
      GuiRenderer renderer,
      double x,
      double y,
      double width,
      double height,
      double radiusTopLeft,
      double radiusTopRight,
      double radiusBottomRight,
      double radiusBottomLeft,
      Color color
   ) {
      if (radiusTopLeft <= 0.0 && radiusTopRight <= 0.0 && radiusBottomRight <= 0.0 && radiusBottomLeft <= 0.0) {
         drawRect(renderer, x, y, width, height, color);
      } else {
         drawRoundedRectCornersInternal(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color);
      }
   }

   public static void drawRoundedGlow(double x, double y, double width, double height, double radius) {
      if (GlowSetting.isGlowEnabled()) {
         if (!(width <= 0.0) && !(height <= 0.0)) {
            if (GlowSetting.getStyle() == GlowSetting.Style.Acid) {
               drawOutlineShader(
                  x,
                  y,
                  width,
                  height,
                  radius,
                  GlowSetting.getAcidColor1(),
                  GlowSetting.getAcidColor2(),
                  GlowSetting.getAcidColor3(),
                  GlowSetting.getAcidColor4()
               );
            } else {
               Color glowColor = GlowSetting.getLurkColor();
               float glowStrength = GlowSetting.getLurkStrength();
               if (!(glowStrength <= 0.0F) && glowColor.a > 0) {
                  drawLurkGlow(x, y, width, height, radius, glowStrength, glowColor);
               }
            }
         }
      }
   }

   public static void drawOutlineShader(double x, double y, double width, double height, double radius, Color color) {
      drawOutlineShader(x, y, width, height, radius, color, color, color, color);
   }

   public static void drawOutlineShader(double x, double y, double width, double height, double radius, Color color1, Color color2, Color color3, Color color4) {
      if (!(width <= 0.0) && !(height <= 0.0) && color1.a > 0) {
         double windowScale = MeteorClient.mc.getWindow().getGuiScale();
         double sx = x / windowScale;
         double sy = y / windowScale;
         double sw = width / windowScale;
         double sh = height / windowScale;
         double sr = radius / windowScale;
         float fbH = MeteorClient.mc.getWindow().getHeight();
         float scale = (float)windowScale;
         float cx = (float)(sx + sw / 2.0);
         float cy = (float)(sy + sh / 2.0);
         float hw = (float)(sw / 2.0);
         float hh = (float)(sh / 2.0);
         float externalAlpha = color1.a / 255.0F;
         ACID_GLOW_UNIFORM_STORAGE.clear();
         GpuBufferSlice[] acidUbos = ACID_GLOW_UNIFORM_STORAGE.writeAll(
            new Render2DUtil.AcidGlowUniformData[]{
               new Render2DUtil.AcidGlowUniformData(
                  cx,
                  cy,
                  hw,
                  hh,
                  fbH,
                  scale,
                  (float)sr,
                  GlowSetting.getAcidInternalAlpha(),
                  color1.r / 255.0F,
                  color1.g / 255.0F,
                  color1.b / 255.0F,
                  externalAlpha,
                  color2.r / 255.0F,
                  color2.g / 255.0F,
                  color2.b / 255.0F,
                  externalAlpha,
                  color3.r / 255.0F,
                  color3.g / 255.0F,
                  color3.b / 255.0F,
                  externalAlpha,
                  color4.r / 255.0F,
                  color4.g / 255.0F,
                  color4.b / 255.0F,
                  externalAlpha,
                  1.0F,
                  GlowSetting.getAcidGlow(),
                  0.0F,
                  0.0F
               )
            }
         );
         MeshRenderer.begin()
            .attachments(MeteorClient.mc.getMainRenderTarget())
            .pipeline(MeteorRenderPipelines.RECT_ACID_GLOW)
            .fullscreen()
            .uniform("AcidGlowData", acidUbos[0])
            .end();
      }
   }

   private static void drawLurkGlow(double x, double y, double width, double height, double radius, float strength, Color color) {
      drawGlowShader(x, y, width, height, radius, strength, color, MeteorRenderPipelines.RECT_SHADOW);
   }

   private static void drawGlowShader(double x, double y, double width, double height, double radius, float strength, Color glowColor, RenderPipeline pipeline) {
      double windowScale = MeteorClient.mc.getWindow().getGuiScale();
      double sx = x / windowScale;
      double sy = y / windowScale;
      double sw = width / windowScale;
      double sh = height / windowScale;
      double sr = radius / windowScale;
      float fbH = MeteorClient.mc.getWindow().getHeight();
      float scale = (float)windowScale;
      float cx = (float)(sx + sw / 2.0);
      float cy = (float)(sy + sh / 2.0);
      float hw = (float)(sw / 2.0);
      float hh = (float)(sh / 2.0);
      float shaderStrength = Math.max(0.001F, strength / (float)windowScale);
      float alpha = glowColor.a / 255.0F;
      RECT_UNIFORM_STORAGE.clear();
      GpuBufferSlice[] rectUbos = RECT_UNIFORM_STORAGE.writeAll(
         new Render2DUtil.RectUniformData[]{
            new Render2DUtil.RectUniformData(
               cx, cy, hw, hh, fbH, scale, (float)sr, shaderStrength, glowColor.r / 255.0F, glowColor.g / 255.0F, glowColor.b / 255.0F, alpha
            )
         }
      );
      MeshRenderer.begin().attachments(MeteorClient.mc.getMainRenderTarget()).pipeline(pipeline).fullscreen().uniform("RectData", rectUbos[0]).end();
   }

   private static void drawRoundedRectCornersInternal(
      double x,
      double y,
      double width,
      double height,
      double radiusTopLeft,
      double radiusTopRight,
      double radiusBottomRight,
      double radiusBottomLeft,
      Color color
   ) {
      double windowScale = MeteorClient.mc.getWindow().getGuiScale();
      double sx = x / windowScale;
      double sy = y / windowScale;
      double sw = width / windowScale;
      double sh = height / windowScale;
      double srTopLeft = Math.max(0.0, radiusTopLeft / windowScale);
      double srTopRight = Math.max(0.0, radiusTopRight / windowScale);
      double srBottomRight = Math.max(0.0, radiusBottomRight / windowScale);
      double srBottomLeft = Math.max(0.0, radiusBottomLeft / windowScale);
      float fbH = MeteorClient.mc.getWindow().getHeight();
      float scale = (float)windowScale;
      float cx = (float)(sx + sw / 2.0);
      float cy = (float)(sy + sh / 2.0);
      float hw = (float)(sw / 2.0);
      float hh = (float)(sh / 2.0);
      RECT_CORNERS_UNIFORM_STORAGE.clear();
      GpuBufferSlice[] rectUbos = RECT_CORNERS_UNIFORM_STORAGE.writeAll(
         new Render2DUtil.RectCornersUniformData[]{
            new Render2DUtil.RectCornersUniformData(
               cx,
               cy,
               hw,
               hh,
               fbH,
               scale,
               0.0F,
               0.0F,
               color.r / 255.0F,
               color.g / 255.0F,
               color.b / 255.0F,
               color.a / 255.0F,
               (float)srTopLeft,
               (float)srTopRight,
               (float)srBottomRight,
               (float)srBottomLeft
            )
         }
      );
      MeshRenderer.begin()
         .attachments(MeteorClient.mc.getMainRenderTarget())
         .pipeline(MeteorRenderPipelines.RECT_ROUNDED_CORNERS)
         .fullscreen()
         .uniform("RectCornersData", rectUbos[0])
         .end();
   }

   private record AcidGlowUniformData(
      float cx,
      float cy,
      float hw,
      float hh,
      float fbH,
      float scale,
      float radius,
      float internalAlpha,
      float c1r,
      float c1g,
      float c1b,
      float c1a,
      float c2r,
      float c2g,
      float c2b,
      float c2a,
      float c3r,
      float c3g,
      float c3b,
      float c3a,
      float c4r,
      float c4g,
      float c4b,
      float c4a,
      float blend,
      float glow,
      float p0,
      float p1
   ) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer)
            .putVec4(this.cx, this.cy, this.hw, this.hh)
            .putVec4(this.fbH, this.scale, this.radius, this.internalAlpha)
            .putVec4(this.c1r, this.c1g, this.c1b, this.c1a)
            .putVec4(this.c2r, this.c2g, this.c2b, this.c2a)
            .putVec4(this.c3r, this.c3g, this.c3b, this.c3a)
            .putVec4(this.c4r, this.c4g, this.c4b, this.c4a)
            .putVec4(this.blend, this.glow, this.p0, this.p1);
      }
   }

   private record RectCornersUniformData(
      float cx,
      float cy,
      float hw,
      float hh,
      float fbH,
      float scale,
      float p0,
      float p1,
      float cr,
      float cg,
      float cb,
      float ca,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft
   ) implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer)
            .putVec4(this.cx, this.cy, this.hw, this.hh)
            .putVec4(this.fbH, this.scale, this.p0, this.p1)
            .putVec4(this.cr, this.cg, this.cb, this.ca)
            .putVec4(this.radiusTopLeft, this.radiusTopRight, this.radiusBottomRight, this.radiusBottomLeft);
      }
   }

   private record RectUniformData(float cx, float cy, float hw, float hh, float fbH, float scale, float r, float p, float cr, float cg, float cb, float ca)
      implements DynamicUniform {
      @Override
      public void write(ByteBuffer buffer) {
         Std140Builder.intoBuffer(buffer)
            .putVec4(this.cx, this.cy, this.hw, this.hh)
            .putVec4(this.fbH, this.scale, this.r, this.p)
            .putVec4(this.cr, this.cg, this.cb, this.ca);
      }
   }
}
