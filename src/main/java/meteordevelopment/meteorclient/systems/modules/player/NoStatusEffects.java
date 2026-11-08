package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StatusEffectListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public class NoStatusEffects extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<MobEffect>> blockedEffects = this.sgGeneral
      .add(
         new StatusEffectListSetting.Builder()
            .name("blocked-effects")
            .description("Effects to block.")
            .defaultValue(MobEffects.LEVITATION.value(), MobEffects.JUMP_BOOST.value(), MobEffects.SLOW_FALLING.value(), MobEffects.DOLPHINS_GRACE.value())
            .build()
      );

   public NoStatusEffects() {
      super(Categories.Player, "no-status-effects", "Blocks specified status effects.");
   }

   public boolean shouldBlock(MobEffect effect) {
      return this.isActive() && this.blockedEffects.get().contains(effect);
   }
}
