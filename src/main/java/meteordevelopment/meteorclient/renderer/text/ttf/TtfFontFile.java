package meteordevelopment.meteorclient.renderer.text.ttf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

public class TtfFontFile {
   private final ByteBuffer fontData;
   private final STBTTFontinfo fontInfo;
   private final int padding;
   public final float scale;
   public final int pixelAscent;
   public final int fontHeight;

   public TtfFontFile(FontFace fontFace, int totalHeight, int padding) throws IOException {
      this.fontData = fontFace.readToDirectByteBuffer();
      this.fontInfo = STBTTFontinfo.create();
      if (!STBTruetype.stbtt_InitFont(this.fontInfo, this.fontData)) {
         throw new IllegalStateException("STB TrueType failed to load ttf font: " + fontFace);
      }

      this.padding = padding;
      this.scale = STBTruetype.stbtt_ScaleForPixelHeight(this.fontInfo, totalHeight - padding * 2);

      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer ascentBuf = stack.callocInt(1);
         IntBuffer descentBuf = stack.callocInt(1);
         IntBuffer lineGapBuf = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(this.fontInfo, ascentBuf, descentBuf, lineGapBuf);
         int ascent = ascentBuf.get();
         int descent = descentBuf.get();
         int lineGap = lineGapBuf.get();
         this.pixelAscent = (int)(ascent * this.scale);
         this.fontHeight = (int)((ascent - descent + lineGap) * this.scale);
      }
   }

   public TtfGlyph generateGlyph(char ch) {
      int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(this.fontInfo, ch);
      byte onEdgeValue = -128;
      float pixelDistScale = (float)onEdgeValue / this.padding;

      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer width = stack.callocInt(1);
         IntBuffer height = stack.callocInt(1);
         IntBuffer xOff = stack.callocInt(1);
         IntBuffer yOff = stack.callocInt(1);
         ByteBuffer sdfPixels = STBTruetype.stbtt_GetGlyphSDF(
            this.fontInfo, this.scale, glyphIndex, this.padding, onEdgeValue, pixelDistScale, width, height, xOff, yOff
         );
         IntBuffer advance = stack.callocInt(1);
         IntBuffer lsb = stack.callocInt(1);
         STBTruetype.stbtt_GetGlyphHMetrics(this.fontInfo, glyphIndex, advance, lsb);
         return new TtfGlyph(sdfPixels, width.get(), height.get(), xOff.get(), yOff.get(), (int)(advance.get() * this.scale));
      }
   }

   public void destroy() {
   }
}
