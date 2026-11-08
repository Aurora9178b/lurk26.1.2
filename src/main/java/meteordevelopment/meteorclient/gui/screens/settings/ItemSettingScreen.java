package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WItemWithLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.Strings;

public class ItemSettingScreen extends WindowScreen {
   private final ItemSetting setting;
   private WTable table;
   private WTextBox filter;
   private String filterText = "";

   public ItemSettingScreen(GuiTheme theme, ItemSetting setting) {
      super(theme, "Select item");
      this.setting = setting;
   }

   @Override
   public void initWidgets() {
      this.filter = this.add(this.theme.textBox("")).minWidth(400.0).expandX().widget();
      this.filter.setFocused(true);
      this.filter.action = () -> {
         this.filterText = this.filter.get().trim();
         this.table.clear();
         this.initTable();
      };
      this.table = this.add(this.theme.table()).expandX().widget();
      this.initTable();
   }

   public void initTable() {
      for (Item item : BuiltInRegistries.ITEM) {
         if ((this.setting.filter == null || this.setting.filter.test(item)) && item != Items.AIR) {
            WItemWithLabel itemLabel = this.theme.itemWithLabel(DisplayItemUtils.toStack(item), Names.get(item));
            if (this.filterText.isEmpty() || Strings.CI.contains(itemLabel.getLabelText(), this.filterText)) {
               this.table.add(itemLabel);
               WButton select = this.table.add(this.theme.button("Select")).expandCellX().right().widget();
               select.action = () -> {
                  this.setting.set(item);
                  this.onClose();
               };
               this.table.row();
            }
         }
      }
   }
}
