package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ButtonSetting;
import meteordevelopment.meteorclient.utils.render.ButtonRenderQueue;
import meteordevelopment.meteorclient.utils.render.GlobalBackgroundRenderer;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MeteorTitleMenuScreen extends Screen {
   private static final String INFO_TITLE = "Lurk Private Version";
   private static final String INFO_DEV = "Dev:Aurora,OMGX";
   private static final int INFO_X = 4;
   private static final int INFO_Y = 4;
   private static final float INFO_SCALE = 2.0F;
   private static final int INFO_GAP = 4;
   private static final String TITLE_TEXT = "LurkClient";
   private static final int TITLE_GAP = 20;
   private static final int PANEL_HORIZONTAL_PADDING = 40;
   private static final int PANEL_VERTICAL_PADDING = 45;
   private static final int PANEL_RADIUS = 9;
   private static final Color PANEL_COLOR = new Color(0, 0, 0, 60);
   private static final int BUTTON_WIDTH = 180;
   private static final int BUTTON_HEIGHT = 28;
   private static final int BUTTON_GAP = 16;
   private static final int BUTTON_COUNT = 5;

   public MeteorTitleMenuScreen() {
      super(Component.literal("Lurk Menu"));
   }

   @Override
   public void init() {
      this.clearWidgets();
      int startX = this.getButtonStartX();
      int startY = this.getButtonStartY();
      this.addMenuButton("Singleplayer", startX, startY, () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
      this.addMenuButton("Multiplayer", startX, startY + 44, () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this)));
      this.addMenuButton("Options", startX, startY + 88, () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options, false)));
      this.addMenuButton("ClickGui", startX, startY + 132, MeteorClient::openClickGui);
      this.addMenuButton("Quit Game", startX, startY + 176, this.minecraft::stop);
   }

   private void addMenuButton(String text, int x, int y, Runnable action) {
      this.addRenderableWidget(new MeteorTitleMenuScreen.MeteorMenuButton(x, y, 180, 28, Component.literal(text), action));
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
      this.drawBackground(graphics);
      this.queuePanel();
      this.queueTitle();
      this.queueInfoText();
      super.extractRenderState(graphics, mouseX, mouseY, deltaTicks);
   }

   private int getButtonStartX() {
      return (this.width - 180) / 6;
   }

   private int getButtonStartY() {
      return (this.height - this.getTotalContentHeight()) / 2 + this.getTitleHeight() + 20;
   }

   private int getButtonStackHeight() {
      return 204;
   }

   private int getTotalContentHeight() {
      return this.getTitleHeight() + 20 + this.getButtonStackHeight();
   }

   private void queuePanel() {
      int panelX = this.getButtonStartX() - 40;
      int panelY = this.getButtonStartY() - 20 - this.getTitleHeight() - 45;
      int panelWidth = 260;
      int panelHeight = this.getTotalContentHeight() + 90;
      ButtonRenderQueue.add(panelX, panelY, panelWidth, panelHeight, 9.0, ButtonSetting.BackgroundMode.Blur, false, PANEL_COLOR, null, true);
   }

   private float getTitleScale() {
      int textWidth = MeteorClient.mc.font.width("LurkClient");
      return textWidth <= 0 ? 1.0F : 180.0F / textWidth;
   }

   private int getTitleHeight() {
      return (int)Math.ceil(9.0F * this.getTitleScale());
   }

   private void queueTitle() {
      float titleScale = this.getTitleScale();
      int titleX = this.getButtonStartX();
      int titleY = this.getButtonStartY() - 20 - this.getTitleHeight();
      ButtonRenderQueue.addText("LurkClient", titleX, titleY, titleScale, -1, false);
   }

   private void queueInfoText() {
      ButtonRenderQueue.addText("Lurk Private Version", 4, 4, 2.0F, -1, false);
      ButtonRenderQueue.addText("Dev:Aurora,OMGX", 4, 4 + (int)Math.ceil(9.0F * 2.0F) + 4, 2.0F, -1, false);
   }

   private void drawBackground(GuiGraphicsExtractor graphics) {
      GlobalBackgroundRenderer.draw(graphics, this.width, this.height);
   }

   private static class MeteorMenuButton extends AbstractWidget {
      private final Runnable action;
      private float progress;

      private MeteorMenuButton(int x, int y, int width, int height, Component message, Runnable action) {
         super(x, y, width, height, message);
         this.action = action;
      }

      @Override
      public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
         ButtonSetting buttonSetting = Modules.get().get(ButtonSetting.class);
         boolean hovered = this.active && this.isHovered();
         float targetProgress = hovered ? 1.0F : 0.0F;
         float speed = buttonSetting == null ? 5.0F : 5.0F * buttonSetting.animationSpeed.get().floatValue();
         this.progress = AnimationUtility.ease(this.progress, targetProgress, speed);
         SettingColor baseColor = buttonSetting != null && !this.active ? getDisabledColor(buttonSetting) : getNormalColor(buttonSetting);
         SettingColor hoverColor = buttonSetting != null && !this.active ? getDisabledColor(buttonSetting) : getHoverColor(buttonSetting);
         Color backgroundColor = interpolate(baseColor, hoverColor, this.progress);
         ButtonRenderQueue.add(
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            getRadius(buttonSetting),
            getBackgroundMode(buttonSetting),
            buttonSetting != null && buttonSetting.enableGlow.get(),
            backgroundColor,
            this.getMessage(),
            this.active
         );
      }

      @Override
      public void onClick(MouseButtonEvent click, boolean doubled) {
         if (this.active) {
            this.action.run();
         }
      }

      @Override
      public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
         this.defaultButtonNarrationText(narrationElementOutput);
      }

      private static SettingColor getNormalColor(ButtonSetting setting) {
         return setting == null ? new SettingColor(30, 30, 30, 150) : setting.normalColor.get();
      }

      private static SettingColor getHoverColor(ButtonSetting setting) {
         return setting == null ? new SettingColor(255, 255, 255, 128) : setting.hoverColor.get();
      }

      private static SettingColor getDisabledColor(ButtonSetting setting) {
         return setting == null ? new SettingColor(160, 160, 160, 150) : setting.disabledColor.get();
      }

      private static double getRadius(ButtonSetting setting) {
         return setting == null ? 4.0 : setting.backgroundRadius.get();
      }

      private static ButtonSetting.BackgroundMode getBackgroundMode(ButtonSetting setting) {
         return setting == null ? ButtonSetting.BackgroundMode.Blur : setting.backgroundMode.get();
      }

      private static Color interpolate(SettingColor base, SettingColor hover, float progress) {
         int r = (int)(base.r + (hover.r - base.r) * progress);
         int g = (int)(base.g + (hover.g - base.g) * progress);
         int b = (int)(base.b + (hover.b - base.b) * progress);
         int a = (int)(base.a + (hover.a - base.a) * progress);
         return new Color(r, g, b, a);
      }
   }
}
