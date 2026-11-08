package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.AbstractGameRulesScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Tuple;
import net.minecraft.util.Util;
import org.meteordev.starscript.Script;

public class DiscordPresence extends Module {
   private final SettingGroup sgLine1 = this.settings.createGroup("Line 1");
   private final SettingGroup sgLine2 = this.settings.createGroup("Line 2");
   private final Setting<List<String>> line1Strings = this.sgLine1
      .add(
         new StringListSetting.Builder()
            .name("line-1-messages")
            .description("Messages used for the first line.")
            .defaultValue("{player}", "{server}")
            .onChanged(var1 -> this.recompileLine1())
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   private final Setting<Integer> line1UpdateDelay = this.sgLine1
      .add(
         new IntSetting.Builder()
            .name("line-1-update-delay")
            .description("How fast to update the first line in ticks.")
            .defaultValue(200)
            .min(10)
            .sliderRange(10, 200)
            .build()
      );
   private final Setting<DiscordPresence.SelectMode> line1SelectMode = this.sgLine1
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("line-1-select-mode"))
                  .description("How to select messages for the first line."))
               .defaultValue(DiscordPresence.SelectMode.Sequential))
            .build()
      );
   private final Setting<List<String>> line2Strings = this.sgLine2
      .add(
         new StringListSetting.Builder()
            .name("line-2-messages")
            .description("Messages used for the second line.")
            .defaultValue(
               "Meteor on Crack!", "{round(server.tps, 1)} TPS", "Playing on {server.difficulty} difficulty.", "{server.player_count} Players online"
            )
            .onChanged(var1 -> this.recompileLine2())
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   private final Setting<Integer> line2UpdateDelay = this.sgLine2
      .add(
         new IntSetting.Builder()
            .name("line-2-update-delay")
            .description("How fast to update the second line in ticks.")
            .defaultValue(60)
            .min(10)
            .sliderRange(10, 200)
            .build()
      );
   private final Setting<DiscordPresence.SelectMode> line2SelectMode = this.sgLine2
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("line-2-select-mode"))
                  .description("How to select messages for the second line."))
               .defaultValue(DiscordPresence.SelectMode.Sequential))
            .build()
      );
   private static final RichPresence rpc = new RichPresence();
   private DiscordPresence.SmallImage currentSmallImage;
   private int ticks;
   private boolean forceUpdate;
   private boolean lastWasInMainMenu;
   private final List<Script> line1Scripts = new ArrayList<>();
   private int line1Ticks;
   private int line1I;
   private final List<Script> line2Scripts = new ArrayList<>();
   private int line2Ticks;
   private int line2I;
   public static final List<Tuple<String, String>> customStates = new ArrayList<>();

   public DiscordPresence() {
      super(Categories.Misc, "discord-presence", "Displays Meteor as your presence on Discord.");
      this.runInMainMenu = true;
   }

   public static void registerCustomState(String packageName, String state) {
      for (Tuple<String, String> pair : customStates) {
         if (pair.getA().equals(packageName)) {
            pair.setB(state);
            return;
         }
      }

      customStates.add(new Tuple<>(packageName, state));
   }

   public static void unregisterCustomState(String packageName) {
      customStates.removeIf(pair -> pair.getA().equals(packageName));
   }

   @Override
   public void onActivate() {
      DiscordIPC.start(835240968533049424L, null);
      rpc.setStart(System.currentTimeMillis() / 1000L);
      String largeText = "%s %s".formatted(MeteorClient.NAME, MeteorClient.VERSION);
      if (!MeteorClient.BUILD_NUMBER.isEmpty()) {
         largeText = largeText + " Build: " + MeteorClient.BUILD_NUMBER;
      }

      rpc.setLargeImage("meteor_client", largeText);
      this.currentSmallImage = DiscordPresence.SmallImage.Snail;
      this.recompileLine1();
      this.recompileLine2();
      this.ticks = 0;
      this.line1Ticks = 0;
      this.line2Ticks = 0;
      this.lastWasInMainMenu = false;
      this.line1I = 0;
      this.line2I = 0;
   }

   @Override
   public void onDeactivate() {
      DiscordIPC.stop();
   }

   private void recompile(List<String> messages, List<Script> scripts) {
      scripts.clear();

      for (String message : messages) {
         Script script = MeteorStarscript.compile(message);
         if (script != null) {
            scripts.add(script);
         }
      }

      this.forceUpdate = true;
   }

   private void recompileLine1() {
      this.recompile(this.line1Strings.get(), this.line1Scripts);
   }

   private void recompileLine2() {
      this.recompile(this.line2Strings.get(), this.line2Scripts);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      boolean update = false;
      if (this.ticks < 200 && !this.forceUpdate) {
         this.ticks++;
      } else {
         this.currentSmallImage = this.currentSmallImage.next();
         this.currentSmallImage.apply();
         update = true;
         this.ticks = 0;
      }

      if (Utils.canUpdate()) {
         if (this.line1Ticks < this.line1UpdateDelay.get() && !this.forceUpdate) {
            this.line1Ticks++;
         } else {
            if (!this.line1Scripts.isEmpty()) {
               int i = Utils.random(0, this.line1Scripts.size());
               if (this.line1SelectMode.get() == DiscordPresence.SelectMode.Sequential) {
                  if (this.line1I >= this.line1Scripts.size()) {
                     this.line1I = 0;
                  }

                  i = this.line1I++;
               }

               String message = MeteorStarscript.run(this.line1Scripts.get(i));
               if (message != null) {
                  rpc.setDetails(message);
               }
            }

            update = true;
            this.line1Ticks = 0;
         }

         if (this.line2Ticks < this.line2UpdateDelay.get() && !this.forceUpdate) {
            this.line2Ticks++;
         } else {
            if (!this.line2Scripts.isEmpty()) {
               int i = Utils.random(0, this.line2Scripts.size());
               if (this.line2SelectMode.get() == DiscordPresence.SelectMode.Sequential) {
                  if (this.line2I >= this.line2Scripts.size()) {
                     this.line2I = 0;
                  }

                  i = this.line2I++;
               }

               String message = MeteorStarscript.run(this.line2Scripts.get(i));
               if (message != null) {
                  rpc.setState(message);
               }
            }

            update = true;
            this.line2Ticks = 0;
         }
      } else if (!this.lastWasInMainMenu) {
         rpc.setDetails(
            MeteorClient.NAME + " " + (MeteorClient.BUILD_NUMBER.isEmpty() ? MeteorClient.VERSION : MeteorClient.VERSION + " " + MeteorClient.BUILD_NUMBER)
         );
         if (this.mc.screen instanceof TitleScreen) {
            rpc.setState("Looking at title screen");
         } else if (this.mc.screen instanceof SelectWorldScreen) {
            rpc.setState("Selecting world");
         } else if (this.mc.screen instanceof CreateWorldScreen || this.mc.screen instanceof AbstractGameRulesScreen) {
            rpc.setState("Creating world");
         } else if (this.mc.screen instanceof EditWorldScreen) {
            rpc.setState("Editing world");
         } else if (this.mc.screen instanceof LevelLoadingScreen) {
            rpc.setState("Loading world");
         } else if (this.mc.screen instanceof JoinMultiplayerScreen) {
            rpc.setState("Selecting server");
         } else if (this.mc.screen instanceof ManageServerScreen) {
            rpc.setState("Adding server");
         } else if (this.mc.screen instanceof ConnectScreen || this.mc.screen instanceof DirectJoinServerScreen) {
            rpc.setState("Connecting to server");
         } else if (this.mc.screen instanceof WidgetScreen) {
            rpc.setState("Browsing Meteor's GUI");
         } else if (this.mc.screen instanceof OptionsScreen
            || this.mc.screen instanceof SkinCustomizationScreen
            || this.mc.screen instanceof SoundOptionsScreen
            || this.mc.screen instanceof VideoSettingsScreen
            || this.mc.screen instanceof ControlsScreen
            || this.mc.screen instanceof LanguageSelectScreen
            || this.mc.screen instanceof ChatOptionsScreen
            || this.mc.screen instanceof PackSelectionScreen
            || this.mc.screen instanceof AccessibilityOptionsScreen) {
            rpc.setState("Changing options");
         } else if (this.mc.screen instanceof WinScreen) {
            rpc.setState("Reading credits");
         } else if (this.mc.screen instanceof RealmsScreen) {
            rpc.setState("Browsing Realms");
         } else {
            boolean setState = false;
            if (this.mc.screen != null) {
               String className = this.mc.screen.getClass().getName();

               for (Tuple<String, String> pair : customStates) {
                  if (className.startsWith(pair.getA())) {
                     rpc.setState(pair.getB());
                     setState = true;
                     break;
                  }
               }
            }

            if (!setState) {
               rpc.setState("In main menu");
            }
         }

         update = true;
      }

      if (update) {
         DiscordIPC.setActivity(rpc);
      }

      this.forceUpdate = false;
      this.lastWasInMainMenu = !Utils.canUpdate();
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (!Utils.canUpdate()) {
         this.lastWasInMainMenu = false;
      }
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WButton help = theme.button("Open documentation.");
      help.action = () -> Util.getPlatform().openUri("https://github.com/MeteorDevelopment/meteor-client/wiki/Starscript");
      return help;
   }

   static {
      registerCustomState("com.terraformersmc.modmenu.gui", "Browsing mods");
      registerCustomState("me.jellysquid.mods.sodium.client", "Changing options");
   }

   public enum SelectMode {
      Random,
      Sequential;
   }

   private enum SmallImage {
      MineGame("minegame", "MineGame159"),
      Snail("seasnail", "seasnail8169");

      private final String key;
      private final String text;

      SmallImage(String key, String text) {
         this.key = key;
         this.text = text;
      }

      void apply() {
         DiscordPresence.rpc.setSmallImage(this.key, this.text);
      }

      DiscordPresence.SmallImage next() {
         return this == MineGame ? Snail : MineGame;
      }
   }
}
