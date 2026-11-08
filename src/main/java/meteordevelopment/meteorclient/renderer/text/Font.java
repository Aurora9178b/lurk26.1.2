package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

public class Font {
   public final Texture texture;
   private static final int SIZE = 4096;
   private static final int PADDING = 2;
   private final int height;
   private final float scale;
   private final float ascent;
   private final ByteBuffer fontBuffer;
   private final STBTTFontinfo fontInfo;
   private final ByteBuffer atlasPixels;
   private final Int2ObjectOpenHashMap<Font.CharData> charMap = new Int2ObjectOpenHashMap<>();
   private int cursorX = 2;
   private int cursorY = 2;
   private int rowHeight;
   private boolean atlasDirty;

   public Font(ByteBuffer buffer, int height) {
      this.height = height;
      this.fontBuffer = buffer;
      this.fontInfo = STBTTFontinfo.create();
      if (!STBTruetype.stbtt_InitFont(this.fontInfo, this.fontBuffer)) {
         throw new IllegalStateException("Failed to initialize TTF font.");
      }

      this.texture = new Texture(4096, 4096, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR, AddressMode.CLAMP_TO_EDGE);
      this.atlasPixels = BufferUtils.createByteBuffer(16777216);
      this.scale = STBTruetype.stbtt_ScaleForPixelHeight(this.fontInfo, height);

      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer ascent = stack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(this.fontInfo, ascent, null, null);
         this.ascent = ascent.get(0);
      }

