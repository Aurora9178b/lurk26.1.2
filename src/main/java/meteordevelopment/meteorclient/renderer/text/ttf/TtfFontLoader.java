package meteordevelopment.meteorclient.renderer.text.ttf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import org.lwjgl.stb.STBTruetype;

public class TtfFontLoader {
   private static final AtomicInteger WORKER_ID = new AtomicInteger();
   private static final ExecutorService GLYPH_WORKER = Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
         Thread thread = new Thread(runnable, "Meteor TTF Glyph Worker " + TtfFontLoader.WORKER_ID.incrementAndGet());
         thread.setDaemon(true);
         return thread;
      }
   });
   public final TtfFontFile fontFile;
   private final HashMap<Character, TtfGlyphDescriptor> glyphMap = new HashMap<>();
   private final HashMap<Character, CompletableFuture<TtfGlyph>> pendingGlyphs = new HashMap<>();
   private final List<TtfGlyphAtlas> atlases = new ArrayList<>();
   private TtfGlyphAtlas currentAtlas;

   public TtfFontLoader(FontFace fontFace) throws IOException {
      this.fontFile = new TtfFontFile(fontFace, 64, 6);
   }

   public void checkAndLoadChar(char ch) {
      if (!this.glyphMap.containsKey(ch)) {
         CompletableFuture<TtfGlyph> pending = this.pendingGlyphs.remove(ch);

         TtfGlyph glyph;
         try {
            glyph = pending != null ? pending.join() : this.fontFile.generateGlyph(ch);
         } catch (RuntimeException ignored) {
            glyph = this.fontFile.generateGlyph(ch);
         }

         this.appendGlyph(ch, glyph);
      }
   }

   public void requestChars(String chars) {
      this.drainReadyGlyphs();

      for (int i = 0; i < chars.length(); i++) {
         char ch = chars.charAt(i);
         if (ch != ' ' && ch != '\n' && !this.glyphMap.containsKey(ch) && !this.pendingGlyphs.containsKey(ch)) {
            this.pendingGlyphs.put(ch, CompletableFuture.supplyAsync(() -> this.fontFile.generateGlyph(ch), GLYPH_WORKER));
         }
      }
   }

   public void drainReadyGlyphs() {
      if (!this.pendingGlyphs.isEmpty()) {
         List<Character> readyChars = null;

         for (Entry<Character, CompletableFuture<TtfGlyph>> entry : this.pendingGlyphs.entrySet()) {
            if (entry.getValue().isDone()) {
               if (readyChars == null) {
                  readyChars = new ArrayList<>();
               }

               readyChars.add(entry.getKey());
            }
         }

         if (readyChars != null) {
            for (char ch : readyChars) {
               CompletableFuture<TtfGlyph> future = this.pendingGlyphs.remove(ch);
               if (future != null && !future.isCompletedExceptionally() && !future.isCancelled()) {
                  this.appendGlyph(ch, future.join());
               }
            }
         }
      }
   }

   private void appendGlyph(char ch, TtfGlyph glyph) {
      if (glyph != null && glyph.glyphData() != null) {
         if (this.currentAtlas == null) {
            this.createNewAtlas();
         }

         TtfGlyphAtlas.GlyphUV uv = this.currentAtlas.appendGlyph(glyph);
         if (uv == null) {
            this.createNewAtlas();
            uv = this.currentAtlas.appendGlyph(glyph);
         }

         if (uv != null) {
            this.glyphMap
               .put(ch, new TtfGlyphDescriptor(this.currentAtlas, uv, glyph.width(), glyph.height(), glyph.xOffset(), glyph.yOffset(), glyph.advance()));
         }

         this.freeGlyph(glyph);
      }
   }

   private void createNewAtlas() {
      this.currentAtlas = new TtfGlyphAtlas();
      this.atlases.add(this.currentAtlas);
   }

   public void checkAndLoadChars(String chars) {
      for (char ch : chars.toCharArray()) {
         this.checkAndLoadChar(ch);
      }
   }

   public void destroy() {
      this.fontFile.destroy();

      for (TtfGlyphAtlas atlas : this.atlases) {
         atlas.destroy();
      }

      this.atlases.clear();
      this.glyphMap.clear();

      for (CompletableFuture<TtfGlyph> future : this.pendingGlyphs.values()) {
         if (future.isDone() && !future.isCompletedExceptionally() && !future.isCancelled()) {
            this.freeGlyph(future.join());
         } else {
            future.cancel(false);
         }
      }

      this.pendingGlyphs.clear();
   }

   private void freeGlyph(TtfGlyph glyph) {
      if (glyph != null && glyph.glyphData() != null) {
         STBTruetype.stbtt_FreeSDF(glyph.glyphData());
      }
   }

   public TtfGlyphDescriptor getGlyph(char ch) {
      return this.glyphMap.get(ch);
   }
}
