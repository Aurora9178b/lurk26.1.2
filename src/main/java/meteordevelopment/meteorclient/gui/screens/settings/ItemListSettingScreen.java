package meteordevelopment.meteorclient.gui.screens.settings;

import java.util.function.Predicate;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ItemListSettingScreen extends CollectionListSettingScreen<Item> {
   public ItemListSettingScreen(GuiTheme theme, ItemListSetting setting) {
      super(theme, "Select Items", setting, setting.get(), BuiltInRegistries.ITEM);
   }

   protected boolean includeValue(Item value) {
      Predicate<Item> filter = ((ItemListSetting)this.setting).filter;
      return filter != null && !filter.test(value) ? false : value != Items.AIR;
   }

   protected WWidget getValueWidget(Item value) {
      return this.theme.itemWithLabel(DisplayItemUtils.toStack(value));
   }

   protected String[] getValueNames(Item value) {
      return new String[]{Names.get(value), BuiltInRegistries.ITEM.getKey(value).toString()};
   }
}
