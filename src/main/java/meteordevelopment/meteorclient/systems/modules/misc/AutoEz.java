package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.player.Player;

public class AutoEz extends Module {
   private static final byte PLAYER_DEATH_EVENT = 3;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<String> customMessage = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("CustomMessage")
            .defaultValue("{selfname} Killed {playername}. Kill count: {killCount}")
            .description("death message")
            .build()
      );
   private final Setting<Boolean> dragonMode = this.sgGeneral
      .add(new BoolSetting.Builder().name("DragonMode").description("Enable Dragon Mode").defaultValue(false).build());
   private int killCount;
   private final String[] dragonMessages = new String[]{"一破-卧龙出山", "二连-一战成名", "三连-举世皆惊", "四连-天下无敌", "五连-诛天灭地"};
   private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
   private final Map<UUID, Long> lastTotemPopTickMap = new HashMap<>();

   public AutoEz() {
      super(Categories.Misc, "AutoEz", "Display a message when a player dies.");
   }

   @Override
   public void onActivate() {
      this.reset();
   }

   @Override
   public void onDeactivate() {
      this.reset();
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.reset();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.reset();
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (event.packet instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEntity(this.mc.level) instanceof Player player) {
               if (!player.getUUID().equals(this.mc.player.getUUID())) {
                  if (packet.getEventId() == 3) {
                     this.handlePlayerDeath(player);
                  } else if (packet.getEventId() == 35) {
                     this.handleTotemPop(player);
                  }
               }
            }
         }
      }
   }

   private void handlePlayerDeath(Player player) {
      this.killCount++;
      if (this.dragonMode.get()) {
         if (this.killCount <= this.dragonMessages.length) {
            ChatUtils.sendPlayerMsg(this.dragonMessages[this.killCount - 1] + " 您已被Lurk客户端击败");
         } else {
            ChatUtils.sendPlayerMsg(this.toChinese(this.killCount) + "连-诛天灭地  您已被Lurk客户端击败");
         }
      } else {
         ChatUtils.sendPlayerMsg(
            this.customMessage
               .get()
               .replace("{playername}", player.getName().getString())
               .replace("{killCount}", String.valueOf(this.killCount))
               .replace("{selfname}", this.mc.player.getName().getString())
         );
      }

      int pops = this.consumeTotemPops(player.getUUID());
      if (pops > 0) {
         ChatUtils.sendMsg(
            ChatFormatting.GRAY,
            "(highlight)%s (default)has died after popping (highlight)%d (default)%s.",
            player.getName().getString(),
            pops,
            pops == 1 ? "totem" : "totems"
         );
      } else {
         ChatUtils.sendMsg(ChatFormatting.GRAY, "(highlight)%s (default)died.", player.getName().getString());
      }
   }

   public int recordTotemPop(Player player) {
      if (player == null) {
         return 0;
      }

      UUID uuid = player.getUUID();
      long tick = this.mc.level != null ? this.mc.level.getGameTime() : player.tickCount;
      synchronized (this.totemPopMap) {
         if (this.lastTotemPopTickMap.getOrDefault(uuid, Long.MIN_VALUE) == tick) {
            return this.totemPopMap.getOrDefault(uuid, 0);
         }

         this.lastTotemPopTickMap.put(uuid, tick);
         int pops = this.totemPopMap.getOrDefault(uuid, 0) + 1;
         this.totemPopMap.put(uuid, pops);
         return pops;
      }
   }

   public int getTotemPops(UUID uuid) {
      synchronized (this.totemPopMap) {
         return this.totemPopMap.getOrDefault(uuid, 0);
      }
   }

   public boolean hasTotemPops(UUID uuid) {
      synchronized (this.totemPopMap) {
         return this.totemPopMap.containsKey(uuid);
      }
   }

   public int consumeTotemPops(UUID uuid) {
      synchronized (this.totemPopMap) {
         this.lastTotemPopTickMap.remove(uuid);
         return this.totemPopMap.removeInt(uuid);
      }
   }

   public void clearTotemPops() {
      synchronized (this.totemPopMap) {
         this.totemPopMap.clear();
         this.lastTotemPopTickMap.clear();
      }
   }

   private void handleTotemPop(Player player) {
      this.recordTotemPop(player);
   }

   private void reset() {
      this.killCount = 0;
      this.clearTotemPops();
   }

   private String toChinese(int number) {
      if (number <= 0) {
         return String.valueOf(number);
      } else {
         String[] digits = new String[]{"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
         if (number < 10) {
            return digits[number];
         } else if (number == 10) {
            return "十";
         } else if (number < 20) {
            return "十" + digits[number % 10];
         } else if (number < 100) {
            int ten = number / 10;
            int one = number % 10;
            return digits[ten] + "十" + (one == 0 ? "" : digits[one]);
         } else {
            return String.valueOf(number);
         }
      }
   }
}
