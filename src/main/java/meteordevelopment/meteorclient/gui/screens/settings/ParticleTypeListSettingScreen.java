package meteordevelopment.meteorclient.gui.screens.settings;

import java.util.List;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public class ParticleTypeListSettingScreen extends CollectionListSettingScreen<ParticleType<?>> {
   public ParticleTypeListSettingScreen(GuiTheme theme, Setting<List<ParticleType<?>>> setting) {
      super(theme, "Select Particles", setting, setting.get(), BuiltInRegistries.PARTICLE_TYPE);
   }

   protected WWidget getValueWidget(ParticleType<?> value) {
      return this.theme.label(Names.get(value));
   }

   protected String[] getValueNames(ParticleType<?> value) {
      return new String[]{Names.get(value), BuiltInRegistries.PARTICLE_TYPE.getKey(value).toString()};
   }
}
