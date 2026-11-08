package meteordevelopment.meteorclient.gui.screens.settings;

import java.util.function.Predicate;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockListSettingScreen extends CollectionListSettingScreen<Block> {
   public BlockListSettingScreen(GuiTheme theme, BlockListSetting setting) {
      super(theme, "Select Blocks", setting, setting.get(), BuiltInRegistries.BLOCK);
   }

   protected boolean includeValue(Block value) {
      if (BuiltInRegistries.BLOCK.getKey(value).getPath().endsWith("_wall_banner")) {
         return false;
      }

      Predicate<Block> filter = ((BlockListSetting)this.setting).filter;
      return filter == null ? value != Blocks.AIR : filter.test(value);
   }

   protected WWidget getValueWidget(Block value) {
      return this.theme.itemWithLabel(DisplayItemUtils.toStack(value), Names.get(value));
   }

   protected String[] getValueNames(Block value) {
      return new String[]{Names.get(value), BuiltInRegistries.BLOCK.getKey(value).toString()};
   }

   protected Block getAdditionalValue(Block value) {
      String path = BuiltInRegistries.BLOCK.getKey(value).getPath();
      return !path.endsWith("_banner")
         ? null
         : BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path.substring(0, path.length() - 6) + "wall_banner"));
   }
}
