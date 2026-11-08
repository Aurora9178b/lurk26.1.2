package meteordevelopment.meteorclient.systems.hud.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPlus;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.utils.Utils;
import org.jetbrains.annotations.Nullable;

public class HudElementPresetsScreen extends WindowScreen {
   private final HudElementInfo<?> info;
   private final int x;
   private final int y;
   private final WTextBox searchBar;
   @Nullable
   private HudElementInfo.Preset firstPreset;

   public HudElementPresetsScreen(GuiTheme theme, HudElementInfo<?> info, int x, int y) {
      super(theme, "Select preset for " + info.title);
      this.info = info;
      this.x = x + 9;
      this.y = y;
      this.searchBar = theme.textBox("");
      this.searchBar.action = () -> {
         this.clear();
         this.initWidgets();
      };
      this.enterAction = () -> {
         if (this.firstPreset != null) {
            Hud.get().add(this.firstPreset, x, y);
            this.onClose();
         }
      };
   }

   @Override
   public void initWidgets() {
      this.firstPreset = null;
      this.add(this.searchBar).expandX();
      this.searchBar.setFocused(true);

      for (HudElementInfo<?>.Preset preset : this.info.presets) {
         if (Utils.searchTextDefault(preset.title, this.searchBar.get(), false)) {
            WHorizontalList l = this.add(this.theme.horizontalList()).expandX().widget();
            l.add(this.theme.label(preset.title));
            WPlus add = l.add(this.theme.plus()).expandCellX().right().widget();
            add.action = () -> {
               Hud.get().add(preset, this.x, this.y);
               this.onClose();
            };
            if (this.firstPreset == null) {
               this.firstPreset = preset;
            }
         }
      }
   }
}
