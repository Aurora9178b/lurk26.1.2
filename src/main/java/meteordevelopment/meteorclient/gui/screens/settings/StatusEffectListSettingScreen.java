package meteordevelopment.meteorclient.gui.screens.settings;

import java.util.List;
import java.util.Optional;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionListSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public class StatusEffectListSettingScreen extends CollectionListSettingScreen<MobEffect> {
   public StatusEffectListSettingScreen(GuiTheme theme, Setting<List<MobEffect>> setting) {
      super(theme, "Select Effects", setting, setting.get(), BuiltInRegistries.MOB_EFFECT);
   }

   protected WWidget getValueWidget(MobEffect value) {
      return this.theme.itemWithLabel(this.getPotionStack(value), Names.get(value));
   }

   protected String[] getValueNames(MobEffect value) {
      return new String[]{Names.get(value), BuiltInRegistries.MOB_EFFECT.getKey(value).toString()};
   }

   private ItemStack getPotionStack(MobEffect effect) {
      ItemStack potion = DisplayItemUtils.toStack(Items.POTION);
      potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(effect.getColor()), List.of(), Optional.empty()));
      return potion;
   }
}
