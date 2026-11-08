package meteordevelopment.meteorclient.utils.network;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public class Capes {
   private static final String CAPE_OWNERS_URL = "https://meteorclient.com/api/capeowners";
   private static final String CAPES_URL = "https://meteorclient.com/api/capes";
   private static final Map<UUID, String> OWNERS = new HashMap<>();
   private static final Map<String, String> URLS = new HashMap<>();
   private static final Map<String, Capes.Cape> TEXTURES = new HashMap<>();
   private static final List<Capes.Cape> TO_REGISTER = new ArrayList<>();
   private static final List<Capes.Cape> TO_RETRY = new ArrayList<>();
   private static final List<Capes.Cape> TO_REMOVE = new ArrayList<>();

   private Capes() {
   }

   @PreInit(dependencies = MeteorExecutor.class)
   public static void init() {
      OWNERS.clear();
      URLS.clear();
      TEXTURES.clear();
      TO_REGISTER.clear();
      TO_RETRY.clear();
      TO_REMOVE.clear();
      MeteorExecutor.execute(
         () -> {
            Stream<String> lines = Http.get("https://meteorclient.com/api/capeowners")
               .exceptionHandler(e -> MeteorClient.LOG.error("Could not load capes: {}", e.getMessage()))
               .sendLines();
            if (lines != null) {
               lines.forEach(s -> {
                  String[] split = s.split(" ");
                  if (split.length >= 2) {
                     OWNERS.put(UUID.fromString(split[0]), split[1]);
                     if (!TEXTURES.containsKey(split[1])) {
                        TEXTURES.put(split[1], new Capes.Cape(split[1]));
                     }
                  }
               });
               lines = Http.get("https://meteorclient.com/api/capes").sendLines();
               if (lines != null) {
                  lines.forEach(s -> {
                     String[] split = s.split(" ");
                     if (split.length >= 2 && !URLS.containsKey(split[0])) {
                        URLS.put(split[0], split[1]);
                     }
                  });
               }
            }
         }
      );
      MeteorClient.EVENT_BUS.subscribe(Capes.class);
   }

   @EventHandler
   private static void onTick(TickEvent.Post event) {
      synchronized (TO_REGISTER) {
         for (Capes.Cape cape : TO_REGISTER) {
            cape.register();
         }

         TO_REGISTER.clear();
      }

      synchronized (TO_RETRY) {
         TO_RETRY.removeIf(Capes.Cape::tick);
      }

      synchronized (TO_REMOVE) {
         for (Capes.Cape cape : TO_REMOVE) {
            URLS.remove(cape.name);
            TEXTURES.remove(cape.name);
            TO_REGISTER.remove(cape);
            TO_RETRY.remove(cape);
         }

         TO_REMOVE.clear();
      }
   }

   public static Identifier get(Player player) {
      String capeName = OWNERS.get(player.getUUID());
      if (capeName != null) {
         Capes.Cape cape = TEXTURES.get(capeName);
         if (cape == null) {
            return null;
         }

         if (cape.isDownloaded()) {
            return cape.getIdentifier();
         }

         cape.download();
         return null;
      } else {
         return null;
      }
   }

   private static class Cape {
      private static int COUNT = 0;
      private final String name;
      private final Identifier identifier = MeteorClient.identifier("capes/" + COUNT++);
      private boolean downloaded;
      private boolean downloading;
      private NativeImage img;
      private int retryTimer;

      public Cape(String name) {
         this.name = name;
      }

      public Identifier getIdentifier() {
         return this.identifier;
      }

      public void download() {
         if (!this.downloaded && !this.downloading && this.retryTimer <= 0) {
            this.downloading = true;
            MeteorExecutor.execute(() -> {
               try {
                  String url = Capes.URLS.get(this.name);
                  if (url == null) {
                     synchronized (Capes.TO_REMOVE) {
                        Capes.TO_REMOVE.add(this);
                        this.downloading = false;
                        return;
                     }
                  }

                  InputStream in = Http.get(url).sendInputStream();
                  if (in == null) {
                     synchronized (Capes.TO_RETRY) {
                        Capes.TO_RETRY.add(this);
                        this.retryTimer = 200;
                        this.downloading = false;
                        return;
                     }
                  }

                  this.img = NativeImage.read(in);
                  synchronized (Capes.TO_REGISTER) {
                     Capes.TO_REGISTER.add(this);
                  }
               } catch (IOException e) {
                  MeteorClient.LOG.error("Failed to download cape '{}'", this.name, e);
               }
            });
         }
      }

      public void register() {
         MeteorClient.mc.getTextureManager().register(this.identifier, new DynamicTexture(null, this.img));
         this.img = null;
         this.downloading = false;
         this.downloaded = true;
      }

      public boolean tick() {
         if (this.retryTimer > 0) {
            this.retryTimer--;
            return false;
         } else {
            this.download();
            return true;
         }
      }

      public boolean isDownloaded() {
         return this.downloaded;
      }
   }
}
