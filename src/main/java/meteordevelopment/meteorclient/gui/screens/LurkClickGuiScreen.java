package meteordevelopment.meteorclient.gui.screens;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.BlockDataSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.BlockListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.BlockSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ColorSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.EnchantmentListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.EntityTypeListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.FontFaceSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ModuleListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.PacketBoolSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ParticleTypeListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.PotionSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ScreenHandlerSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.SoundEventListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.StatusEffectAmplifierMapSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.StatusEffectListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.StorageBlockListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BlockSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.FontFaceSetting;
import meteordevelopment.meteorclient.settings.GenericSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.ParticleTypeListSetting;
import meteordevelopment.meteorclient.settings.PotionSetting;
import meteordevelopment.meteorclient.settings.ScreenHandlerListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.settings.StatusEffectAmplifierMapSetting;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.systems.macros.Macro;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ButtonSetting;
import meteordevelopment.meteorclient.systems.modules.client.ClickGUI;
import meteordevelopment.meteorclient.systems.profiles.Profile;
import meteordevelopment.meteorclient.systems.profiles.Profiles;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public class LurkClickGuiScreen extends WidgetScreen {
   private final LurkClickGuiScreen.Page initialPage;
   private static final Map<Module, Double> SAVED_MODULE_SETTINGS_SCROLLS = new HashMap<>();
   private static LurkClickGuiScreen.Page savedPage = LurkClickGuiScreen.Page.MODULES;
   private static Category savedCategory;
   private static Module savedModule;
   private static double savedModuleScroll;
   private static double savedTargetModuleScroll;
   private static double savedSettingsScroll;
   private static double savedTargetSettingsScroll;

   public LurkClickGuiScreen(GuiTheme theme) {
      super(theme, "Lurk ClickGUI");
      this.initialPage = savedPage;
   }

   public LurkClickGuiScreen(GuiTheme theme, LurkClickGuiScreen.Page initialPage) {
      super(theme, "Lurk ClickGUI");
      this.initialPage = initialPage;
   }

   @Override
   public void initWidgets() {
      this.add(new LurkClickGuiScreen.LurkPanel()).widget();
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return true;
   }

   private class LurkPanel extends WWidget {
      private static final Identifier CLIENT_ICON = MeteorClient.identifier("icon.png");
      private static final double HEADER_H = 70.0;
      private static final double SIDEBAR_W = 126.0;
      private static final double RADIUS = 10.0;
      private static final double PAD = 12.0;
      private static final double CATEGORY_ROW_H = 28.0;
      private static final double MODULE_ROW_H = 48.0;
      private static final double MODULE_GAP = 7.0;
      private static final double SETTING_ROW_H = 32.0;
      private static final double SETTING_GAP = 6.0;
      private static final double PAGE_ROW_H = 40.0;
      private static final double PAGE_GAP = 7.0;
      private final List<Category> categories = new ArrayList<>();
      private final Map<Object, Double> sliderAnimations = new HashMap<>();
      private final Map<Object, Double> toggleAnimations = new HashMap<>();
      private final Map<Module, Double> moduleSettingsScrolls = LurkClickGuiScreen.SAVED_MODULE_SETTINGS_SCROLLS;
      private LurkClickGuiScreen.Page activePage = LurkClickGuiScreen.this.initialPage;
      private Category selectedCategory = LurkClickGuiScreen.savedCategory;
      private Module selectedModule = LurkClickGuiScreen.this.initialPage == LurkClickGuiScreen.Page.MODULES ? LurkClickGuiScreen.savedModule : null;
      private Macro selectedMacro;
      private Profile selectedProfile;
      private Module bindingModule;
      private KeybindSetting bindingSetting;
      private String query = "";
      private String friendInput = "";
      private boolean searchFocused;
      private boolean friendInputFocused;
      private Setting<?> editingSetting;
      private String editBuffer = "";
      private boolean numberEditFresh;
      private Setting<?> draggingNumberSetting;
      private DoubleListSetting draggingDoubleListSetting;
      private int draggingDoubleListIndex = -1;
      private StringListSetting editingStringListSetting;
      private int editingStringListIndex = -1;
      private double moduleScroll = LurkClickGuiScreen.this.initialPage == LurkClickGuiScreen.Page.MODULES ? LurkClickGuiScreen.savedModuleScroll : 0.0;
      private double settingsScroll = LurkClickGuiScreen.this.initialPage == LurkClickGuiScreen.Page.MODULES ? LurkClickGuiScreen.savedSettingsScroll : 0.0;
      private double targetModuleScroll = LurkClickGuiScreen.this.initialPage == LurkClickGuiScreen.Page.MODULES
         ? LurkClickGuiScreen.savedTargetModuleScroll
         : 0.0;
      private double targetSettingsScroll = LurkClickGuiScreen.this.initialPage == LurkClickGuiScreen.Page.MODULES
         ? LurkClickGuiScreen.savedTargetSettingsScroll
         : 0.0;
      private double lastMouseX;
      private double lastMouseY;
      private double sidebarIndicatorY;
      private double sidebarIndicatorH;
      private double settingsPanelProgress;
      private double panelX;
      private double panelY;
      private double panelW;
      private double panelH;
      private double contentX;
      private double contentY;
      private double contentW;
      private double contentH;
      private double moduleListX;
      private double moduleListY;
      private double moduleListW;
      private double moduleListH;
      private double settingsX;
      private double settingsY;
      private double settingsW;
      private double settingsH;
      private double searchX;
      private double searchY;
      private double searchW;
      private double searchH;

      private LurkPanel() {
         this.activateCurrentPage();
      }

      @Override
      protected void onCalculateSize() {
         this.width = Utils.getWindowWidth();
         this.height = Utils.getWindowHeight();
         this.refreshCategories();
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         this.refreshCategories();
         this.updateSettingsPanelAnimation();
         this.updateLayout();
         this.moduleScroll = this.ease(this.moduleScroll, this.targetModuleScroll, 14.0F);
         this.settingsScroll = this.ease(this.settingsScroll, this.targetSettingsScroll, 14.0F);
         this.clampScrolls();
         this.saveSelectedModuleState();
         this.saveUiState();
         this.drawBackdrop(renderer);
         this.drawBrand(renderer);
         this.drawSidebar(renderer, mouseX, mouseY);
         this.drawHeader(renderer, mouseX, mouseY);
         if (this.activePage == LurkClickGuiScreen.Page.MODULES) {
            this.drawModules(renderer, mouseX, mouseY);
            this.drawSettings(renderer, mouseX, mouseY);
         } else {
            this.drawPage(renderer, mouseX, mouseY);
         }
      }

      @Override
      public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
         double mouseX = click.x();
         double mouseY = click.y();
         int button = click.button();
         if (this.bindingSetting != null) {
            if (this.bindingSetting.get().canBindTo(false, button, 0)) {
               this.bindingSetting.get().set(false, button, 0);
               this.bindingSetting.onChanged();
               this.bindingSetting = null;
               return true;
            } else {
               return true;
            }
         } else if (this.bindingModule == null || button != 2 && button <= 1) {
            this.updateLayout();
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            LurkClickGuiScreen.Page page = this.clickedPage(mouseX, mouseY);
            if (page != null && button == 0) {
               this.selectPage(page);
               return true;
            }

            if (this.activePage != LurkClickGuiScreen.Page.MODULES) {
               this.searchFocused = false;
               if (this.handlePageClick(mouseX, mouseY, button)) {
                  return true;
               }

               this.editingSetting = null;
               this.friendInputFocused = false;
               return false;
            } else {
               if (this.inside(mouseX, mouseY, this.searchX, this.searchY, this.searchW, this.searchH)) {
                  this.searchFocused = true;
                  this.friendInputFocused = false;
                  this.editingSetting = null;
                  this.bindingModule = null;
                  return true;
               }

               this.searchFocused = false;
               this.friendInputFocused = false;
               Category category = this.clickedCategory(mouseX, mouseY);
               if (category != null && button == 0) {
                  this.selectedCategory = category;
                  this.closeSelectedModule();
                  this.targetModuleScroll = 0.0;
                  this.moduleScroll = 0.0;
                  this.saveUiState();
                  return true;
               }

               Module module = this.clickedModule(mouseX, mouseY);
               if (module != null) {
                  if (button == 0) {
                     if (this.clickedKeybind(mouseX, mouseY, module)) {
                        this.bindingModule = module;
                        this.bindingSetting = null;
                        this.editingSetting = null;
                     } else {
                        module.toggle();
                        module.sendToggledMsg();
                     }

                     return true;
                  }

                  if (button == 1) {
                     if (this.selectedModule == module) {
                        this.closeSelectedModule();
                        return true;
                     }

                     this.selectModule(module);
                     return true;
                  }
               }

               if (this.selectedModule != null
                  && this.settingsPanelProgress > 0.92
                  && this.inside(mouseX, mouseY, this.settingsX, this.settingsY, this.settingsW, this.settingsH)
                  && this.handleSettingClick(mouseX, mouseY, button)) {
                  return true;
               }

               this.editingSetting = null;
               return false;
            }
         } else {
            this.bindingModule.keybind.set(false, button, 0);
            this.bindingModule = null;
            return true;
         }
      }

      @Override
      public boolean onMouseReleased(MouseButtonEvent click) {
         this.draggingNumberSetting = null;
         this.draggingDoubleListSetting = null;
         this.draggingDoubleListIndex = -1;
         return false;
      }

      @Override
      public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         if (this.draggingNumberSetting != null) {
            this.updateNumberSetting(this.draggingNumberSetting, mouseX);
         }

         if (this.draggingDoubleListSetting != null && this.draggingDoubleListIndex >= 0) {
            this.updateDoubleListValue(this.draggingDoubleListSetting, this.draggingDoubleListIndex, mouseX);
         }
      }

      @Override
      public boolean onMouseScrolled(double amount) {
         this.updateLayout();
         if ((this.activePage != LurkClickGuiScreen.Page.MODULES || this.settingsPanelProgress > 0.92)
            && this.inside(this.lastMouseX, this.lastMouseY, this.settingsX, this.settingsY, this.settingsW, this.settingsH)) {
            this.targetSettingsScroll = this.clamp(this.targetSettingsScroll - amount * 34.0, 0.0, Math.max(0.0, this.settingsContentHeight() - this.settingsH));
            if (this.activePage == LurkClickGuiScreen.Page.FRIENDS && !this.friendInputVisible(this.targetSettingsScroll)) {
               this.friendInputFocused = false;
            }

            return true;
         } else if (this.inside(this.lastMouseX, this.lastMouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)) {
            this.targetModuleScroll = this.clamp(this.targetModuleScroll - amount * 34.0, 0.0, Math.max(0.0, this.listContentHeight() - this.moduleListH));
            return true;
         } else {
            return false;
         }
      }

      @Override
      public boolean onKeyPressed(KeyEvent input) {
         if (this.bindingSetting != null) {
            if (input.key() != 256 && input.key() != 261 && input.key() != 259) {
               if (this.bindingSetting.get().canBindTo(true, input.key(), input.modifiers())) {
                  this.bindingSetting.get().set(true, input.key(), input.modifiers());
                  this.bindingSetting.onChanged();
                  this.bindingSetting = null;
                  return true;
               } else {
                  return true;
               }
            } else {
               this.bindingSetting.get().reset();
               this.bindingSetting.onChanged();
               this.bindingSetting = null;
               return true;
            }
         } else {
            if (this.bindingModule != null) {
               if (input.key() == 256 || input.key() == 261 || input.key() == 259) {
                  this.bindingModule.keybind.reset();
                  this.bindingModule = null;
                  return true;
               }

               if (this.bindingModule.keybind.canBindTo(true, input.key(), input.modifiers())) {
                  this.bindingModule.keybind.set(true, input.key(), input.modifiers());
                  this.bindingModule = null;
                  return true;
               }
            }

            if (this.editingStringListSetting != null) {
               if (input.key() == 257 || input.key() == 335) {
                  this.applyStringListEdit();
                  return true;
               } else if (input.key() == 256) {
                  this.clearStringListEdit();
                  return true;
               } else if (input.key() == 259 && !this.editBuffer.isEmpty()) {
                  this.editBuffer = this.editBuffer.substring(0, this.editBuffer.length() - 1);
                  return true;
               } else {
                  return true;
               }
            } else if (this.isNumberSetting(this.editingSetting)) {
               if (input.key() != 257 && input.key() != 335) {
                  if (input.key() == 256) {
                     this.editingSetting = null;
                     this.editBuffer = "";
                     this.numberEditFresh = false;
                     return true;
                  }

                  if (input.key() == 259) {
                     if (this.numberEditFresh) {
                        this.editBuffer = "";
                        this.numberEditFresh = false;
                     } else if (!this.editBuffer.isEmpty()) {
                        this.editBuffer = this.editBuffer.substring(0, this.editBuffer.length() - 1);
                     }

                     return true;
                  } else {
                     return true;
                  }
               } else {
                  if (this.applyNumberEdit()) {
                     this.editingSetting = null;
                     this.editBuffer = "";
                     this.numberEditFresh = false;
                  }

                  return true;
               }
            } else if (this.editingSetting instanceof StringSetting stringSetting) {
               if (input.key() == 257 || input.key() == 335) {
                  stringSetting.set(this.editBuffer);
                  this.editingSetting = null;
                  this.editBuffer = "";
                  return true;
               } else if (input.key() == 256) {
                  this.editingSetting = null;
                  this.editBuffer = "";
                  return true;
               } else if (input.key() == 259 && !this.editBuffer.isEmpty()) {
                  this.editBuffer = this.editBuffer.substring(0, this.editBuffer.length() - 1);
                  return true;
               } else {
                  return true;
               }
            } else if (this.friendInputFocused) {
               if (input.key() == 257 || input.key() == 335) {
                  this.addFriend();
                  return true;
               } else if (input.key() == 256) {
                  this.friendInputFocused = false;
                  return true;
               } else if (input.key() == 259 && !this.friendInput.isEmpty()) {
                  this.friendInput = this.friendInput.substring(0, this.friendInput.length() - 1);
                  return true;
               } else {
                  return input.key() == 259;
               }
            } else if (!this.searchFocused) {
               return false;
            } else if (input.key() == 256) {
               this.searchFocused = false;
               this.query = "";
               this.targetModuleScroll = 0.0;
               this.moduleScroll = 0.0;
               return true;
            } else if (input.key() == 259 && !this.query.isEmpty()) {
               this.query = this.query.substring(0, this.query.length() - 1);
               this.targetModuleScroll = 0.0;
               this.moduleScroll = 0.0;
               return true;
            } else {
               return input.key() == 259;
            }
         }
      }

      @Override
      public boolean onCharTyped(CharacterEvent input) {
         int codepoint = input.codepoint();
         if (Character.isISOControl(codepoint)) {
            return false;
         }

         String value = Character.toString(codepoint);
         if (this.isNumberSetting(this.editingSetting)) {
            this.appendNumberEditChar(codepoint, value);
            return true;
         }

         if (this.editingSetting instanceof StringSetting) {
            this.editBuffer = this.editBuffer + value;
            return true;
         }

         if (this.editingStringListSetting != null) {
            if (this.editingStringListSetting.filter != null && !this.editingStringListSetting.filter.filter(this.editBuffer, codepoint)) {
               return true;
            }

            this.editBuffer = this.editBuffer + value;
            return true;
         } else if (this.friendInputFocused) {
            if (codepoint != 32) {
               this.friendInput = this.friendInput + value;
            }

            return true;
         } else if (this.searchFocused) {
            this.query = this.query + value;
            this.targetModuleScroll = 0.0;
            this.moduleScroll = 0.0;
            return true;
         } else {
            return false;
         }
      }

      private void refreshCategories() {
         this.categories.clear();

         for (Category category : Modules.loopCategories()) {
            if (!this.visibleModules(category, false).isEmpty()) {
               this.categories.add(category);
            }
         }

         if (this.selectedCategory == null || !this.categories.contains(this.selectedCategory)) {
            this.selectedCategory = this.categories.isEmpty() ? null : this.categories.get(0);
         }
      }

      private void updateLayout() {
         double maxPanelW = Math.max(360.0, Utils.getWindowWidth() - 52.0);
         double maxPanelH = Math.max(280.0, Utils.getWindowHeight() - 52.0);
         this.panelW = this.clamp(Utils.getWindowWidth() * 0.66, Math.min(620.0, maxPanelW), maxPanelW);
         this.panelH = this.clamp(Utils.getWindowHeight() * 0.66, Math.min(360.0, maxPanelH), maxPanelH);
         this.panelX = (Utils.getWindowWidth() - this.panelW) * 0.5;
         this.panelY = (Utils.getWindowHeight() - this.panelH) * 0.5;
         this.contentX = this.panelX + 126.0;
         this.contentY = this.panelY + this.headerH();
         this.contentW = this.panelW - 126.0;
         this.contentH = this.panelH - this.headerH();
         double innerX = this.contentX + 12.0;
         double innerY = this.contentY + 12.0;
         double innerW = this.contentW - 24.0;
         double innerH = this.contentH - 24.0;
         if (this.activePage == LurkClickGuiScreen.Page.MODULES) {
            double gap = 10.0;
            double targetModuleListW = Math.max(190.0, innerW * 0.46);
            double targetSettingsW = Math.max(220.0, innerW - targetModuleListW - gap);
            double progress = this.settingsPanelProgress;
            this.moduleListX = innerX;
            this.moduleListY = innerY;
            this.moduleListW = innerW + (targetModuleListW - innerW) * progress;
            this.moduleListH = innerH;
            this.settingsW = targetSettingsW * progress;
            this.settingsX = this.moduleListX + this.moduleListW + gap;
            this.settingsY = innerY;
            this.settingsH = innerH;
         } else if (this.activePage != LurkClickGuiScreen.Page.MACROS && this.activePage != LurkClickGuiScreen.Page.PROFILES) {
            this.moduleListX = this.panelX + this.panelW + 10000.0;
            this.moduleListY = innerY;
            this.moduleListW = 0.0;
            this.moduleListH = 0.0;
            this.settingsX = innerX;
            this.settingsY = innerY;
            this.settingsW = innerW;
            this.settingsH = innerH;
         } else {
            double gap = 10.0;
            this.moduleListW = Math.max(190.0, innerW * 0.46);
            this.settingsW = Math.max(220.0, innerW - this.moduleListW - gap);
            this.moduleListX = innerX;
            this.moduleListY = innerY;
            this.moduleListH = innerH;
            this.settingsX = this.moduleListX + this.moduleListW + gap;
            this.settingsY = innerY;
            this.settingsH = innerH;
         }

         this.searchH = Math.max(this.ui(22.0), this.theme.textHeight() + this.ui(8.0));
         this.searchW = this.clamp(
            this.theme.textWidth(this.query.isEmpty() ? "Search" : this.query) + this.ui(22.0), this.ui(118.0), Math.max(this.ui(118.0), this.contentW * 0.34)
         );
         this.searchX = this.panelX + this.panelW - 12.0 - this.searchW;
         this.searchY = this.panelY + this.ui(12.0);
         this.clampScrolls();
      }

      private void drawBackdrop(GuiRenderer renderer) {
         ClickGUI.renderBackground(renderer, this.panelX, this.panelY, this.panelW, this.panelH, 10.0, new Color(18, 18, 20, 82));
         Render2DUtil.drawRoundedRect(renderer, this.panelX, this.panelY, this.panelW, this.panelH, 10.0, new Color(18, 18, 20, 72));
         renderer.quad(this.panelX + 126.0, this.panelY, 1.0, this.panelH, new Color(255, 255, 255, 22));
         renderer.quad(this.contentX, this.panelY + this.headerH(), this.contentW, 1.0, new Color(255, 255, 255, 18));
      }

      private void drawBrand(GuiRenderer renderer) {
         renderer.texture(CLIENT_ICON, this.panelX + 10.0, this.panelY + 10.0, 24.0, 24.0, new Color(255, 255, 255, 245));
         renderer.text("Lurk", this.panelX + 44.0, this.panelY + 8.0, new Color(255, 255, 255, 245), true);
         renderer.text("ClickGui", this.panelX + 45.0, this.panelY + 27.0, new Color(205, 205, 210, 170), false);
      }

      private void drawSidebar(GuiRenderer renderer, double mouseX, double mouseY) {
         double y = this.contentY + 9.0;
         double rowX = this.panelX + 12.0;
         double rowW = 104.0;
         double targetIndicatorY = y;
         double targetIndicatorH = 0.0;
         if (this.activePage != LurkClickGuiScreen.Page.MODULES) {
            renderer.text(this.activePage.title(), rowX, y + 2.0, new Color(255, 255, 255, 235), true);
            y += 30.0;

            for (String line : this.pageSummary()) {
               renderer.text(this.trim(line, rowW, false), rowX, y, new Color(185, 185, 192, 155), false);
               y += 17.0;
            }

            Render2DUtil.drawRoundedRect(renderer, this.panelX + 4.0, this.contentY + 11.0, 2.5, 21.0, 2.0, new Color(232, 232, 232, 205));
         } else {
            double rowH = this.categoryRowH();

            for (Category category : this.categories) {
               boolean selected = category == this.selectedCategory;
               boolean hovered = this.inside(mouseX, mouseY, rowX, y, rowW, rowH);
               if (selected) {
                  targetIndicatorY = y + 4.0;
                  targetIndicatorH = rowH - 8.0;
                  Render2DUtil.drawRoundedRect(renderer, rowX - 5.0, y + 3.0, rowW + 7.0, rowH - 6.0, 7.0, new Color(255, 255, 255, 38));
               } else if (hovered) {
                  Render2DUtil.drawRoundedRect(renderer, rowX - 5.0, y + 3.0, rowW + 7.0, rowH - 6.0, 7.0, new Color(255, 255, 255, 18));
               }

               renderer.text(
                  this.trim(category.name, rowW, false),
                  rowX,
                  y + (rowH - this.theme.textHeight()) * 0.5,
                  !selected && !hovered ? new Color(175, 175, 182, 155) : new Color(255, 255, 255, 235),
                  false
               );
               y += rowH + this.ui(3.0);
            }

            this.sidebarIndicatorY = this.ease(this.sidebarIndicatorY <= 0.0 ? targetIndicatorY : this.sidebarIndicatorY, targetIndicatorY, 16.0F);
            this.sidebarIndicatorH = this.ease(this.sidebarIndicatorH, targetIndicatorH, 16.0F);
            if (this.sidebarIndicatorH > 0.5) {
               Render2DUtil.drawRoundedRect(
                  renderer, this.panelX + 4.0, this.sidebarIndicatorY, 2.5, this.sidebarIndicatorH, 2.0, new Color(232, 232, 232, 205)
               );
            }
         }
      }

      private void drawHeader(GuiRenderer renderer, double mouseX, double mouseY) {
         String title = this.activePage == LurkClickGuiScreen.Page.MODULES
            ? (this.query.isBlank() ? (this.selectedCategory == null ? "Modules" : this.selectedCategory.name) : "Search")
            : this.activePage.title();
         renderer.text(
            this.trim(title, this.searchX - this.contentX - 24.0, true), this.contentX + 12.0, this.panelY + this.ui(14.0), new Color(255, 255, 255, 240), true
         );
         this.drawTabs(renderer, mouseX, mouseY);
         if (this.activePage == LurkClickGuiScreen.Page.MODULES) {
            boolean hovered = this.inside(mouseX, mouseY, this.searchX, this.searchY, this.searchW, this.searchH);
            ButtonSetting buttonSetting = Modules.get().get(ButtonSetting.class);
            Color searchBoxColor = new Color(buttonSetting.searchBoxColor.get());
            Color searchBackground = !hovered && !this.searchFocused
               ? searchBoxColor
               : new Color(searchBoxColor.r, searchBoxColor.g, searchBoxColor.b, Math.min(255, searchBoxColor.a + 24));
            if (buttonSetting.searchBoxBloom.get()) {
               Render2DUtil.drawOutlineShader(this.searchX, this.searchY, this.searchW, this.searchH, this.searchH * 0.5, searchBackground);
            }

            ClickGUI.renderBackground(renderer, this.searchX, this.searchY, this.searchW, this.searchH, this.searchH * 0.5, searchBackground);
            Render2DUtil.drawRoundedRect(
               renderer,
               this.searchX,
               this.searchY,
               this.searchW,
               this.searchH,
               this.searchH * 0.5,
               !hovered && !this.searchFocused ? new Color(255, 255, 255, 10) : new Color(255, 255, 255, 18)
            );
            String text = this.query.isEmpty() && !this.searchFocused ? "Search" : this.query;
            if (this.searchFocused && (System.currentTimeMillis() / 420L & 1L) == 0L) {
               text = text + "|";
            }

            renderer.text(
               this.trim(text, this.searchW - 16.0, false),
               this.searchX + 8.0,
               this.searchY + (this.searchH - this.theme.textHeight()) * 0.5,
               this.query.isEmpty() ? new Color(185, 185, 190, 135) : new Color(255, 255, 255, 220),
               false
            );
         }
      }

      private void drawTabs(GuiRenderer renderer, double mouseX, double mouseY) {
         double x = this.contentX + 12.0;
         double tabH = this.buttonHeight();
         double y = this.panelY + this.headerH() - tabH - this.ui(6.0);
         double maxRight = this.contentX + this.contentW - 12.0;

         for (LurkClickGuiScreen.Page page : this.visiblePages()) {
            String name = page.title();
            double tabW = this.theme.textWidth(name) + this.ui(14.0);
            if (x + tabW > maxRight) {
               break;
            }

            boolean selected = page == this.activePage;
            boolean hovered = this.inside(mouseX, mouseY, x, y, tabW, tabH);
            if (selected || hovered) {
               Render2DUtil.drawRoundedRect(renderer, x, y, tabW, tabH, 7.0, selected ? new Color(255, 255, 255, 32) : new Color(255, 255, 255, 18));
            }

            renderer.text(
               this.trim(name, tabW - this.ui(14.0), false),
               x + this.ui(7.0),
               y + (tabH - this.theme.textHeight()) * 0.5,
               selected ? new Color(255, 255, 255, 230) : (hovered ? new Color(225, 225, 230, 210) : new Color(175, 175, 182, 150)),
               false
            );
            x += tabW + this.ui(4.0);
         }
      }

      private void drawModules(GuiRenderer renderer, double mouseX, double mouseY) {
         List<Module> modules = this.currentModules();
         renderer.scissorStart(this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH);
         double y = this.moduleListY - this.moduleScroll;
         double rowH = this.moduleRowH();
         double gap = this.moduleGap();

         for (Module module : modules) {
            this.drawModuleRow(renderer, module, this.moduleListX, y, this.moduleListW, rowH, mouseX, mouseY);
            y += rowH + gap;
         }

         if (modules.isEmpty()) {
            renderer.text("No modules found", this.moduleListX + 10.0, this.moduleListY + 12.0, new Color(190, 190, 196, 170), false);
         }

         renderer.scissorEnd();
      }

      private void drawModuleRow(GuiRenderer renderer, Module module, double x, double y, double w, double h, double mouseX, double mouseY) {
         if (!(y < this.moduleListY) && !(y + h > this.moduleListY + this.moduleListH)) {
            boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
            boolean active = module.isActive();
            boolean selected = module == this.selectedModule;
            Color bg = active ? new Color(34, 40, 36, hovered ? 150 : 128) : (!hovered && !selected ? new Color(24, 24, 28, 96) : new Color(35, 35, 39, 122));
            Render2DUtil.drawRoundedRect(renderer, x, y, w, h, 7.0, bg);
            Color nameColor = active ? new Color(255, 255, 255, 245) : (hovered ? new Color(232, 232, 236, 235) : new Color(194, 194, 202, 220));
            double textX = x + this.ui(8.0);
            double nameY = y + this.ui(9.0);
            renderer.text(this.trim(module.title, Math.max(10.0, w - this.ui(104.0)), false), textX, nameY, nameColor, false);
            String description = module.description == null ? "" : module.description;
            if (!description.isBlank()) {
               renderer.text(
                  this.trim(description, Math.max(10.0, w - this.ui(104.0)), false),
                  textX,
                  nameY + this.theme.textHeight() + this.ui(5.0),
                  new Color(182, 182, 188, 150),
                  false
               );
            }

            double keyX = this.keybindX(x, w);
            double keySize = Math.max(this.ui(18.0), this.theme.textHeight() + this.ui(8.0));
            double keyY = y + (h - keySize) * 0.5;
            boolean keyHovered = this.inside(mouseX, mouseY, keyX, keyY, keySize, keySize);
            Render2DUtil.drawRoundedRect(
               renderer,
               keyX,
               keyY,
               keySize,
               keySize,
               keySize * 0.5,
               this.bindingModule == module ? new Color(232, 232, 232, 175) : (keyHovered ? new Color(82, 82, 82, 170) : new Color(56, 56, 56, 125))
            );
            renderer.text(
               this.trim(this.bindingModule == module ? "..." : this.compactBind(module), keySize - this.ui(6.0), false),
               keyX + this.ui(4.0),
               keyY + (keySize - this.theme.textHeight()) * 0.5,
               new Color(255, 255, 255, 230),
               false
            );
            double toggleX = x + w - this.ui(28.0);
            double toggleW = this.ui(22.0);
            double toggleH = this.ui(14.0);
            double toggleY = y + (h - toggleH) * 0.5;
            Render2DUtil.drawRoundedRect(
               renderer, toggleX, toggleY, toggleW, toggleH, toggleH * 0.5, active ? new Color(232, 232, 232, 220) : new Color(58, 58, 62, hovered ? 155 : 125)
            );
            double toggleProgress = this.animated(this.toggleAnimations, module, active ? 1.0 : 0.0, 18.0F);
            Render2DUtil.drawRoundedRect(
               renderer,
               toggleX + this.ui(2.0) + (toggleW - this.ui(14.0)) * toggleProgress,
               toggleY + this.ui(2.0),
               this.ui(10.0),
               this.ui(10.0),
               this.ui(5.0),
               new Color(255, 255, 255, 245)
            );
         }
      }

      private void drawSettings(GuiRenderer renderer, double mouseX, double mouseY) {
         if (this.selectedModule != null) {
            if (!(this.settingsPanelProgress < 0.02) && !(this.settingsW <= 1.0)) {
               ClickGUI.renderBackground(renderer, this.settingsX, this.settingsY, this.settingsW, this.settingsH, 8.0, new Color(15, 15, 17, 74));
               Render2DUtil.drawRoundedRect(renderer, this.settingsX, this.settingsY, this.settingsW, this.settingsH, 8.0, new Color(15, 15, 17, 74));
               renderer.scissorStart(this.settingsX, this.settingsY, this.settingsW, this.settingsH);
               double y = this.settingsY + 10.0 - this.settingsScroll;
               renderer.text(this.selectedModule.title, this.settingsX + 10.0, y, new Color(255, 255, 255, 245), true);
               renderer.text("Settings", this.settingsX + 10.0, y + 22.0, new Color(205, 205, 210, 160), false);
               y += 46.0;
               boolean rendered = false;

               for (SettingGroup group : this.selectedModule.settings) {
                  double before = y;
                  y = this.drawSettingGroup(renderer, group, y, mouseX, mouseY);
                  rendered |= y != before;
               }

               if (!rendered) {
                  renderer.text("No settings available", this.settingsX + 10.0, y, new Color(170, 170, 176, 160), false);
               }

               renderer.scissorEnd();
            }
         }
      }

      private void drawPage(GuiRenderer renderer, double mouseX, double mouseY) {
         switch (this.activePage) {
            case CONFIG:
               this.drawSystemSettingsPage(renderer, Config.get().settings, "Client config", "Global client settings", mouseX, mouseY);
               break;
            case GUI:
               this.drawSystemSettingsPage(renderer, this.theme.settings, "Interface", "Theme and layout settings", mouseX, mouseY);
               break;
            case HUD:
               this.drawSystemSettingsPage(renderer, Hud.get().settings, "HUD", "Hud elements and editor tools", mouseX, mouseY);
               break;
            case FRIENDS:
               this.drawFriendsPage(renderer, mouseX, mouseY);
               break;
            case MACROS:
               this.drawMacrosPage(renderer, mouseX, mouseY);
               break;
            case PROFILES:
               this.drawProfilesPage(renderer, mouseX, mouseY);
               break;
            case PATHING:
               this.drawSystemSettingsPage(renderer, PathManagers.get().getSettings().get(), this.activePage.title(), "Pathing settings", mouseX, mouseY);
         }
      }

      private void drawSystemSettingsPage(GuiRenderer renderer, Settings settings, String title, String subtitle, double mouseX, double mouseY) {
         this.drawPane(renderer, this.settingsX, this.settingsY, this.settingsW, this.settingsH);
         renderer.scissorStart(this.settingsX, this.settingsY, this.settingsW, this.settingsH);
         double y = this.settingsY + 10.0 - this.settingsScroll;
         renderer.text(title, this.settingsX + 10.0, y, new Color(255, 255, 255, 245), true);
         renderer.text(subtitle, this.settingsX + 10.0, y + 22.0, new Color(205, 205, 210, 160), false);
         y += 46.0;
         if (this.activePage == LurkClickGuiScreen.Page.GUI) {
            y = this.drawGuiActions(renderer, y, mouseX, mouseY);
         } else if (this.activePage == LurkClickGuiScreen.Page.HUD) {
            y = this.drawHudActions(renderer, y, mouseX, mouseY);
         }

         boolean rendered = false;

         for (SettingGroup group : settings) {
            double before = y;
            y = this.drawSettingGroup(renderer, group, y, mouseX, mouseY);
            rendered |= y != before;
         }

         if (!rendered) {
            renderer.text("No settings available", this.settingsX + 10.0, y, new Color(170, 170, 176, 160), false);
         }

         renderer.scissorEnd();
      }

      private double drawGuiActions(GuiRenderer renderer, double y, double mouseX, double mouseY) {
         double h = this.buttonHeight();
         double gap = this.ui(6.0);
         double x = this.settingsX + this.ui(10.0);
         double resetLayoutW = this.buttonWidth("Reset Layout", 92.0);
         double resetColorsW = this.buttonWidth("Reset Colors", 92.0);
         this.drawPageButton(renderer, x, y, resetLayoutW, h, "Reset Layout", mouseX, mouseY, false);
         this.drawPageButton(renderer, x + resetLayoutW + gap, y, resetColorsW, h, "Reset Colors", mouseX, mouseY, false);
         return y + h + this.ui(12.0);
      }

      private double drawHudActions(GuiRenderer renderer, double y, double mouseX, double mouseY) {
         Hud hud = Hud.get();
         double h = this.buttonHeight();
         double gap = this.ui(6.0);
         double x = this.settingsX + this.ui(10.0);
         String activeText = hud.active ? "Active" : "Inactive";
         double editW = this.buttonWidth("Edit", 58.0);
         double activeW = this.buttonWidth(activeText, 72.0);
         double clearW = this.buttonWidth("Clear", 58.0);
         this.drawPageButton(renderer, x, y, editW, h, "Edit", mouseX, mouseY, false);
         this.drawPageButton(renderer, x + editW + gap, y, activeW, h, activeText, mouseX, mouseY, hud.active);
         this.drawPageButton(renderer, x + editW + activeW + gap * 2.0, y, clearW, h, "Clear", mouseX, mouseY, false);
         y += h + this.ui(8.0);
         double resetElementsW = this.buttonWidth("Reset Elements", 118.0);
         double resetSettingsW = this.buttonWidth("Reset Settings", 106.0);
         this.drawPageButton(renderer, x, y, resetElementsW, h, "Reset Elements", mouseX, mouseY, false);
         this.drawPageButton(renderer, x + resetElementsW + gap, y, resetSettingsW, h, "Reset Settings", mouseX, mouseY, false);
         return y + h + this.ui(12.0);
      }

      private void drawFriendsPage(GuiRenderer renderer, double mouseX, double mouseY) {
         this.drawPane(renderer, this.settingsX, this.settingsY, this.settingsW, this.settingsH);
         renderer.scissorStart(this.settingsX, this.settingsY, this.settingsW, this.settingsH);
         double y = this.settingsY + 10.0 - this.settingsScroll;
         renderer.text("Friends", this.settingsX + 10.0, y, new Color(255, 255, 255, 245), true);
         renderer.text(Friends.get().count() + " saved players", this.settingsX + 10.0, y + 22.0, new Color(205, 205, 210, 160), false);
         y += 46.0;
         double addW = this.buttonWidth("Add", 62.0);
         double addH = this.buttonHeight();
         double inputH = Math.max(this.ui(24.0), addH);
         double inputW = Math.max(this.ui(120.0), this.settingsW - addW - this.ui(30.0));
         boolean inputVisible = this.visibleInSettings(y, inputH);
         if (inputVisible) {
            boolean inputHovered = this.inside(mouseX, mouseY, this.settingsX + this.ui(10.0), y, inputW, inputH);
            Render2DUtil.drawRoundedRect(
               renderer,
               this.settingsX + this.ui(10.0),
               y,
               inputW,
               inputH,
               6.0,
               this.friendInputFocused ? new Color(255, 255, 255, 30) : (inputHovered ? new Color(255, 255, 255, 22) : new Color(255, 255, 255, 14))
            );
            String inputText = this.friendInput.isEmpty() && !this.friendInputFocused ? "Username" : this.friendInput;
            if (this.friendInputFocused && (System.currentTimeMillis() / 420L & 1L) == 0L) {
               inputText = inputText + "|";
            }

            renderer.text(
               this.trim(inputText, inputW - this.ui(16.0), false),
               this.settingsX + this.ui(18.0),
               y + (inputH - this.theme.textHeight()) * 0.5,
               this.friendInput.isEmpty() ? new Color(185, 185, 192, 135) : new Color(235, 235, 240, 220),
               false
            );
            this.drawPageButton(
               renderer, this.settingsX + this.settingsW - addW - this.ui(10.0), y + (inputH - addH) * 0.5, addW, addH, "Add", mouseX, mouseY, false
            );
         } else {
            this.friendInputFocused = false;
         }

         y += inputH + this.ui(12.0);
         if (Friends.get().isEmpty()) {
            renderer.text("No friends added", this.settingsX + 10.0, y, new Color(170, 170, 176, 160), false);
         } else {
            double rowH = this.pageRowH();
            double gap = this.pageGap();

            for (Friend friend : Friends.get()) {
               if (this.isVisibleRow(y, rowH, this.settingsY, this.settingsH)) {
                  friend.requestUpdateInfo();
               }

               this.drawFriendRow(renderer, friend, this.settingsX + 10.0, y, this.settingsW - 20.0, rowH, mouseX, mouseY);
               y += rowH + gap;
            }
         }

         renderer.scissorEnd();
      }

      private void drawFriendRow(GuiRenderer renderer, Friend friend, double x, double y, double w, double h, double mouseX, double mouseY) {
         if (!(y < this.settingsY) && !(y + h > this.settingsY + this.settingsH)) {
            boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
            Render2DUtil.drawRoundedRect(renderer, x, y, w, h, 7.0, hovered ? new Color(255, 255, 255, 24) : new Color(255, 255, 255, 14));
            double headSize = Math.min(this.ui(24.0), h - this.ui(12.0));
            double headY = y + (h - headSize) * 0.5;
            double removeW = this.buttonWidth("Remove", 60.0);
            double removeH = this.buttonHeight();
            renderer.texture(x + this.ui(8.0), headY, headSize, headSize, friend.getHead().needsRotate() ? 90.0 : 0.0, friend.getHead());
            renderer.text(
               this.trim(friend.getName(), w - removeW - this.ui(58.0), false),
               x + this.ui(40.0),
               y + (h - this.theme.textHeight()) * 0.5,
               new Color(235, 235, 240, 225),
               false
            );
            this.drawPageButton(renderer, x + w - removeW - this.ui(10.0), y + (h - removeH) * 0.5, removeW, removeH, "Remove", mouseX, mouseY, false);
         }
      }

      private void drawMacrosPage(GuiRenderer renderer, double mouseX, double mouseY) {
         this.drawListPane(renderer, "Macros", Macros.get().isEmpty() ? "No macros" : "Create and edit macro commands", mouseX, mouseY);
         double headerH = this.listHeaderH();
         renderer.scissorStart(this.moduleListX, this.moduleListY + headerH, this.moduleListW, Math.max(0.0, this.moduleListH - headerH));
         double y = this.moduleListY + headerH + this.ui(2.0) - this.moduleScroll;
         double rowH = this.pageRowH();
         double gap = this.pageGap();

         for (Macro macro : Macros.get()) {
            this.drawMacroRow(renderer, macro, this.moduleListX + 10.0, y, this.moduleListW - 20.0, rowH, mouseX, mouseY);
            y += rowH + gap;
         }

         renderer.scissorEnd();
         this.drawSelectedSettingsPane(
            renderer,
            this.selectedMacro == null ? null : this.selectedMacro.settings,
            this.selectedMacro == null ? "Select a macro" : this.selectedMacro.name.get(),
            "Macro settings",
            mouseX,
            mouseY
         );
      }

      private void drawProfilesPage(GuiRenderer renderer, double mouseX, double mouseY) {
         this.drawListPane(renderer, "Profiles", Profiles.get().isEmpty() ? "No profiles" : "Save and load client states", mouseX, mouseY);
         double headerH = this.listHeaderH();
         renderer.scissorStart(this.moduleListX, this.moduleListY + headerH, this.moduleListW, Math.max(0.0, this.moduleListH - headerH));
         double y = this.moduleListY + headerH + this.ui(2.0) - this.moduleScroll;
         double rowH = this.pageRowH();
         double gap = this.pageGap();

         for (Profile profile : Profiles.get()) {
            this.drawProfileRow(renderer, profile, this.moduleListX + 10.0, y, this.moduleListW - 20.0, rowH, mouseX, mouseY);
            y += rowH + gap;
         }

         renderer.scissorEnd();
         this.drawSelectedSettingsPane(
            renderer,
            this.selectedProfile == null ? null : this.selectedProfile.settings,
            this.selectedProfile == null ? "Select a profile" : this.selectedProfile.name.get(),
            "Profile settings",
            mouseX,
            mouseY
         );
      }

      private void drawListPane(GuiRenderer renderer, String title, String subtitle, double mouseX, double mouseY) {
         this.drawPane(renderer, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH);
         renderer.text(title, this.moduleListX + 10.0, this.moduleListY + 9.0, new Color(255, 255, 255, 245), true);
         double newW = this.buttonWidth("New", 52.0);
         double newH = this.buttonHeight();
         renderer.text(
            this.trim(subtitle, this.moduleListW - newW - this.ui(30.0), false),
            this.moduleListX + 10.0,
            this.moduleListY + 31.0,
            new Color(205, 205, 210, 160),
            false
         );
         this.drawPageButton(
            renderer, this.moduleListX + this.moduleListW - newW - this.ui(10.0), this.moduleListY + this.ui(10.0), newW, newH, "New", mouseX, mouseY, false
         );
      }

      private void drawMacroRow(GuiRenderer renderer, Macro macro, double x, double y, double w, double h, double mouseX, double mouseY) {
         if (!(y > this.moduleListY + this.moduleListH) && !(y + h < this.moduleListY)) {
            boolean selected = macro == this.selectedMacro;
            boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
            Render2DUtil.drawRoundedRect(
               renderer, x, y, w, h, 7.0, selected ? new Color(255, 255, 255, 32) : (hovered ? new Color(255, 255, 255, 24) : new Color(255, 255, 255, 14))
            );
            String name = macro.name.get().isBlank() ? "Unnamed Macro" : macro.name.get();
            double textX = x + this.ui(8.0);
            double firstY = y + (h - this.theme.textHeight() * 2.0 - this.ui(4.0)) * 0.5;
            renderer.text(this.trim(name, w - this.ui(18.0), false), textX, firstY, new Color(235, 235, 240, 225), false);
            renderer.text(
               this.trim(macro.keybind.get().toString(), w - this.ui(18.0), false),
               textX,
               firstY + this.theme.textHeight() + this.ui(4.0),
               new Color(190, 190, 198, 160),
               false
            );
         }
      }

      private void drawProfileRow(GuiRenderer renderer, Profile profile, double x, double y, double w, double h, double mouseX, double mouseY) {
         if (!(y > this.moduleListY + this.moduleListH) && !(y + h < this.moduleListY)) {
            boolean selected = profile == this.selectedProfile;
            boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
            Render2DUtil.drawRoundedRect(
               renderer, x, y, w, h, 7.0, selected ? new Color(255, 255, 255, 32) : (hovered ? new Color(255, 255, 255, 24) : new Color(255, 255, 255, 14))
            );
            double textX = x + this.ui(8.0);
            double firstY = y + (h - this.theme.textHeight() * 2.0 - this.ui(4.0)) * 0.5;
            renderer.text(
               this.trim(profile.name.get().isBlank() ? "Unnamed Profile" : profile.name.get(), w - this.ui(18.0), false),
               textX,
               firstY,
               new Color(235, 235, 240, 225),
               false
            );
            renderer.text(
               this.trim(this.profileFlags(profile), w - this.ui(18.0), false),
               textX,
               firstY + this.theme.textHeight() + this.ui(4.0),
               new Color(190, 190, 198, 160),
               false
            );
         }
      }

      private void drawSelectedSettingsPane(GuiRenderer renderer, Settings settings, String title, String subtitle, double mouseX, double mouseY) {
         this.drawPane(renderer, this.settingsX, this.settingsY, this.settingsW, this.settingsH);
         renderer.scissorStart(this.settingsX, this.settingsY, this.settingsW, this.settingsH);
         double y = this.settingsY + 10.0 - this.settingsScroll;
         renderer.text(this.trim(title, this.settingsW - 20.0, true), this.settingsX + 10.0, y, new Color(255, 255, 255, 245), true);
         renderer.text(subtitle, this.settingsX + 10.0, y + 22.0, new Color(205, 205, 210, 160), false);
         y += 46.0;
         if (settings == null) {
            renderer.text("Pick an item on the left", this.settingsX + 10.0, y, new Color(170, 170, 176, 160), false);
            renderer.scissorEnd();
         } else {
            y = this.drawSelectedActions(renderer, y, mouseX, mouseY);
            boolean rendered = false;

            for (SettingGroup group : settings) {
               double before = y;
               y = this.drawSettingGroup(renderer, group, y, mouseX, mouseY);
               rendered |= y != before;
            }

            if (!rendered) {
               renderer.text("No settings available", this.settingsX + 10.0, y, new Color(170, 170, 176, 160), false);
            }

            renderer.scissorEnd();
         }
      }

      private double drawSelectedActions(GuiRenderer renderer, double y, double mouseX, double mouseY) {
         double h = this.buttonHeight();
         double gap = this.ui(6.0);
         double x = this.settingsX + this.ui(10.0);
         if (this.activePage == LurkClickGuiScreen.Page.MACROS && this.selectedMacro != null) {
            double runW = this.buttonWidth("Run", 54.0);
            double deleteW = this.buttonWidth("Delete", 62.0);
            this.drawPageButton(renderer, x, y, runW, h, "Run", mouseX, mouseY, false);
            this.drawPageButton(renderer, x + runW + gap, y, deleteW, h, "Delete", mouseX, mouseY, false);
            return y + h + this.ui(12.0);
         } else if (this.activePage == LurkClickGuiScreen.Page.PROFILES && this.selectedProfile != null) {
            double saveW = this.buttonWidth("Save", 54.0);
            double loadW = this.buttonWidth("Load", 54.0);
            double deleteW = this.buttonWidth("Delete", 62.0);
            this.drawPageButton(renderer, x, y, saveW, h, "Save", mouseX, mouseY, false);
            this.drawPageButton(renderer, x + saveW + gap, y, loadW, h, "Load", mouseX, mouseY, false);
            this.drawPageButton(renderer, x + saveW + loadW + gap * 2.0, y, deleteW, h, "Delete", mouseX, mouseY, false);
            return y + h + this.ui(12.0);
         } else {
            return y;
         }
      }

      private void drawPane(GuiRenderer renderer, double x, double y, double w, double h) {
         ClickGUI.renderBackground(renderer, x, y, w, h, 8.0, new Color(15, 15, 17, 74));
         Render2DUtil.drawRoundedRect(renderer, x, y, w, h, 8.0, new Color(15, 15, 17, 74));
      }

      private double ui(double value) {
         return this.theme.scale(value);
      }

      private double buttonWidth(String text, double min) {
         return Math.max(this.ui(min), this.theme.textWidth(text) + this.ui(14.0));
      }

      private double buttonHeight() {
         return Math.max(this.ui(22.0), this.theme.textHeight() + this.ui(8.0));
      }

      private double smallButtonHeight() {
         return Math.max(this.ui(20.0), this.theme.textHeight() + this.ui(6.0));
      }

      private double categoryRowH() {
         return Math.max(this.ui(28.0), this.theme.textHeight() + this.ui(14.0));
      }

      private double moduleRowH() {
         return Math.max(this.ui(48.0), this.theme.textHeight() * 2.0 + this.ui(22.0));
      }

      private double moduleGap() {
         return this.ui(7.0);
      }

      private double pageRowH() {
         return Math.max(this.ui(40.0), this.theme.textHeight() * 2.0 + this.ui(20.0));
      }

      private double pageGap() {
         return this.ui(7.0);
      }

      private boolean isVisibleRow(double rowY, double rowH, double clipY, double clipH) {
         return rowY + rowH >= clipY && rowY <= clipY + clipH;
      }

      private double settingGap() {
         return this.ui(6.0);
      }

      private double settingRowH() {
         return Math.max(this.ui(32.0), this.theme.textHeight() + this.ui(18.0));
      }

      private double listHeaderH() {
         return Math.max(this.ui(44.0), this.buttonHeight() + this.ui(22.0));
      }

      private double headerH() {
         return Math.max(this.ui(70.0), this.theme.textHeight() + this.buttonHeight() + this.ui(34.0));
      }

      private void drawPageButton(GuiRenderer renderer, double x, double y, double w, double h, String text, double mouseX, double mouseY, boolean active) {
         boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
         Color color = active ? new Color(143, 235, 176, hovered ? 92 : 72) : (hovered ? new Color(255, 255, 255, 34) : new Color(255, 255, 255, 22));
         Render2DUtil.drawRoundedRect(renderer, x, y, w, h, 6.0, color);
         double pad = this.ui(7.0);
         renderer.text(
            this.trim(text, Math.max(1.0, w - pad * 2.0), false), x + pad, y + (h - this.theme.textHeight()) * 0.5, new Color(235, 235, 240, 220), false
         );
      }

      private double drawSettingGroup(GuiRenderer renderer, SettingGroup group, double y, double mouseX, double mouseY) {
         List<Setting<?>> visibleSettings = this.visibleSettings(group);
         if (visibleSettings.isEmpty()) {
            return y;
         }

         renderer.text(group.name, this.settingsX + 10.0, y, new Color(220, 220, 225, 210), false);
         y += this.ui(18.0);

         for (Setting<?> setting : visibleSettings) {
            double settingH = this.settingHeight(setting);
            this.drawSettingRow(renderer, setting, this.settingsX + 10.0, y, this.settingsW - 20.0, settingH, mouseX, mouseY);
            y += settingH + this.settingGap();
         }

         return y + this.ui(4.0);
      }

      private void drawSettingRow(GuiRenderer renderer, Setting<?> setting, double x, double y, double w, double h, double mouseX, double mouseY) {
         if (!(y > this.settingsY + this.settingsH) && !(y + h < this.settingsY)) {
            boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
            Render2DUtil.drawRoundedRect(renderer, x, y, w, h, 6.0, hovered ? new Color(255, 255, 255, 24) : new Color(255, 255, 255, 14));
            renderer.text(this.trim(setting.title, w * 0.48, false), x + 8.0, y + 9.0, new Color(235, 235, 240, 225), false);
            if (setting instanceof BoolSetting boolSetting) {
               double toggleW = this.ui(22.0);
               double toggleH = this.ui(14.0);
               double knob = this.ui(10.0);
               double toggleX = x + w - toggleW - this.ui(10.0);
               double toggleY = y + (h - toggleH) * 0.5;
               boolean enabled = boolSetting.get();
               Render2DUtil.drawRoundedRect(
                  renderer, toggleX, toggleY, toggleW, toggleH, toggleH * 0.5, enabled ? new Color(143, 235, 176, 220) : new Color(58, 58, 62, 145)
               );
               double toggleProgress = this.animated(this.toggleAnimations, boolSetting, enabled ? 1.0 : 0.0, 18.0F);
               Render2DUtil.drawRoundedRect(
                  renderer,
                  toggleX + this.ui(2.0) + (toggleW - knob - this.ui(4.0)) * toggleProgress,
                  toggleY + this.ui(2.0),
                  knob,
                  knob,
                  knob * 0.5,
                  new Color(255, 255, 255, 245)
               );
            } else if (setting instanceof DoubleSetting doubleSetting) {
               String value = this.editingSetting == doubleSetting
                  ? this.editBuffer + this.editCursor()
                  : this.formatNumber(doubleSetting.get(), doubleSetting.decimalPlaces);
               this.drawSlider(renderer, x, y, w, h, this.numberProgress(doubleSetting), value, doubleSetting);
            } else if (setting instanceof IntSetting intSetting) {
               String value = this.editingSetting == intSetting ? this.editBuffer + this.editCursor() : String.valueOf(intSetting.get());
               this.drawSlider(renderer, x, y, w, h, this.numberProgress(intSetting), value, intSetting);
            } else if (setting instanceof DoubleListSetting doubleListSetting) {
               this.drawDoubleListSetting(renderer, doubleListSetting, x, y, w, h, mouseX, mouseY);
            } else if (setting instanceof StringListSetting stringListSetting) {
               this.drawStringListSetting(renderer, stringListSetting, x, y, w, h, mouseX, mouseY);
            } else if (this.opensSeparateScreen(setting)) {
               String openText = "[Open]";
               double openW = this.buttonWidth(openText, 48.0);
               double openH = Math.max(this.ui(18.0), this.theme.textHeight() + this.ui(5.0));
               double openX = x + w - openW - this.ui(10.0);
               double openY = y + (h - openH) * 0.5;
               if (setting instanceof ColorSetting colorSetting) {
                  double swatchSize = openH;
                  double swatchX = openX - swatchSize - this.ui(6.0);
                  Render2DUtil.drawRoundedRect(renderer, swatchX, openY, swatchSize, swatchSize, 3.0, new Color(255, 255, 255, 42));
                  Render2DUtil.drawRoundedRect(
                     renderer, swatchX + this.ui(2.0), openY + this.ui(2.0), swatchSize - this.ui(4.0), swatchSize - this.ui(4.0), 2.0, colorSetting.get()
                  );
               }

               Render2DUtil.drawRoundedRect(renderer, openX, openY, openW, openH, 6.0, new Color(255, 255, 255, hovered ? 36 : 24));
               renderer.text(
                  this.trim(openText, openW - this.ui(12.0), false),
                  openX + this.ui(6.0),
                  openY + (openH - this.theme.textHeight()) * 0.5,
                  new Color(235, 235, 240, 220),
                  false
               );
            } else if (setting instanceof KeybindSetting keybindSetting) {
               String value = this.bindingSetting == keybindSetting ? "..." : keybindSetting.get().toString();
               double bindW = Math.min(w * 0.45, Math.max(this.ui(42.0), this.theme.textWidth(value) + this.ui(16.0)));
               double bindH = Math.max(this.ui(18.0), this.theme.textHeight() + this.ui(5.0));
               double bindX = x + w - bindW - this.ui(10.0);
               double bindY = y + (h - bindH) * 0.5;
               Render2DUtil.drawRoundedRect(
                  renderer,
                  bindX,
                  bindY,
                  bindW,
                  bindH,
                  6.0,
                  this.bindingSetting == keybindSetting ? new Color(232, 232, 232, 80) : new Color(255, 255, 255, hovered ? 32 : 20)
               );
               renderer.text(
                  this.trim(value, bindW - this.ui(12.0), false),
                  bindX + this.ui(6.0),
                  bindY + (bindH - this.theme.textHeight()) * 0.5,
                  new Color(235, 235, 240, 220),
                  false
               );
            } else {
               String value = this.editingSetting == setting
                  ? this.editBuffer + ((System.currentTimeMillis() / 420L & 1L) == 0L ? "|" : "")
                  : setting.get().toString();
               renderer.text(
                  this.trim(value, w * 0.44, false),
                  x + w - Math.min(w * 0.44, this.theme.textWidth(value)) - 10.0,
                  y + 9.0,
                  new Color(205, 205, 212, 190),
                  false
               );
            }
         }
      }

      private void drawSlider(GuiRenderer renderer, double x, double y, double w, double h, double progress, String value, Object animationKey) {
         double animatedProgress = this.animated(this.sliderAnimations, animationKey, progress, 18.0F);
         double sliderW = Math.min(this.ui(100.0), w * 0.38);
         double sliderX = x + w - sliderW - this.ui(10.0);
         double sliderH = this.ui(8.0);
         double sliderY = y + (h - sliderH) * 0.5;
         Render2DUtil.drawRoundedRect(renderer, sliderX, sliderY, sliderW, sliderH, sliderH * 0.5, new Color(58, 58, 62, 150));
         Render2DUtil.drawRoundedRect(renderer, sliderX, sliderY, sliderW * animatedProgress, sliderH, sliderH * 0.5, new Color(232, 232, 232, 205));
         String label = this.trim(value, Math.max(1.0, sliderX - x - this.ui(18.0)), false);
         renderer.text(
            label, sliderX - this.theme.textWidth(label) - this.ui(8.0), y + (h - this.theme.textHeight()) * 0.5, new Color(205, 205, 212, 190), false
         );
      }

      private void drawDoubleListSetting(GuiRenderer renderer, DoubleListSetting setting, double x, double y, double w, double h, double mouseX, double mouseY) {
         renderer.text(
            this.trim(setting.get().size() + " values", w * 0.44, false),
            x + w - this.theme.textWidth(setting.get().size() + " values") - 10.0,
            y + 9.0,
            new Color(205, 205, 212, 190),
            false
         );
         double rowY = y + this.ui(32.0);
         double valueRowH = this.ui(20.0);

         for (int i = 0; i < setting.get().size(); i++) {
            double value = setting.get().get(i);
            boolean hovered = this.inside(mouseX, mouseY, x + this.ui(8.0), rowY, w - this.ui(16.0), valueRowH);
            Render2DUtil.drawRoundedRect(
               renderer, x + this.ui(8.0), rowY, w - this.ui(16.0), valueRowH, 5.0, hovered ? new Color(255, 255, 255, 26) : new Color(255, 255, 255, 16)
            );
            String label = this.formatNumber(value, setting.decimalPlaces);
            renderer.text(label, x + this.ui(16.0), rowY + (valueRowH - this.theme.textHeight()) * 0.5, new Color(220, 220, 226, 210), false);
            double sliderX = x + this.ui(78.0);
            double sliderW = Math.max(this.ui(40.0), w - this.ui(118.0));
            double progress = this.doubleListProgress(setting, value);
            double animatedProgress = this.animated(this.sliderAnimations, "double-list:" + System.identityHashCode(setting) + ":" + i, progress, 18.0F);
            Render2DUtil.drawRoundedRect(
               renderer, sliderX, rowY + (valueRowH - this.ui(8.0)) * 0.5, sliderW, this.ui(8.0), this.ui(4.0), new Color(58, 58, 62, 150)
            );
            Render2DUtil.drawRoundedRect(
               renderer,
               sliderX,
               rowY + (valueRowH - this.ui(8.0)) * 0.5,
               sliderW * animatedProgress,
               this.ui(8.0),
               this.ui(4.0),
               new Color(232, 232, 232, 205)
            );
            double removeW = this.ui(18.0);
            double removeH = Math.min(valueRowH - this.ui(4.0), this.ui(16.0));
            double removeX = x + w - this.ui(28.0);
            double removeY = rowY + (valueRowH - removeH) * 0.5;
            Render2DUtil.drawRoundedRect(
               renderer,
               removeX,
               removeY,
               removeW,
               removeH,
               5.0,
               new Color(255, 90, 90, this.inside(mouseX, mouseY, removeX, removeY, removeW, removeH) ? 70 : 42)
            );
            renderer.text(
               "-",
               removeX + (removeW - this.theme.textWidth("-")) * 0.5,
               removeY + (removeH - this.theme.textHeight()) * 0.5,
               new Color(255, 255, 255, 230),
               false
            );
            rowY += this.ui(26.0);
         }

         this.drawSmallButton(renderer, x + 8.0, rowY, 44.0, "Add", mouseX, mouseY);
         this.drawSmallButton(renderer, x + 58.0, rowY, 50.0, "Reset", mouseX, mouseY);
      }

      private void drawStringListSetting(GuiRenderer renderer, StringListSetting setting, double x, double y, double w, double h, double mouseX, double mouseY) {
         renderer.text(
            this.trim(setting.get().size() + " values", w * 0.44, false),
            x + w - this.theme.textWidth(setting.get().size() + " values") - 10.0,
            y + 9.0,
            new Color(205, 205, 212, 190),
            false
         );
         double rowY = y + this.ui(32.0);
         double valueRowH = this.ui(20.0);

         for (int i = 0; i < setting.get().size(); i++) {
            boolean editing = this.editingStringListSetting == setting && this.editingStringListIndex == i;
            String raw = editing ? this.editBuffer + ((System.currentTimeMillis() / 420L & 1L) == 0L ? "|" : "") : setting.get().get(i);
            boolean hovered = this.inside(mouseX, mouseY, x + this.ui(8.0), rowY, w - this.ui(42.0), valueRowH);
            Render2DUtil.drawRoundedRect(
               renderer,
               x + this.ui(8.0),
               rowY,
               w - this.ui(42.0),
               valueRowH,
               5.0,
               editing ? new Color(255, 255, 255, 34) : (hovered ? new Color(255, 255, 255, 26) : new Color(255, 255, 255, 16))
            );
            renderer.text(
               this.trim(raw, w - this.ui(60.0), false),
               x + this.ui(16.0),
               rowY + (valueRowH - this.theme.textHeight()) * 0.5,
               new Color(220, 220, 226, 210),
               false
            );
            double removeW = this.ui(18.0);
            double removeH = Math.min(valueRowH - this.ui(4.0), this.ui(16.0));
            double removeX = x + w - this.ui(28.0);
            double removeY = rowY + (valueRowH - removeH) * 0.5;
            Render2DUtil.drawRoundedRect(
               renderer,
               removeX,
               removeY,
               removeW,
               removeH,
               5.0,
               new Color(255, 90, 90, this.inside(mouseX, mouseY, removeX, removeY, removeW, removeH) ? 70 : 42)
            );
            renderer.text(
               "-",
               removeX + (removeW - this.theme.textWidth("-")) * 0.5,
               removeY + (removeH - this.theme.textHeight()) * 0.5,
               new Color(255, 255, 255, 230),
               false
            );
            rowY += this.ui(26.0);
         }

         this.drawSmallButton(renderer, x + 8.0, rowY, 44.0, "Add", mouseX, mouseY);
         this.drawSmallButton(renderer, x + 58.0, rowY, 50.0, "Reset", mouseX, mouseY);
      }

      private void drawSmallButton(GuiRenderer renderer, double x, double y, double w, String text, double mouseX, double mouseY) {
         double h = this.smallButtonHeight();
         boolean hovered = this.inside(mouseX, mouseY, x, y, w, h);
         Render2DUtil.drawRoundedRect(renderer, x, y, w, h, 5.0, hovered ? new Color(255, 255, 255, 34) : new Color(255, 255, 255, 22));
         renderer.text(
            this.trim(text, Math.max(1.0, w - this.ui(14.0)), false),
            x + this.ui(7.0),
            y + (h - this.theme.textHeight()) * 0.5,
            new Color(235, 235, 240, 220),
            false
         );
      }

      private Category clickedCategory(double mouseX, double mouseY) {
         double y = this.contentY + 9.0;
         double rowX = this.panelX + 12.0;
         double rowW = 104.0;
         double rowH = this.categoryRowH();

         for (Category category : this.categories) {
            if (this.inside(mouseX, mouseY, rowX, y, rowW, rowH)) {
               return category;
            }

            y += rowH + this.ui(3.0);
         }

         return null;
      }

      private Module clickedModule(double mouseX, double mouseY) {
         if (!this.inside(mouseX, mouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)) {
            return null;
         }

         List<Module> modules = this.currentModules();
         double y = this.moduleListY - this.moduleScroll;
         double rowH = this.moduleRowH();
         double gap = this.moduleGap();

         for (Module module : modules) {
            if (this.visibleInModuleList(y, rowH) && this.inside(mouseX, mouseY, this.moduleListX, y, this.moduleListW, rowH)) {
               return module;
            }

            y += rowH + gap;
         }

         return null;
      }

      private boolean clickedKeybind(double mouseX, double mouseY, Module module) {
         if (!this.inside(mouseX, mouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)) {
            return false;
         }

         List<Module> modules = this.currentModules();
         double y = this.moduleListY - this.moduleScroll;
         double rowH = this.moduleRowH();
         double gap = this.moduleGap();
         double keySize = Math.max(this.ui(18.0), this.theme.textHeight() + this.ui(8.0));

         for (Module candidate : modules) {
            if (candidate == module) {
               return this.visibleInModuleList(y, rowH)
                  && this.inside(mouseX, mouseY, this.keybindX(this.moduleListX, this.moduleListW), y + (rowH - keySize) * 0.5, keySize, keySize);
            }

            y += rowH + gap;
         }

         return false;
      }

      private boolean handlePageClick(double mouseX, double mouseY, int button) {
         if (button != 0) {
            return false;
         } else if (this.activePage == LurkClickGuiScreen.Page.FRIENDS) {
            return this.handleFriendsClick(mouseX, mouseY);
         } else if (this.activePage == LurkClickGuiScreen.Page.MACROS) {
            return this.handleMacrosClick(mouseX, mouseY) || this.handleSettingClick(mouseX, mouseY, button);
         } else if (this.activePage != LurkClickGuiScreen.Page.PROFILES) {
            return this.handleSystemActions(mouseX, mouseY) ? true : this.handleSettingClick(mouseX, mouseY, button);
         } else {
            return this.handleProfilesClick(mouseX, mouseY) || this.handleSettingClick(mouseX, mouseY, button);
         }
      }

      private boolean handleFriendsClick(double mouseX, double mouseY) {
         double y = this.settingsY + 56.0 - this.settingsScroll;
         double addW = this.buttonWidth("Add", 62.0);
         double addH = this.buttonHeight();
         double inputH = Math.max(this.ui(24.0), addH);
         double inputW = Math.max(this.ui(120.0), this.settingsW - addW - this.ui(30.0));
         boolean inputVisible = this.visibleInSettings(y, inputH);
         if (inputVisible && this.inside(mouseX, mouseY, this.settingsX + this.ui(10.0), y, inputW, inputH)) {
            this.friendInputFocused = true;
            this.editingSetting = null;
            return true;
         }

         if (inputVisible && this.inside(mouseX, mouseY, this.settingsX + this.settingsW - addW - this.ui(10.0), y + (inputH - addH) * 0.5, addW, addH)) {
            this.addFriend();
            return true;
         }

         this.friendInputFocused = false;
         y += inputH + this.ui(12.0);
         double rowH = this.pageRowH();
         double gap = this.pageGap();
         double removeW = this.buttonWidth("Remove", 60.0);
         double removeH = this.buttonHeight();

         for (Friend friend : Friends.get()) {
            double rowX = this.settingsX + 10.0;
            double rowW = this.settingsW - 20.0;
            if (this.inside(mouseX, mouseY, rowX + rowW - removeW - this.ui(10.0), y + (rowH - removeH) * 0.5, removeW, removeH)) {
               Friends.get().remove(friend);
               return true;
            }

            y += rowH + gap;
         }

         return this.inside(mouseX, mouseY, this.settingsX, this.settingsY, this.settingsW, this.settingsH);
      }

      private boolean handleMacrosClick(double mouseX, double mouseY) {
         double newW = this.buttonWidth("New", 52.0);
         double newH = this.buttonHeight();
         if (this.inside(mouseX, mouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)
            && this.inside(mouseX, mouseY, this.moduleListX + this.moduleListW - newW - this.ui(10.0), this.moduleListY + this.ui(10.0), newW, newH)) {
            this.selectedMacro = this.createMacro();
            this.selectSettings(this.selectedMacro.settings);
            return true;
         }

         if (this.inside(mouseX, mouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)) {
            double y = this.moduleListY + this.listHeaderH() + this.ui(2.0) - this.moduleScroll;
            double rowH = this.pageRowH();
            double gap = this.pageGap();

            for (Macro macro : Macros.get()) {
               if (this.visibleInModuleList(y, rowH) && this.inside(mouseX, mouseY, this.moduleListX + 10.0, y, this.moduleListW - 20.0, rowH)) {
                  this.selectedMacro = macro;
                  this.selectedProfile = null;
                  this.resetSettingsScroll();
                  this.selectSettings(macro.settings);
                  return true;
               }

               y += rowH + gap;
            }
         }

         if (this.selectedMacro != null) {
            double actionsY = this.settingsY + 56.0 - this.settingsScroll;
            double h = this.buttonHeight();
            double actionGap = this.ui(6.0);
            double x = this.settingsX + this.ui(10.0);
            double runW = this.buttonWidth("Run", 54.0);
            double deleteW = this.buttonWidth("Delete", 62.0);
            if (this.inside(mouseX, mouseY, x, actionsY, runW, h)) {
               this.selectedMacro.onAction();
               return true;
            }

            if (this.inside(mouseX, mouseY, x + runW + actionGap, actionsY, deleteW, h)) {
               Macros.get().remove(this.selectedMacro);
               this.selectedMacro = null;
               this.resetSettingsScroll();
               return true;
            }
         }

         return false;
      }

      private boolean handleProfilesClick(double mouseX, double mouseY) {
         double newW = this.buttonWidth("New", 52.0);
         double newH = this.buttonHeight();
         if (this.inside(mouseX, mouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)
            && this.inside(mouseX, mouseY, this.moduleListX + this.moduleListW - newW - this.ui(10.0), this.moduleListY + this.ui(10.0), newW, newH)) {
            this.selectedProfile = this.createProfile();
            this.selectSettings(this.selectedProfile.settings);
            return true;
         }

         if (this.inside(mouseX, mouseY, this.moduleListX, this.moduleListY, this.moduleListW, this.moduleListH)) {
            double y = this.moduleListY + this.listHeaderH() + this.ui(2.0) - this.moduleScroll;
            double rowH = this.pageRowH();
            double gap = this.pageGap();

            for (Profile profile : Profiles.get()) {
               if (this.visibleInModuleList(y, rowH) && this.inside(mouseX, mouseY, this.moduleListX + 10.0, y, this.moduleListW - 20.0, rowH)) {
                  this.selectedProfile = profile;
                  this.selectedMacro = null;
                  this.resetSettingsScroll();
                  this.selectSettings(profile.settings);
                  return true;
               }

               y += rowH + gap;
            }
         }

         if (this.selectedProfile != null) {
            double actionsY = this.settingsY + 56.0 - this.settingsScroll;
            double h = this.buttonHeight();
            double actionGap = this.ui(6.0);
            double x = this.settingsX + this.ui(10.0);
            double saveW = this.buttonWidth("Save", 54.0);
            double loadW = this.buttonWidth("Load", 54.0);
            double deleteW = this.buttonWidth("Delete", 62.0);
            if (this.inside(mouseX, mouseY, x, actionsY, saveW, h)) {
               this.saveProfile(this.selectedProfile);
               return true;
            }

            if (this.inside(mouseX, mouseY, x + saveW + actionGap, actionsY, loadW, h)) {
               this.selectedProfile.load();
               return true;
            }

            if (this.inside(mouseX, mouseY, x + saveW + loadW + actionGap * 2.0, actionsY, deleteW, h)) {
               Profiles.get().remove(this.selectedProfile);
               this.selectedProfile = null;
               this.resetSettingsScroll();
               return true;
            }
         }

         return false;
      }

      private boolean handleSystemActions(double mouseX, double mouseY) {
         double y = this.settingsY + 56.0 - this.settingsScroll;
         double h = this.buttonHeight();
         double gap = this.ui(6.0);
         double x = this.settingsX + this.ui(10.0);
         if (this.activePage == LurkClickGuiScreen.Page.GUI) {
            double resetLayoutW = this.buttonWidth("Reset Layout", 92.0);
            double resetColorsW = this.buttonWidth("Reset Colors", 92.0);
            if (this.inside(mouseX, mouseY, x, y, resetLayoutW, h)) {
               this.theme.clearWindowConfigs();
               return true;
            }

            if (this.inside(mouseX, mouseY, x + resetLayoutW + gap, y, resetColorsW, h)) {
               this.theme.settings.reset();
               return true;
            }
         } else if (this.activePage == LurkClickGuiScreen.Page.HUD) {
            Hud hud = Hud.get();
            String activeText = hud.active ? "Active" : "Inactive";
            double editW = this.buttonWidth("Edit", 58.0);
            double activeW = this.buttonWidth(activeText, 72.0);
            double clearW = this.buttonWidth("Clear", 58.0);
            if (this.inside(mouseX, mouseY, x, y, editW, h)) {
               MeteorClient.mc.setScreen(new HudEditorScreen(this.theme));
               return true;
            }

            if (this.inside(mouseX, mouseY, x + editW + gap, y, activeW, h)) {
               hud.active = !hud.active;
               return true;
            }

            if (this.inside(mouseX, mouseY, x + editW + activeW + gap * 2.0, y, clearW, h)) {
               hud.clear();
               return true;
            }

            y += h + this.ui(8.0);
            double resetElementsW = this.buttonWidth("Reset Elements", 118.0);
            double resetSettingsW = this.buttonWidth("Reset Settings", 106.0);
            if (this.inside(mouseX, mouseY, x, y, resetElementsW, h)) {
               hud.resetToDefaultElements();
               return true;
            }

            if (this.inside(mouseX, mouseY, x + resetElementsW + gap, y, resetSettingsW, h)) {
               hud.settings.reset();
               return true;
            }
         }

         return false;
      }

      private boolean handleSettingClick(double mouseX, double mouseY, int button) {
         if (button != 0) {
            return false;
         }

         Settings settings = this.currentSettings();
         if (settings == null) {
            return false;
         }

         double y = this.settingsGroupsStartY();

         for (SettingGroup group : settings) {
            List<Setting<?>> visibleSettings = this.visibleSettings(group);
            if (!visibleSettings.isEmpty()) {
               y += this.ui(18.0);

               for (Setting<?> setting : visibleSettings) {
                  double settingH = this.settingHeight(setting);
                  if (this.inside(mouseX, mouseY, this.settingsX + 10.0, y, this.settingsW - 20.0, settingH)) {
                     this.activateSetting(setting, mouseX, mouseY, this.settingsX + 10.0, y, this.settingsW - 20.0, settingH);
                     return true;
                  }

                  y += settingH + this.settingGap();
               }

               y += this.ui(4.0);
            }
         }

         return false;
      }

      private void activateSetting(Setting<?> setting, double mouseX, double mouseY, double rowX, double rowY, double rowW, double rowH) {
         this.editingSetting = null;
         this.bindingModule = null;
         this.bindingSetting = null;
         if (setting instanceof BoolSetting boolSetting) {
            boolSetting.set(!boolSetting.get());
         } else if (setting instanceof EnumSetting enumSetting) {
            List<String> suggestions = enumSetting.getSuggestions();
            int index = suggestions.indexOf(enumSetting.get().toString());
            enumSetting.parse(suggestions.get((index + 1) % suggestions.size()));
         } else if (!(setting instanceof DoubleSetting) && !(setting instanceof IntSetting)) {
            if (setting instanceof StringSetting stringSetting) {
               this.editingSetting = stringSetting;
               this.editBuffer = stringSetting.get();
            } else if (setting instanceof DoubleListSetting doubleListSetting) {
               this.handleDoubleListClick(doubleListSetting, mouseX, mouseY, rowX, rowY, rowW);
            } else if (setting instanceof StringListSetting stringListSetting) {
               this.handleStringListClick(stringListSetting, mouseX, mouseY, rowX, rowY, rowW);
            } else if (setting instanceof KeybindSetting keybindSetting) {
               this.bindingSetting = keybindSetting;
            } else if (!this.openSettingScreen(setting)) {
               ;
            }
         } else {
            if (this.numberSliderHit(setting, mouseX, mouseY, rowX, rowY, rowW, rowH)) {
               this.draggingNumberSetting = setting;
               this.updateNumberSetting(setting, mouseX);
            } else {
               this.beginNumberEdit(setting);
            }
         }
      }

      private void handleDoubleListClick(DoubleListSetting setting, double mouseX, double mouseY, double rowX, double rowY, double rowW) {
         List<Double> values = new ArrayList<>(setting.get());
         double valueY = rowY + this.ui(32.0);
         double valueRowH = this.ui(20.0);

         for (int i = 0; i < values.size(); i++) {
            double removeW = this.ui(18.0);
            double removeH = Math.min(valueRowH - this.ui(4.0), this.ui(16.0));
            double removeX = rowX + rowW - this.ui(28.0);
            double removeY = valueY + (valueRowH - removeH) * 0.5;
            if (this.inside(mouseX, mouseY, removeX, removeY, removeW, removeH)) {
               values.remove(i);
               setting.set(values);
               return;
            }

            if (this.inside(mouseX, mouseY, rowX + this.ui(78.0), valueY, Math.max(this.ui(40.0), rowW - this.ui(118.0)), valueRowH)) {
               this.draggingDoubleListSetting = setting;
               this.draggingDoubleListIndex = i;
               this.updateDoubleListValue(setting, i, mouseX);
               return;
            }

            valueY += this.ui(26.0);
         }

         double buttonH = this.smallButtonHeight();
         if (this.inside(mouseX, mouseY, rowX + 8.0, valueY, 44.0, buttonH)) {
            values.add(this.defaultDoubleListValue(setting));
            setting.set(values);
         } else {
            if (this.inside(mouseX, mouseY, rowX + 58.0, valueY, 50.0, buttonH)) {
               setting.reset();
            }
         }
      }

      private void handleStringListClick(StringListSetting setting, double mouseX, double mouseY, double rowX, double rowY, double rowW) {
         List<String> values = new ArrayList<>(setting.get());
         double valueY = rowY + this.ui(32.0);
         double valueRowH = this.ui(20.0);

         for (int i = 0; i < values.size(); i++) {
            double removeW = this.ui(18.0);
            double removeH = Math.min(valueRowH - this.ui(4.0), this.ui(16.0));
            double removeX = rowX + rowW - this.ui(28.0);
            double removeY = valueY + (valueRowH - removeH) * 0.5;
            if (this.inside(mouseX, mouseY, removeX, removeY, removeW, removeH)) {
               values.remove(i);
               setting.set(values);
               this.clearStringListEdit();
               return;
            }

            if (this.inside(mouseX, mouseY, rowX + this.ui(8.0), valueY, rowW - this.ui(42.0), valueRowH)) {
               this.editingStringListSetting = setting;
               this.editingStringListIndex = i;
               this.editBuffer = values.get(i);
               this.editingSetting = null;
               return;
            }

            valueY += this.ui(26.0);
         }

         double buttonH = this.smallButtonHeight();
         if (this.inside(mouseX, mouseY, rowX + 8.0, valueY, 44.0, buttonH)) {
            values.add("");
            setting.set(values);
            this.editingStringListSetting = setting;
            this.editingStringListIndex = values.size() - 1;
            this.editBuffer = "";
         } else {
            if (this.inside(mouseX, mouseY, rowX + 58.0, valueY, 50.0, buttonH)) {
               setting.reset();
               this.clearStringListEdit();
            }
         }
      }

      private void updateNumberSetting(Setting<?> setting, double mouseX) {
         double rowW = this.settingsW - 20.0;
         double sliderW = Math.min(this.ui(100.0), rowW * 0.38);
         double sliderX = this.settingsX + 10.0 + rowW - sliderW - this.ui(10.0);
         double progress = this.clamp((mouseX - sliderX) / sliderW, 0.0, 1.0);
         if (setting instanceof DoubleSetting doubleSetting) {
            double min = Double.isFinite(doubleSetting.min) ? doubleSetting.min : doubleSetting.sliderMin;
            double max = Double.isFinite(doubleSetting.max) ? doubleSetting.max : doubleSetting.sliderMax;
            if (max <= min) {
               return;
            }

            double value = min + (max - min) * progress;
            doubleSetting.set(value);
         } else if (setting instanceof IntSetting intSetting) {
            int min = intSetting.min == Integer.MIN_VALUE ? intSetting.sliderMin : intSetting.min;
            int max = intSetting.max == Integer.MAX_VALUE ? intSetting.sliderMax : intSetting.max;
            if (max <= min) {
               return;
            }

            int value = (int)Math.round(min + (max - min) * progress);
            intSetting.set(value);
         }
      }

      private double listContentHeight() {
         if (this.activePage == LurkClickGuiScreen.Page.MACROS) {
            int count = 0;

            for (Macro ignored : Macros.get()) {
               count++;
            }

            return this.listHeaderH() + this.ui(2.0) + Math.max(0, count) * (this.pageRowH() + this.pageGap());
         } else {
            if (this.activePage != LurkClickGuiScreen.Page.PROFILES) {
               return this.modulesContentHeight();
            }

            int count = 0;

            for (Profile ignored : Profiles.get()) {
               count++;
            }

            return this.listHeaderH() + this.ui(2.0) + Math.max(0, count) * (this.pageRowH() + this.pageGap());
         }
      }

      private double modulesContentHeight() {
         List<Module> modules = this.currentModules();
         return modules.isEmpty() ? 0.0 : modules.size() * this.moduleRowH() + Math.max(0, modules.size() - 1) * this.moduleGap();
      }

      private double settingsContentHeight() {
         if (this.activePage == LurkClickGuiScreen.Page.FRIENDS) {
            return this.ui(58.0) + Math.max(this.ui(24.0), this.buttonHeight()) + this.ui(12.0) + Friends.get().count() * (this.pageRowH() + this.pageGap());
         }

         Settings settings = this.currentSettings();
         if (settings == null) {
            return 0.0;
         }

         double total = 46.0 + this.settingsActionHeight();

         for (SettingGroup group : settings) {
            List<Setting<?>> visibleSettings = this.visibleSettings(group);
            if (!visibleSettings.isEmpty()) {
               double settingsHeight = 0.0;

               for (Setting<?> setting : visibleSettings) {
                  settingsHeight += this.settingHeight(setting);
               }

               total += this.ui(18.0) + settingsHeight + Math.max(0, visibleSettings.size() - 1) * this.settingGap() + this.ui(10.0);
            }
         }

         return total;
      }

      private List<Setting<?>> visibleSettings(SettingGroup group) {
         List<Setting<?>> settings = new ArrayList<>();

         for (Setting<?> setting : group) {
            if (setting.isVisible()) {
               settings.add(setting);
            }
         }

         return settings;
      }

      private List<Module> currentModules() {
         List<Module> modules = new ArrayList<>();
         String filter = this.query.trim().toLowerCase();
         if (filter.isEmpty()) {
            if (this.selectedCategory != null) {
               modules.addAll(this.visibleModules(this.selectedCategory, true));
            }
         } else {
            for (Category category : this.categories) {
               for (Module module : this.visibleModules(category, false)) {
                  if (module.title.toLowerCase().contains(filter) || module.name.toLowerCase().contains(filter)) {
                     modules.add(module);
                  }
               }
            }
         }

         modules.sort(Comparator.comparing(modulex -> modulex.title));
         return modules;
      }

      private List<Module> visibleModules(Category category, boolean sorted) {
         List<Module> modules = new ArrayList<>();

         for (Module module : Modules.get().getGroup(category)) {
            if (!Config.get().hiddenModules.get().contains(module)) {
               modules.add(module);
            }
         }

         if (sorted) {
            modules.sort(Comparator.comparing(modulex -> modulex.title));
         }

         return modules;
      }

      private List<LurkClickGuiScreen.Page> visiblePages() {
         List<LurkClickGuiScreen.Page> pages = new ArrayList<>();
         pages.add(LurkClickGuiScreen.Page.MODULES);
         pages.add(LurkClickGuiScreen.Page.CONFIG);
         pages.add(LurkClickGuiScreen.Page.GUI);
         pages.add(LurkClickGuiScreen.Page.HUD);
         pages.add(LurkClickGuiScreen.Page.FRIENDS);
         pages.add(LurkClickGuiScreen.Page.MACROS);
         pages.add(LurkClickGuiScreen.Page.PROFILES);
         if (PathManagers.get().getSettings().get().sizeGroups() > 0) {
            pages.add(LurkClickGuiScreen.Page.PATHING);
         }

         return pages;
      }

      private void selectPage(LurkClickGuiScreen.Page page) {
         if (this.activePage != page) {
            this.saveSelectedModuleState();
            this.saveUiState();
            this.activePage = page;
            this.selectedModule = null;
            this.bindingModule = null;
            this.bindingSetting = null;
            this.editingSetting = null;
            this.clearStringListEdit();
            this.searchFocused = false;
            this.friendInputFocused = false;
            this.query = "";
            this.moduleScroll = 0.0;
            this.targetModuleScroll = 0.0;
            this.resetSettingsScroll();
            this.activateCurrentPage();
            this.saveUiState();
         }
      }

      private void selectModule(Module module) {
         if (module != null) {
            this.saveSelectedModuleState();
            this.saveUiState();
            this.selectedModule = module;
            double savedScroll = this.moduleSettingsScrolls.getOrDefault(module, 0.0);
            this.targetSettingsScroll = savedScroll;
            this.settingsScroll = savedScroll;
            this.editingSetting = null;
            this.bindingModule = null;
            this.saveUiState();
         }
      }

      private void closeSelectedModule() {
         this.saveSelectedModuleState();
         this.selectedModule = null;
         this.resetSettingsScroll();
         this.editingSetting = null;
         this.bindingModule = null;
         this.saveUiState();
      }

      private void saveSelectedModuleState() {
         if (this.selectedModule != null) {
            this.moduleSettingsScrolls.put(this.selectedModule, this.targetSettingsScroll);
         }
      }

      private void saveUiState() {
         LurkClickGuiScreen.savedPage = this.activePage;
         if (this.activePage == LurkClickGuiScreen.Page.MODULES) {
            LurkClickGuiScreen.savedCategory = this.selectedCategory;
            LurkClickGuiScreen.savedModule = this.selectedModule;
            LurkClickGuiScreen.savedModuleScroll = this.moduleScroll;
            LurkClickGuiScreen.savedTargetModuleScroll = this.targetModuleScroll;
            LurkClickGuiScreen.savedSettingsScroll = this.settingsScroll;
            LurkClickGuiScreen.savedTargetSettingsScroll = this.targetSettingsScroll;
         }
      }

      private void activateCurrentPage() {
         Settings settings = this.currentSettings();
         if (settings != null) {
            settings.onActivated();
         }
      }

      private void selectSettings(Settings settings) {
         if (settings != null) {
            settings.onActivated();
         }
      }

      private Settings currentSettings() {
         return switch (this.activePage) {
            case MODULES -> this.selectedModule == null ? null : this.selectedModule.settings;
            case CONFIG -> Config.get().settings;
            case GUI -> this.theme.settings;
            case HUD -> Hud.get().settings;
            default -> null;
            case MACROS -> this.selectedMacro == null ? null : this.selectedMacro.settings;
            case PROFILES -> this.selectedProfile == null ? null : this.selectedProfile.settings;
            case PATHING -> PathManagers.get().getSettings().get();
         };
      }

      private double settingsGroupsStartY() {
         return this.settingsY + 56.0 + this.settingsActionHeight() - this.settingsScroll;
      }

      private double settingsActionHeight() {
         double row = this.buttonHeight() + this.ui(12.0);
         if (this.activePage == LurkClickGuiScreen.Page.GUI) {
            return row;
         } else if (this.activePage == LurkClickGuiScreen.Page.HUD) {
            return this.buttonHeight() * 2.0 + this.ui(20.0);
         } else if (this.activePage == LurkClickGuiScreen.Page.MACROS && this.selectedMacro != null) {
            return row;
         } else {
            return this.activePage == LurkClickGuiScreen.Page.PROFILES && this.selectedProfile != null ? row : 0.0;
         }
      }

      private void resetSettingsScroll() {
         this.settingsScroll = 0.0;
         this.targetSettingsScroll = 0.0;
      }

      private List<String> pageSummary() {
         List<String> lines = new ArrayList<>();
         switch (this.activePage) {
            case CONFIG:
               lines.add("Global settings");
               lines.add(Config.get().settings.sizeGroups() + " groups");
               break;
            case GUI:
               lines.add("Lurk theme");
               lines.add(this.theme.settings.sizeGroups() + " groups");
               break;
            case HUD:
               lines.add(Hud.get().active ? "HUD active" : "HUD inactive");
               lines.add(Hud.get().settings.sizeGroups() + " groups");
               break;
            case FRIENDS:
               lines.add(Friends.get().count() + " friends");
               lines.add("Add or remove");
               break;
            case MACROS:
               int count = 0;

               for (Macro ignored : Macros.get()) {
                  count++;
               }

               lines.add(count + " macros");
               lines.add(this.selectedMacro == null ? "No selection" : this.selectedMacro.name.get());
               break;
            case PROFILES:
               int profileCount = 0;

               for (Profile ignored : Profiles.get()) {
                  profileCount++;
               }

               lines.add(profileCount + " profiles");
               lines.add(this.selectedProfile == null ? "No selection" : this.selectedProfile.name.get());
               break;
            case PATHING:
               lines.add("Path manager");
               lines.add(PathManagers.get().getSettings().get().sizeGroups() + " groups");
         }

         return lines;
      }

      private void addFriend() {
         String name = this.friendInput.trim();
         if (!name.isEmpty()) {
            Friend friend = new Friend(name);
            if (Friends.get().add(friend)) {
               this.friendInput = "";
               MeteorExecutor.execute(friend::updateInfo);
            }
         }
      }

      private Macro createMacro() {
         Macro macro = new Macro();
         macro.name.set(this.nextMacroName());
         Macros.get().add(macro);
         return macro;
      }

      private String nextMacroName() {
         int index = 1;

         while (Macros.get().get("Macro " + index) != null) {
            index++;
         }

         return "Macro " + index;
      }

      private Profile createProfile() {
         Profile profile = new Profile();
         profile.name.set(this.nextProfileName());
         Profiles.get().add(profile);
         return profile;
      }

      private String nextProfileName() {
         int index = 1;

         while (Profiles.get().get("Profile " + index) != null) {
            index++;
         }

         return "Profile " + index;
      }

      private void saveProfile(Profile profile) {
         List<String> valid = new ArrayList<>();

         for (String address : profile.loadOnJoin.get()) {
            if (Utils.resolveAddress(address)) {
               valid.add(address);
            }
         }

         profile.loadOnJoin.set(valid);
         Profiles.get().add(profile);
      }

      private String profileFlags(Profile profile) {
         List<String> flags = new ArrayList<>();
         if (profile.hud.get()) {
            flags.add("HUD");
         }

         if (profile.macros.get()) {
            flags.add("Macros");
         }

         if (profile.modules.get()) {
            flags.add("Modules");
         }

         if (profile.waypoints.get()) {
            flags.add("Waypoints");
         }

         return flags.isEmpty() ? "No save targets" : String.join(", ", flags);
      }

      private LurkClickGuiScreen.Page clickedPage(double mouseX, double mouseY) {
         double x = this.contentX + 12.0;
         double tabH = this.buttonHeight();
         double y = this.panelY + this.headerH() - tabH - this.ui(6.0);
         double maxRight = this.contentX + this.contentW - 12.0;

         for (LurkClickGuiScreen.Page page : this.visiblePages()) {
            double tabW = this.theme.textWidth(page.title()) + this.ui(14.0);
            if (x + tabW > maxRight) {
               break;
            }

            if (this.inside(mouseX, mouseY, x, y, tabW, tabH)) {
               return page;
            }

            x += tabW + this.ui(4.0);
         }

         return null;
      }

      private double keybindX(double rowX, double rowW) {
         return rowX + rowW - this.ui(54.0);
      }

      private String compactBind(Module module) {
         if (!module.keybind.isSet()) {
            return "-";
         }

         String bind = module.keybind.toString();
         bind = bind.replace("Mouse ", "M");
         bind = bind.replace("Left ", "L");
         bind = bind.replace("Right ", "R");
         bind = bind.replace("Control", "Ctrl");
         return bind.length() > 3 ? bind.substring(0, 3) : bind;
      }

      private boolean isNumberSetting(Setting<?> setting) {
         return setting instanceof DoubleSetting || setting instanceof IntSetting;
      }

      private void beginNumberEdit(Setting<?> setting) {
         this.draggingNumberSetting = null;
         this.editingStringListSetting = null;
         this.editingStringListIndex = -1;
         this.editingSetting = setting;
         this.editBuffer = this.numberEditValue(setting);
         this.numberEditFresh = true;
      }

      private String numberEditValue(Setting<?> setting) {
         if (setting instanceof DoubleSetting doubleSetting) {
            return this.formatNumber(doubleSetting.get(), doubleSetting.decimalPlaces);
         } else {
            return setting instanceof IntSetting intSetting ? String.valueOf(intSetting.get()) : "";
         }
      }

      private String editCursor() {
         return (System.currentTimeMillis() / 420L & 1L) == 0L ? "|" : "";
      }

      private boolean acceptsNumberEditChar(Setting<?> setting, int codepoint) {
         char c = (char)codepoint;
         if (Character.isDigit(c)) {
            return true;
         } else {
            return (c == '-' || c == '+') && this.editBuffer.isEmpty() ? true : setting instanceof DoubleSetting && c == '.' && !this.editBuffer.contains(".");
         }
      }

      private void appendNumberEditChar(int codepoint, String value) {
         String previous = this.editBuffer;
         if (this.numberEditFresh) {
            this.editBuffer = "";
         }

         if (this.acceptsNumberEditChar(this.editingSetting, codepoint)) {
            this.editBuffer = this.editBuffer + value;
            this.numberEditFresh = false;
         } else {
            this.editBuffer = previous;
         }
      }

      private boolean applyNumberEdit() {
         String value = this.editBuffer.trim();
         if (value.isEmpty()) {
            return false;
         }

         try {
            if (this.editingSetting instanceof DoubleSetting doubleSetting) {
               double parsed = Double.parseDouble(value);
               if (!Double.isFinite(parsed)) {
                  return false;
               }

               if (Double.isFinite(doubleSetting.min)) {
                  parsed = Math.max(doubleSetting.min, parsed);
               }

               if (Double.isFinite(doubleSetting.max)) {
                  parsed = Math.min(doubleSetting.max, parsed);
               }

               return doubleSetting.set(parsed);
            } else if (this.editingSetting instanceof IntSetting intSetting) {
               long parsed = Long.parseLong(value);
               parsed = Math.max(intSetting.min, parsed);
               parsed = Math.min(intSetting.max, parsed);
               return intSetting.set((int)parsed);
            } else {
               return false;
            }
         } catch (NumberFormatException ignored) {
            return false;
         }
      }

      private boolean numberSliderHit(Setting<?> setting, double mouseX, double mouseY, double rowX, double rowY, double rowW, double rowH) {
         if (setting instanceof DoubleSetting doubleSetting && doubleSetting.noSlider) {
            return false;
         } else if (setting instanceof IntSetting intSetting && intSetting.noSlider) {
            return false;
         } else {
            double sliderW = Math.min(this.ui(100.0), rowW * 0.38);
            double sliderX = rowX + rowW - sliderW - this.ui(10.0);
            return this.inside(mouseX, mouseY, sliderX - this.ui(4.0), rowY, sliderW + this.ui(8.0), rowH);
         }
      }

      private double numberProgress(DoubleSetting setting) {
         double min = Double.isFinite(setting.min) ? setting.min : setting.sliderMin;
         double max = Double.isFinite(setting.max) ? setting.max : setting.sliderMax;
         return max <= min ? 0.0 : this.clamp((setting.get() - min) / (max - min), 0.0, 1.0);
      }

      private double numberProgress(IntSetting setting) {
         int min = setting.min == Integer.MIN_VALUE ? setting.sliderMin : setting.min;
         int max = setting.max == Integer.MAX_VALUE ? setting.sliderMax : setting.max;
         return max <= min ? 0.0 : this.clamp((double)(setting.get() - min) / (max - min), 0.0, 1.0);
      }

      private String formatNumber(double value, int decimals) {
         int places = Math.max(0, Math.min(4, decimals));
         return String.format("%." + places + "f", value);
      }

      private boolean opensSeparateScreen(Setting<?> setting) {
         return !(setting instanceof BoolSetting)
            && !(setting instanceof EnumSetting)
            && !(setting instanceof DoubleSetting)
            && !(setting instanceof IntSetting)
            && !(setting instanceof StringSetting)
            && !(setting instanceof DoubleListSetting)
            && !(setting instanceof StringListSetting)
            && !(setting instanceof KeybindSetting);
      }

      private boolean openSettingScreen(Setting<?> setting) {
         if (setting instanceof GenericSetting<?> genericSetting) {
            MeteorClient.mc.setScreen(genericSetting.createScreen(this.theme));
         } else if (setting instanceof ColorSetting colorSetting) {
            MeteorClient.mc.setScreen(new ColorSettingScreen(this.theme, colorSetting));
         } else if (setting instanceof BlockSetting blockSetting) {
            MeteorClient.mc.setScreen(new BlockSettingScreen(this.theme, blockSetting));
         } else if (setting instanceof BlockListSetting blockListSetting) {
            MeteorClient.mc.setScreen(new BlockListSettingScreen(this.theme, blockListSetting));
         } else if (setting instanceof ItemSetting itemSetting) {
            MeteorClient.mc.setScreen(new ItemSettingScreen(this.theme, itemSetting));
         } else if (setting instanceof ItemListSetting itemListSetting) {
            MeteorClient.mc.setScreen(new ItemListSettingScreen(this.theme, itemListSetting));
         } else if (setting instanceof EntityTypeListSetting entityTypeListSetting) {
            MeteorClient.mc.setScreen(new EntityTypeListSettingScreen(this.theme, entityTypeListSetting));
         } else if (setting instanceof EnchantmentListSetting enchantmentListSetting) {
            MeteorClient.mc.setScreen(new EnchantmentListSettingScreen(this.theme, enchantmentListSetting));
         } else if (setting instanceof ModuleListSetting moduleListSetting) {
            MeteorClient.mc.setScreen(new ModuleListSettingScreen(this.theme, moduleListSetting));
         } else if (setting instanceof PacketListSetting packetListSetting) {
            MeteorClient.mc.setScreen(new PacketBoolSettingScreen(this.theme, packetListSetting));
         } else if (setting instanceof ParticleTypeListSetting particleTypeListSetting) {
            MeteorClient.mc.setScreen(new ParticleTypeListSettingScreen(this.theme, particleTypeListSetting));
         } else if (setting instanceof SoundEventListSetting soundEventListSetting) {
            MeteorClient.mc.setScreen(new SoundEventListSettingScreen(this.theme, soundEventListSetting));
         } else if (setting instanceof StatusEffectAmplifierMapSetting statusEffectAmplifierMapSetting) {
            MeteorClient.mc.setScreen(new StatusEffectAmplifierMapSettingScreen(this.theme, statusEffectAmplifierMapSetting));
         } else if (setting instanceof StatusEffectListSetting statusEffectListSetting) {
            MeteorClient.mc.setScreen(new StatusEffectListSettingScreen(this.theme, statusEffectListSetting));
         } else if (setting instanceof StorageBlockListSetting storageBlockListSetting) {
            MeteorClient.mc.setScreen(new StorageBlockListSettingScreen(this.theme, storageBlockListSetting));
         } else if (setting instanceof ScreenHandlerListSetting screenHandlerListSetting) {
            MeteorClient.mc.setScreen(new ScreenHandlerSettingScreen(this.theme, screenHandlerListSetting));
         } else if (setting instanceof BlockDataSetting blockDataSetting) {
            MeteorClient.mc.setScreen(new BlockDataSettingScreen(this.theme, blockDataSetting));
         } else if (setting instanceof PotionSetting potionSetting) {
            MeteorClient.mc.setScreen(new PotionSettingScreen(this.theme, potionSetting));
         } else {
            if (!(setting instanceof FontFaceSetting fontFaceSetting)) {
               return false;
            }

            MeteorClient.mc.setScreen(new FontFaceSettingScreen(this.theme, fontFaceSetting));
         }

         return true;
      }

      private void clampScrolls() {
         double moduleMax = Math.max(0.0, this.listContentHeight() - this.moduleListH);
         double settingsMax = Math.max(0.0, this.settingsContentHeight() - this.settingsH);
         this.targetModuleScroll = this.clamp(this.targetModuleScroll, 0.0, moduleMax);
         this.targetSettingsScroll = this.clamp(this.targetSettingsScroll, 0.0, settingsMax);
         this.moduleScroll = this.clamp(this.moduleScroll, 0.0, moduleMax);
         this.settingsScroll = this.clamp(this.settingsScroll, 0.0, settingsMax);
      }

      private void updateSettingsPanelAnimation() {
         double target = this.activePage == LurkClickGuiScreen.Page.MODULES && this.selectedModule != null ? 1.0 : 0.0;
         this.settingsPanelProgress = this.ease(this.settingsPanelProgress, target, 18.0F);
         if (Math.abs(this.settingsPanelProgress - target) < 0.002) {
            this.settingsPanelProgress = target;
         }
      }

      private double ease(double current, double target, float speed) {
         return AnimationUtility.ease(current, target, speed);
      }

      private double animated(Map<Object, Double> animations, Object key, double target, float speed) {
         double current = animations.getOrDefault(key, target);
         double value = this.ease(current, target, speed);
         if (Math.abs(value - target) < 0.001) {
            value = target;
         }

         animations.put(key, value);
         return value;
      }

      private double settingHeight(Setting<?> setting) {
         if (setting instanceof DoubleListSetting doubleListSetting) {
            return this.ui(62.0) + doubleListSetting.get().size() * this.ui(26.0);
         } else {
            return setting instanceof StringListSetting stringListSetting ? this.ui(62.0) + stringListSetting.get().size() * this.ui(26.0) : this.settingRowH();
         }
      }

      private void updateDoubleListValue(DoubleListSetting setting, int index, double mouseX) {
         if (index >= 0 && index < setting.get().size()) {
            double rowW = this.settingsW - 20.0;
            double sliderX = this.settingsX + 10.0 + this.ui(78.0);
            double sliderW = Math.max(this.ui(40.0), rowW - this.ui(118.0));
            double progress = this.clamp((mouseX - sliderX) / sliderW, 0.0, 1.0);
            double min = Double.isFinite(setting.min) ? setting.min : setting.sliderMin;
            double max = Double.isFinite(setting.max) ? setting.max : setting.sliderMax;
            if (!(max <= min)) {
               List<Double> values = new ArrayList<>(setting.get());
               values.set(index, min + (max - min) * progress);
               setting.set(values);
            }
         }
      }

      private double doubleListProgress(DoubleListSetting setting, double value) {
         double min = Double.isFinite(setting.min) ? setting.min : setting.sliderMin;
         double max = Double.isFinite(setting.max) ? setting.max : setting.sliderMax;
         return max <= min ? 0.0 : this.clamp((value - min) / (max - min), 0.0, 1.0);
      }

      private double defaultDoubleListValue(DoubleListSetting setting) {
         if (Double.isFinite(setting.sliderMin)) {
            return setting.sliderMin;
         } else {
            return Double.isFinite(setting.min) ? setting.min : 0.0;
         }
      }

      private void applyStringListEdit() {
         if (this.editingStringListSetting != null
            && this.editingStringListIndex >= 0
            && this.editingStringListIndex < this.editingStringListSetting.get().size()) {
            List<String> values = new ArrayList<>(this.editingStringListSetting.get());
            values.set(this.editingStringListIndex, this.editBuffer);
            this.editingStringListSetting.set(values);
            this.clearStringListEdit();
         } else {
            this.clearStringListEdit();
         }
      }

      private void clearStringListEdit() {
         this.editingStringListSetting = null;
         this.editingStringListIndex = -1;
         this.editBuffer = "";
      }

      private String trim(String text, double maxWidth, boolean title) {
         if (text != null && !text.isEmpty()) {
            if (this.theme.textWidth(text, text.length(), title) <= maxWidth) {
               return text;
            }

            String ellipsis = "...";

            for (int i = text.length(); i > 0; i--) {
               String candidate = text.substring(0, i) + ellipsis;
               if (this.theme.textWidth(candidate, candidate.length(), title) <= maxWidth) {
                  return candidate;
               }
            }

            return ellipsis;
         } else {
            return "";
         }
      }

      private boolean inside(double mouseX, double mouseY, double x, double y, double w, double h) {
         return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
      }

      private boolean visibleInSettings(double y, double h) {
         return y >= this.settingsY && y + h <= this.settingsY + this.settingsH;
      }

      private boolean visibleInModuleList(double y, double h) {
         return y >= this.moduleListY && y + h <= this.moduleListY + this.moduleListH;
      }

      private boolean friendInputVisible(double scroll) {
         double inputY = this.settingsY + 10.0 - scroll + 46.0;
         double inputH = Math.max(this.ui(24.0), this.buttonHeight());
         return inputY >= this.settingsY && inputY + inputH <= this.settingsY + this.settingsH;
      }

      private double clamp(double value, double min, double max) {
         return Math.max(min, Math.min(max, value));
      }
   }

   public enum Page {
      MODULES("Modules"),
      CONFIG("Config"),
      GUI("GUI"),
      HUD("HUD"),
      FRIENDS("Friends"),
      MACROS("Macros"),
      PROFILES("Profiles"),
      PATHING("Pathing");

      private final String title;

      Page(String title) {
         this.title = title;
      }

      public String title() {
         return this == PATHING ? PathManagers.get().getName() : this.title;
      }
   }
}
