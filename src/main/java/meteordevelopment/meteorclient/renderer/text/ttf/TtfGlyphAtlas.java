package meteordevelopment.meteorclient.renderer.text.ttf;

import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Texture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public class TtfGlyphAtlas {
   private static final int SIZE = 512;
   private static final int GLYPH_GUTTER = 2;
   private static final int UV_INSET = 1;
   private static final AtomicInteger NEXT_TEXTURE_ID = new AtomicInteger();
   private final Texture texture;
   private final Identifier textureId = MeteorClient.identifier("ttf_atlas/" + NEXT_TEXTURE_ID.getAndIncrement());
   private int currentX;
   private int currentY;
   private int currentRowHeight;

   public TtfGlyphAtlas() {
      GpuTexture gpuTexture = RenderSystem.getDevice().createTexture(() -> "Meteor-TtfGlyphAtlas", 5, TextureFormat.RED8, 512, 512, 1, 1);
      GpuTextureView textureView = RenderSystem.getDevice().createTextureView(gpuTexture);
      GpuSampler sampler = RenderSystem.getDevice()
         .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
      this.texture = new Texture(gpuTexture, textureView, sampler, true, true);
      fillTextureWithTransparentDistance(gpuTexture);
      Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
   }

   private static void fillTextureWithTransparentDistance(GpuTexture texture) {
      ByteBuffer transparent = MemoryUtil.memAlloc(262144);

      try {
         MemoryUtil.memSet(MemoryUtil.memAddress(transparent), 255, 262144L);
         RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, transparent, Format.LUMINANCE, 0, 0, 0, 0, 512, 512);
      } finally {
         MemoryUtil.memFree(transparent);
      }
   }

   public TtfGlyphAtlas.GlyphUV appendGlyph(TtfGlyph glyph) {
      if (glyph.glyphData() == null) {
         return null;
      }

      int cellWidth = glyph.width() + 4;
      int cellHeight = glyph.height() + 4;
      if (this.currentX + cellWidth >= 512) {
         this.currentX = 0;
         this.currentY = this.currentY + this.currentRowHeight;
         this.currentRowHeight = 0;
      }

      if (this.currentY + cellHeight >= 512) {
         return null;
      }

      int glyphX = this.currentX + 2;
      int glyphY = this.currentY + 2;
      RenderSystem.getDevice()
         .createCommandEncoder()
         .writeToTexture(this.texture.getTexture(), glyph.glyphData(), Format.LUMINANCE, 0, 0, glyphX, glyphY, glyph.width(), glyph.height());
      TtfGlyphAtlas.GlyphUV uv = new TtfGlyphAtlas.GlyphUV(
         (glyphX + 1) / 512.0F, (glyphY + 1) / 512.0F, (glyphX + glyph.width() - 1) / 512.0F, (glyphY + glyph.height() - 1) / 512.0F
      );
      this.currentX += cellWidth;
      this.currentRowHeight = Math.max(this.currentRowHeight, cellHeight);
      return uv;
   }

   public Texture getTexture() {
      return this.texture;
   }

   public Identifier getTextureId() {
      return this.textureId;
   }

   public void destroy() {
      Minecraft.getInstance().getTextureManager().release(this.textureId);
   }

   public record GlyphUV(float u0, float v0, float u1, float v1) {
   }
}
