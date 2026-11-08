package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.GameType;

public class BetterTab extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Integer> tabSize = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("tablist-size")
            .description("How many players in total to display in the tablist.")
            .defaultValue(100)
            .min(1)
            .sliderRange(1, 1000)
            .build()
      );
   public final Setting<Integer> tabHeight = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("column-height")
            .description("How many players to display in each column.")
            .defaultValue(20)
            .min(1)
            .sliderRange(1, 1000)
            .build()
      );
   private final Setting<Boolean> self = this.sgGeneral
      .add(new BoolSetting.Builder().name("highlight-self").description("Highlights yourself in the tablist.").defaultValue(true).build());
   private final Setting<SettingColor> selfColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("self-color")
            .description("The color to highlight your name with.")
            .defaultValue(new SettingColor(250, 130, 30))
            .visible(this.self::get)
            .build()
      );
   private final Setting<Boolean> friends = this.sgGeneral
      .add(new BoolSetting.Builder().name("highlight-friends").description("Highlights friends in the tablist.").defaultValue(true).build());
   public final Setting<Boolean> accurateLatency = this.sgGeneral
      .add(new BoolSetting.Builder().name("accurate-latency").description("Shows latency as a number in the tablist.").defaultValue(true).build());
   private final Setting<Boolean> gamemode = this.sgGeneral
      .add(new BoolSetting.Builder().name("gamemode").description("Display gamemode next to the nick.").defaultValue(false).build());

   public BetterTab() {
      super(Categories.Render, "better-tab", "Various improvements to the tab list.");
   }

   public Component getPlayerName(PlayerInfo playerListEntry) {
      Color color = null;
      Component name = playerListEntry.getTabListDisplayName();
      if (name == null) {
         name = Component.literal(playerListEntry.getProfile().name());
      }

      if (playerListEntry.getProfile().id().toString().equals(this.mc.player.getGameProfile().id().toString()) && this.self.get()) {
         color = this.selfColor.get();
      } else if (this.friends.get() && Friends.get().isFriend(playerListEntry)) {
         Friend friend = Friends.get().get(playerListEntry);
         if (friend != null) {
            color = Config.get().friendColor.get();
         }
      }

      if (color != null) {
         String nameString = name.getString();

         for (ChatFormatting format : ChatFormatting.values()) {
            if (format.isColor()) {
               nameString = nameString.replace(format.toString(), "");
            }
         }

         name = Component.literal(nameString).setStyle(name.getStyle().withColor(TextColor.fromRgb(color.getPacked())));
      }

      if (this.gamemode.get()) {
         GameType gm = playerListEntry.getGameMode();
         String gmText = "?";
         if (gm != null) {
            gmText = switch (gm) {
               case SPECTATOR -> "Sp";
               case SURVIVAL -> "S";
               case CREATIVE -> "C";
               case ADVENTURE -> "A";
            };
         }

         MutableComponent text = Component.literal("");
         text.append(name);
         text.append(" [" + gmText + "]");
         name = text;
      }

      return name;
   }
}
