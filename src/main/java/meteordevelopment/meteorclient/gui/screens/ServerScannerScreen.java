package meteordevelopment.meteorclient.gui.screens;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.filechooser.FileSystemView;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class ServerScannerScreen extends WindowScreen {
   private static final int MIN_PORT = 0;
   private static final int MAX_PORT = 65535;
   private static final int TOTAL_PORTS = 65536;
   private static final int WORKERS = 128;
   private static final int CONNECT_TIMEOUT_MS = 450;
   private static final int READ_TIMEOUT_MS = 700;
   private static final int STATUS_PROTOCOL_VERSION = 767;
   private static final Color VALID_COLOR = new Color(90, 255, 120);
   private static String lastHost = "127.0.0.1";
   private final Object writerLock = new Object();
   private final Object finishLock = new Object();
   private final List<ServerScannerScreen.Result> results = Collections.synchronizedList(new ArrayList<>());
   private final AtomicInteger nextPort = new AtomicInteger(0);
   private final AtomicInteger scannedPorts = new AtomicInteger();
   private final AtomicInteger activeWorkers = new AtomicInteger();
   private WTextBox ipBox;
   private WLabel statusLabel;
   private WLabel foundLabel;
   private WLabel fileLabel;
   private WTable resultsTable;
   private WButton startButton;
   private WButton stopButton;
   private ExecutorService executor;
   private BufferedWriter writer;
   private Path outputFile;
   private volatile boolean scanning;
   private volatile boolean stopRequested;
   private volatile String currentHost = lastHost;
   private volatile String statusText = "Idle";
   private int renderedResultCount;

   public ServerScannerScreen(GuiTheme theme) {
      super(theme, "Server Scanner");
   }

   @Override
   public void initWidgets() {
      WTable settings = this.add(this.theme.table()).expandX().minWidth(460.0).widget();
      settings.add(this.theme.label("IP"));
      this.ipBox = settings.add(this.theme.textBox(this.currentHost.isBlank() ? lastHost : this.currentHost, "127.0.0.1")).minWidth(320.0).expandX().widget();
      settings.row();
      WLabel statusTitle = settings.add(this.theme.label("Status")).widget();
      statusTitle.color = this.theme.textSecondaryColor();
      this.statusLabel = settings.add(this.theme.label(this.statusText)).expandX().widget();
      settings.row();
      WLabel foundTitle = settings.add(this.theme.label("Found")).widget();
      foundTitle.color = this.theme.textSecondaryColor();
      this.foundLabel = settings.add(this.theme.label(Integer.toString(this.results.size()))).expandX().widget();
      settings.row();
      WLabel fileTitle = settings.add(this.theme.label("File")).widget();
      fileTitle.color = this.theme.textSecondaryColor();
      this.fileLabel = settings.add(this.theme.label(this.getOutputFile().toString(), 360.0)).expandX().widget();
      this.fileLabel.color = this.theme.textSecondaryColor();
      this.add(this.theme.horizontalSeparator()).expandX();
      WHorizontalList buttons = this.add(this.theme.horizontalList()).expandX().widget();
      this.startButton = buttons.add(this.theme.button("Start")).expandX().widget();
      this.startButton.action = this::startScan;
      this.stopButton = buttons.add(this.theme.button("Stop")).expandX().widget();
      this.stopButton.action = this::requestStop;
      WButton clearButton = buttons.add(this.theme.button("Clear")).expandX().widget();
      clearButton.action = () -> {
         if (!this.scanning) {
            this.results.clear();
            this.renderedResultCount = -1;
            this.refreshResultsTable();
            this.setStatus("Cleared");
         }
      };
      this.add(this.theme.horizontalSeparator("Valid Servers")).expandX();
      this.resultsTable = this.add(this.theme.table()).expandX().minWidth(460.0).widget();
      this.refreshResultsTable();
   }

   @Override
   public void tick() {
      String progress = this.scanning ? this.statusText + "  " + this.scannedPorts.get() + "/65536" : this.statusText;
      if (this.statusLabel != null && !this.statusLabel.get().equals(progress)) {
         this.statusLabel.set(progress);
      }

      if (this.foundLabel != null) {
         String found = Integer.toString(this.results.size());
         if (!this.foundLabel.get().equals(found)) {
            this.foundLabel.set(found);
         }
      }

      if (this.fileLabel != null) {
         String file = this.getOutputFile().toString();
         if (!this.fileLabel.get().equals(file)) {
            this.fileLabel.set(file);
         }
      }

      this.setButtonText(this.startButton, this.scanning ? "Scanning" : "Start");
      this.setButtonText(this.stopButton, this.scanning ? "Stop" : "Stopped");
      if (this.renderedResultCount != this.results.size()) {
         this.refreshResultsTable();
      }
   }

   @Override
   public void onClose() {
      this.requestStop();
      super.onClose();
   }

   @Override
   protected void onClosed() {
      this.requestStop();
      this.closeWriter();
   }

   private void startScan() {
      if (!this.scanning) {
         String host = this.normalizeHost(this.ipBox.get());
         if (host.isBlank()) {
            this.setStatus("IP is empty");
         } else {
            InetAddress address;
            try {
               address = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
               this.setStatus("Invalid IP");
               return;
            }

            this.currentHost = host;
            lastHost = host;
            this.results.clear();
            this.renderedResultCount = -1;
            this.refreshResultsTable();
            this.nextPort.set(0);
            this.scannedPorts.set(0);
            this.stopRequested = false;
            this.scanning = true;
            this.setStatus("Scanning " + host);

            try {
               this.openWriter();
            } catch (IOException e) {
               this.scanning = false;
               this.setStatus("Cannot write ip.txt");
               return;
            }

            this.activeWorkers.set(128);
            this.executor = Executors.newFixedThreadPool(128, new ServerScannerScreen.ScannerThreadFactory());

            for (int i = 0; i < 128; i++) {
               this.executor.execute(() -> {
                  try {
                     this.scanLoop(host, address);
                  } finally {
                     if (this.activeWorkers.decrementAndGet() == 0) {
                        this.finishScan();
                     }
                  }
               });
            }
         }
      }
   }

   private void scanLoop(String host, InetAddress address) {
      while (!this.stopRequested) {
         int port = this.nextPort.getAndIncrement();
         if (port > 65535) {
            return;
         }

         long started = System.currentTimeMillis();
         if (this.pingMinecraftStatus(host, address, port)) {
            long latency = Math.max(0L, System.currentTimeMillis() - started);
            ServerScannerScreen.Result result = new ServerScannerScreen.Result(port, latency);
            this.results.add(result);
            this.writeValid(host, result);
         }

         this.scannedPorts.incrementAndGet();
      }
   }

   private boolean pingMinecraftStatus(String host, InetAddress address, int port) {
      try (Socket socket = new Socket()) {
         socket.connect(new InetSocketAddress(address, port), 450);
         socket.setSoTimeout(700);
         DataOutputStream output = new DataOutputStream(socket.getOutputStream());
         this.writeHandshake(output, host, port);
         this.writeStatusRequest(output);
         output.flush();
         InputStream input = socket.getInputStream();
         int length = this.readVarInt(input);
         if (length <= 0 || length > 2097152) {
            return false;
         }

         int packetId = this.readVarInt(input);
         int jsonLength = this.readVarInt(input);
         return packetId == 0 && jsonLength > 0 && jsonLength <= length;
      } catch (IOException ignored) {
         return false;
      }
   }

   private void writeHandshake(DataOutputStream output, String host, int port) throws IOException {
      ByteArrayOutputStream packet = new ByteArrayOutputStream();
      this.writeVarInt(packet, 0);
      this.writeVarInt(packet, 767);
      this.writeString(packet, host);
      packet.write(port >>> 8 & 0xFF);
      packet.write(port & 0xFF);
      this.writeVarInt(packet, 1);
      this.writeVarInt(output, packet.size());
      packet.writeTo(output);
   }

   private void writeStatusRequest(DataOutputStream output) throws IOException {
      this.writeVarInt(output, 1);
      this.writeVarInt(output, 0);
   }

   private void writeString(ByteArrayOutputStream output, String value) throws IOException {
      byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      this.writeVarInt(output, bytes.length);
      output.write(bytes);
   }

   private void writeVarInt(ByteArrayOutputStream output, int value) {
      while ((value & -128) != 0) {
         output.write(value & 127 | 128);
         value >>>= 7;
      }

      output.write(value);
   }

   private void writeVarInt(DataOutputStream output, int value) throws IOException {
      while ((value & -128) != 0) {
         output.writeByte(value & 127 | 128);
         value >>>= 7;
      }

      output.writeByte(value);
   }

   private int readVarInt(InputStream input) throws IOException {
      int value = 0;
      int position = 0;

      do {
         int current = input.read();
         if (current == -1) {
            throw new IOException("Disconnected");
         }

         value |= (current & 127) << position;
         if ((current & 128) == 0) {
            return value;
         }

         position += 7;
      } while (position < 35);

      throw new IOException("VarInt too big");
   }

   private void requestStop() {
      if (this.scanning || this.executor != null) {
         this.stopRequested = true;
         this.setStatus("Stopping");
         ExecutorService service = this.executor;
         if (service != null) {
            service.shutdownNow();
         }
      }
   }

   private void finishScan() {
      synchronized (this.finishLock) {
         if (this.scanning) {
            this.scanning = false;
            this.stopRequested = false;
            ExecutorService service = this.executor;
            this.executor = null;
            if (service != null) {
               service.shutdownNow();
            }

            this.closeWriter();
            this.sortResults();
            this.setStatus(this.scannedPorts.get() >= 65536 ? "Done" : "Stopped");
         }
      }
   }

   private void openWriter() throws IOException {
      this.outputFile = this.getOutputFile();
      Files.createDirectories(this.outputFile.getParent());
      synchronized (this.writerLock) {
         this.closeWriter();
         this.writer = Files.newBufferedWriter(
            this.outputFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
         );
      }
   }

   private void closeWriter() {
      synchronized (this.writerLock) {
         if (this.writer != null) {
            try {
               this.writer.close();
            } catch (IOException var4) {
            }

            this.writer = null;
         }
      }
   }

   private void writeValid(String host, ServerScannerScreen.Result result) {
      synchronized (this.writerLock) {
         if (this.writer != null) {
            try {
               this.writer.write(host + ":" + result.port + " 有效");
               this.writer.newLine();
               this.writer.flush();
            } catch (IOException ignored) {
               this.setStatus("Write failed");
            }
         }
      }
   }

   private void refreshResultsTable() {
      if (this.resultsTable != null) {
         this.renderedResultCount = this.results.size();
         this.resultsTable.clear();
         if (this.results.isEmpty()) {
            WLabel empty = this.resultsTable.add(this.theme.label(this.scanning ? "Scanning..." : "No valid servers")).expandX().widget();
            empty.color = this.theme.textSecondaryColor();
         } else {
            List<ServerScannerScreen.Result> snapshot;
            synchronized (this.results) {
               snapshot = new ArrayList<>(this.results);
            }

            snapshot.sort(Comparator.comparingInt(resultx -> resultx.port));
            this.resultsTable.add(this.theme.label("Address"));
            this.resultsTable.add(this.theme.label("Latency"));
            this.resultsTable.row();

            for (ServerScannerScreen.Result result : snapshot) {
               WLabel address = this.resultsTable.add(this.theme.label(this.currentHost + ":" + result.port)).expandX().widget();
               address.color = VALID_COLOR;
               WLabel latency = this.resultsTable.add(this.theme.label(result.latency + "ms")).widget();
               latency.color = this.theme.textSecondaryColor();
               this.resultsTable.row();
            }
         }
      }
   }

   private void sortResults() {
      synchronized (this.results) {
         this.results.sort(Comparator.comparingInt(result -> result.port));
      }

      this.renderedResultCount = -1;
   }

   private Path getOutputFile() {
      if (this.outputFile != null) {
         return this.outputFile;
      }

      File desktop = FileSystemView.getFileSystemView().getHomeDirectory();
      if (desktop == null) {
         desktop = new File(System.getProperty("user.home"), "Desktop");
      }

      return desktop.toPath().resolve("ip.txt");
   }

   private String normalizeHost(String raw) {
      String host = raw == null ? "" : raw.trim();
      if (host.startsWith("[") && host.contains("]")) {
         return host.substring(1, host.indexOf(93)).trim();
      }

      int firstColon = host.indexOf(58);
      if (firstColon > 0 && firstColon == host.lastIndexOf(58)) {
         String possiblePort = host.substring(firstColon + 1);
         if (possiblePort.chars().allMatch(Character::isDigit)) {
            return host.substring(0, firstColon).trim();
         }
      }

      return host;
   }

   private void setStatus(String status) {
      this.statusText = status;
   }

   private void setButtonText(WButton button, String text) {
      if (button != null && !button.getText().equals(text)) {
         button.set(text);
      }
   }

   private record Result(int port, long latency) {
   }

   private static class ScannerThreadFactory implements ThreadFactory {
      private final AtomicInteger id = new AtomicInteger();

      @Override
      public Thread newThread(Runnable runnable) {
         Thread thread = new Thread(runnable, "Server Scanner " + this.id.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      }
   }
}
