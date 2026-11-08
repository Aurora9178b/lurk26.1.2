package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Locale;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.mixininterface.IChatListener;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Ignore extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<String>> playerList = this.sgGeneral
      .add(new StringListSetting.Builder().name("player-list").description("Players whose chat messages should be hidden.").build());

   public Ignore() {
      super(Categories.Misc, "Ignore", "Hides chat messages from selected players.");
   }

   @EventHandler
   private void onReceiveMessage(ReceiveMessageEvent event) {
      if (!this.playerList.get().isEmpty()) {
         GameProfile sender = ((IChatListener)this.mc.getChatListener()).meteor$getSender();
         if (sender != null && this.isIgnored(sender.name())) {
            event.cancel();
         } else {
            String playerName = this.extractPlayerName(event.getMessage().getString());
            if (!playerName.isEmpty() && this.isIgnored(playerName)) {
               event.cancel();
            }
         }
      }
   }

   private boolean isIgnored(String name) {
      for (String ignored : this.playerList.get()) {
         if (ignored != null && ignored.trim().equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   private String extractPlayerName(String message) {
      if (message != null && !message.isBlank()) {
         int start = message.indexOf(60);
         int end = message.indexOf(62, start + 1);
         if (start >= 0 && end > start) {
            String name = message.substring(start + 1, end);
            if (this.isValidName(name)) {
               return name;
            }
         }

         String lower = message.toLowerCase(Locale.ROOT);

         for (String ignored : this.playerList.get()) {
            if (ignored != null) {
               String name = ignored.trim();
               if (this.isValidName(name)) {
                  int index = lower.indexOf(name.toLowerCase(Locale.ROOT));
                  if (index >= 0 && this.isNameBoundary(message, index, index + name.length())) {
                     return name;
                  }
               }
            }
         }

         return "";
      } else {
         return "";
      }
   }

   private boolean isValidName(String name) {
      return name != null && name.matches("[A-Za-z0-9_]{1,16}");
   }

   private boolean isNameBoundary(String text, int start, int end) {
      return (start <= 0 || !this.isNameChar(text.charAt(start - 1))) && (end >= text.length() || !this.isNameChar(text.charAt(end)));
   }

   private boolean isNameChar(char c) {
      return Character.isLetterOrDigit(c) || c == '_';
   }
}
