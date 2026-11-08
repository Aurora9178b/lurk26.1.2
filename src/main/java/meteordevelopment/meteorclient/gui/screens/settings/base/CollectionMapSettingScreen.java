package meteordevelopment.meteorclient.gui.screens.settings.base;

import java.util.Comparator;
import java.util.Map;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import org.jetbrains.annotations.Nullable;

public abstract class CollectionMapSettingScreen<K, V> extends WindowScreen {
   private final Setting<?> setting;
   protected final Map<K, V> map;
   private final Iterable<K> registry;
   private WTable table;
   private String filterText = "";

   public CollectionMapSettingScreen(GuiTheme theme, String title, Setting<?> setting, Map<K, V> map, Iterable<K> registry) {
      super(theme, title);
      this.setting = setting;
      this.map = map;
      this.registry = registry;
   }

   @Override
   public void initWidgets() {
      WTextBox filter = this.add(this.theme.textBox("")).minWidth(400.0).expandX().widget();
      filter.setFocused(true);
      filter.action = () -> {
         this.filterText = filter.get().trim();
         this.table.clear();
         this.initTable();
      };
      this.table = this.add(this.theme.table()).expandX().widget();
      this.initTable();
   }

   private void initTable() {
      Comparator<K> prioritizeChanged = Comparator.comparing(key -> !(this.map.get(key) instanceof IChangeable changeable && changeable.isChanged()));
      Iterable<K> sorted = SortingHelper.sortWithPriority(this.registry, this::includeValue, this::getValueNames, this.filterText, prioritizeChanged);
      sorted.forEach(t -> {
         V data = this.map.get(t);
         boolean isChanged = data instanceof IChangeable changeable && changeable.isChanged();
         this.table.add(this.getValueWidget((K)t)).expandCellX();
         this.table.add(this.theme.label(isChanged ? "*" : " "));
         this.table.add(this.getDataWidget((K)t, data));
         WButton reset = this.table.add(this.theme.button(GuiRenderer.RESET)).widget();
         reset.action = () -> this.removeValue((K)t);
         reset.tooltip = "Reset";
         this.table.row();
      });
   }

   protected void invalidateTable() {
      this.table.clear();
      this.initTable();
   }

   protected void removeValue(K value) {
      if (this.map.remove(value) != null) {
         this.setting.onChanged();
         this.invalidateTable();
      }
   }

   protected boolean includeValue(K value) {
      return true;
   }

   protected abstract WWidget getValueWidget(K var1);

   protected abstract WWidget getDataWidget(K var1, @Nullable V var2);

   protected abstract String[] getValueNames(K var1);
}