      this.loadWhitespace(32, this.getAdvance(32));
      this.loadWhitespace(9, this.getAdvance(32) * 4.0F);
      this.texture.upload(this.atlasPixels.duplicate());
   }

   public double getWidth(String string, int length) {
      if (string != null && !string.isEmpty() && length > 0) {
         int end = Math.min(length, string.length());
         this.ensureChars(string, end, false);
         double maxWidth = 0.0;
         double width = 0.0;
         int previous = 0;
         int i = 0;

         while (i < end) {
            int cp = string.codePointAt(i);
            int charCount = Character.charCount(cp);
            if (i + charCount > end) {
               break;
            }

            i += charCount;
            if (cp == 10) {
               maxWidth = Math.max(maxWidth, width);
               width = 0.0;
               previous = 0;
            } else {
               Font.CharData c = this.charData(cp);
               if (previous != 0) {
                  width += this.getKerning(previous, cp);
               }

               width += c.xAdvance;
               previous = cp;
            }
         }

         return Math.max(maxWidth, width);
      } else {
         return 0.0;
      }
   }

   public int getHeight() {
      return this.height;
   }

   public float getAscent() {
      return this.ascent;
   }

   public float getScale() {
      return this.scale;
   }

   public double render(MeshBuilder mesh, String string, double x, double y, Color color, double scale) {
      if (string != null && !string.isEmpty()) {
         int length = string.length();
         this.ensureChars(string, length, true);
         mesh.ensureCapacity(string.codePointCount(0, length) * 4, string.codePointCount(0, length) * 6);
         double startX = x;
         double baselineY = y + this.ascent * this.scale * scale;
         int previous = 0;
         int i = 0;

         while (i < length) {
            int cp = string.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == 10) {
               x = startX;
               baselineY += this.height * scale;
               previous = 0;
            } else {
               Font.CharData c = this.charData(cp);
               if (previous != 0) {
                  x += this.getKerning(previous, cp) * scale;
               }

               if (!c.empty) {
                  mesh.quad(
                     mesh.vec2(x + c.x0 * scale, baselineY + c.y0 * scale).vec2(c.u0, c.v0).color(color).next(),
                     mesh.vec2(x + c.x0 * scale, baselineY + c.y1 * scale).vec2(c.u0, c.v1).color(color).next(),
                     mesh.vec2(x + c.x1 * scale, baselineY + c.y1 * scale).vec2(c.u1, c.v1).color(color).next(),
                     mesh.vec2(x + c.x1 * scale, baselineY + c.y0 * scale).vec2(c.u1, c.v0).color(color).next()
                  );
               }

               x += c.xAdvance * scale;
               previous = cp;
            }
         }

         return x;
      } else {
         return x;
      }
   }

   private void ensureChars(String string, int end, boolean upload) {
      int i = 0;

      while (i < end) {
         int cp = string.codePointAt(i);
         int charCount = Character.charCount(cp);
         if (i + charCount > end) {
            break;
         }

         i += charCount;
         if (!this.charMap.containsKey(cp)) {
            this.loadCodepoint(cp);
         }
      }

      if (upload && this.atlasDirty) {
         this.texture.upload(this.atlasPixels.duplicate());
         this.atlasDirty = false;
      }
   }

   private Font.CharData charData(int cp) {
      Font.CharData data = this.charMap.get(cp);
      if (data != null) {
         return data;
      }

      this.loadCodepoint(cp);
      if (this.atlasDirty) {
         this.texture.upload(this.atlasPixels.duplicate());
         this.atlasDirty = false;
      }

      data = this.charMap.get(cp);
      return data != null ? data : this.charMap.get(32);
   }

   private void loadCodepoint(int cp) {
      if (!this.charMap.containsKey(cp)) {
         if (cp == 10 || cp == 13 || isZeroWidth(cp)) {
            this.loadWhitespace(cp, 0.0F);
         } else if (Character.isWhitespace(cp)) {
            this.loadWhitespace(cp, cp == 9 ? this.getAdvance(32) * 4.0F : this.getAdvance(cp));
         } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
               IntBuffer widthBuffer = stack.callocInt(1);
               IntBuffer heightBuffer = stack.callocInt(1);
               IntBuffer xOffsetBuffer = stack.callocInt(1);
               IntBuffer yOffsetBuffer = stack.callocInt(1);
               ByteBuffer glyphPixels = STBTruetype.stbtt_GetCodepointBitmap(
                  this.fontInfo, this.scale, this.scale, cp, widthBuffer, heightBuffer, xOffsetBuffer, yOffsetBuffer
               );
               float advance = this.getAdvance(cp);
               int glyphWidth = widthBuffer.get(0);
               int glyphHeight = heightBuffer.get(0);
               if (glyphPixels != null && glyphWidth > 0 && glyphHeight > 0) {
                  Font.AtlasSlot slot = this.reserve(glyphWidth + 4, glyphHeight + 4);
                  if (slot == null) {
                     this.charMap.put(cp, this.charMap.get(32));
                     STBTruetype.stbtt_FreeBitmap(glyphPixels);
                     return;
                  }

                  int atlasX = slot.x + 2;
                  int atlasY = slot.y + 2;

                  for (int row = 0; row < glyphHeight; row++) {
                     int sourceIndex = row * glyphWidth;
                     int targetIndex = (atlasY + row) * 4096 + atlasX;

                     for (int column = 0; column < glyphWidth; column++) {
                        this.atlasPixels.put(targetIndex + column, glyphPixels.get(sourceIndex + column));
                     }
                  }

                  float invSize = 2.4414062E-4F;
                  this.charMap
                     .put(
                        cp,
                        new Font.CharData(
                           xOffsetBuffer.get(0),
                           yOffsetBuffer.get(0),
                           xOffsetBuffer.get(0) + glyphWidth,
                           yOffsetBuffer.get(0) + glyphHeight,
                           atlasX * invSize,
                           atlasY * invSize,
                           (atlasX + glyphWidth) * invSize,
                           (atlasY + glyphHeight) * invSize,
                           advance,
                           false
                        )
                     );
                  this.atlasDirty = true;
                  STBTruetype.stbtt_FreeBitmap(glyphPixels);
                  return;
               }

               this.charMap.put(cp, new Font.CharData(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, advance, true));
               if (glyphPixels != null) {
                  STBTruetype.stbtt_FreeBitmap(glyphPixels);
               }
            }
         }
      }
   }

   private void loadWhitespace(int cp, float advance) {
      this.charMap.put(cp, new Font.CharData(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, advance, true));
   }

   private Font.AtlasSlot reserve(int width, int height) {
      if (width < 4096 && height < 4096) {
         if (this.cursorX + width >= 4096) {
            this.cursorX = 2;
            this.cursorY = this.cursorY + this.rowHeight + 2;
            this.rowHeight = 0;
         }

         if (this.cursorY + height >= 4096) {
            return null;
         }

         Font.AtlasSlot slot = new Font.AtlasSlot(this.cursorX, this.cursorY);
         this.cursorX += width + 2;
         this.rowHeight = Math.max(this.rowHeight, height);
         return slot;
      } else {
         return null;
      }
   }

   private float getAdvance(int cp) {
      try (MemoryStack stack = MemoryStack.stackPush()) {
         IntBuffer advance = stack.mallocInt(1);
         STBTruetype.stbtt_GetCodepointHMetrics(this.fontInfo, cp, advance, null);
         return advance.get(0) * this.scale;
      }
   }

   private float getKerning(int previous, int current) {
      return STBTruetype.stbtt_GetCodepointKernAdvance(this.fontInfo, previous, current) * this.scale;
   }

   private static boolean isZeroWidth(int cp) {
      return cp == 8203 || cp == 8204 || cp == 8205 || cp == 8288 || cp == 65279 || cp >= 65024 && cp <= 65039;
   }

   private record AtlasSlot(int x, int y) {
   }

   private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance, boolean empty) {
   }
}
