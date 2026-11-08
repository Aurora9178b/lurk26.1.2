package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.RenderTypeAccessor;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.text.ttf.TtfFontLoader;
import meteordevelopment.meteorclient.renderer.text.ttf.TtfGlyphAtlas;
import meteordevelopment.meteorclient.renderer.text.ttf.TtfGlyphDescriptor;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.font.TextRenderable.Styled;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

public final class MeteorFontGlyph implements BakedGlyph {
   private static final float VANILLA_LINE_HEIGHT = 9.0F;
   private static final float LETTER_SPACING = 0.0F;
   private static final float SPACE_WIDTH = 3.0F;
   private static final float SHADOW_OFFSET = 0.45F;
   private static final float BOLD_OFFSET = 0.45F;
   private static final boolean ANTI_ALIASING = true;
   private static final Map<TtfGlyphAtlas, RenderType> RENDER_TYPES = new IdentityHashMap<>();
   private static final Map<Integer, MeteorFontGlyph> GLYPHS = new HashMap<>();
   private static final Map<Integer, Float> ADVANCES = new HashMap<>();
   private static TtfFontLoader font;
   private final int codepoint;
   private final TtfFontLoader fontLoader;
   @Nullable
   private final TtfGlyphDescriptor descriptor;
   private final GlyphInfo info;

   private MeteorFontGlyph(int codepoint, TtfFontLoader fontLoader, @Nullable TtfGlyphDescriptor descriptor) {
      this.codepoint = codepoint;
      this.fontLoader = fontLoader;
      this.descriptor = descriptor;
      this.info = new MeteorFontGlyph.MeteorGlyphInfo(advance(codepoint, Style.EMPTY, fontLoader));
   }

   @Nullable
   public static MeteorFontGlyph create(int codepoint) {
      TtfFontLoader font = font();
      if (font == null) {
         return null;
      }

      if (codepoint >= 0 && codepoint <= 65535) {
         MeteorFontGlyph cached = GLYPHS.get(codepoint);
         if (cached != null) {
            return cached;
         }

         MeteorFontGlyph glyph;
         if (Character.isWhitespace(codepoint)) {
            glyph = new MeteorFontGlyph(codepoint, font, null);
         } else {
            char ch = (char)codepoint;
            font.checkAndLoadChar(ch);
            TtfGlyphDescriptor descriptor = font.getGlyph(ch);
            if (descriptor == null) {
               return null;
            }

            glyph = new MeteorFontGlyph(codepoint, font, descriptor);
         }

         GLYPHS.put(codepoint, glyph);
         return glyph;
      } else {
         return null;
      }
   }

   @Nullable
   public static Float width(String text) {
      TtfFontLoader font = font();
      if (font == null) {
         return null;
      }

      float[] width = new float[]{0.0F};
      StringDecomposer.iterateFormatted(text, Style.EMPTY, (var2x, style, codepoint) -> {
         width[0] += advance(codepoint, style, font);
         return true;
      });
      return width[0];
   }

   @Nullable
   public static Float width(FormattedCharSequence text) {
      TtfFontLoader font = font();
      if (font == null) {
         return null;
      }

      float[] width = new float[]{0.0F};
      text.accept((var2x, style, codepoint) -> {
         width[0] += advance(codepoint, style, font);
         return true;
      });
      return width[0];
   }

   @Nullable
   public static Float width(FormattedText text) {
      TtfFontLoader font = font();
      if (font == null) {
         return null;
      }

      float[] width = new float[]{0.0F};
      StringDecomposer.iterateFormatted(text, Style.EMPTY, (var2x, style, codepoint) -> {
         width[0] += advance(codepoint, style, font);
         return true;
      });
      return width[0];
   }

   @Nullable
   public static Float advance(int codepoint, Style style) {
      TtfFontLoader font = font();
      return font == null ? null : advance(codepoint, style, font);
   }

   public static void clearCache() {
      GLYPHS.clear();
      RENDER_TYPES.clear();
      ADVANCES.clear();
      if (font != null) {
         font.destroy();
         font = null;
      }
   }

