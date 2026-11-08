package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.AccessorPlayerTabOverlay;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.LiquidGlassSetting;
import meteordevelopment.meteorclient.systems.modules.player.NameProtect;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
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

public class DynamicIslandV2 extends HudElement {
   public static final HudElementInfo<DynamicIslandV2> INFO = new HudElementInfo<>(
      Hud.GROUP, "dynamic-island-v2", "Dynamic Island V2", "Displays a liquid-glass status island.", DynamicIslandV2::new
   );
   private static final Identifier PLAYER_ICON = MeteorClient.identifier("google/player.png");
   private static final String CLIENT_NAME = "Lurk Beta";
   private static final Color LEFT_TINT = new Color(255, 255, 255, 74);
   private static final Color CLIENT_TEXT = new Color(78, 86, 132, 242);
   private static final Color TEXT = new Color(236, 239, 255, 236);
   private static final Color MUTED_TEXT = new Color(207, 214, 236, 218);
   private static final Color SEPARATOR = new Color(255, 255, 255, 104);
   private static final Color SIGNAL = new Color(94, 238, 132, 235);
   private static final float OPEN_ANIMATION_SPEED = 5.0F;
   private static final double TABLIST_HORIZONTAL_PADDING = 11.0;
   private static final double TABLIST_VERTICAL_PADDING = 9.0;
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
   private static final double MODULE_TOGGLE_W = 22.0;
   private static final double MODULE_TOGGLE_H = 13.0;
   private static final double MODULE_TOGGLE_PAD = 1.5;
   private static final Color MODULE_TEXT_PRIMARY = new Color(255, 255, 255, 255);
   private static final Color MODULE_TEXT_MUTED = new Color(160, 160, 160, 255);
   private static final Color MODULE_TOGGLE_ENABLED = new Color(138, 255, 160, 235);
   private static final Color MODULE_SUCCESS = new Color(103, 255, 119, 255);
   private static final Color MODULE_ERROR = new Color(255, 92, 92, 255);
   private static final List<DynamicIslandV2.ModuleNotification> moduleNotifications = new CopyOnWriteArrayList<>();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private float leftBackgroundWidth;
   private float backgroundWidthProgress;
   private float backgroundHeightProgress;
   private DynamicIslandV2.IslandState previousState = DynamicIslandV2.IslandState.NORMAL;
   private DynamicIslandV2.TabListLayout cachedTabListLayout = DynamicIslandV2.TabListLayout.EMPTY;
   private long cachedTabListLayoutTime;
   private double cachedTabListWidth = -1.0;
   private double cachedTabListScale = -1.0;
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("Scale of the island.").defaultValue(1.0).min(0.5).sliderRange(0.5, 3.0).build());
   private final Setting<Double> radius = this.sgGeneral
      .add(new DoubleSetting.Builder().name("radius").description("Corner radius of the island.").defaultValue(14.0).min(0.0).sliderRange(0.0, 32.0).build());
   private final Setting<Boolean> hideUsername = this.sgGeneral
      .add(new BoolSetting.Builder().name("hide-username").description("Hides your username.").defaultValue(true).build());
   private final Setting<SettingColor> backgroundColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color of the liquid glass background.")
            .defaultValue(new SettingColor(0, 0, 0, 100))
            .build()
      );

   private DynamicIslandV2() {
      super(INFO);
      this.setSize(270.0, 44.0);
   }

   public static boolean shouldHideVanillaTabList() {
      if (!isTabListKeyDown()) {
         return false;
      }

      if (!Hud.get().active) {
         return false;
      }

      for (HudElement element : Hud.get()) {
         if (element instanceof DynamicIslandV2 && element.isActive()) {
            return true;
         }
      }

      return false;
   }

   public static void notifyModuleToggle(String moduleName, boolean toggled) {
      if (moduleName != null && !moduleName.isBlank()) {
         moduleNotifications.removeIf(notification -> notification.moduleName.equals(moduleName));
         moduleNotifications.add(new DynamicIslandV2.ModuleNotification(moduleName, toggled, System.currentTimeMillis(), toggled ? 0.0F : 1.0F));
      }
   }

   @Override
   public void render(HudRenderer renderer) {
      double s = this.scale.get();
      double textScale = 0.92 * s;
      double height = 44.0 * s;
      double radius = Math.min(this.radius.get() * s, height * 0.5);
      String username = this.hideUsername.get() ? "Hidden" : this.getUsername();
      String server = this.getServer();
      String ping = (MeteorClient.mc.player == null ? 0 : PlayerUtils.getPing()) + " ms";
      double clientWidth = renderer.textWidth("Lurk Beta", false, textScale);
      double usernameWidth = renderer.textWidth(username, false, textScale);
      double serverWidth = renderer.textWidth(server, false, textScale);
      double pingWidth = renderer.textWidth(ping, false, textScale);
      double leftWidth = clientWidth + 32.0 * s;
      double separatorGap = 11.0 * s;
      double separatorWidth = Math.max(1.0, s);
      double iconSize = 16.0 * s;
      double signalWidth = 12.0 * s;
      double rightWidth = iconSize + 5.0 * s + usernameWidth + 8.0 * s + separatorWidth + 8.0 * s + serverWidth + 10.0 * s + signalWidth + 5.0 * s + pingWidth;
      double width = leftWidth + separatorGap + separatorWidth + separatorGap + rightWidth + 13.0 * s;
      boolean showTabList = isTabListKeyDown();
      List<DynamicIslandV2.ModuleNotification> activeModuleNotifications = getActiveModuleNotifications();
      DynamicIslandV2.IslandState state = showTabList
         ? DynamicIslandV2.IslandState.TABLIST
         : (!activeModuleNotifications.isEmpty() ? DynamicIslandV2.IslandState.MODULE_TOGGLE : DynamicIslandV2.IslandState.NORMAL);
      if (this.previousState != DynamicIslandV2.IslandState.NORMAL && state == DynamicIslandV2.IslandState.NORMAL) {
         this.leftBackgroundWidth = 0.0F;
      }

      if (this.previousState == DynamicIslandV2.IslandState.NORMAL && state != DynamicIslandV2.IslandState.NORMAL) {
         this.backgroundWidthProgress = (float)width;
         this.backgroundHeightProgress = (float)height;
      }

      this.previousState = state;
      if (state == DynamicIslandV2.IslandState.TABLIST) {
         this.renderTabListState(renderer, s, width, height, radius);
      } else if (state == DynamicIslandV2.IslandState.MODULE_TOGGLE) {
         this.renderModuleToggleState(renderer, s, width, height, radius, activeModuleNotifications);
      } else {
         this.animateBackground(width, height);
         this.setSize(width, height);
         this.drawAnimatedBackground(renderer, width, height, radius);
         double innerPad = 4.0 * s;
         double leftHeight = height - innerPad * 2.0;
         double leftRadius = Math.min(Math.max(0.001, radius - innerPad), leftHeight * 0.5);
         this.leftBackgroundWidth = AnimationUtility.ease(this.leftBackgroundWidth, (float)leftWidth, 5.0F);
         double animatedLeftWidth = Math.min(leftWidth, Math.max(0.0, this.leftBackgroundWidth));
         if (animatedLeftWidth > 0.5) {
            this.drawGlass(renderer, this.x + innerPad, this.y + innerPad, animatedLeftWidth, leftHeight, leftRadius, LEFT_TINT);
         }

         double textY = this.y + (height - renderer.textHeight(false, textScale)) * 0.5;
         renderer.text("Lurk Beta", this.x + innerPad + (leftWidth - clientWidth) * 0.5, textY, CLIENT_TEXT, false, textScale);
         double separatorX = this.x + innerPad + leftWidth + separatorGap;
         this.drawSeparator(renderer, separatorX, this.y, height, s);
         double cursor = separatorX + separatorWidth + separatorGap;
         double iconY = this.y + (height - iconSize) * 0.5;
         renderer.texture(PLAYER_ICON, cursor, iconY, iconSize, iconSize, MUTED_TEXT);
         cursor += iconSize + 5.0 * s;
         renderer.text(username, cursor, textY, TEXT, false, textScale);
         cursor += usernameWidth + 8.0 * s;
         this.drawSeparator(renderer, cursor, this.y, height, s);
         cursor += separatorWidth + 8.0 * s;
         renderer.text(server, cursor, textY, MUTED_TEXT, false, textScale);
         cursor += serverWidth + 10.0 * s;
         this.drawSignal(renderer, cursor, this.y + height * 0.5, s);
         cursor += signalWidth + 5.0 * s;
         renderer.text(ping, cursor, textY, MUTED_TEXT, false, textScale);
      }
   }

   private void renderTabListState(HudRenderer renderer, double scale, double defaultWidth, double defaultHeight, double radius) {
      double width = Math.max(defaultWidth, 320.0 * scale);
      DynamicIslandV2.TabListLayout layout = this.getTabListLayout(renderer, scale, width);
      double height = Math.max(defaultHeight, layout.height() + 18.0);
      this.setSize(width, height);
      this.drawGlass(renderer, this.x, this.y, width, height, Math.min(radius, height * 0.5), this.backgroundColor.get());
      this.renderTabList(renderer, scale, width, this.y + (height - layout.height()) * 0.5, layout);
   }

   private void renderModuleToggleState(
      HudRenderer renderer, double scale, double defaultWidth, double defaultHeight, double radius, List<DynamicIslandV2.ModuleNotification> notifications
   ) {
      double contentWidth = this.calculateModuleNotificationsWidth(renderer, scale, notifications);
      double contentHeight = this.calculateModuleNotificationsHeight(renderer, scale, notifications);
      double width = contentWidth;
      double height = Math.max(defaultHeight, contentHeight + 8.0 * scale);
      this.setSize(width, height);
      this.drawGlass(renderer, this.x, this.y, width, height, Math.min(radius, height * 0.5), this.backgroundColor.get());
      this.renderModuleNotifications(renderer, scale, notifications, width, this.y + (height - contentHeight) * 0.5);
   }

   private void animateBackground(double targetWidth, double targetHeight) {
      if (this.backgroundWidthProgress <= 0.0F) {
         this.backgroundWidthProgress = (float)targetWidth;
      }

      if (this.backgroundHeightProgress <= 0.0F) {
         this.backgroundHeightProgress = (float)targetHeight;
      }

      this.backgroundWidthProgress = AnimationUtility.ease(this.backgroundWidthProgress, (float)targetWidth, 5.0F);
      this.backgroundHeightProgress = AnimationUtility.ease(this.backgroundHeightProgress, (float)targetHeight, 5.0F);
   }

   private void drawAnimatedBackground(HudRenderer renderer, double targetWidth, double targetHeight, double radius) {
      double backgroundWidth = Math.max(1.0, this.backgroundWidthProgress);
      double backgroundHeight = Math.max(1.0, this.backgroundHeightProgress);
      double backgroundX = this.x + (targetWidth - backgroundWidth) * 0.5;
      double backgroundY = this.y + (targetHeight - backgroundHeight) * 0.5;
      this.drawGlass(
         renderer, backgroundX, backgroundY, backgroundWidth, backgroundHeight, Math.min(radius, backgroundHeight * 0.5), this.backgroundColor.get()
      );
   }

   private void drawGlass(HudRenderer renderer, double x, double y, double width, double height, double radius, Color tint) {
      LiquidGlassUtil.render(
         renderer,
         x,
         y,
         width,
         height,
         Math.max(0.001, radius),
         tint,
         LiquidGlassSetting.getBlur(),
         LiquidGlassSetting.getNoise(),
         LiquidGlassSetting.getRefraction(),
         0.0F,
         0.0F,
         1.0F,
         1.0F
      );
   }

   private void drawSeparator(HudRenderer renderer, double x, double y, double height, double scale) {
      double lineHeight = height - 20.0 * scale;
      renderer.quad(x, y + (height - lineHeight) * 0.5, Math.max(1.0, scale), lineHeight, SEPARATOR);
   }

   private void drawSignal(HudRenderer renderer, double x, double centerY, double scale) {
      double barWidth = Math.max(1.5, 2.0 * scale);
      double gap = 1.5 * scale;
      double bottom = centerY + 4.0 * scale;

      for (int i = 0; i < 3; i++) {
         double barHeight = (3.0 + i * 2.0) * scale;
         double barX = x + i * (barWidth + gap);
         Render2DUtil.drawRoundedRect(renderer, barX, bottom - barHeight, barWidth, barHeight, barWidth * 0.5, SIGNAL);
      }
   }

   private String getUsername() {
      if (MeteorClient.mc.player == null) {
         return MeteorClient.mc.getUser().getName();
      }

      NameProtect nameProtect = Modules.get().get(NameProtect.class);
      String name = MeteorClient.mc.player.getName().getString();
      return nameProtect != null && nameProtect.isActive() ? nameProtect.getName(name) : name;
   }

   private String getServer() {
      return MeteorClient.mc.getCurrentServer() != null ? MeteorClient.mc.getCurrentServer().ip : "Singleplayer";
   }

   private static List<DynamicIslandV2.ModuleNotification> getActiveModuleNotifications() {
      long now = System.currentTimeMillis();
      moduleNotifications.removeIf(notification -> now - notification.createTime >= 2200L);
      return List.copyOf(moduleNotifications);
   }

   private double calculateModuleNotificationsWidth(HudRenderer renderer, double scale, List<DynamicIslandV2.ModuleNotification> notifications) {
      double width = 0.0;

      for (DynamicIslandV2.ModuleNotification notification : notifications) {
         width = Math.max(width, this.calculateModuleNotificationWidth(renderer, scale, notification));
      }

      return width;
   }

   private double calculateModuleNotificationsHeight(HudRenderer renderer, double scale, List<DynamicIslandV2.ModuleNotification> notifications) {
      return this.calculateModuleNotificationHeight(renderer, scale) * notifications.size();
   }

   private double calculateModuleNotificationWidth(HudRenderer renderer, double scale, DynamicIslandV2.ModuleNotification notification) {
      if (notification == null) {
         return 0.0;
      }

      double titleWidth = renderer.textWidth("Module Toggled", true, moduleTextScale(scale, 0.68));
      double subtitleWidth = renderer.textWidth(getModuleSubtitle(notification), true, moduleTextScale(scale, 0.54));
      return moduleHeight(scale, 6.0) + moduleIcon(scale, 28.0) + moduleHeight(scale, 6.0) + Math.max(titleWidth, subtitleWidth) + moduleHeight(scale, 8.0);
   }

   private double calculateModuleNotificationHeight(HudRenderer renderer, double scale) {
      double titleHeight = renderer.textHeight(true, moduleTextScale(scale, 0.68));
      double subtitleHeight = renderer.textHeight(true, moduleTextScale(scale, 0.54));
      return Math.max(moduleHeight(scale, 30.0), moduleHeight(scale, 6.5) + titleHeight + moduleHeight(scale, 1.5) + subtitleHeight + moduleHeight(scale, 5.0));
   }

   private void renderModuleNotification(HudRenderer renderer, double scale, DynamicIslandV2.ModuleNotification notification, double backgroundWidth, double y) {
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

   private void renderModuleNotifications(
      HudRenderer renderer, double scale, List<DynamicIslandV2.ModuleNotification> notifications, double backgroundWidth, double startY
   ) {
      double rowHeight = this.calculateModuleNotificationHeight(renderer, scale);
      double rowY = startY;

      for (DynamicIslandV2.ModuleNotification notification : notifications) {
         this.renderModuleNotification(renderer, scale, notification, backgroundWidth, rowY);
         rowY += rowHeight;
      }
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

   private void renderModuleSubtitle(HudRenderer renderer, DynamicIslandV2.ModuleNotification notification, double x, double y, double scale) {
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

   private static String getModuleSubtitle(DynamicIslandV2.ModuleNotification notification) {
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

   private static boolean isTabListKeyDown() {
      return MeteorClient.mc.player != null
         && MeteorClient.mc.level != null
         && MeteorClient.mc.getConnection() != null
         && MeteorClient.mc.options.keyPlayerList.isDown();
   }

   private DynamicIslandV2.TabListLayout getTabListLayout(HudRenderer renderer, double scale, double currentWidth) {
      long now = System.currentTimeMillis();
      if (this.cachedTabListLayout != DynamicIslandV2.TabListLayout.EMPTY
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

   private DynamicIslandV2.TabListLayout buildTabListLayout(HudRenderer renderer, double scale, double currentWidth) {
      PlayerTabOverlay tabList = MeteorClient.mc.gui.getTabList();
      AccessorPlayerTabOverlay accessor = (AccessorPlayerTabOverlay)tabList;
      List<PlayerInfo> players = new ArrayList<>(accessor.lurk$getPlayerInfos());
      List<DynamicIslandV2.TextLine> headerLines = componentLines(accessor.lurk$getHeader(), new Color(255, 255, 255, 235));
      List<DynamicIslandV2.TextLine> footerLines = componentLines(accessor.lurk$getFooter(), new Color(190, 195, 205, 220));
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
            new DynamicIslandV2.TextLine(List.of(new DynamicIslandV2.TextSegment("and " + hiddenCount + " more...", new Color(190, 195, 205, 220))))
         );
      }

      double columnWidth = columnCount == 1 ? availableWidth : (availableWidth - (columnCount - 1) * 8.0) / columnCount;
      double headerHeight = headerLines.isEmpty() ? headerLineHeight + 2.0 : headerLines.size() * headerLineHeight + 3.0;
      double footerHeight = footerLines.isEmpty() ? 0.0 : footerLines.size() * footerLineHeight + 4.0;
      double height = headerHeight + rowsPerColumn * rowHeight + footerHeight;
      List<DynamicIslandV2.PlayerRow> rows = new ArrayList<>(visibleCount);
      double maxNameWidth = columnWidth - renderer.tabTextWidth("9999ms", true, pingScale) - 9.0;

      for (int i = 0; i < visibleCount; i++) {
         PlayerInfo player = players.get(i);
         String ping = Math.max(0, player.getLatency()) + "ms";
         Color fallbackNameColor = player.getGameMode() == GameType.SPECTATOR ? new Color(170, 170, 170, 210) : new Color(255, 255, 255, 235);
         Component displayName = tabList.getNameForDisplay(player);
         List<DynamicIslandV2.TextSegment> name = trimSegments(renderer, componentSegments(displayName, fallbackNameColor), maxNameWidth, textScale);
         rows.add(
            new DynamicIslandV2.PlayerRow(
               name,
               ping,
               getPingColor(player.getLatency()),
               MeteorClient.mc.player != null && player.getProfile().id().equals(MeteorClient.mc.player.getUUID())
            )
         );
      }

      return new DynamicIslandV2.TabListLayout(
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

   private void renderTabList(HudRenderer renderer, double scale, double currentWidth, double y, DynamicIslandV2.TabListLayout layout) {
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
            for (DynamicIslandV2.TextLine line : layout.headerLines()) {
               List<DynamicIslandV2.TextSegment> trimmed = trimSegments(renderer, line.segments(), currentWidth - 22.0, layout.headerScale());
               renderCenteredSegments(renderer, trimmed, centerX, cursorY, layout.headerScale());
               cursorY += layout.headerLineHeight();
            }
         }

         cursorY = y + layout.headerHeight();

         for (int i = 0; i < layout.visibleCount(); i++) {
            DynamicIslandV2.PlayerRow player = layout.rows().get(i);
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

            for (DynamicIslandV2.TextLine line : layout.footerLines()) {
               List<DynamicIslandV2.TextSegment> trimmed = trimSegments(renderer, line.segments(), currentWidth - 22.0, layout.textScale());
               renderCenteredSegments(renderer, trimmed, centerX, cursorY, layout.textScale());
               cursorY += layout.footerLineHeight();
            }
         }
      }
   }

   private static List<DynamicIslandV2.TextLine> componentLines(Component component, Color fallback) {
      if (component != null && !component.getString().isBlank()) {
         List<List<DynamicIslandV2.TextSegment>> lines = new ArrayList<>();
         lines.add(new ArrayList<>());
         component.visit((style, part) -> {
            appendStyledText(lines, part, colorFromStyle(style, fallback), fallback);
            return Optional.empty();
         }, Style.EMPTY);
         List<DynamicIslandV2.TextLine> output = new ArrayList<>();

         for (List<DynamicIslandV2.TextSegment> line : lines) {
            if (!lineText(line).isBlank()) {
               output.add(new DynamicIslandV2.TextLine(line));
            }
         }

         return output;
      } else {
         return List.of();
      }
   }

   private static List<DynamicIslandV2.TextSegment> componentSegments(Component component, Color fallback) {
      if (component != null && !component.getString().isEmpty()) {
         List<List<DynamicIslandV2.TextSegment>> lines = new ArrayList<>();
         lines.add(new ArrayList<>());
         component.visit((style, part) -> {
            appendStyledText(lines, part, colorFromStyle(style, fallback), fallback);
            return Optional.empty();
         }, Style.EMPTY);
         List<DynamicIslandV2.TextSegment> segments = new ArrayList<>();

         for (List<DynamicIslandV2.TextSegment> line : lines) {
            segments.addAll(line);
         }

         return segments.isEmpty() ? List.of(new DynamicIslandV2.TextSegment(component.getString(), fallback)) : segments;
      } else {
         return List.of(new DynamicIslandV2.TextSegment("", fallback));
      }
   }

   private static void appendStyledText(List<List<DynamicIslandV2.TextSegment>> lines, String text, Color color, Color fallback) {
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

   private static void flushSegment(List<List<DynamicIslandV2.TextSegment>> lines, StringBuilder part, Color color) {
      if (!part.isEmpty()) {
         lines.get(lines.size() - 1).add(new DynamicIslandV2.TextSegment(part.toString(), color));
         part.setLength(0);
      }
   }

   private static String lineText(List<DynamicIslandV2.TextSegment> segments) {
      StringBuilder builder = new StringBuilder();

      for (DynamicIslandV2.TextSegment segment : segments) {
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

   private static List<DynamicIslandV2.TextSegment> trimSegments(
      HudRenderer renderer, List<DynamicIslandV2.TextSegment> segments, double maxWidth, double scale
   ) {
      if (segmentsWidth(renderer, segments, scale) <= maxWidth) {
         return segments;
      }

      List<DynamicIslandV2.TextSegment> output = new ArrayList<>();
      String ellipsis = "...";
      double ellipsisWidth = renderer.tabTextWidth(ellipsis, true, scale);
      double width = 0.0;
      Color lastColor = new Color(255, 255, 255, 235);

      for (DynamicIslandV2.TextSegment segment : segments) {
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
                  output.add(new DynamicIslandV2.TextSegment(kept.toString(), segment.color()));
               }

               output.add(new DynamicIslandV2.TextSegment(ellipsis, segment.color()));
               return output;
            }

            kept.append(codePointText);
            i += Character.charCount(codePoint);
         }

         if (!kept.isEmpty()) {
            String text = kept.toString();
            output.add(new DynamicIslandV2.TextSegment(text, segment.color()));
            width += renderer.tabTextWidth(text, true, scale);
         }
      }

      output.add(new DynamicIslandV2.TextSegment(ellipsis, lastColor));
      return output;
   }

   private static void renderCenteredSegments(HudRenderer renderer, List<DynamicIslandV2.TextSegment> segments, double centerX, double y, double scale) {
      renderSegments(renderer, segments, centerX - segmentsWidth(renderer, segments, scale) / 2.0, y, scale);
   }

   private static void renderSegments(HudRenderer renderer, List<DynamicIslandV2.TextSegment> segments, double x, double y, double scale) {
      double cursor = x;

      for (DynamicIslandV2.TextSegment segment : segments) {
         if (!segment.text().isEmpty()) {
            renderer.tabText(segment.text(), cursor, y, segment.color(), true, scale);
            cursor += renderer.tabTextWidth(segment.text(), true, scale);
         }
      }
   }

   private static double segmentsWidth(HudRenderer renderer, List<DynamicIslandV2.TextSegment> segments, double scale) {
      double width = 0.0;

      for (DynamicIslandV2.TextSegment segment : segments) {
         width += renderer.tabTextWidth(segment.text(), true, scale);
      }

      return width;
   }

   private enum IslandState {
      NORMAL,
      TABLIST,
      MODULE_TOGGLE;
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

   private record PlayerRow(List<DynamicIslandV2.TextSegment> name, String ping, Color pingColor, boolean self) {
   }

   private record TabListLayout(
      List<DynamicIslandV2.PlayerRow> rows,
      List<DynamicIslandV2.TextLine> headerLines,
      List<DynamicIslandV2.TextLine> footerLines,
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
      private static final DynamicIslandV2.TabListLayout EMPTY = new DynamicIslandV2.TabListLayout(
         List.of(), List.of(), List.of(), 0, 1, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0
      );
   }

   private record TextLine(List<DynamicIslandV2.TextSegment> segments) {
   }

   private record TextSegment(String text, Color color) {
   }
}
