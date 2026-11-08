package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;

public class GamemodeNotifier extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgFilters = this.settings.createGroup("Filters");
   private final SettingGroup sgNotify = this.settings.createGroup("Notification");
   private final Setting<Integer> checkInterval = this.sgGeneral
      .add(
         new IntSetting.Builder().name("check-interval").description("Ticks between each check.").defaultValue(20).min(1).max(200).sliderRange(1, 200).build()
      );
   private final Setting<Boolean> ignoreSelf = this.sgFilters
      .add(new BoolSetting.Builder().name("ignore-self").description("Ignore your own game mode changes.").defaultValue(true).build());
   private final Setting<Boolean> ignoreFriends = this.sgFilters
      .add(new BoolSetting.Builder().name("ignore-friends").description("Ignore friends' game mode changes.").defaultValue(true).build());
   private final Setting<Boolean> notifySurvival = this.sgNotify
      .add(new BoolSetting.Builder().name("notify-survival").description("Notify when a player switches to survival mode.").defaultValue(true).build());
   private final Setting<Boolean> notifyCreative = this.sgNotify
      .add(new BoolSetting.Builder().name("notify-creative").description("Notify when a player switches to creative mode.").defaultValue(true).build());
   private final Setting<Boolean> notifyAdventure = this.sgNotify
      .add(new BoolSetting.Builder().name("notify-adventure").description("Notify when a player switches to adventure mode.").defaultValue(true).build());
   private final Setting<Boolean> notifySpectator = this.sgNotify
      .add(new BoolSetting.Builder().name("notify-spectator").description("Notify when a player switches to spectator mode.").defaultValue(true).build());
   private final Setting<Boolean> sendToChat = this.sgNotify
      .add(
         new BoolSetting.Builder()
            .name("send-to-chat")
            .description("Send the message as a public chat message instead of a client notification.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> playSound = this.sgNotify
      .add(new BoolSetting.Builder().name("play-sound").description("Play a sound when a player changes game mode.").defaultValue(true).build());
   private final Setting<String> chatMessageFormat = this.sgNotify
      .add(
         new StringSetting.Builder()
            .name("chat-message-format")
            .description("Format for public chat messages. Use {player} and {mode}.")
            .defaultValue("{player} is {mode} Mode.")
            .visible(this.sendToChat::get)
            .build()
      );
   private final Setting<String> clientMessageFormat = this.sgNotify
      .add(
         new StringSetting.Builder()
            .name("client-message-format")
            .description("Format for client notifications. Use {player} and {mode}.")
            .defaultValue("§7{player} is {mode} Mode")
            .visible(() -> !this.sendToChat.get())
            .build()
      );
   private final Map<UUID, GameType> lastGameModes = new HashMap<>();
   private final Map<UUID, Integer> chatIdMap = new HashMap<>();
   private int tickCounter;

   public GamemodeNotifier() {
      super(Categories.Misc, "GamemodeNotifier", "Notifies when players change game mode.");
   }

   @Override
   public void onActivate() {
      this.reset();
      this.cacheCurrentGameModes();
   }

   @Override
   public void onDeactivate() {
      this.reset();
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.reset();
      this.cacheCurrentGameModes();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.reset();
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.getConnection() != null) {
         this.tickCounter++;
         if (this.tickCounter >= this.checkInterval.get()) {
            this.tickCounter = 0;
            Set<UUID> currentUuids = new HashSet<>();

            for (PlayerInfo info : this.mc.getConnection().getOnlinePlayers()) {
               if (this.valid(info)) {
                  UUID uuid = info.getProfile().id();
                  currentUuids.add(uuid);
                  if ((!this.ignoreSelf.get() || !uuid.equals(this.mc.player.getUUID())) && (!this.ignoreFriends.get() || !Friends.get().isFriend(info))) {
                     GameType gameMode = info.getGameMode();
                     if (gameMode != null) {
                        GameType previous = this.lastGameModes.put(uuid, gameMode);
                        if (previous != null && previous != gameMode && this.shouldNotifyFor(gameMode)) {
                           this.sendNotification(info.getProfile().name(), gameMode, uuid);
                        }
                     }
                  }
               }
            }

            this.lastGameModes.keySet().retainAll(currentUuids);
            this.chatIdMap.keySet().retainAll(currentUuids);
         }
      }
   }

   private void cacheCurrentGameModes() {
      if (this.mc.getConnection() != null) {
         for (PlayerInfo info : this.mc.getConnection().getOnlinePlayers()) {
            if (this.valid(info) && info.getGameMode() != null) {
               this.lastGameModes.put(info.getProfile().id(), info.getGameMode());
            }
         }
      }
   }

   private boolean valid(PlayerInfo info) {
      return info != null && info.getProfile() != null && info.getProfile().id() != null && info.getProfile().name() != null;
   }

   private boolean shouldNotifyFor(GameType gameMode) {
      return switch (gameMode) {
         case SURVIVAL -> this.notifySurvival.get();
         case CREATIVE -> this.notifyCreative.get();
         case ADVENTURE -> this.notifyAdventure.get();
         case SPECTATOR -> this.notifySpectator.get();
         default -> false;
      };
   }

   private String modeName(GameType gameMode) {
      return switch (gameMode) {
         case SURVIVAL -> "Survival";
         case CREATIVE -> "Creative";
         case ADVENTURE -> "Adventure";
         case SPECTATOR -> "Spectator";
         default -> gameMode.getName();
      };
   }

   private void sendNotification(String playerName, GameType gameMode, UUID uuid) {
      String text = (this.sendToChat.get() ? this.chatMessageFormat.get() : this.clientMessageFormat.get())
         .replace("{player}", playerName)
         .replace("{mode}", this.modeName(gameMode));
      if (this.sendToChat.get()) {
         ChatUtils.sendPlayerMsg(text, false);
      } else {
         ChatUtils.sendMsg(this.chatIdMap.computeIfAbsent(uuid, UUID::hashCode), null, null, Component.literal(text));
      }

      if (this.playSound.get() && this.mc.player != null) {
         this.mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
      }
   }

   private void reset() {
      this.tickCounter = 0;
      this.lastGameModes.clear();
      this.chatIdMap.clear();
   }
}
