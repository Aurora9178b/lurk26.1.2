package meteordevelopment.meteorclient.systems.config;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.FontFaceSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class Config extends System<Config> {
   public final Settings settings = new Settings();
   private final SettingGroup sgVisual = this.settings.createGroup("Visual");
   private final SettingGroup sgModules = this.settings.createGroup("Modules");
   private final SettingGroup sgChat = this.settings.createGroup("Chat");
   private final SettingGroup sgMisc = this.settings.createGroup("Misc");
   public final Setting<Boolean> customFont = this.sgVisual
      .add(new BoolSetting.Builder().name("custom-font").description("Use a custom font.").defaultValue(true).build());
   public final Setting<FontFace> font = this.sgVisual
      .add(new FontFaceSetting.Builder().name("font").description("Custom font to use.").visible(this.customFont::get).onChanged(Fonts::load).build());
   public final Setting<Double> rainbowSpeed = this.sgVisual
      .add(new DoubleSetting.Builder().name("rainbow-speed").description("The global rainbow speed.").defaultValue(0.5).range(0.0, 10.0).sliderMax(5.0).build());
   public final Setting<Boolean> titleScreenCredits = this.sgVisual
      .add(new BoolSetting.Builder().name("title-screen-credits").description("Show Meteor credits on title screen").defaultValue(true).build());
   public final Setting<Boolean> titleScreenSplashes = this.sgVisual
      .add(new BoolSetting.Builder().name("title-screen-splashes").description("Show Meteor splash texts on title screen").defaultValue(true).build());
   public final Setting<Boolean> customWindowTitle = this.sgVisual
      .add(
         new BoolSetting.Builder()
            .name("custom-window-title")
            .description("Show custom text in the window title.")
            .defaultValue(false)
            .onModuleActivated(var0 -> MeteorClient.mc.updateTitle())
            .onChanged(var0 -> MeteorClient.mc.updateTitle())
            .build()
      );
   public final Setting<String> customWindowTitleText = this.sgVisual
      .add(
         new StringSetting.Builder()
            .name("window-title-text")
            .description("The text it displays in the window title.")
            .visible(this.customWindowTitle::get)
            .defaultValue("Minecraft {mc_version} - {meteor.name} {meteor.version}")
            .onChanged(var0 -> MeteorClient.mc.updateTitle())
            .build()
      );
   public final Setting<SettingColor> friendColor = this.sgVisual
      .add(new ColorSetting.Builder().name("friend-color").description("The color used to show friends.").defaultValue(new SettingColor(0, 255, 180)).build());
   public final Setting<Boolean> syncListSettingWidths = this.sgVisual
      .add(
         new BoolSetting.Builder()
            .name("sync-list-setting-widths")
            .description("Prevents the list setting screens from moving around as you add & remove elements.")
            .defaultValue(false)
            .build()
      );
   public final Setting<Config.ButtonPosition> accountButtonAnchor = this.sgVisual
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("accounts-button"))
                  .description("Controls the position and visibility of the accounts button in the multiplayer screen."))
               .defaultValue(Config.ButtonPosition.TopRight))
            .build()
      );
   public final Setting<Boolean> showAccountStatus = this.sgVisual
      .add(
         new BoolSetting.Builder()
            .name("account-status")
            .description("Shows information about the current account in the multiplayer screen.")
            .defaultValue(true)
            .build()
      );
   public final Setting<Config.ButtonPosition> proxiesButtonAnchor = this.sgVisual
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("proxies-button"))
                  .description("Controls the position and visibility of the proxies button in the multiplayer screen."))
               .defaultValue(Config.ButtonPosition.TopRight))
            .build()
      );
   public final Setting<Boolean> showProxiesStatus = this.sgVisual
      .add(
         new BoolSetting.Builder()
            .name("proxy-status")
            .description("Shows information about the current proxy in the multiplayer screen.")
            .defaultValue(true)
            .build()
      );
   public final Setting<List<Module>> hiddenModules = this.sgModules
      .add(new ModuleListSetting.Builder().name("hidden-modules").description("Prevent these modules from being rendered as options in the clickgui.").build());
   public final Setting<Integer> moduleSearchCount = this.sgModules
      .add(
         new IntSetting.Builder()
            .name("module-search-count")
            .description("Amount of modules and settings to be shown in the module search bar.")
            .defaultValue(8)
            .min(1)
            .sliderMax(12)
            .build()
      );
   public final Setting<Boolean> moduleAliases = this.sgModules
      .add(
         new BoolSetting.Builder()
            .name("search-module-aliases")
            .description("Whether or not module aliases will be used in the module search bar.")
            .defaultValue(true)
            .build()
      );
   public final Setting<String> prefix = this.sgChat.add(new StringSetting.Builder().name("prefix").description("Prefix.").defaultValue(".").build());
   public final Setting<Boolean> chatFeedback = this.sgChat
      .add(new BoolSetting.Builder().name("chat-feedback").description("Sends chat feedback when meteor performs certain actions.").defaultValue(true).build());
   public final Setting<Boolean> deleteChatFeedback = this.sgChat
      .add(
         new BoolSetting.Builder()
            .name("delete-chat-feedback")
            .description("Delete previous matching chat feedback to keep chat clear.")
            .visible(this.chatFeedback::get)
            .defaultValue(true)
            .build()
      );
   public final Setting<Integer> rotationHoldTicks = this.sgMisc
      .add(
         new IntSetting.Builder()
            .name("rotation-hold")
            .description("Hold long to hold server side rotation when not sending any packets.")
            .defaultValue(4)
            .build()
      );
   public final Setting<Boolean> useTeamColor = this.sgMisc
      .add(
         new BoolSetting.Builder()
            .name("use-team-color")
            .description("Uses player's team color for rendering things like esp and tracers.")
            .defaultValue(true)
            .build()
      );
   public List<String> dontShowAgainPrompts = new ArrayList<>();

   public Config() {
      super("config");
   }

   public static Config get() {
      return Systems.get(Config.class);
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putString("version", MeteorClient.VERSION.toString());
      tag.put("settings", this.settings.toTag());
      tag.put("dontShowAgainPrompts", this.listToTag(this.dontShowAgainPrompts));
      return tag;
   }

   public Config fromTag(CompoundTag tag) {
      if (tag.contains("settings")) {
         this.settings.fromTag(tag.getCompoundOrEmpty("settings"));
      }

      if (tag.contains("dontShowAgainPrompts")) {
         this.dontShowAgainPrompts = this.listFromTag(tag, "dontShowAgainPrompts");
      }

      return this;
   }

   private ListTag listToTag(List<String> list) {
      ListTag nbt = new ListTag();

      for (String item : list) {
         nbt.add(StringTag.valueOf(item));
      }

      return nbt;
   }

   private List<String> listFromTag(CompoundTag tag, String key) {
      List<String> list = new ArrayList<>();

      for (Tag item : tag.getListOrEmpty(key)) {
         list.add(item.asString().orElse(""));
      }

      return list;
   }

   public enum ButtonPosition {
      TopLeft,
      TopRight,
      BottomLeft,
      BottomRight,
      Hidden;
   }
}
