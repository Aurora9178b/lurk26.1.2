package meteordevelopment.meteorclient.gui.screens.settings;

import java.util.Set;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.DynamicRegistryListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentListSettingScreen extends DynamicRegistryListSettingScreen<Enchantment> {
   public EnchantmentListSettingScreen(GuiTheme theme, Setting<Set<ResourceKey<Enchantment>>> setting) {
      super(theme, "Select Enchantments", setting, setting.get(), Registries.ENCHANTMENT);
   }

   protected WWidget getValueWidget(ResourceKey<Enchantment> value) {
      return this.theme.label(Names.get(value));
   }

   protected String[] getValueNames(ResourceKey<Enchantment> value) {
      return new String[]{Names.get(value), value.identifier().toString()};
   }
}
