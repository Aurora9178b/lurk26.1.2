package meteordevelopment.meteorclient.utils.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import meteordevelopment.meteorclient.MeteorClient;
import org.apache.commons.io.IOUtils;

public class StreamUtils {
   private StreamUtils() {
   }

   public static void copy(File from, File to) {
      try (
         InputStream in = new FileInputStream(from);
         OutputStream out = new FileOutputStream(to);
      ) {
         in.transferTo(out);
      } catch (IOException e) {
         MeteorClient.LOG.error("Error copying from file '{}' to file '{}'.", from.getName(), to.getName(), e);
      }
   }

   public static void copy(InputStream in, File to) {
      try (OutputStream out = new FileOutputStream(to)) {
         in.transferTo(out);
      } catch (IOException var12) {
         MeteorClient.LOG.error("Error writing to file '{}'.", to.getName());
      } finally {
         IOUtils.closeQuietly(in);
      }
   }
}
