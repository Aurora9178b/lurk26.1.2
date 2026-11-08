package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class RandomSuffix extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<String>> suffixes = this.sgGeneral
      .add(
         new StringListSetting.Builder()
            .name("suffixes")
            .description("Random suffixes appended to sent messages.")
            .defaultValue("HL ON TOP", "LurkClient", "NovaClient", "BBBBBBBBBBB")
            .build()
      );

   public RandomSuffix() {
      super(Categories.Misc, "RandomSuffix", "Appends a random suffix to your chat messages.");
   }

   @EventHandler
   private void onMessageSend(SendMessageEvent event) {
      if (!this.suffixes.get().isEmpty()) {
         String suffix = this.suffixes.get().get(ThreadLocalRandom.current().nextInt(this.suffixes.get().size()));
         if (!suffix.isBlank()) {
            event.message = event.message + " " + suffix;
         }
      }
   }
}
