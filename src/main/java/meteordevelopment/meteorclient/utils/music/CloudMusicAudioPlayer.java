package meteordevelopment.meteorclient.utils.music;

import java.io.BufferedInputStream;
import java.net.URI;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.DataLine.Info;
import meteordevelopment.meteorclient.MeteorClient;

public class CloudMusicAudioPlayer {
   private final Object pauseLock = new Object();
   private volatile Thread thread;
   private volatile SourceDataLine line;
   private volatile boolean stopRequested;
   private volatile boolean paused;
   private volatile boolean playing;
   private volatile int streamId;
   private volatile long playedFrames;
   private volatile float frameRate = 1.0F;

   public void play(String url, Runnable onFinished, CloudMusicAudioPlayer.ErrorHandler onError) {
      this.stop();
      int currentStreamId = ++this.streamId;
      this.stopRequested = false;
      this.paused = false;
      this.playing = true;
      this.playedFrames = 0L;
      this.frameRate = 1.0F;
      this.thread = new Thread(() -> {
         boolean finished = false;

         try {
            finished = this.stream(url, currentStreamId);
         } catch (Exception e) {
            if (!this.stopRequested && currentStreamId == this.streamId) {
               MeteorClient.LOG.error("Failed to play cloud music stream.", e);
               if (onError != null) {
                  onError.onError(e);
               }
            }
         } finally {
            if (currentStreamId == this.streamId) {
               this.playing = false;
               this.paused = false;
               this.line = null;
            }
         }

         if (finished && onFinished != null) {
            onFinished.run();
         }
      }, "CloudMusic-AudioPlayer");
      this.thread.setDaemon(true);
      this.thread.start();
   }

   public void pause() {
      if (this.playing && !this.paused) {
         this.paused = true;
         SourceDataLine current = this.line;
         if (current != null) {
            current.stop();
         }
      }
   }

   public void resume() {
      if (this.playing && this.paused) {
         this.paused = false;
         SourceDataLine current = this.line;
         if (current != null) {
            current.start();
         }

         synchronized (this.pauseLock) {
            this.pauseLock.notifyAll();
         }
      }
   }

   public void stop() {
      this.streamId++;
      this.stopRequested = true;
      this.paused = false;
      synchronized (this.pauseLock) {
         this.pauseLock.notifyAll();
      }

      this.closeLine();
      Thread current = this.thread;
      if (current != null) {
         current.interrupt();
      }

      this.thread = null;
      this.playing = false;
      this.playedFrames = 0L;
   }

   public boolean isPlaying() {
      return this.playing && !this.paused;
   }

   public boolean isPaused() {
      return this.playing && this.paused;
   }

   public boolean hasStream() {
      return this.playing;
   }

   public long positionMs() {
      float rate = this.frameRate;
      return rate <= 0.0F ? 0L : (long)(this.playedFrames * 1000.0 / rate);
   }

   private boolean stream(String url, int currentStreamId) throws Exception {
      SourceDataLine current = null;

      try (
         BufferedInputStream input = new BufferedInputStream(URI.create(url).toURL().openStream());
         AudioInputStream source = AudioSystem.getAudioInputStream(input);
         AudioInputStream decoded = AudioSystem.getAudioInputStream(this.decodedFormat(source.getFormat()), source);
      ) {
         AudioFormat format = decoded.getFormat();
         this.frameRate = format.getFrameRate() > 0.0F ? format.getFrameRate() : format.getSampleRate();
         Info info = new Info(SourceDataLine.class, format);
         current = (SourceDataLine)AudioSystem.getLine(info);
         this.line = current;
         current.open(format);
         current.start();
         byte[] buffer = new byte[8192];

         int read;
         while (!this.stopRequested && currentStreamId == this.streamId && (read = decoded.read(buffer, 0, buffer.length)) != -1) {
            this.waitIfPaused();
            if (!this.stopRequested && currentStreamId == this.streamId) {
               int written = current.write(buffer, 0, read);
               if (format.getFrameSize() > 0) {
                  this.playedFrames = this.playedFrames + written / format.getFrameSize();
               }
            }
         }

         if (!this.stopRequested && currentStreamId == this.streamId) {
            current.drain();
         }

         return !this.stopRequested && currentStreamId == this.streamId;
      } finally {
         if (this.line == current) {
            this.line = null;
         }

         this.closeLine(current);
      }
   }

   private AudioFormat decodedFormat(AudioFormat source) {
      return new AudioFormat(Encoding.PCM_SIGNED, source.getSampleRate(), 16, source.getChannels(), source.getChannels() * 2, source.getSampleRate(), false);
   }

   private void waitIfPaused() throws InterruptedException {
      synchronized (this.pauseLock) {
         while (this.paused && !this.stopRequested) {
            this.pauseLock.wait();
         }
      }
   }

   private void closeLine() {
      SourceDataLine current = this.line;
      this.line = null;
      this.closeLine(current);
   }

   private void closeLine(SourceDataLine current) {
      if (current != null) {
         current.stop();
         current.flush();
         current.close();
      }
   }

   @FunctionalInterface
   public interface ErrorHandler {
      void onError(Exception var1);
   }
}
