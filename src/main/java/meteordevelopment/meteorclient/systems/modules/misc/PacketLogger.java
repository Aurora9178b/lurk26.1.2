package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PacketLogger extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgOutput = this.settings.createGroup("Output");
   private final Setting<Set<Class<? extends Packet<?>>>> s2cPackets = this.sgGeneral
      .add(
         new PacketListSetting.Builder()
            .name("S2C-packets")
            .description("Server-to-client packets to log.")
            .filter(aClass -> PacketUtils.getS2CPackets().contains(aClass))
            .build()
      );
   private final Setting<Set<Class<? extends Packet<?>>>> c2sPackets = this.sgGeneral
      .add(
         new PacketListSetting.Builder()
            .name("C2S-packets")
            .description("Client-to-server packets to log.")
            .filter(aClass -> PacketUtils.getC2SPackets().contains(aClass))
            .build()
      );
   private final Setting<Boolean> showTimestamp = this.sgOutput
      .add(new BoolSetting.Builder().name("show-timestamp").description("Show timestamp for each logged packet.").defaultValue(true).build());
   private final Setting<Boolean> showPacketData = this.sgOutput
      .add(new BoolSetting.Builder().name("show-packet-data").description("Show the packet's toString() data for debugging.").defaultValue(false).build());
   private final Setting<Boolean> showCount = this.sgOutput
      .add(new BoolSetting.Builder().name("show-count").description("Show how many times each packet type has been logged.").defaultValue(true).build());
   private final Setting<Boolean> showSummary = this.sgOutput
      .add(new BoolSetting.Builder().name("show-summary").description("Show final packet count summary when module is deactivated.").defaultValue(true).build());
   private final Setting<Boolean> logToChat = this.sgOutput
      .add(new BoolSetting.Builder().name("log-to-chat").description("Log packets to chat.").defaultValue(true).build());
   private final Setting<Boolean> logToFile = this.sgOutput
      .add(new BoolSetting.Builder().name("log-to-file").description("Save packet logs to a file in the meteor-client folder.").defaultValue(false).build());
   private final Setting<Integer> flushInterval = this.sgOutput
      .add(
         new IntSetting.Builder()
            .name("flush-interval")
            .description("How often to flush logs to disk (in seconds).")
            .defaultValue(1)
            .min(1)
            .sliderMax(10)
            .visible(this.logToFile::get)
            .build()
      );
   private final Setting<Integer> maxFileSizeMB = this.sgOutput
      .add(
         new IntSetting.Builder()
            .name("max-file-size-mb")
            .description("Maximum size per log file in MB. Creates new file when exceeded.")
            .defaultValue(10)
            .min(1)
            .sliderMax(100)
            .visible(this.logToFile::get)
            .build()
      );
   private final Setting<Integer> maxTotalLogsMB = this.sgOutput
      .add(
         new IntSetting.Builder()
            .name("max-total-logs-mb")
            .description("Maximum total disk space for all packet logs in MB. Deletes oldest when exceeded.")
            .defaultValue(50)
            .min(1)
            .sliderMax(500)
            .visible(this.logToFile::get)
            .build()
      );
   private static final Path PACKET_LOGS_DIR = MeteorClient.FOLDER.toPath().resolve("packet-logs");
   private static final int LINE_SEPARATOR_BYTES = System.lineSeparator().getBytes(StandardCharsets.UTF_8).length;
   private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
   private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
   private final Reference2IntOpenHashMap<Class<? extends Packet<?>>> packetCounts = new Reference2IntOpenHashMap<>();
   private @Nullable BufferedWriter fileWriter;
   private long lastFlushMs;
   private long currentFileSizeBytes;
   private int currentFileIndex;
   private @Nullable LocalDateTime sessionStartTime;

   public PacketLogger() {
      super(Categories.Misc, "packet-logger", "Allows you to log certain packets.");
      this.runInMainMenu = true;
   }

   @Override
   public void onActivate() {
      this.closeFileWriter();
      this.packetCounts.clear();
      this.lastFlushMs = System.currentTimeMillis();
      this.sessionStartTime = LocalDateTime.now();
      this.currentFileIndex = 0;
      this.currentFileSizeBytes = 0L;
      if (this.logToFile.get()) {
         try {
            Files.createDirectories(PACKET_LOGS_DIR);
            this.cleanupOldLogs();
            this.openNewLogFile();
         } catch (IOException e) {
            this.error("Failed to initialize packet logging: %s", e.getMessage());
            this.fileWriter = null;
         }
      }
   }

   @Override
   public void onDeactivate() {
      if (this.showSummary.get() && !this.packetCounts.isEmpty()) {
         this.logSummary();
      }

      this.closeFileWriter();
   }

   private void logPacket(String direction, Packet<?> packet) {
      if (this.logToChat.get() || this.logToFile.get()) {
         Class<? extends Packet<?>> packetClass = (Class<? extends Packet<?>>)packet.getClass();
         this.packetCounts.addTo(packetClass, 1);
         StringBuilder msg = new StringBuilder(128);
         if (this.showTimestamp.get()) {
            msg.append("[").append(LocalDateTime.now().format(TIME_FORMATTER)).append("] ");
         }

         msg.append(direction).append(" ").append(PacketUtils.getName(packetClass));
         if (this.showCount.get()) {
            msg.append(" (#").append(this.packetCounts.getInt(packetClass)).append(")");
         }

         if (this.showPacketData.get()) {
            msg.append("\n  Data: ").append(packet);
         }

         String line = msg.toString();
         if (this.logToChat.get()) {
            this.info(line);
         }

         if (this.logToFile.get()) {
            this.writeLine(line);
         }
      }
   }

   private void logSummary() {
      int totalPackets = this.packetCounts.values().intStream().sum();
      List<String> lines = new ArrayList<>();
      lines.add("--- SUMMARY ---");
      lines.add("Final packet counts (total " + totalPackets + "):");
      this.packetCounts
         .reference2IntEntrySet()
         .stream()
         .sorted((a, b) -> Integer.compare(b.getIntValue(), a.getIntValue()))
         .forEach(e -> lines.add("  %s: %d".formatted(PacketUtils.getName(e.getKey()), e.getIntValue())));

      for (String line : lines) {
         if (this.logToChat.get()) {
            this.info(line);
         }

         if (this.logToFile.get()) {
            this.writeLine(line);
         }
      }
   }

   private void writeLine(String line) {
      if (this.fileWriter != null) {
         try {
            int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + LINE_SEPARATOR_BYTES;
            if (this.currentFileSizeBytes + lineBytes > this.maxFileSizeMB.get().intValue() * 1024L * 1024L) {
               this.openNewLogFile();
            }

            this.fileWriter.write(line);
            this.fileWriter.newLine();
            this.currentFileSizeBytes += lineBytes;
            long now = System.currentTimeMillis();
            if (now - this.lastFlushMs >= this.flushInterval.get().intValue() * 1000L) {
               this.fileWriter.flush();
               this.lastFlushMs = now;
            }
         } catch (IOException e) {
            this.error("Failed to write to packet log file: %s. File logging disabled.", e.getMessage());
            this.closeFileWriter();
         }
      }
   }

   private void openNewLogFile() throws IOException {
      if (this.fileWriter != null) {
         this.fileWriter.close();
      }

      if (this.sessionStartTime == null) {
         this.sessionStartTime = LocalDateTime.now();
      }

      String fileName = "packets-%s-%d.log".formatted(this.sessionStartTime.format(FILE_NAME_FORMATTER), this.currentFileIndex++);
      this.fileWriter = Files.newBufferedWriter(PACKET_LOGS_DIR.resolve(fileName), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      this.currentFileSizeBytes = 0L;
      this.cleanupOldLogs();
   }

   private void closeFileWriter() {
      if (this.fileWriter != null) {
         try {
            this.fileWriter.flush();
            this.fileWriter.close();
         } catch (IOException var2) {
         }

         this.fileWriter = null;
      }
   }

   private void cleanupOldLogs() throws IOException {
      long maxBytes = this.maxTotalLogsMB.get().intValue() * 1024L * 1024L;
      List<PacketLogger.LogFileEntry> logFiles = new ArrayList<>();

      try (Stream<Path> stream = Files.list(PACKET_LOGS_DIR)) {
         for (Path p : stream.toList()) {
            String name = p.getFileName().toString();
            if (name.startsWith("packets-") && name.endsWith(".log")) {
               try {
                  logFiles.add(new PacketLogger.LogFileEntry(p, Files.size(p), Files.getLastModifiedTime(p).toMillis()));
               } catch (IOException var10) {
               }
            }
         }
      }

      logFiles.sort(Comparator.comparingLong(PacketLogger.LogFileEntry::lastModified));
      long totalSize = 0L;

      for (PacketLogger.LogFileEntry entry : logFiles) {
         totalSize += entry.size();
         if (totalSize > maxBytes) {
            Files.deleteIfExists(entry.path());
         }
      }
   }

   @EventHandler(priority = 201)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (this.s2cPackets.get().contains(event.packet.getClass())) {
         this.logPacket("<- S2C", event.packet);
      }
   }

   @EventHandler(priority = 201)
   private void onSendPacket(PacketEvent.Send event) {
      if (this.c2sPackets.get().contains(event.packet.getClass())) {
         this.logPacket("-> C2S", event.packet);
      }
   }

   private record LogFileEntry(Path path, long size, long lastModified) {
   }
}
