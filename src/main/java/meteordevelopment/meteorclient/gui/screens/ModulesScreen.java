package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.MacosUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Items;

public class ModulesScreen extends TabScreen {
   private ModulesScreen.WCategoryController controller;
   private WWindow searchWindow;
   private WTextBox searchTextBox;

   public ModulesScreen(GuiTheme theme) {
      super(theme, Tabs.get().getFirst());
   }

   @Override
   public void initWidgets() {
      this.controller = this.add(new ModulesScreen.WCategoryController()).widget();
      WVerticalList help = this.add(this.theme.verticalList()).pad(4.0).bottom().widget();
      help.add(this.theme.label("Left click - Toggle module"));
      help.add(this.theme.label("Right click - Open module settings"));
   }

   @Override
   public void init() {
      super.init();
      this.controller.refresh();
   }

   protected WWindow createCategory(WContainer c, Category category, List<Module> moduleList) {
      WWindow w = this.theme.window(category.name);
      w.id = category.name;
      w.padding = 0.0;
      w.spacing = 0.0;
      if (this.theme.categoryIcons()) {
         w.beforeHeaderInit = wContainer -> wContainer.add(this.theme.item(category.icon.get())).pad(2.0);
      }

      c.add(w);
      w.view.scrollOnlyWhenMouseOver = true;
      w.view.hasScrollBar = false;
      w.view.spacing = 0.0;

      for (Module module : moduleList) {
         w.add(this.theme.module(module)).expandX();
      }

      return w;
   }

   protected void createSearchW(WContainer w, String text) {
      if (!text.isEmpty()) {
         List<Tuple<Module, String>> modules = Modules.get().searchTitles(text);
         if (!modules.isEmpty()) {
            WSection section = w.add(this.theme.section("Modules")).expandX().widget();
            section.spacing = 0.0;
            int count = 0;

            for (Tuple<Module, String> p : modules) {
               if (count >= Config.get().moduleSearchCount.get() || count >= modules.size()) {
                  break;
               }

               section.add(this.theme.module(p.getA(), p.getB())).expandX();
               count++;
            }
         }

         Set<Module> settings = Modules.get().searchSettingTitles(text);
         if (!settings.isEmpty()) {
            WSection section = w.add(this.theme.section("Settings")).expandX().widget();
            section.spacing = 0.0;
            int count = 0;

            for (Module module : settings) {
               if (count >= Config.get().moduleSearchCount.get() || count >= settings.size()) {
                  break;
               }

               section.add(this.theme.module(module)).expandX();
               count++;
            }
         }
      }
   }

   protected WWindow createSearch(WContainer c) {
      WWindow w = this.theme.window("Search");
      w.id = "search";
      this.searchWindow = w;
      if (this.theme.categoryIcons()) {
         w.beforeHeaderInit = wContainer -> wContainer.add(this.theme.item(DisplayItemUtils.toStack(Items.COMPASS))).pad(2.0);
      }

      c.add(w);
      w.view.scrollOnlyWhenMouseOver = true;
      w.view.hasScrollBar = false;
      w.view.maxHeight -= 20.0;
      WVerticalList l = this.theme.verticalList();
      WTextBox text = w.add(this.theme.textBox("")).minWidth(140.0).expandX().widget();
      text.setFocused(true);
      this.searchTextBox = text;
      text.action = () -> {
         l.clear();
         this.createSearchW(l, text.get());
      };
      w.add(l).expandX();
      this.createSearchW(l, text.get());
      return w;
   }

   @Override
   public boolean keyPressed(KeyEvent value) {
      if (this.locked) {
         return false;
      }

      boolean cntrl = MacosUtil.IS_MACOS ? value.modifiers() == 8 : value.modifiers() == 2;
      if (cntrl && value.key() == 70) {
         if (this.searchWindow != null) {
            this.searchWindow.setExpanded(true);
         }

         if (this.searchTextBox != null) {
            this.searchTextBox.setFocused(true);
            this.searchTextBox.setCursorMax();
         }

         return true;
      } else {
         return super.keyPressed(value);
      }
   }

   protected Cell<WWindow> createFavorites(WContainer c) {
      boolean hasFavorites = Modules.get().getAll().stream().anyMatch(module -> module.favorite);
      if (!hasFavorites) {
         return null;
      }

      WWindow w = this.theme.window("Favorites");
      w.id = "favorites";
      w.padding = 0.0;
      w.spacing = 0.0;
      if (this.theme.categoryIcons()) {
         w.beforeHeaderInit = wContainer -> wContainer.add(this.theme.item(DisplayItemUtils.toStack(Items.NETHER_STAR))).pad(2.0);
      }

      Cell<WWindow> cell = c.add(w);
      w.view.scrollOnlyWhenMouseOver = true;
      w.view.hasScrollBar = false;
      w.view.spacing = 0.0;
      this.createFavoritesW(w);
      return cell;
   }

   protected boolean createFavoritesW(WWindow w) {
      List<Module> modules = new ArrayList<>();

      for (Module module : Modules.get().getAll()) {
         if (module.favorite) {
            modules.add(module);
         }
      }

      modules.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name));

      for (Module module : modules) {
         w.add(this.theme.module(module)).expandX();
      }

      return !modules.isEmpty();
   }

   @Override
   public boolean toClipboard() {
      return NbtUtils.toClipboard(Modules.get());
   }

   @Override
   public boolean fromClipboard() {
      return NbtUtils.fromClipboard(Modules.get());
   }

   @Override
   public void reload() {
   }

   protected class WCategoryController extends WContainer {
      public final List<WWindow> windows = new ArrayList<>();
      private Cell<WWindow> favorites;

      @Override
      public void init() {
         List<Module> moduleList = new ArrayList<>();

         for (Category category : Modules.loopCategories()) {
            for (Module module : Modules.get().getGroup(category)) {
               if (!Config.get().hiddenModules.get().contains(module)) {
                  moduleList.add(module);
               }
            }

            if (!moduleList.isEmpty()) {
               this.windows.add(ModulesScreen.this.createCategory(this, category, moduleList));
               moduleList.clear();
            }
         }

         this.windows.add(ModulesScreen.this.createSearch(this));
         this.refresh();
      }

      protected void refresh() {
         if (this.favorites == null) {
            this.favorites = ModulesScreen.this.createFavorites(this);
            if (this.favorites != null) {
               this.windows.add(this.favorites.widget());
            }
         } else {
            this.favorites.widget().clear();
            if (!ModulesScreen.this.createFavoritesW(this.favorites.widget())) {
               this.remove(this.favorites);
               this.windows.remove(this.favorites.widget());
               this.favorites = null;
            }
         }
      }

      @Override
      protected void onCalculateWidgetPositions() {
         double pad = this.theme.scale(4.0);
         double h = this.theme.scale(40.0);
         double x = this.x + pad;
         double y = this.y;

         for (Cell<?> cell : this.cells) {
            double windowWidth = Utils.getWindowWidth();
            double windowHeight = Utils.getWindowHeight();
            if (x + cell.width > windowWidth) {
               x += pad;
               y += h;
            }

            if (x > windowWidth) {
               x = windowWidth / 2.0 - cell.width / 2.0;
               if (x < 0.0) {
                  x = 0.0;
               }
            }

            if (y > windowHeight) {
               y = windowHeight / 2.0 - cell.height / 2.0;
               if (y < 0.0) {
                  y = 0.0;
               }
            }

            cell.x = x;
            cell.y = y;
            cell.width = cell.widget().width;
            cell.height = cell.widget().height;
            cell.alignWidget();
            x += cell.width + pad;
         }
      }
   }
}
