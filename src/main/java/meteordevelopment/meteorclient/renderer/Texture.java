package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class Texture extends AbstractTexture {
   private final boolean closeTexture;
   private final boolean closeSampler;

   public Texture(int width, int height, TextureFormat format, FilterMode min, FilterMode mag) {
      this(width, height, format, min, mag, AddressMode.REPEAT);
   }

   public Texture(int width, int height, TextureFormat format, FilterMode min, FilterMode mag, AddressMode addressMode) {
      this.texture = RenderSystem.getDevice().createTexture("", 15, format, width, height, 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getSampler(addressMode, addressMode, min, mag, false);
      this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
      this.closeTexture = true;
      this.closeSampler = false;
   }

   public Texture(GpuTexture texture, GpuTextureView textureView, GpuSampler sampler, boolean closeTexture, boolean closeSampler) {
      this.texture = texture;
      this.textureView = textureView;
      this.sampler = sampler;
      this.closeTexture = closeTexture;
      this.closeSampler = closeSampler;
   }

   @Override
   public void close() {
      if (this.closeSampler) {
         this.sampler.close();
      }

      if (this.closeTexture) {
         this.textureView.close();
         this.texture.close();
      }
   }

   public int getWidth() {
      return this.getTexture().getWidth(0);
   }

   public int getHeight() {
      return this.getTexture().getHeight(0);
   }

   public void upload(byte[] bytes) {
      this.upload(BufferUtils.createByteBuffer(bytes.length).put(bytes));
   }

   public void upload(ByteBuffer buffer) {
      NativeImage image = this.getImage();
      buffer.rewind();
      MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), image.getPointer(), buffer.remaining());
      RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, image);
      image.close();
   }

   @NotNull
   private NativeImage getImage() {
      Format imageFormat = switch (this.texture.getFormat()) {
         case RGBA8 -> Format.RGBA;
         case RED8 -> Format.LUMINANCE;
         default -> throw new IllegalArgumentException();
      };
      return new NativeImage(imageFormat, this.getWidth(), this.getHeight(), false);
   }

   public static Texture readResource(String path, boolean flipY, FilterMode filter) {
      try (InputStream in = Texture.class.getResourceAsStream(path)) {
         if (in == null) {
            return null;
         }

         ByteBuffer data = TextureUtil.readResource(in).rewind();

         try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);
            STBImage.stbi_set_flip_vertically_on_load(flipY);
            ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 4);
            Texture texture = new Texture(width.get(0), height.get(0), TextureFormat.RGBA8, filter, filter);
            texture.upload(image);
            STBImage.stbi_image_free(image);
            STBImage.stbi_set_flip_vertically_on_load(false);
            return texture;
         }
      } catch (IOException var16) {
         return null;
      }
   }
}
