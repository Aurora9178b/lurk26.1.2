package meteordevelopment.meteorclient.utils.music;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class MusicManager {
   private static final int MAX_IMAGE_TEXTURES = 64;
   private static final long RETRY_DELAY_MS = 5000L;
   private static final Object IMAGE_LOCK = new Object();
   private static final LinkedHashMap<String, Identifier> imageTextures = new LinkedHashMap<>(16, 0.75F, true);
   private static final Set<String> loadingImages = new HashSet<>();
   private static final Map<String, Long> failedImages = new HashMap<>();

   private MusicManager() {
   }

   public static Identifier imageTexture(String url, Runnable onLoaded) {
      if (url != null && !url.isBlank()) {
         String imageUrl = thumbnailUrl(url);
         synchronized (IMAGE_LOCK) {
            Identifier texture = imageTextures.get(imageUrl);
            if (texture != null) {
               return texture;
            }

            long now = System.currentTimeMillis();
            if (loadingImages.contains(imageUrl) || now - failedImages.getOrDefault(imageUrl, 0L) < 5000L) {
               return null;
            }

            loadingImages.add(imageUrl);
         }

         MeteorExecutor.execute(() -> loadImage(imageUrl, onLoaded));
         return null;
      } else {
         return null;
      }
   }

   private static void loadImage(String imageUrl, Runnable onLoaded) {
      NativeImage image = null;

      try (InputStream stream = CloudMusicApi.image(imageUrl)) {
         image = readNativeImage(stream);
         NativeImage finalImage = image;
         image = null;
         MeteorClient.mc.execute(() -> registerImage(imageUrl, finalImage, onLoaded));
      } catch (Exception e) {
         if (image != null) {
            image.close();
         }

         synchronized (IMAGE_LOCK) {
            loadingImages.remove(imageUrl);
            failedImages.put(imageUrl, System.currentTimeMillis());
         }

         MeteorClient.LOG.warn("Failed to load cloud music image from {}.", imageUrl, e);
      }
   }

   private static void registerImage(String imageUrl, NativeImage image, Runnable onLoaded) {
      List<Identifier> evicted = new ArrayList<>();
      synchronized (IMAGE_LOCK) {
         loadingImages.remove(imageUrl);
         failedImages.remove(imageUrl);
         Identifier previous = imageTextures.remove(imageUrl);
         if (previous != null) {
            evicted.add(previous);
         }

         Identifier id = MeteorClient.identifier("music-manager/image-" + Integer.toUnsignedString(imageUrl.hashCode()) + "-" + System.nanoTime());
         imageTextures.put(imageUrl, id);
         MeteorClient.mc.getTextureManager().register(id, new DynamicTexture(null, image));

         while (imageTextures.size() > 64) {
            Entry<String, Identifier> eldest = imageTextures.entrySet().iterator().next();
            evicted.add(eldest.getValue());
            imageTextures.remove(eldest.getKey());
         }
      }

      for (Identifier texture : evicted) {
         MeteorClient.mc.getTextureManager().release(texture);
      }

      if (onLoaded != null) {
         onLoaded.run();
      }
   }

   private static NativeImage readNativeImage(InputStream stream) throws IOException {
      byte[] bytes = stream.readAllBytes();
      if (isPng(bytes)) {
         return NativeImage.read(new ByteArrayInputStream(bytes));
      }

      BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null) {
         return NativeImage.read(new ByteArrayInputStream(bytes));
      }

      try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
         return !ImageIO.write(image, "png", out)
            ? NativeImage.read(new ByteArrayInputStream(bytes))
            : NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
      }
   }

   private static boolean isPng(byte[] bytes) {
      return bytes.length >= 8
         && bytes[0] == -119
         && bytes[1] == 80
         && bytes[2] == 78
         && bytes[3] == 71
         && bytes[4] == 13
         && bytes[5] == 10
         && bytes[6] == 26
         && bytes[7] == 10;
   }

   private static String thumbnailUrl(String url) {
      if (url == null || url.isBlank()) {
         return url;
      }

      if (!url.contains("music.126.net") && !url.contains("music.163.com")) {
         return url;
      }

      int param = url.indexOf("?param=");
      return param >= 0 ? url.substring(0, param) + "?param=180y180" : url + (url.contains("?") ? "&" : "?") + "param=180y180";
   }

   static {
      ImageIO.setUseCache(false);
   }
}
