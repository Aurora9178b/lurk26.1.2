package meteordevelopment.meteorclient.systems.hud.elements;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.AccessorPlayerTabOverlay;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.MusicPlayer;
import meteordevelopment.meteorclient.systems.modules.movement.NewBlink;
import meteordevelopment.meteorclient.systems.modules.movement.Scaffold;
import meteordevelopment.meteorclient.utils.music.CloudMusicApi;
import meteordevelopment.meteorclient.utils.music.MusicManager;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;

public class DynamicIslandHud extends HudElement {
   private static final Identifier ICON = MeteorClient.identifier("icon.png");
   private static final Identifier WARNING_ICON = MeteorClient.identifier("google/warning.png");
   private static final Identifier SCAFFOLD_ICON = MeteorClient.identifier("google/block.png");
   private static final Identifier BLINK_ICON = MeteorClient.identifier("google/bug.png");
   private static final double ICON_SIZE = 32.0;
   private static final double WARNING_ICON_SIZE = 64.0;
   private static final double PADDING = 10.0;
   private static final double NORMAL_ICON_LEFT_PADDING = 6.0;
   private static final double NORMAL_ICON_VERTICAL_PADDING = 6.0;
   private static final double ICON_TO_TEXT_SPACING = 6.0;
   private static final double LOW_HEALTH_ICON_TO_TEXT_SPACING = 5.0;
   private static final double LOW_HEALTH_RIGHT_PADDING = 8.0;
   private static final double SEPARATOR_SPACING = 10.0;
   private static final double TOP_PADDING = 3.0;
   private static final double BOTTOM_PADDING = 3.0;
   private static final double ROW_SPACING = -2.0;
   private static final double LOW_HEALTH_VERTICAL_PADDING = 6.0;
   private static final double LOW_HEALTH_ROW_SPACING = 2.0;
   private static final double NORMAL_SECOND_ROW_SCALE = 0.7;
   private static final double TABLIST_TEXT_GAP = 50.0;
   private static final double TABLIST_BOTTOM_PADDING = 9.0;
   private static final double TABLIST_HORIZONTAL_PADDING = 11.0;
   private static final double TABLIST_ROW_GAP = 2.0;
   private static final double TABLIST_COLUMN_GAP = 8.0;
   private static final double TABLIST_MIN_COLUMN_WIDTH = 128.0;
   private static final double TABLIST_MAX_ROW_COUNT = 20.0;
   private static final double TABLIST_TEXT_SCALE = 0.8;
   private static final double TABLIST_HEADER_SCALE = 0.8;
   private static final double TABLIST_PING_SCALE = 0.8;
   private static final long TABLIST_LAYOUT_CACHE_MS = 1000L;
   private static final double TABLIST_LAYOUT_WIDTH_TOLERANCE = 24.0;
   private static final long MODULE_NOTIFICATION_DURATION_MS = 2200L;
   private static final double MODULE_NOTIFICATION_MIN_WIDTH = 104.0;
   private static final double MODULE_NOTIFICATION_HEIGHT = 30.0;
   private static final double MODULE_TEXT_SCALE = 1.38;
   private static final double MODULE_ICON_SCALE = 1.38;
   private static final double MODULE_HEIGHT_SCALE = 1.296;
   private static final double MODULE_SIDE_PADDING = 6.0;
   private static final double MODULE_RIGHT_PADDING = 8.0;
   private static final double MODULE_ICON_AREA_SIZE = 28.0;
   private static final double MODULE_TEXT_SPACING = 6.0;
   private static final double MODULE_TITLE_SCALE = 0.68;
   private static final double MODULE_SUBTITLE_SCALE = 0.54;
   private static final double MODULE_TITLE_Y = 6.5;
   private static final double MODULE_SUBTITLE_Y = 18.0;
   private static final double MODULE_TOGGLE_W = 22.0;
   private static final double MODULE_TOGGLE_H = 13.0;
   private static final double MODULE_TOGGLE_PAD = 1.5;
   private static final Color MODULE_TEXT_PRIMARY = new Color(255, 255, 255, 255);
   private static final Color MODULE_TEXT_MUTED = new Color(160, 160, 160, 255);
   private static final Color MODULE_ACCENT = new Color(232, 232, 232, 255);
   private static final Color MODULE_TOGGLE_ENABLED = new Color(138, 255, 160, 235);
   private static final Color MODULE_SUCCESS = new Color(103, 255, 119, 255);
   private static final Color MODULE_ERROR = new Color(255, 92, 92, 255);
   private static final List<DynamicIslandHud.ModuleNotification> moduleNotifications = new CopyOnWriteArrayList<>();
   private static final double SCAFFOLD_HEIGHT = 30.0;
   private static final double SCAFFOLD_TEXT_SCALE = 1.38;
   private static final double SCAFFOLD_ICON_SCALE = 1.38;
   private static final double SCAFFOLD_HEIGHT_SCALE = 1.296;
   private static final double SCAFFOLD_ICON_SIZE = 32.0;
   private static final Color SCAFFOLD_PRIMARY = new Color(255, 255, 255, 255);
   private static final Color SCAFFOLD_SECONDARY = new Color(176, 181, 190, 255);
   private static final double BLINK_ICON_BACKGROUND_RADIUS = 4.0;
   private static final Color BLINK_ICON_BACKGROUND = new Color(125, 18, 18, 230);
   private static final Color BLINK_PRIMARY = new Color(255, 255, 255, 255);
   private static final Color BLINK_SECONDARY = new Color(255, 255, 255, 255);
   private static final Color BLINK_PROGRESS_BACKGROUND = new Color(255, 255, 255, 42);
   private static final Color BLINK_PROGRESS = new Color(255, 255, 255, 230);
   private static final String CLIENT_NAME = "LurkClient";
   private static final String CLIENT_VERSION = "6.7.4";
   private static final String CLIENT_VERSION_SEPARATOR = " ";
   private static final double MUSIC_VERTICAL_PADDING = 5.0;
   private static final double MUSIC_TEXT_SPACING = 7.0;
   private static final double MUSIC_RIGHT_PADDING = 8.0;
   private static final double MUSIC_ROW_GAP = 1.0;
   private static final double MUSIC_BAR_GAP = 3.5;
   private static final double MUSIC_BAR_HEIGHT = 2.5;
   private static final double MUSIC_TIME_GAP = 2.0;
   private static final double MUSIC_MIN_TEXT_WIDTH = 110.0;
   private static final double MUSIC_MAX_TEXT_WIDTH = 230.0;
   private static final double MUSIC_COVER_SCALE = 0.7;
   private static final double GUI_PIXELS_PER_CM = 37.7952755906;
   private static final double MUSIC_MIN_WIDTH_CM = 12.0;
   private float animationProgress = 0.0F;
   private float backgroundWidthProgress = 0.0F;
   private float backgroundHeightProgress = 0.0F;
   private float blinkProgress = 0.0F;
   private DynamicIslandHud.TabListLayout cachedTabListLayout = DynamicIslandHud.TabListLayout.EMPTY;
   private long cachedTabListLayoutTime;
   private double cachedTabListWidth = -1.0;
   private double cachedTabListScale = -1.0;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<SettingColor> backgroundColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("background-color").description("Color of the background.").defaultValue(new SettingColor(0, 0, 0, 100)).build());
   private final Setting<Double> backgroundRadius = this.sgGeneral
      .add(new DoubleSetting.Builder().name("background-radius").description("Radius of the background corners.").defaultValue(37.0).min(0.0).max(50.0).build());
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("Scale of the HUD element.").defaultValue(2.0).min(0.5).max(5.0).build());
   private final Setting<SettingColor> clientGradientColor1 = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("client-gradient-color-1")
            .description("First color of the LurkClient name gradient.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
      );
   private final Setting<SettingColor> clientGradientColor2 = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("client-gradient-color-2")
            .description("Second color of the LurkClient name gradient.")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
      );
   private final Setting<Integer> clientGradientSpeed = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("client-gradient-speed")
            .description("Speed of the LurkClient name gradient.")
            .defaultValue(5)
            .sliderRange(0, 100)
            .build()
      );
   public static final HudElementInfo<DynamicIslandHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "dynamic-island", "Displays a dynamic island HUD element.", DynamicIslandHud::new
   );

   public DynamicIslandHud() {
      super(INFO);
   }

   public static boolean shouldHideVanillaTabList() {
      if (!isTabListKeyDown()) {
         return false;
      }

      if (!Hud.get().active) {
         return false;
      }

      for (HudElement element : Hud.get()) {
         if (element instanceof DynamicIslandHud && element.isActive()) {
            return true;
         }
      }

      return false;
   }

   public static void notifyModuleToggle(String moduleName, boolean toggled) {
      if (moduleName != null && !moduleName.isBlank()) {
         moduleNotifications.removeIf(notification -> notification.moduleName.equals(moduleName));
         moduleNotifications.add(new DynamicIslandHud.ModuleNotification(moduleName, toggled, System.currentTimeMillis(), toggled ? 0.0F : 1.0F));
      }
   }

   private double getScale() {
      return this.scale.get();
   }

   @Override
   public void render(HudRenderer renderer) {
      if (MeteorClient.mc.player != null) {
         boolean isLowHealth = MeteorClient.mc.player.getHealth() < 10.0F;
         List<DynamicIslandHud.ModuleNotification> activeModuleNotifications = getActiveModuleNotifications();
         boolean showModuleNotification = !activeModuleNotifications.isEmpty() && !isLowHealth;
         DynamicIslandHud.ScaffoldStatus scaffoldStatus = this.getScaffoldStatus();
         boolean showScaffoldStatus = scaffoldStatus.visible() && !isLowHealth;
         DynamicIslandHud.BlinkStatus blinkStatus = this.getBlinkStatus();
         boolean showBlinkStatus = blinkStatus.visible() && !isLowHealth;
         DynamicIslandHud.MusicState musicState = this.getMusicState();
         boolean showMusic = musicState.visible() && !isLowHealth;
         float targetProgress = isLowHealth ? 1.0F : 0.0F;
         this.animationProgress = AnimationUtility.ease(this.animationProgress, targetProgress, 5.0F);
         double scale = this.getScale();
         double iconSize = this.getIconSize(scale);
         double textHeight = renderer.textHeight(true, scale);
         double normalSecondRowTextHeight = renderer.textHeight(true, scale * 0.7);
         double normalTextBackgroundHeight = 3.0 + textHeight + -2.0 + normalSecondRowTextHeight + 3.0;
         double normalIconBackgroundHeight = iconSize + 12.0;
         double normalBackgroundHeight = Math.max(normalTextBackgroundHeight, normalIconBackgroundHeight);
         double musicBackgroundHeight = showMusic ? this.calculateMusicHeight(renderer, scale) : normalBackgroundHeight;
         double baseNormalBackgroundHeight = showMusic ? musicBackgroundHeight : normalBackgroundHeight;
         double lowHealthTextHeight = textHeight + 2.0 + textHeight;
         double lowHealthBackgroundHeight = 6.0 + Math.max(this.getWarningIconSize(scale), lowHealthTextHeight) + 6.0;
         double baseBackgroundHeight = baseNormalBackgroundHeight + (lowHealthBackgroundHeight - baseNormalBackgroundHeight) * this.animationProgress;
         double radius = this.backgroundRadius.get();
         double normalWidth = showMusic
            ? this.calculateMusicWidth(renderer, scale, musicState, musicBackgroundHeight, radius)
            : this.calculateNormalWidth(renderer, scale, normalBackgroundHeight, radius);
         double lowHealthWidth = this.calculateLowHealthWidth(renderer, scale);
         double moduleNotificationsHeight = showModuleNotification ? this.calculateModuleNotificationsHeight(renderer, scale, activeModuleNotifications) : 0.0;
         double blinkStatusHeight = showBlinkStatus ? this.calculateBlinkHeight(renderer, scale) : 0.0;
         double scaffoldStatusHeight = showScaffoldStatus ? this.calculateScaffoldHeight(renderer, scale) : 0.0;
         double extensionHeight = blinkStatusHeight + scaffoldStatusHeight + moduleNotificationsHeight;
         double contentBackgroundHeight = baseBackgroundHeight + extensionHeight;
         double currentWidth = normalWidth + (lowHealthWidth - normalWidth) * this.animationProgress;
         if (showMusic) {
            currentWidth = Math.max(currentWidth, musicMinWidth(scale));
         }

         if (showScaffoldStatus) {
            currentWidth = Math.max(currentWidth, this.calculateScaffoldWidth(renderer, scale, scaffoldStatus));
         }

         if (showBlinkStatus) {
            currentWidth = Math.max(currentWidth, this.calculateBlinkWidth(renderer, scale, blinkStatus));
         }

         if (showModuleNotification) {
            currentWidth = Math.max(currentWidth, this.calculateModuleNotificationsWidth(renderer, scale, activeModuleNotifications));
         }

         boolean showTabList = isTabListKeyDown();
         if (showTabList) {
            currentWidth = Math.max(currentWidth, normalWidth);
         }

         DynamicIslandHud.TabListLayout tabListLayout = showTabList
            ? this.getTabListLayout(renderer, scale, currentWidth)
            : DynamicIslandHud.TabListLayout.EMPTY;
         double tabListY = this.y
            + (
               extensionHeight > 0.0
                  ? contentBackgroundHeight + 50.0
                  : this.getTabListOffset(renderer, scale, normalBackgroundHeight, baseBackgroundHeight, isLowHealth, showMusic)
            );
         double targetBackgroundHeight = showTabList
            ? Math.max(contentBackgroundHeight, tabListY - this.y + tabListLayout.height() + 9.0)
            : contentBackgroundHeight;
         if (this.backgroundWidthProgress <= 0.0F) {
            this.backgroundWidthProgress = (float)currentWidth;
         }

         if (this.backgroundHeightProgress <= 0.0F) {
            this.backgroundHeightProgress = (float)contentBackgroundHeight;
         }

         this.backgroundWidthProgress = AnimationUtility.ease(this.backgroundWidthProgress, (float)currentWidth, 5.0F);
         this.backgroundHeightProgress = AnimationUtility.ease(this.backgroundHeightProgress, (float)targetBackgroundHeight, 5.0F);
         double backgroundWidth = Math.max(1.0, this.backgroundWidthProgress);
         if (showMusic) {
            backgroundWidth = Math.max(backgroundWidth, musicMinWidth(scale));
         }

         double backgroundHeight = Math.max(contentBackgroundHeight, this.backgroundHeightProgress);
         this.setSize(backgroundWidth, backgroundHeight);
         BlurUtil.render(this.x, this.y, (int)backgroundWidth, (int)backgroundHeight, (float)radius);
         Render2DUtil.drawRoundedRect(renderer, this.x, this.y, backgroundWidth, backgroundHeight, radius, this.backgroundColor.get());
         Render2DUtil.drawRoundedGlow(this.x, this.y, backgroundWidth, backgroundHeight, radius);
         if (isLowHealth) {
            double warningIconSize = this.getWarningIconSize(scale);
            double iconX = this.x + 10.0;
            double iconY = this.y + (baseBackgroundHeight - warningIconSize) / 2.0;
            renderer.texture(WARNING_ICON, (int)iconX, (int)iconY, (int)warningIconSize, (int)warningIconSize, new Color(255, 255, 255, 255));
            this.renderLowHealthText(renderer, scale, backgroundWidth, baseBackgroundHeight);
         } else {
            if (showMusic) {
               this.renderMusicContent(renderer, scale, musicState, baseBackgroundHeight, radius);
            } else {
               this.renderNormalContent(renderer, scale, iconSize, baseBackgroundHeight, radius);
            }

            double extensionY = this.y + baseBackgroundHeight;
            if (showBlinkStatus) {
               this.renderBlinkStatus(renderer, scale, blinkStatus, backgroundWidth, extensionY);
               extensionY += blinkStatusHeight;
            }

            if (showScaffoldStatus) {
               this.renderScaffoldStatus(renderer, scale, scaffoldStatus, backgroundWidth, extensionY);
               extensionY += scaffoldStatusHeight;
            }

            if (showModuleNotification) {
               this.renderModuleNotifications(renderer, scale, activeModuleNotifications, backgroundWidth, extensionY);
            }
         }

         if (showTabList) {
            this.renderTabList(renderer, scale, backgroundWidth, tabListY, tabListLayout);
         }
      }
   }

   private double calculateNormalWidth(HudRenderer renderer, double scale, double backgroundHeight, double radius) {
      double versionWidth = this.getClientVersionWidth(renderer, scale);
      double devWidth = renderer.textWidth("Developer Version", true, scale);
      String userName = MeteorClient.mc.player.getName().getString();
      double userWidth = this.getUserWidth(renderer, scale, userName);
      double separatorWidth = 1.0;
      double firstRowContentWidth = versionWidth + 10.0 + separatorWidth + 10.0 + devWidth + 10.0 + separatorWidth + 10.0 + userWidth;
      double iconSize = this.getIconSize(scale);
      double iconLeftOffset = this.getNormalIconLeftOffset(backgroundHeight, iconSize, radius);
      return iconLeftOffset + iconSize + 6.0 + firstRowContentWidth + 10.0;
   }

   private double calculateLowHealthWidth(HudRenderer renderer, double scale) {
      double firstRowWidth = renderer.textWidth("LowHealth", true, scale);
      double secondRowWidth = renderer.textWidth("You are in low health!", true, scale);
      double contentWidth = Math.max(firstRowWidth, secondRowWidth);
      double iconSize = this.getWarningIconSize(scale);
      return 10.0 + iconSize + 5.0 + contentWidth + 8.0;
   }

   private double calculateModuleNotificationWidth(HudRenderer renderer, double scale, DynamicIslandHud.ModuleNotification notification) {
      if (notification == null) {
         return 0.0;
      }

      double titleWidth = renderer.textWidth("Module Toggled", true, moduleTextScale(scale, 0.68));
      double subtitleWidth = renderer.textWidth(getModuleSubtitle(notification), true, moduleTextScale(scale, 0.54));
      return Math.max(
         moduleHeight(scale, 104.0),
         moduleHeight(scale, 6.0) + moduleIcon(scale, 28.0) + moduleHeight(scale, 6.0) + Math.max(titleWidth, subtitleWidth) + moduleHeight(scale, 8.0)
      );
   }

   private double calculateModuleNotificationHeight(HudRenderer renderer, double scale) {
      double titleHeight = renderer.textHeight(true, moduleTextScale(scale, 0.68));
      double subtitleHeight = renderer.textHeight(true, moduleTextScale(scale, 0.54));
      return Math.max(moduleHeight(scale, 30.0), moduleHeight(scale, 6.5) + titleHeight + moduleHeight(scale, 1.5) + subtitleHeight + moduleHeight(scale, 5.0));
   }

   private double calculateModuleNotificationsWidth(HudRenderer renderer, double scale, List<DynamicIslandHud.ModuleNotification> notifications) {
      double width = 0.0;

      for (DynamicIslandHud.ModuleNotification notification : notifications) {
         width = Math.max(width, this.calculateModuleNotificationWidth(renderer, scale, notification));
      }

      return width;
   }

   private double calculateModuleNotificationsHeight(HudRenderer renderer, double scale, List<DynamicIslandHud.ModuleNotification> notifications) {
      return this.calculateModuleNotificationHeight(renderer, scale) * notifications.size();
   }

   private double calculateMusicWidth(HudRenderer renderer, double scale, DynamicIslandHud.MusicState state, double backgroundHeight, double radius) {
      double coverSize = musicCoverSize(renderer, scale);
      double coverLeftOffset = this.getNormalIconLeftOffset(backgroundHeight, coverSize, radius);
      double titleScale = scale;
      double detailScale = scale * 0.7;
      double titleWidth = renderer.textWidth(state.title(), true, titleScale);
      double artistWidth = renderer.textWidth(state.artist(), true, detailScale);
      double timeWidth = renderer.textWidth(formatMusicTime(state.positionMs()) + " / " + formatMusicTime(state.durationMs()), true, detailScale);
      double textWidth = Math.max(110.0 * scale, Math.max(Math.max(titleWidth, artistWidth), timeWidth));
      textWidth = Math.min(textWidth, 230.0 * scale);
      double contentWidth = coverLeftOffset + coverSize + 7.0 * scale + textWidth + 8.0 * scale;
      return Math.max(contentWidth, musicMinWidth(scale));
   }

   private double calculateMusicHeight(HudRenderer renderer, double scale) {
      return musicTextHeight(renderer, scale) + 5.0 * scale * 2.0;
   }

   private double calculateScaffoldWidth(HudRenderer renderer, double scale, DynamicIslandHud.ScaffoldStatus status) {
      String info = scaffoldInfoText(status);
      double titleWidth = renderer.textWidth("Scaffold", true, scaffoldTextScale(scale, 0.66));
      double infoWidth = renderer.textWidth(info, true, scaffoldTextScale(scale, 0.5));
      double contentWidth = Math.max(titleWidth, infoWidth);
      return scaffoldSize(scale, 5.0) + scaffoldIcon(scale, 32.0) + scaffoldSize(scale, 8.0) + contentWidth + scaffoldSize(scale, 8.0);
   }

   private double calculateScaffoldHeight(HudRenderer renderer, double scale) {
      double titleHeight = renderer.textHeight(true, scaffoldTextScale(scale, 0.66));
      double infoHeight = renderer.textHeight(true, scaffoldTextScale(scale, 0.5));
      return Math.max(scaffoldSize(scale, 30.0), scaffoldSize(scale, 4.5) + titleHeight + scaffoldSize(scale, 2.0) + infoHeight + scaffoldSize(scale, 5.0));
   }

   private double calculateBlinkWidth(HudRenderer renderer, double scale, DynamicIslandHud.BlinkStatus status) {
      double textScale = blinkTextScale(scale);
      double titleWidth = renderer.textWidth("Blink Enable", true, textScale);
      double infoWidth = renderer.textWidth(blinkInfoText(status), true, textScale);
      double contentWidth = Math.max(titleWidth, infoWidth);
      double textHeight = renderer.textHeight(true, textScale);
      double iconBackgroundSize = textHeight * 2.0 + scaffoldSize(scale, 2.0);
      return scaffoldSize(scale, 5.0) + iconBackgroundSize + scaffoldSize(scale, 8.0) + contentWidth + scaffoldSize(scale, 8.0);
   }

   private double calculateBlinkHeight(HudRenderer renderer, double scale) {
      double lineHeight = renderer.textHeight(true, blinkTextScale(scale));
      return Math.max(
         scaffoldSize(scale, 30.0),
         scaffoldSize(scale, 4.5)
            + lineHeight
            + scaffoldSize(scale, 2.0)
            + lineHeight
            + mmToGuiPixels(1.0) * scale
            + mmToGuiPixels(1.0) * scale
            + scaffoldSize(scale, 5.0)
      );
   }

   private double getIconSize(double scale) {
      return 32.0 * scale;
   }

   private double getWarningIconSize(double scale) {
      return 64.0;
   }

   private static boolean isTabListKeyDown() {
      return MeteorClient.mc.player != null
         && MeteorClient.mc.level != null
         && MeteorClient.mc.getConnection() != null
         && MeteorClient.mc.options.keyPlayerList.isDown();
   }

   private double getTabListOffset(
      HudRenderer renderer, double scale, double normalBackgroundHeight, double baseBackgroundHeight, boolean isLowHealth, boolean showMusic
   ) {
      if (isLowHealth) {
         return baseBackgroundHeight + 50.0;
      }

      if (showMusic) {
         return baseBackgroundHeight + 50.0;
      }

      double textHeight = renderer.textHeight(true, scale);
      double secondRowTextHeight = renderer.textHeight(true, scale * 0.7);
      double contentHeight = textHeight + -2.0 + secondRowTextHeight;
      double firstRowY = (normalBackgroundHeight - contentHeight) / 2.0;
      double secondRowY = firstRowY + textHeight + -2.0;
      return secondRowY + secondRowTextHeight + 50.0;
   }

   private DynamicIslandHud.TabListLayout getTabListLayout(HudRenderer renderer, double scale, double currentWidth) {
      long now = System.currentTimeMillis();
      if (this.cachedTabListLayout != DynamicIslandHud.TabListLayout.EMPTY
         && now - this.cachedTabListLayoutTime < 1000L
         && Math.abs(this.cachedTabListWidth - currentWidth) < 24.0
         && Math.abs(this.cachedTabListScale - scale) < 0.001) {
         return this.cachedTabListLayout;
      }

      this.cachedTabListLayout = this.buildTabListLayout(renderer, scale, currentWidth);
      this.cachedTabListLayoutTime = now;
      this.cachedTabListWidth = currentWidth;
      this.cachedTabListScale = scale;
      return this.cachedTabListLayout;
   }

   private DynamicIslandHud.TabListLayout buildTabListLayout(HudRenderer renderer, double scale, double currentWidth) {
      PlayerTabOverlay tabList = MeteorClient.mc.gui.getTabList();
      AccessorPlayerTabOverlay accessor = (AccessorPlayerTabOverlay)tabList;
      List<PlayerInfo> players = new ArrayList<>(accessor.lurk$getPlayerInfos());
      List<DynamicIslandHud.TextLine> headerLines = componentLines(accessor.lurk$getHeader(), new Color(255, 255, 255, 235));
      List<DynamicIslandHud.TextLine> footerLines = componentLines(accessor.lurk$getFooter(), new Color(190, 195, 205, 220));
      double textScale = scale * 0.8;
      double headerScale = scale * 0.8;
      double pingScale = scale * 0.8;
      double rowHeight = Math.max(renderer.tabTextHeight(true, textScale), renderer.tabTextHeight(true, pingScale)) + 3.0;
      double headerLineHeight = renderer.tabTextHeight(true, headerScale) + 1.0;
      double footerLineHeight = renderer.tabTextHeight(true, textScale) + 1.0;
      double availableWidth = Math.max(128.0, currentWidth - 22.0);
      int maxColumns = Math.max(1, (int)Math.floor((availableWidth + 8.0) / 136.0));
      int columnCount = players.isEmpty() ? 1 : Math.max(1, (int)Math.ceil(players.size() / 20.0));
      columnCount = Math.min(columnCount, maxColumns);
      int rowsPerColumn = players.isEmpty() ? 0 : (int)Math.ceil((double)players.size() / columnCount);
      rowsPerColumn = Math.min(rowsPerColumn, 20);
      int visibleCount = Math.min(players.size(), rowsPerColumn * columnCount);
      int hiddenCount = Math.max(0, players.size() - visibleCount);
      if (hiddenCount > 0) {
         footerLines = new ArrayList<>(footerLines);
         footerLines.add(
            new DynamicIslandHud.TextLine(List.of(new DynamicIslandHud.TextSegment("and " + hiddenCount + " more...", new Color(190, 195, 205, 220))))
         );
      }

      double columnWidth = columnCount == 1 ? availableWidth : (availableWidth - (columnCount - 1) * 8.0) / columnCount;
      double headerHeight = headerLines.isEmpty() ? headerLineHeight + 2.0 : headerLines.size() * headerLineHeight + 3.0;
      double footerHeight = footerLines.isEmpty() ? 0.0 : footerLines.size() * footerLineHeight + 4.0;
      double height = headerHeight + rowsPerColumn * rowHeight + footerHeight;
      List<DynamicIslandHud.PlayerRow> rows = new ArrayList<>(visibleCount);
      double maxNameWidth = columnWidth - renderer.tabTextWidth("9999ms", true, pingScale) - 9.0;

      for (int i = 0; i < visibleCount; i++) {
         PlayerInfo player = players.get(i);
         String ping = Math.max(0, player.getLatency()) + "ms";
         Color fallbackNameColor = player.getGameMode() == GameType.SPECTATOR ? new Color(170, 170, 170, 210) : new Color(255, 255, 255, 235);
         Component displayName = tabList.getNameForDisplay(player);
         List<DynamicIslandHud.TextSegment> name = trimSegments(renderer, componentSegments(displayName, fallbackNameColor), maxNameWidth, textScale);
         rows.add(
            new DynamicIslandHud.PlayerRow(name, ping, getPingColor(player.getLatency()), player.getProfile().id().equals(MeteorClient.mc.player.getUUID()))
         );
      }

      return new DynamicIslandHud.TabListLayout(
         rows,
         headerLines,
         footerLines,
         players.size(),
         columnCount,
         rowsPerColumn,
         visibleCount,
         columnWidth,
         rowHeight,
         headerHeight,
         headerLineHeight,
         footerLineHeight,
         textScale,
         headerScale,
         pingScale,
         height
      );
   }

   private void renderTabList(HudRenderer renderer, double scale, double currentWidth, double y, DynamicIslandHud.TabListLayout layout) {
      if (!(layout.height() <= 0.0)) {
         double centerX = this.x + currentWidth / 2.0;
         double contentX = this.x + 11.0;
         double cursorY = y;
         if (layout.headerLines().isEmpty()) {
            String title = "Player List (" + layout.totalPlayers() + ")";
            renderer.tabText(
               title,
               centerX - renderer.tabTextWidth(title, true, layout.headerScale()) / 2.0,
               cursorY,
               new Color(255, 255, 255, 235),
               true,
               layout.headerScale()
            );
         } else {
            for (DynamicIslandHud.TextLine line : layout.headerLines()) {
               List<DynamicIslandHud.TextSegment> trimmed = trimSegments(renderer, line.segments(), currentWidth - 22.0, layout.headerScale());
               renderCenteredSegments(renderer, trimmed, centerX, cursorY, layout.headerScale());
               cursorY += layout.headerLineHeight();
            }
         }

         cursorY = y + layout.headerHeight();

         for (int i = 0; i < layout.visibleCount(); i++) {
            DynamicIslandHud.PlayerRow player = layout.rows().get(i);
            int column = layout.rowsPerColumn() == 0 ? 0 : i / layout.rowsPerColumn();
            int row = layout.rowsPerColumn() == 0 ? 0 : i % layout.rowsPerColumn();
            double entryX = contentX + column * (layout.columnWidth() + 8.0);
            double entryY = cursorY + row * layout.rowHeight();
            Color rowColor = player.self() ? new Color(255, 255, 255, 42) : new Color(255, 255, 255, row % 2 == 0 ? 18 : 10);
            renderer.quad(entryX, entryY + 0.5, layout.columnWidth(), layout.rowHeight() - 2.0, rowColor);
            double pingWidth = renderer.tabTextWidth(player.ping(), true, layout.pingScale());
            renderSegments(renderer, player.name(), entryX + 3.0, entryY + 1.0, layout.textScale());
            renderer.tabText(player.ping(), entryX + layout.columnWidth() - pingWidth - 3.0, entryY + 1.5, player.pingColor(), true, layout.pingScale());
         }

         if (!layout.footerLines().isEmpty()) {
            cursorY = y + layout.headerHeight() + layout.rowsPerColumn() * layout.rowHeight() + 3.0;

            for (DynamicIslandHud.TextLine line : layout.footerLines()) {
               List<DynamicIslandHud.TextSegment> trimmed = trimSegments(renderer, line.segments(), currentWidth - 22.0, layout.textScale());
               renderCenteredSegments(renderer, trimmed, centerX, cursorY, layout.textScale());
               cursorY += layout.footerLineHeight();
            }
         }
      }
   }

   private static List<DynamicIslandHud.TextLine> componentLines(Component component, Color fallback) {
      if (component != null && !component.getString().isBlank()) {
         List<List<DynamicIslandHud.TextSegment>> lines = new ArrayList<>();
         lines.add(new ArrayList<>());
         component.visit((style, part) -> {
            appendStyledText(lines, part, colorFromStyle(style, fallback), fallback);
            return Optional.empty();
         }, Style.EMPTY);
         List<DynamicIslandHud.TextLine> output = new ArrayList<>();

         for (List<DynamicIslandHud.TextSegment> line : lines) {
            if (!lineText(line).isBlank()) {
               output.add(new DynamicIslandHud.TextLine(line));
            }
         }

         return output;
      } else {
         return List.of();
      }
   }

   private static List<DynamicIslandHud.TextSegment> componentSegments(Component component, Color fallback) {
      if (component != null && !component.getString().isEmpty()) {
         List<List<DynamicIslandHud.TextSegment>> lines = new ArrayList<>();
         lines.add(new ArrayList<>());
         component.visit((style, part) -> {
            appendStyledText(lines, part, colorFromStyle(style, fallback), fallback);
            return Optional.empty();
         }, Style.EMPTY);
         List<DynamicIslandHud.TextSegment> segments = new ArrayList<>();

         for (List<DynamicIslandHud.TextSegment> line : lines) {
            segments.addAll(line);
         }

         return segments.isEmpty() ? List.of(new DynamicIslandHud.TextSegment(component.getString(), fallback)) : segments;
      } else {
         return List.of(new DynamicIslandHud.TextSegment("", fallback));
      }
   }

   private static void appendStyledText(List<List<DynamicIslandHud.TextSegment>> lines, String text, Color color, Color fallback) {
      if (text != null && !text.isEmpty()) {
         Color current = color == null ? fallback : color;
         StringBuilder part = new StringBuilder();

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
               flushSegment(lines, part, current);
               lines.add(new ArrayList<>());
            } else if (c == 167 && i + 1 < text.length()) {
               flushSegment(lines, part, current);
               Color legacyColor = colorFromLegacy(text.charAt(++i), fallback);
               if (legacyColor != null) {
                  current = legacyColor;
               }
            } else {
               part.append(c);
            }
         }

         flushSegment(lines, part, current);
      }
   }

   private static void flushSegment(List<List<DynamicIslandHud.TextSegment>> lines, StringBuilder part, Color color) {
      if (!part.isEmpty()) {
         lines.get(lines.size() - 1).add(new DynamicIslandHud.TextSegment(part.toString(), color));
         part.setLength(0);
      }
   }

   private static String lineText(List<DynamicIslandHud.TextSegment> segments) {
      StringBuilder builder = new StringBuilder();

      for (DynamicIslandHud.TextSegment segment : segments) {
         builder.append(segment.text());
      }

      return builder.toString();
   }

   private static Color colorFromStyle(Style style, Color fallback) {
      TextColor color = style == null ? null : style.getColor();
      if (color == null) {
         return fallback;
      }

      int value = color.getValue();
      return new Color(value >> 16 & 0xFF, value >> 8 & 0xFF, value & 0xFF, fallback.a);
   }

   private static Color colorFromLegacy(char code, Color fallback) {
      return switch (Character.toLowerCase(code)) {
         case '0' -> new Color(0, 0, 0, fallback.a);
         case '1' -> new Color(0, 0, 170, fallback.a);
         case '2' -> new Color(0, 170, 0, fallback.a);
         case '3' -> new Color(0, 170, 170, fallback.a);
         case '4' -> new Color(170, 0, 0, fallback.a);
         case '5' -> new Color(170, 0, 170, fallback.a);
         case '6' -> new Color(255, 170, 0, fallback.a);
         case '7' -> new Color(170, 170, 170, fallback.a);
         case '8' -> new Color(85, 85, 85, fallback.a);
         case '9' -> new Color(85, 85, 255, fallback.a);
         default -> null;
         case 'a' -> new Color(85, 255, 85, fallback.a);
         case 'b' -> new Color(85, 255, 255, fallback.a);
         case 'c' -> new Color(255, 85, 85, fallback.a);
         case 'd' -> new Color(255, 85, 255, fallback.a);
         case 'e' -> new Color(255, 255, 85, fallback.a);
         case 'f' -> new Color(255, 255, 255, fallback.a);
         case 'r' -> fallback;
      };
   }

   private static Color getPingColor(int ping) {
      if (ping < 0) {
         return new Color(140, 145, 155, 220);
      } else if (ping < 80) {
         return new Color(103, 255, 119, 230);
      } else if (ping < 160) {
         return new Color(245, 195, 80, 230);
      } else {
         return ping < 300 ? new Color(255, 150, 75, 230) : new Color(255, 92, 92, 230);
      }
   }

   private static String trimText(HudRenderer renderer, String text, double maxWidth, double scale) {
      if (renderer.tabTextWidth(text, true, scale) <= maxWidth) {
         return text;
      }

      String ellipsis = "...";
      double ellipsisWidth = renderer.tabTextWidth(ellipsis, true, scale);
      StringBuilder kept = new StringBuilder();
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         String codePointText = new String(Character.toChars(codePoint));
         if (renderer.tabTextWidth(kept + codePointText, true, scale) + ellipsisWidth > maxWidth) {
            break;
         }

         kept.append(codePointText);
         i += Character.charCount(codePoint);
      }

      return kept + ellipsis;
   }

   private static String trimHudText(HudRenderer renderer, String text, double maxWidth, double scale) {
      if (text != null && !text.isEmpty()) {
         if (renderer.textWidth(text, true, scale) <= maxWidth) {
            return text;
         }

         String ellipsis = "...";
         double ellipsisWidth = renderer.textWidth(ellipsis, true, scale);
         StringBuilder kept = new StringBuilder();
         int i = 0;

         while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            String codePointText = new String(Character.toChars(codePoint));
            if (renderer.textWidth(kept + codePointText, true, scale) + ellipsisWidth > maxWidth) {
               break;
            }

            kept.append(codePointText);
            i += Character.charCount(codePoint);
         }

         return kept + ellipsis;
      } else {
         return "";
      }
   }

   private static List<DynamicIslandHud.TextSegment> trimSegments(
      HudRenderer renderer, List<DynamicIslandHud.TextSegment> segments, double maxWidth, double scale
   ) {
      if (segmentsWidth(renderer, segments, scale) <= maxWidth) {
         return segments;
      }

      List<DynamicIslandHud.TextSegment> output = new ArrayList<>();
      String ellipsis = "...";
      double ellipsisWidth = renderer.tabTextWidth(ellipsis, true, scale);
      double width = 0.0;
      Color lastColor = new Color(255, 255, 255, 235);

      for (DynamicIslandHud.TextSegment segment : segments) {
         StringBuilder kept = new StringBuilder();
         lastColor = segment.color();
         int i = 0;

         while (i < segment.text().length()) {
            int codePoint = segment.text().codePointAt(i);
            String codePointText = new String(Character.toChars(codePoint));
            String next = kept + codePointText;
            double nextWidth = renderer.tabTextWidth(next, true, scale);
            if (width + nextWidth + ellipsisWidth > maxWidth) {
               if (!kept.isEmpty()) {
                  output.add(new DynamicIslandHud.TextSegment(kept.toString(), segment.color()));
               }

               output.add(new DynamicIslandHud.TextSegment(ellipsis, segment.color()));
               return output;
            }

            kept.append(codePointText);
            i += Character.charCount(codePoint);
         }

         if (!kept.isEmpty()) {
            String text = kept.toString();
            output.add(new DynamicIslandHud.TextSegment(text, segment.color()));
            width += renderer.tabTextWidth(text, true, scale);
         }
      }

      output.add(new DynamicIslandHud.TextSegment(ellipsis, lastColor));
      return output;
   }

   private static void renderCenteredSegments(HudRenderer renderer, List<DynamicIslandHud.TextSegment> segments, double centerX, double y, double scale) {
      renderSegments(renderer, segments, centerX - segmentsWidth(renderer, segments, scale) / 2.0, y, scale);
   }

   private static void renderSegments(HudRenderer renderer, List<DynamicIslandHud.TextSegment> segments, double x, double y, double scale) {
      double cursor = x;

      for (DynamicIslandHud.TextSegment segment : segments) {
         if (!segment.text().isEmpty()) {
            renderer.tabText(segment.text(), cursor, y, segment.color(), true, scale);
            cursor += renderer.tabTextWidth(segment.text(), true, scale);
         }
      }
   }

   private static double segmentsWidth(HudRenderer renderer, List<DynamicIslandHud.TextSegment> segments, double scale) {
      double width = 0.0;

      for (DynamicIslandHud.TextSegment segment : segments) {
         width += renderer.tabTextWidth(segment.text(), true, scale);
      }

      return width;
   }

   private void renderNormalContent(HudRenderer renderer, double scale, double iconSize, double backgroundHeight, double radius) {
      double iconLeftOffset = this.getNormalIconLeftOffset(backgroundHeight, iconSize, radius);
      double iconX = this.x + iconLeftOffset;
      double iconY = this.y + (backgroundHeight - iconSize) / 2.0;
      renderer.texture(ICON, (int)iconX, (int)iconY, (int)iconSize, (int)iconSize, new Color(255, 255, 255, 255));
      this.renderNormalText(renderer, scale, iconSize, backgroundHeight, iconLeftOffset);
   }

   private void renderMusicContent(HudRenderer renderer, double scale, DynamicIslandHud.MusicState state, double backgroundHeight, double radius) {
      double coverSize = musicCoverSize(renderer, scale);
      double coverLeftOffset = this.getNormalIconLeftOffset(backgroundHeight, coverSize, radius);
      double coverX = this.x + coverLeftOffset;
      double coverY = this.y + (backgroundHeight - coverSize) / 2.0;
      double coverRadius = Math.max(4.0, 5.0 * scale);
      Render2DUtil.drawRoundedRect(renderer, coverX, coverY, coverSize, coverSize, coverRadius, new Color(255, 255, 255, 30));
      renderer.texture(ICON, coverX + coverSize * 0.2, coverY + coverSize * 0.2, coverSize * 0.6, coverSize * 0.6, new Color(255, 255, 255, 180));
      Identifier cover = MusicManager.imageTexture(state.coverUrl(), null);
      if (cover != null) {
         renderer.texture(cover, coverX, coverY, coverSize, coverSize, new Color(255, 255, 255, 255));
      }

      double textX = coverX + coverSize + 7.0 * scale;
      double textRight = this.x + this.getWidth() - 8.0 * scale;
      double textWidth = Math.max(20.0, textRight - textX);
      double titleScale = scale;
      double detailScale = scale * 0.7;
      double titleHeight = renderer.textHeight(true, titleScale);
      double artistHeight = renderer.textHeight(true, detailScale);
      double timeHeight = renderer.textHeight(true, detailScale);
      double contentHeight = titleHeight + 1.0 * scale + artistHeight + 3.5 * scale + 2.5 * scale + 2.0 * scale + timeHeight;
      double titleY = this.y + (backgroundHeight - contentHeight) / 2.0;
      double artistY = titleY + titleHeight + 1.0 * scale;
      double barY = artistY + artistHeight + 3.5 * scale;
      double timeY = barY + 2.5 * scale + 2.0 * scale;
      renderer.text(trimHudText(renderer, state.title(), textWidth, titleScale), textX, titleY, new Color(255, 255, 255, 255), true, titleScale);
      renderer.text(trimHudText(renderer, state.artist(), textWidth, detailScale), textX, artistY, new Color(190, 195, 205, 210), true, detailScale);
      double progress = state.durationMs() <= 0L ? 0.0 : Math.max(0.0, Math.min(1.0, (double)state.positionMs() / state.durationMs()));
      Render2DUtil.drawRoundedRect(renderer, textX, barY, textWidth, 2.5 * scale, 2.5 * scale * 0.5, new Color(255, 255, 255, 38));
      Render2DUtil.drawRoundedRect(renderer, textX, barY, textWidth * progress, 2.5 * scale, 2.5 * scale * 0.5, new Color(255, 255, 255, 220));
      String leftTime = formatMusicTime(state.positionMs());
      String rightTime = formatMusicTime(state.durationMs());
      renderer.text(leftTime, textX, timeY, new Color(255, 255, 255, 145), true, detailScale);
      renderer.text(rightTime, textX + textWidth - renderer.textWidth(rightTime, true, detailScale), timeY, new Color(255, 255, 255, 145), true, detailScale);
   }

   private double getNormalIconLeftOffset(double backgroundHeight, double iconSize, double radius) {
      double verticalPadding = Math.max(0.0, (backgroundHeight - iconSize) / 2.0);
      double clampedRadius = Math.min(radius, backgroundHeight / 2.0);
      if (!(clampedRadius <= 0.0) && !(verticalPadding >= clampedRadius)) {
         double dy = clampedRadius - verticalPadding;
         double inset = clampedRadius - Math.sqrt(Math.max(0.0, clampedRadius * clampedRadius - dy * dy));
         return 6.0 + inset;
      } else {
         return 6.0;
      }
   }

   private void renderNormalText(HudRenderer renderer, double scale, double iconSize, double backgroundHeight, double iconLeftOffset) {
      double versionWidth = this.getClientVersionWidth(renderer, scale);
      double devWidth = renderer.textWidth("Developer Version", true, scale);
      String userName = MeteorClient.mc.player.getName().getString();
      double userWidth = this.getUserWidth(renderer, scale, userName);
      double secondRowScale = scale * 0.7;
      String recodedText = "Recoded";
      String fpsText = "FPS: " + MeteorClient.mc.getFps();
      String timeText = "Time: " + this.getTimeString();
      double separatorWidth = 1.0;
      double separatorHeight = backgroundHeight * 0.8;
      double textHeight = renderer.textHeight(true, scale);
      double secondRowTextHeight = renderer.textHeight(true, secondRowScale);
      double contentHeight = textHeight + -2.0 + secondRowTextHeight;
      double textStartX = this.x + iconLeftOffset + iconSize + 6.0;
      double firstRowY = this.y + (backgroundHeight - contentHeight) / 2.0;
      double secondRowY = firstRowY + textHeight + -2.0;
      Color firstRowColor = new Color(255, 255, 255, 255);
      Color secondRowColor = new Color(255, 255, 255, 102);
      double currentX = textStartX;
      this.renderClientName(renderer, currentX, firstRowY, scale);
      double versionStartX = currentX;
      currentX += versionWidth + 10.0;
      double separatorY = this.y + (backgroundHeight - separatorHeight) / 2.0;
      renderer.quad(currentX, separatorY, separatorWidth, separatorHeight, new Color(255, 255, 255, 255));
      currentX += separatorWidth + 10.0;
      renderer.text("Developer Version", currentX, firstRowY, firstRowColor, true, scale);
      double devStartX = currentX;
      currentX += devWidth + 10.0;
      renderer.quad(currentX, separatorY, separatorWidth, separatorHeight, new Color(255, 255, 255, 255));
      currentX += separatorWidth + 10.0;
      this.renderUserText(renderer, currentX, firstRowY, scale, userName, firstRowColor);
      double userStartX = currentX;
      renderer.text(recodedText, versionStartX, secondRowY, secondRowColor, true, secondRowScale);
      renderer.text(fpsText, devStartX, secondRowY, secondRowColor, true, secondRowScale);
      renderer.text(timeText, userStartX, secondRowY, secondRowColor, true, secondRowScale);
   }

   private double getClientVersionWidth(HudRenderer renderer, double scale) {
      return renderer.textWidth("LurkClient", true, scale) + renderer.textWidth(" 6.7.4", true, scale);
   }

   private double getUserWidth(HudRenderer renderer, double scale, String userName) {
      return renderer.textWidth("User: ", true, scale) + renderer.textWidth(userName, true, scale);
   }

   private void renderClientName(HudRenderer renderer, double x, double y, double scale) {
      double cursorX = this.renderGradientText(renderer, "LurkClient", x, y, scale, 0.0);
      renderer.text(" 6.7.4", cursorX, y, new Color(255, 255, 255, 255), true, scale);
   }

   private void renderUserText(HudRenderer renderer, double x, double y, double scale, String userName, Color prefixColor) {
      renderer.text("User: ", x, y, prefixColor, true, scale);
      this.renderGradientText(renderer, userName, x + renderer.textWidth("User: ", true, scale), y, scale, 0.0);
   }

   private double renderGradientText(HudRenderer renderer, String text, double x, double y, double scale, double phaseOffset) {
      long now = System.currentTimeMillis();
      double speed = this.clientGradientSpeed.get().intValue() / 10.0;
      double time = speed <= 0.0 ? 0.0 : now / 1000.0 * speed;
      int charIndex = 0;

      for (int i = 0; i < text.length(); charIndex++) {
         int codePoint = text.codePointAt(i);
         String character = new String(Character.toChars(codePoint));
         double charX = x + renderer.textWidth(text.substring(0, i), false, scale);
         double wave = (Math.sin(time + phaseOffset + charIndex * 0.65) + 1.0) * 0.5;
         Color color = lerpColor(this.clientGradientColor1.get(), this.clientGradientColor2.get(), (float)wave);
         renderer.text(character, charX, y, color, true, scale);
         i += Character.charCount(codePoint);
      }

      return x + renderer.textWidth(text, true, scale);
   }

   private String getTimeString() {
      LocalDateTime now = LocalDateTime.now();
      int hours = now.getHour();
      int minutes = now.getMinute();
      int seconds = now.getSecond();
      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
   }

   private DynamicIslandHud.MusicState getMusicState() {
      MusicPlayer player = Modules.get().get(MusicPlayer.class);
      if (player != null && player.isPlaying()) {
         CloudMusicApi.Song song = player.getCurrentSong();
         if (song == null) {
            return DynamicIslandHud.MusicState.EMPTY;
         }

         String title = song.name();
         if (title == null || title.isBlank()) {
            title = "Unknown Song";
         }

         String artist = song.artist();
         if (artist == null || artist.isBlank()) {
            artist = "Unknown Artist";
         }

         return new DynamicIslandHud.MusicState(true, title, artist, song.coverUrl(), player.getPositionMs(), song.durationMs());
      } else {
         return DynamicIslandHud.MusicState.EMPTY;
      }
   }

   private static double musicCoverSize(HudRenderer renderer, double scale) {
      return musicTextHeight(renderer, scale) * 0.7;
   }

   private static double musicTextHeight(HudRenderer renderer, double scale) {
      double titleScale = scale;
      double detailScale = scale * 0.7;
      return renderer.textHeight(true, titleScale)
         + 1.0 * scale
         + renderer.textHeight(true, detailScale)
         + 3.5 * scale
         + 2.5 * scale
         + 2.0 * scale
         + renderer.textHeight(true, detailScale);
   }

   private static double musicMinWidth(double scale) {
      return cmToGuiPixels(12.0) * scale;
   }

   private static double cmToGuiPixels(double cm) {
      return cm * 37.7952755906;
   }

   private static double mmToGuiPixels(double mm) {
      return cmToGuiPixels(mm / 10.0);
   }

   private static String formatMusicTime(long durationMs) {
      if (durationMs <= 0L) {
         return "--:--";
      }

      long seconds = durationMs / 1000L;
      long minutes = seconds / 60L;
      seconds %= 60L;
      return String.format(Locale.US, "%02d:%02d", minutes, seconds);
   }

   private void renderLowHealthText(HudRenderer renderer, double scale, double currentWidth, double backgroundHeight) {
      String title = "LowHealth";
      String subtitle = "You are in low health!";
      double titleWidth = renderer.textWidth(title, true, scale);
      double subtitleWidth = renderer.textWidth(subtitle, true, scale);
      double textAreaWidth = Math.max(titleWidth, subtitleWidth);
      double titleHeight = renderer.textHeight(true, scale);
      double subtitleHeight = renderer.textHeight(true, scale);
      double contentHeight = titleHeight + 2.0 + subtitleHeight;
      double warningIconSize = this.getWarningIconSize(scale);
      double textAreaX = this.x + 10.0 + warningIconSize + 5.0;
      double availableTextWidth = currentWidth - (textAreaX - this.x) - 8.0;
      double textStartX = textAreaX + Math.max(0.0, (availableTextWidth - textAreaWidth) / 2.0);
      double firstRowY = this.y + (backgroundHeight - contentHeight) / 2.0;
      double secondRowY = firstRowY + titleHeight + 2.0;
      renderer.text(title, textStartX, firstRowY, new Color(255, 255, 255, 255), true, scale);
      renderer.text(subtitle, textStartX, secondRowY, new Color(255, 255, 255, 255), true, scale);
   }

   private DynamicIslandHud.ScaffoldStatus getScaffoldStatus() {
      Scaffold scaffold = Modules.get().get(Scaffold.class);
      if (scaffold != null && scaffold.isActive() && MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         int blocks = scaffold.getBlockCount();
         if (blocks <= 0) {
            return DynamicIslandHud.ScaffoldStatus.EMPTY;
         }

         double dx = MeteorClient.mc.player.getX() - MeteorClient.mc.player.xOld;
         double dz = MeteorClient.mc.player.getZ() - MeteorClient.mc.player.zOld;
         double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;
         return new DynamicIslandHud.ScaffoldStatus(true, blocks, bps);
      } else {
         return DynamicIslandHud.ScaffoldStatus.EMPTY;
      }
   }

   private DynamicIslandHud.BlinkStatus getBlinkStatus() {
      NewBlink blink = Modules.get().get(NewBlink.class);
      return blink != null && blink.isActive() && MeteorClient.mc.player != null && MeteorClient.mc.level != null
         ? new DynamicIslandHud.BlinkStatus(true, blink.getStoredPacketCount(), blink.getPulsePackets())
         : DynamicIslandHud.BlinkStatus.EMPTY;
   }

   private void renderBlinkStatus(HudRenderer renderer, double scale, DynamicIslandHud.BlinkStatus status, double backgroundWidth, double y) {
      double contentWidth = this.calculateBlinkWidth(renderer, scale, status);
      double x = this.x + Math.max(0.0, (backgroundWidth - contentWidth) * 0.5);
      double height = this.calculateBlinkHeight(renderer, scale);
      double textScale = blinkTextScale(scale);
      double textHeight = renderer.textHeight(true, textScale);
      double iconSize = textHeight * 2.0 + scaffoldSize(scale, 2.0);
      double iconX = x + scaffoldSize(scale, 5.0);
      double titleY = y + scaffoldSize(scale, 4.5);
      double infoY = titleY + textHeight + scaffoldSize(scale, 2.0);
      double iconBackgroundSize = iconSize;
      double iconBackgroundX = iconX;
      double iconBackgroundY = titleY;
      double iconRadius = 4.0 * scale;
      Render2DUtil.drawRoundedRect(renderer, iconBackgroundX, iconBackgroundY, iconBackgroundSize, iconBackgroundSize, iconRadius, BLINK_ICON_BACKGROUND);
      double iconTextureSize = iconBackgroundSize * 0.9;
      double iconTextureX = iconBackgroundX + (iconBackgroundSize - iconTextureSize) * 0.5;
      double iconTextureY = iconBackgroundY + (iconBackgroundSize - iconTextureSize) * 0.5;
      renderer.texture(BLINK_ICON, iconTextureX, iconTextureY, iconTextureSize, iconTextureSize, new Color(255, 255, 255, 255));
      double contentX = iconX + iconSize + scaffoldSize(scale, 8.0);
      double textRight = contentX + Math.max(renderer.textWidth("Blink Enable", true, textScale), renderer.textWidth(blinkInfoText(status), true, textScale));
      double progressX = iconX;
      double progressY = iconBackgroundY + iconBackgroundSize + mmToGuiPixels(1.0) * scale;
      double progressWidth = Math.max(1.0, textRight - progressX);
      double progressHeight = Math.max(1.0, mmToGuiPixels(1.0) * scale);
      double targetProgress = status.pulsePackets <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, (double)status.queuedPackets / status.pulsePackets));
      this.blinkProgress = AnimationUtility.ease(this.blinkProgress, (float)targetProgress, 20.0F);
      renderer.text("Blink Enable", contentX, titleY, BLINK_PRIMARY, true, textScale);
      renderer.text(blinkInfoText(status), contentX, infoY, BLINK_SECONDARY, true, textScale);
      Render2DUtil.drawRoundedRect(renderer, progressX, progressY, progressWidth, progressHeight, progressHeight * 0.5, BLINK_PROGRESS_BACKGROUND);
      Render2DUtil.drawRoundedRect(renderer, progressX, progressY, progressWidth * this.blinkProgress, progressHeight, progressHeight * 0.5, BLINK_PROGRESS);
   }

   private void renderScaffoldStatus(HudRenderer renderer, double scale, DynamicIslandHud.ScaffoldStatus status, double backgroundWidth, double y) {
      double contentWidth = this.calculateScaffoldWidth(renderer, scale, status);
      double x = this.x + Math.max(0.0, (backgroundWidth - contentWidth) * 0.5);
      double height = this.calculateScaffoldHeight(renderer, scale);
      double iconSize = scaffoldIcon(scale, 32.0);
      double iconX = x + scaffoldSize(scale, 5.0);
      double iconY = y + (height - iconSize) / 2.0;
      renderer.texture(SCAFFOLD_ICON, iconX, iconY, iconSize, iconSize, new Color(255, 255, 255, 255));
      double contentX = iconX + iconSize + scaffoldSize(scale, 8.0);
      double titleScale = scaffoldTextScale(scale, 0.66);
      double infoScale = scaffoldTextScale(scale, 0.5);
      double titleY = y + scaffoldSize(scale, 4.5);
      double infoY = titleY + renderer.textHeight(true, titleScale) + scaffoldSize(scale, 2.0);
      renderer.text("Scaffold", contentX, titleY, SCAFFOLD_PRIMARY, true, titleScale);
      renderer.text(scaffoldInfoText(status), contentX, infoY, SCAFFOLD_SECONDARY, true, infoScale);
   }

   private static List<DynamicIslandHud.ModuleNotification> getActiveModuleNotifications() {
      long now = System.currentTimeMillis();
      moduleNotifications.removeIf(notification -> now - notification.createTime >= 2200L);
      return List.copyOf(moduleNotifications);
   }

   private void renderModuleNotifications(
      HudRenderer renderer, double scale, List<DynamicIslandHud.ModuleNotification> notifications, double backgroundWidth, double startY
   ) {
      double y = startY;

      for (DynamicIslandHud.ModuleNotification notification : notifications) {
         this.renderModuleNotification(renderer, scale, notification, backgroundWidth, y);
         y += this.calculateModuleNotificationHeight(renderer, scale);
      }
   }

   private void renderModuleNotification(HudRenderer renderer, double scale, DynamicIslandHud.ModuleNotification notification, double backgroundWidth, double y) {
      notification.toggleProgress = AnimationUtility.ease(notification.toggleProgress, notification.toggled ? 1.0F : 0.0F, 5.0F);
      double contentWidth = this.calculateModuleNotificationWidth(renderer, scale, notification);
      double contentX = this.x + Math.max(0.0, (backgroundWidth - contentWidth) * 0.5);
      double height = this.calculateModuleNotificationHeight(renderer, scale);
      double iconAreaX = contentX + moduleHeight(scale, 6.0);
      double iconAreaSize = moduleIcon(scale, 28.0);
      this.renderModuleToggle(renderer, iconAreaX, y, iconAreaSize, height, scale, notification.toggleProgress, notification.toggled);
      double textX = iconAreaX + iconAreaSize + moduleHeight(scale, 6.0);
      double titleScale = moduleTextScale(scale, 0.68);
      double subtitleScale = moduleTextScale(scale, 0.54);
      double titleY = y + moduleHeight(scale, 6.5);
      double subtitleY = titleY + renderer.textHeight(true, titleScale) + moduleHeight(scale, 1.5);
      renderer.text("Module Toggled", textX, titleY, MODULE_TEXT_PRIMARY, true, titleScale);
      this.renderModuleSubtitle(renderer, notification, textX, subtitleY, subtitleScale);
   }

   private void renderModuleToggle(
      HudRenderer renderer, double iconAreaX, double y, double iconAreaSize, double height, double scale, float progress, boolean toggled
   ) {
      double toggleW = moduleIcon(scale, 22.0);
      double toggleH = moduleIcon(scale, 13.0);
      double togglePad = moduleIcon(scale, 1.5);
      double toggleX = iconAreaX + (iconAreaSize - toggleW) * 0.5;
      double toggleY = y + (height - toggleH) * 0.5;
      Color toggleOff = new Color(70, 70, 74, 132);
      Color outlineOff = new Color(120, 120, 126, 155);
      Color toggleOn = toggled ? MODULE_TOGGLE_ENABLED : toggleOff;
      Color outlineOn = toggled ? withAlpha(MODULE_TOGGLE_ENABLED, 255) : outlineOff;
      Color toggleBg = lerpColor(toggleOff, toggleOn, progress);
      Color toggleOutline = lerpColor(outlineOff, outlineOn, progress);
      double outlineSize = moduleHeight(scale, 1.2);
      Render2DUtil.drawRoundedRect(
         renderer,
         toggleX - outlineSize,
         toggleY - outlineSize,
         toggleW + outlineSize * 2.0,
         toggleH + outlineSize * 2.0,
         toggleH * 0.5 + outlineSize,
         toggleOutline
      );
      Render2DUtil.drawRoundedRect(renderer, toggleX, toggleY, toggleW, toggleH, toggleH * 0.5, toggleBg);
      double baseKnobSize = toggleH - togglePad * 2.0;
      double knobSize = baseKnobSize * (0.86 + 0.18 * progress);
      double knobMin = toggleX + togglePad;
      double knobMax = toggleX + toggleW - knobSize - togglePad;
      double knobX = knobMin + (knobMax - knobMin) * progress;
      double knobY = toggleY + (toggleH - knobSize) * 0.5;
      Render2DUtil.drawRoundedRect(renderer, knobX, knobY, knobSize, knobSize, knobSize * 0.5, new Color(255, 255, 255, 245));
   }

   private void renderModuleSubtitle(HudRenderer renderer, DynamicIslandHud.ModuleNotification notification, double x, double y, double scale) {
      String moduleName = notification.moduleName;
      String middle = " has been ";
      String suffix = notification.toggled ? "Enable" : "Disable";
      double cursorX = x;
      if (!moduleName.isEmpty()) {
         renderer.text(moduleName, cursorX, y, MODULE_TEXT_PRIMARY, true, scale);
         cursorX += renderer.textWidth(moduleName, true, scale);
      }

      renderer.text(middle, cursorX, y, MODULE_TEXT_MUTED, true, scale);
      cursorX += renderer.textWidth(middle, true, scale);
      renderer.text(suffix, cursorX, y, notification.toggled ? MODULE_SUCCESS : MODULE_ERROR, true, scale);
   }

   private static String getModuleSubtitle(DynamicIslandHud.ModuleNotification notification) {
      return notification.moduleName + (notification.toggled ? " has been Enable" : " has been Disable");
   }

   private static Color withAlpha(Color color, int alpha) {
      return new Color(color.r, color.g, color.b, Math.max(0, Math.min(255, alpha)));
   }

   private static Color lerpColor(Color from, Color to, float progress) {
      float t = Math.max(0.0F, Math.min(1.0F, progress));
      return new Color(
         (int)(from.r + (to.r - from.r) * t), (int)(from.g + (to.g - from.g) * t), (int)(from.b + (to.b - from.b) * t), (int)(from.a + (to.a - from.a) * t)
      );
   }

   private static double moduleTextScale(double scale, double base) {
      return scale * 1.38 * base;
   }

   private static double moduleIcon(double scale, double base) {
      return scale * 1.38 * base;
   }

   private static double moduleHeight(double scale, double base) {
      return scale * 1.296 * base;
   }

   private static double scaffoldTextScale(double scale, double base) {
      return scale * 1.38 * base;
   }

   private static double scaffoldIcon(double scale, double base) {
      return scale * 1.38 * base;
   }

   private static double scaffoldSize(double scale, double base) {
      return scale * 1.296 * base;
   }

   private static String scaffoldInfoText(DynamicIslandHud.ScaffoldStatus status) {
      return "Blocks remaining: " + status.blocks + " | " + String.format(Locale.US, "%.2f bps", status.bps);
   }

   private static double blinkTextScale(double scale) {
      return scaffoldTextScale(scale, 0.58);
   }

   private static String blinkInfoText(DynamicIslandHud.BlinkStatus status) {
      return "Sending packets: " + status.queuedPackets + "/" + status.pulsePackets;
   }

   private record BlinkStatus(boolean visible, int queuedPackets, int pulsePackets) {
      private static final DynamicIslandHud.BlinkStatus EMPTY = new DynamicIslandHud.BlinkStatus(false, 0, 0);
   }

   private static class ModuleNotification {
      private final String moduleName;
      private final boolean toggled;
      private final long createTime;
      private float toggleProgress;

      private ModuleNotification(String moduleName, boolean toggled, long createTime, float toggleProgress) {
         this.moduleName = moduleName;
         this.toggled = toggled;
         this.createTime = createTime;
         this.toggleProgress = toggleProgress;
      }
   }

   private record MusicState(boolean visible, String title, String artist, String coverUrl, long positionMs, long durationMs) {
      private static final DynamicIslandHud.MusicState EMPTY = new DynamicIslandHud.MusicState(false, "", "", "", 0L, 0L);
   }

   private record PlayerRow(List<DynamicIslandHud.TextSegment> name, String ping, Color pingColor, boolean self) {
   }

   private record ScaffoldStatus(boolean visible, int blocks, double bps) {
      private static final DynamicIslandHud.ScaffoldStatus EMPTY = new DynamicIslandHud.ScaffoldStatus(false, 0, 0.0);
   }

   private record TabListLayout(
      List<DynamicIslandHud.PlayerRow> rows,
      List<DynamicIslandHud.TextLine> headerLines,
      List<DynamicIslandHud.TextLine> footerLines,
      int totalPlayers,
      int columnCount,
      int rowsPerColumn,
      int visibleCount,
      double columnWidth,
      double rowHeight,
      double headerHeight,
      double headerLineHeight,
      double footerLineHeight,
      double textScale,
      double headerScale,
      double pingScale,
      double height
   ) {
      private static final DynamicIslandHud.TabListLayout EMPTY = new DynamicIslandHud.TabListLayout(
         List.of(), List.of(), List.of(), 0, 1, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0
      );
   }

   private record TextLine(List<DynamicIslandHud.TextSegment> segments) {
   }

   private record TextSegment(String text, Color color) {
   }
}
