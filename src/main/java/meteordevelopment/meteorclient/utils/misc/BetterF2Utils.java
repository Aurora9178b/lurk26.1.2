package meteordevelopment.meteorclient.utils.misc;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterF2;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent.OpenFile;
import net.minecraft.network.chat.contents.TranslatableContents;

public class BetterF2Utils {
   public static Consumer<Component> wrapScreenshotCallback(Consumer<Component> original) {
      return original instanceof BetterF2Utils.ScreenshotCallback ? original : new BetterF2Utils.ScreenshotCallback(original);
   }

   public static Consumer<NativeImage> wrapNativeImageCallback(Consumer<NativeImage> original) {
      return original instanceof BetterF2Utils.NativeImageCallback ? original : new BetterF2Utils.NativeImageCallback(original);
   }

   private static File getScreenshotFile(Component message) {
      if (message.getContents() instanceof TranslatableContents contents) {
         if (!"screenshot.success".equals(contents.getKey())) {
            return null;
         }

         Object[] args = contents.getArgs();

         for (Object arg : args) {
            if (arg instanceof Component component) {
               File file = findOpenFile(component);
               if (file != null) {
                  return file;
               }
            }
         }

         return findOpenFile(message);
      } else {
         return findOpenFile(message);
      }
   }

   private static File findOpenFile(Component component) {
      if (component.getStyle().getClickEvent() instanceof OpenFile openFile) {
         return openFile.file();
      } else {
         for (Component sibling : component.getSiblings()) {
            File file = findOpenFile(sibling);
            if (file != null) {
               return file;
            }
         }

         return null;
      }
   }

   private static void copyImageToClipboard(NativeImage nativeImage) {
      try {
         Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new BetterF2Utils.ImageTransferable(toBufferedImage(nativeImage)), null);
      } catch (Throwable t) {
         MeteorClient.LOG.warn("Failed to copy screenshot to clipboard.", t);
      }
   }

   private static void copyImageFileToClipboard(File file) {
      if (file != null && file.isFile()) {
         if (isWindows()) {
            MeteorExecutor.execute(() -> copyImageFileToClipboardWindows(file));
         } else {
            try {
               Toolkit.getDefaultToolkit()
                  .getSystemClipboard()
                  .setContents(new BetterF2Utils.ImageTransferable(Toolkit.getDefaultToolkit().createImage(file.getAbsolutePath())), null);
            } catch (Throwable t) {
               MeteorClient.LOG.warn("Failed to copy screenshot file to clipboard.", t);
            }
         }
      }
   }

   private static void copyImageFileToClipboardWindows(File file) {
      String script = "Add-Type -AssemblyName System.Windows.Forms\nAdd-Type -AssemblyName System.Drawing\n$image = [System.Drawing.Image]::FromFile($env:METEOR_SCREENSHOT_FILE)\ntry {\n    [System.Windows.Forms.Clipboard]::SetImage($image)\n} finally {\n    $image.Dispose()\n}\n";

      try {
         ProcessBuilder processBuilder = new ProcessBuilder("powershell", "-NoProfile", "-Sta", "-ExecutionPolicy", "Bypass", "-Command", script);
         processBuilder.environment().put("METEOR_SCREENSHOT_FILE", file.getAbsolutePath());
         processBuilder.start();
      } catch (Throwable t) {
         MeteorClient.LOG.warn("Failed to copy screenshot file to clipboard through PowerShell.", t);
      }
   }

   private static boolean isWindows() {
      return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
   }

   private static BufferedImage toBufferedImage(NativeImage nativeImage) {
      int width = nativeImage.getWidth();
      int height = nativeImage.getHeight();
      BufferedImage image = new BufferedImage(width, height, 2);

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            image.setRGB(x, y, nativeImage.getPixel(x, y));
         }
      }

      return image;
   }

   private record ImageTransferable(Image image) implements Transferable {
      @Override
      public DataFlavor[] getTransferDataFlavors() {
         return new DataFlavor[]{DataFlavor.imageFlavor};
      }

      @Override
      public boolean isDataFlavorSupported(DataFlavor flavor) {
         return DataFlavor.imageFlavor.equals(flavor);
      }

      @Override
      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
         if (!this.isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
         } else {
            return this.image;
         }
      }
   }

   private record NativeImageCallback(Consumer<NativeImage> original) implements Consumer<NativeImage> {
      public void accept(NativeImage image) {
         if (Modules.get().isActive(BetterF2.class)) {
            BetterF2Utils.copyImageToClipboard(image);
         }

         this.original.accept(image);
      }
   }

   private record ScreenshotCallback(Consumer<Component> original) implements Consumer<Component> {
      public void accept(Component message) {
         this.original.accept(message);
         if (Modules.get().isActive(BetterF2.class)) {
            File screenshot = BetterF2Utils.getScreenshotFile(message);
            if (screenshot != null) {
               BetterF2Utils.copyImageFileToClipboard(screenshot);
            }
         }
      }
   }
}