   @Nullable
   private static TtfFontLoader font() {
      if (font != null) {
         return font;
      }

      FontFamily family = Fonts.getFamily("montserrat");
      FontFace face = family == null ? null : family.get(FontInfo.Type.Regular);
      if (face == null) {
         face = Fonts.DEFAULT_FONT;
      }

      if (face == null) {
         return null;
      }

      try {
         font = new TtfFontLoader(face);
         return font;
      } catch (Exception e) {
         MeteorClient.LOG.error("Failed to load Minecraft replacement font: {}", face, e);
         return null;
      }
   }

   private static float minecraftScale(TtfFontLoader font) {
      return 9.0F / font.fontFile.fontHeight;
   }

   private static float advance(int codepoint, Style style, TtfFontLoader font) {
      int key = codepoint | (style.isBold() ? 1073741824 : 0);
      Float cached = ADVANCES.get(key);
      if (cached != null) {
         return cached;
      }

      if (codepoint == 10 || codepoint == 13) {
         ADVANCES.put(key, 0.0F);
         return 0.0F;
      }

      if (Character.isWhitespace(codepoint)) {
         float advance = 3.0F + (style.isBold() ? 1.0F : 0.0F);
         ADVANCES.put(key, advance);
         return advance;
      }

      if (codepoint >= 0 && codepoint <= 65535) {
         char ch = (char)codepoint;
         font.checkAndLoadChar(ch);
         TtfGlyphDescriptor descriptor = font.getGlyph(ch);
         if (descriptor == null) {
            ADVANCES.put(key, 0.0F);
            return 0.0F;
         } else {
            float advance = descriptor.advance() * minecraftScale(font) + 0.0F + (style.isBold() ? 1.0F : 0.0F);
            ADVANCES.put(key, advance);
            return advance;
         }
      } else {
         ADVANCES.put(key, 0.0F);
         return 0.0F;
      }
   }

   @Override
   public GlyphInfo info() {
      return this.info;
   }

