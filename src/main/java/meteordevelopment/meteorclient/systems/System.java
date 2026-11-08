package meteordevelopment.meteorclient.systems;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.files.StreamUtils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import net.minecraft.ReportedException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.commons.io.FilenameUtils;

public abstract class System<T> implements ISerializable<T> {
   private final String name;
   private File file;
   protected boolean isFirstInit;
   private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

   public System(String name) {
      this.name = name;
      if (name != null) {
         this.file = new File(MeteorClient.FOLDER, name + ".nbt");
         this.isFirstInit = !this.file.exists();
      }
   }

   public void init() {
   }

   public void save(File folder) {
      File file = this.getFile();
      if (file != null) {
         CompoundTag tag = this.toTag();
         if (tag != null) {
            try {
               File tempFile = File.createTempFile("meteor-client", file.getName());
               NbtIo.write(tag, tempFile.toPath());
               if (folder != null) {
                  file = new File(folder, file.getName());
               }

               file.getParentFile().mkdirs();

               try {
                  Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
               } catch (AtomicMoveNotSupportedException var6) {
                  StreamUtils.copy(tempFile, file);
               }

               tempFile.delete();
            } catch (IOException e) {
               MeteorClient.LOG.error("Error saving {}. Possibly corrupted?", this.name, e);
            }
         }
      }
   }

   public void save() {
      this.save(null);
   }

   public void load(File folder) {
      File file = this.getFile();
      if (file != null) {
         try {
            if (folder != null) {
               file = new File(folder, file.getName());
            }

            if (file.exists()) {
               try {
                  this.fromTag(NbtIo.read(file.toPath()));
               } catch (ReportedException e) {
                  String backupName = FilenameUtils.removeExtension(file.getName()) + "-" + ZonedDateTime.now().format(DATE_TIME_FORMATTER) + ".backup.nbt";
                  File backup = new File(file.getParentFile(), backupName);

                  try {
                     Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                  } catch (AtomicMoveNotSupportedException var7) {
                     StreamUtils.copy(file, backup);
                  }

                  MeteorClient.LOG.error("Error loading {}. Possibly corrupted?", this.name, e);
                  MeteorClient.LOG.info("Saved settings backup to '{}'.", backup);
               }
            }
         } catch (IOException e) {
            MeteorClient.LOG.error("Error loading {}. Possibly corrupted?", this.name, e);
         }
      }
   }

   public void load() {
      this.load(null);
   }

   public File getFile() {
      return this.file;
   }

   public String getName() {
      return this.name;
   }

   @Override
   public CompoundTag toTag() {
      return null;
   }

   @Override
   public T fromTag(CompoundTag tag) {
      return null;
   }
}
