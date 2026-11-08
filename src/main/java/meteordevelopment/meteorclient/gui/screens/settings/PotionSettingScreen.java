package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.PotionSetting;
import meteordevelopment.meteorclient.utils.misc.MyPotion;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

public class PotionSettingScreen extends WindowScreen {
   private final PotionSetting setting;

   public PotionSettingScreen(GuiTheme theme, PotionSetting setting) {
      super(theme, "Select Potion");
      this.setting = setting;
   }

   @Override
   public void initWidgets() {
      WTable table = this.add(this.theme.table()).expandX().widget();

      for (MyPotion potion : MyPotion.values()) {
         ItemStack stack = potion.potion.get();
         table.add(this.theme.itemWithLabel(stack, I18n.get(stack.getItem().getDescriptionId())));
         WButton select = table.add(this.theme.button("Select")).widget();
         select.action = () -> {
            this.setting.set(potion);
            this.onClose();
         };
         table.row();
      }
   }
}