   @Override
   public @Nullable Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset) {
      return this.descriptor == null ? null : new MeteorFontGlyph.GlyphInstance(this, x, y, color, shadowColor, style, boldOffset, shadowOffset);
   }

   private RenderType renderType() {
      if (this.descriptor == null) {
         throw new IllegalStateException("Whitespace glyphs do not have render types");
      } else {
         return RENDER_TYPES.computeIfAbsent(
            this.descriptor.atlas(),
            atlas -> RenderTypeAccessor.create(
               "meteor_ttf_text_aa",
               RenderSetup.builder(MeteorRenderPipelines.TTF_FONT_AA)
                  .withTexture("Sampler0", atlas.getTextureId(), () -> atlas.getTexture().getSampler())
                  .bufferSize(786432)
                  .createRenderSetup()
            )
         );
      }
   }

   private float baselineY(float y) {
      return y + this.fontLoader.fontFile.pixelAscent * this.scale();
   }

   private float scale() {
      return minecraftScale(this.fontLoader);
   }

   private float left(float x) {
      return x;
   }

   private static float extraThickness(boolean bold) {
      return bold ? 0.06F : 0.0F;
   }

   private float top(float y) {
      return this.descriptor == null ? y : this.baselineY(y) + this.descriptor.yOffset() * this.scale();
   }

   private float right(float x, boolean hasShadow, float shadowOffset, boolean bold, boolean italic) {
      if (this.descriptor == null) {
         return x + this.info.getAdvance(bold);
      }

      float right = x + this.descriptor.width() * this.scale();
      if (hasShadow) {
         right += shadowOffset;
      }

      if (bold) {
         right += extraThickness(true);
      }

      if (italic) {
         right++;
      }

      return right;
   }

   private float bottom(float y, boolean hasShadow, float shadowOffset, boolean bold) {
      if (this.descriptor == null) {
         return y;
      }

      float bottom = this.top(y) + this.descriptor.height() * this.scale();
      if (hasShadow) {
         bottom += shadowOffset;
      }

      if (bold) {
         bottom += extraThickness(true);
      }

      return bottom;
   }

   private void renderGlyph(
      Matrix4fc pose, VertexConsumer buffer, MeteorFontGlyph.GlyphInstance instance, float offsetX, float offsetY, float z, int color, boolean bold
   ) {
      if (this.descriptor != null) {
         float x0 = instance.x + offsetX;
         float x1 = x0 + this.descriptor.width() * this.scale();
         float y0 = this.top(instance.y) + offsetY;
         float y1 = y0 + this.descriptor.height() * this.scale();
         float extraThickness = extraThickness(bold);
         float shearTop = instance.style.isItalic() ? 1.0F - 0.25F * (y0 - instance.y) : 0.0F;
         float shearBottom = instance.style.isItalic() ? 1.0F - 0.25F * (y1 - instance.y) : 0.0F;
         TtfGlyphAtlas.GlyphUV uv = this.descriptor.uv();
         buffer.addVertex(pose, x0 + shearTop - extraThickness, y0 - extraThickness, z).setUv(uv.u0(), uv.v0()).setColor(color);
         buffer.addVertex(pose, x0 + shearBottom - extraThickness, y1 + extraThickness, z).setUv(uv.u0(), uv.v1()).setColor(color);
         buffer.addVertex(pose, x1 + shearBottom + extraThickness, y1 + extraThickness, z).setUv(uv.u1(), uv.v1()).setColor(color);
         buffer.addVertex(pose, x1 + shearTop + extraThickness, y0 - extraThickness, z).setUv(uv.u1(), uv.v0()).setColor(color);
      }
   }

   private record GlyphInstance(MeteorFontGlyph glyph, float x, float y, int color, int shadowColor, Style style, float boldOffset, float shadowOffset)
      implements Styled,
      MeteorTextRenderable {
      private boolean hasShadow() {
         return this.shadowColor != 0;
      }

      @Override
      public void render(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, boolean flat) {
         float depth;
         if (this.hasShadow()) {
            this.glyph.renderGlyph(pose, buffer, this, this.shadowOffset, this.shadowOffset, 0.0F, this.shadowColor, this.style.isBold());
            depth = flat ? 0.0F : 0.03F;
         } else {
            depth = 0.0F;
         }

         this.glyph.renderGlyph(pose, buffer, this, 0.0F, 0.0F, depth, this.color, this.style.isBold());
         if (this.style.isBold()) {
            this.glyph.renderGlyph(pose, buffer, this, this.boldOffset, 0.0F, depth + (flat ? 0.0F : 0.001F), this.color, true);
         }
      }

      @Override
      public RenderType renderType(DisplayMode displayMode) {
         return this.glyph.renderType();
      }

      @Override
      public GpuTextureView textureView() {
         if (this.glyph.descriptor == null) {
            throw new IllegalStateException("Whitespace glyphs do not have textures");
         } else {
            return this.glyph.descriptor.atlas().getTexture().getTextureView();
         }
      }

      @Override
      public GpuSampler meteor$fontSampler() {
         if (this.glyph.descriptor == null) {
            throw new IllegalStateException("Whitespace glyphs do not have samplers");
         } else {
            return this.glyph.descriptor.atlas().getTexture().getSampler();
         }
      }

      @Override
      public RenderPipeline guiPipeline() {
         return MeteorRenderPipelines.TTF_FONT_AA;
      }

      @Override
      public float left() {
         return this.glyph.left(this.x);
      }

      @Override
      public float top() {
         return this.glyph.top(this.y);
      }

      @Override
      public float right() {
         return this.glyph.right(this.x, this.hasShadow(), this.shadowOffset, this.style.isBold(), this.style.isItalic());
      }

      @Override
      public float activeRight() {
         return this.x + this.glyph.info.getAdvance(this.style.isBold());
      }

      @Override
      public float bottom() {
         return this.glyph.bottom(this.y, this.hasShadow(), this.shadowOffset, this.style.isBold());
      }
   }

   private record MeteorGlyphInfo(float advance) implements GlyphInfo {
      @Override
      public float getAdvance() {
         return this.advance;
      }

      @Override
      public float getBoldOffset() {
         return 0.45F;
      }

      @Override
      public float getShadowOffset() {
         return 0.45F;
      }
   }
}
