package meteordevelopment.meteorclient.gui.screens.settings;

import java.util.Set;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.PacketListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.network.PacketUtils;
import net.minecraft.network.protocol.Packet;

public class PacketBoolSettingScreen extends CollectionListSettingScreen<Class<? extends Packet<?>>> {
   public PacketBoolSettingScreen(GuiTheme theme, Setting<Set<Class<? extends Packet<?>>>> setting) {
      super(theme, "Select Packets", setting, setting.get(), PacketUtils.PACKETS);
   }

   protected boolean includeValue(Class<? extends Packet<?>> value) {
      Predicate<Class<? extends Packet<?>>> filter = ((PacketListSetting)this.setting).filter;
      return filter == null ? true : filter.test(value);
   }

   protected WWidget getValueWidget(Class<? extends Packet<?>> value) {
      return this.theme.label(PacketUtils.getName(value));
   }

   protected String[] getValueNames(Class<? extends Packet<?>> value) {
      return new String[]{PacketUtils.getName(value), value.getSimpleName()};
   }
}
