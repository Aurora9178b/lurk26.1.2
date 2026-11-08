package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;

public class PlayerNotifier extends Module {
   private final Map<UUID, String> uuidNameCache = new ConcurrentHashMap<>();
   private boolean loginPacket = true;

   public PlayerNotifier() {
      super(Categories.Misc, "PlayerNotifier", "Notifies when players join or leave.");
   }

   @Override
   public void onActivate() {
      this.uuidNameCache.clear();
      this.cacheCurrentPlayers();
   }

   @Override
   public void onDeactivate() {
      this.uuidNameCache.clear();
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.loginPacket = true;
      this.uuidNameCache.clear();
      this.cacheCurrentPlayers();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.loginPacket = true;
      this.uuidNameCache.clear();
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      switch (event.packet) {
         case ClientboundPlayerInfoUpdatePacket packet:
            if (!packet.actions().contains(Action.ADD_PLAYER)) {
               return;
            }

            if (this.loginPacket) {
               this.loginPacket = false;
               this.cacheEntries(packet);
               return;
            }

            for (Entry entry : packet.newEntries()) {
               GameProfile profile = entry.profile();
               if (this.valid(profile)) {
                  this.uuidNameCache.put(profile.id(), profile.name());
                  this.sendJoin(profile.name());
               }
            }
            break;
         case ClientboundPlayerInfoRemovePacket packet:
            label55: {
               if (this.mc.getConnection() == null) {
                  return;
               }

               for (UUID uuid : packet.profileIds()) {
                  String name = this.uuidNameCache.get(uuid);
                  if (name == null) {
                     PlayerInfo info = this.mc.getConnection().getPlayerInfo(uuid);
                     if (info != null && this.valid(info.getProfile())) {
                        name = info.getProfile().name();
                     }
                  }

                  if (name != null && name.length() > 2) {
                     this.sendLeave(name);
                     this.uuidNameCache.remove(uuid);
                  }
               }
               break label55;
            }
         default:
      }
   }

   private void cacheEntries(ClientboundPlayerInfoUpdatePacket packet) {
      for (Entry entry : packet.newEntries()) {
         GameProfile profile = entry.profile();
         if (this.valid(profile)) {
            this.uuidNameCache.put(profile.id(), profile.name());
         }
      }
   }

   private void cacheCurrentPlayers() {
      if (this.mc.getConnection() != null) {
         for (PlayerInfo info : this.mc.getConnection().getOnlinePlayers()) {
            GameProfile profile = info.getProfile();
            if (this.valid(profile)) {
               this.uuidNameCache.put(profile.id(), profile.name());
            }
         }
      }
   }

   private boolean valid(GameProfile profile) {
      return profile != null && profile.id() != null && profile.name() != null && profile.name().length() > 2;
   }

   private void sendJoin(String name) {
      if (this.mc != null && this.mc.player != null) {
         this.mc
            .execute(
               () -> {
                  if (this.mc.player != null) {
                     this.mc
                        .player
                        .sendSystemMessage(
                           Component.literal("[+]").withStyle(ChatFormatting.GREEN).append(Component.literal(" " + name).withStyle(ChatFormatting.WHITE))
                        );
                  }
               }
            );
      }
   }

   private void sendLeave(String name) {
      if (this.mc != null && this.mc.player != null) {
         this.mc
            .execute(
               () -> {
                  if (this.mc.player != null) {
                     this.mc
                        .player
                        .sendSystemMessage(
                           Component.literal("[-]").withStyle(ChatFormatting.RED).append(Component.literal(" " + name).withStyle(ChatFormatting.WHITE))
                        );
                  }
               }
            );
      }
   }
}
