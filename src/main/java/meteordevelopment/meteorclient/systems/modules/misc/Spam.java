package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.List;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import org.apache.commons.lang3.RandomStringUtils;

public class Spam extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<String>> messages = this.sgGeneral
      .add(new StringListSetting.Builder().name("messages").description("Messages to use for spam.").defaultValue(List.of("Meteor on Crack!")).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The delay between specified messages in ticks.").defaultValue(20).min(0).sliderMax(200).build());
   private final Setting<Boolean> disableOnLeave = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-on-leave").description("Disables spam when you leave a server.").defaultValue(true).build());
   private final Setting<Boolean> disableOnDisconnect = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("disable-on-disconnect")
            .description("Disables spam when you are disconnected from a server.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> random = this.sgGeneral
      .add(new BoolSetting.Builder().name("randomise").description("Selects a random message from your spam message list.").defaultValue(false).build());
   private final Setting<Boolean> autoSplitMessages = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-split-messages")
            .description("Automatically split up large messages after a certain length")
            .defaultValue(false)
            .build()
      );
   private final Setting<Integer> splitLength = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("split-length")
            .description("The length after which to split messages in chat")
            .visible(this.autoSplitMessages::get)
            .defaultValue(256)
            .min(1)
            .sliderMax(256)
            .build()
      );
   private final Setting<Integer> autoSplitDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("split-delay")
            .description("The delay between split messages in ticks.")
            .visible(this.autoSplitMessages::get)
            .defaultValue(20)
            .min(0)
            .sliderMax(200)
            .build()
      );
   private final Setting<Boolean> bypass = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("bypass")
            .description("Add random text at the end of the message to try to bypass anti spams.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> uppercase = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("include-uppercase-characters")
            .description("Whether the bypass text should include uppercase characters.")
            .visible(this.bypass::get)
            .defaultValue(true)
            .build()
      );
   private final Setting<Integer> length = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("length")
            .description("Number of characters used to bypass anti spam.")
            .visible(this.bypass::get)
            .defaultValue(16)
            .sliderRange(1, 256)
            .build()
      );
   private int messageI;
   private int timer;
   private int splitNum;
   private String text;

   public Spam() {
      super(Categories.Misc, "spam", "Spams specified messages in chat.");
   }

   @Override
   public void onActivate() {
      this.timer = this.delay.get();
      this.messageI = 0;
      this.splitNum = 0;
   }

   @EventHandler
   private void onScreenOpen(OpenScreenEvent event) {
      if (this.disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) {
         this.toggle();
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      if (this.disableOnLeave.get()) {
         this.toggle();
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (!this.messages.get().isEmpty()) {
         if (this.timer <= 0) {
            if (this.text == null) {
               int i;
               if (this.random.get()) {
                  i = Utils.random(0, this.messages.get().size());
               } else {
                  if (this.messageI >= this.messages.get().size()) {
                     this.messageI = 0;
                  }

                  i = this.messageI++;
               }

               this.text = this.messages.get().get(i);
               if (this.bypass.get()) {
                  String bypass = RandomStringUtils.insecure().nextAlphabetic(this.length.get());
                  if (!this.uppercase.get()) {
                     bypass = bypass.toLowerCase();
                  }

                  this.text = this.text + " " + bypass;
               }
            }

            if (this.autoSplitMessages.get() && this.text.length() > this.splitLength.get()) {
               double length = this.text.length();
               int splits = (int)Math.ceil(length / this.splitLength.get().intValue());
               int start = this.splitNum * this.splitLength.get();
               int end = Math.min(start + this.splitLength.get(), this.text.length());
               ChatUtils.sendPlayerMsg(this.text.substring(start, end));
               this.splitNum = ++this.splitNum % splits;
               this.timer = this.autoSplitDelay.get();
               if (this.splitNum == 0) {
                  this.timer = this.delay.get();
                  this.text = null;
               }
            } else {
               if (this.text.length() > 256) {
                  this.text = this.text.substring(0, 256);
               }

               ChatUtils.sendPlayerMsg(this.text);
               this.timer = this.delay.get();
               this.text = null;
            }
         } else {
            this.timer--;
         }
      }
   }
}
