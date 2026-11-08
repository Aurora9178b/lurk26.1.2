package meteordevelopment.meteorclient.systems.modules.render;

import java.util.List;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemHighlight extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Item>> items = this.sgGeneral.add(new ItemListSetting.Builder().name("items").description("Items to highlight.").build());
   private final Setting<SettingColor> color = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("color")
            .description("The color to highlight the items with.")
            .defaultValue(new SettingColor(225, 25, 255, 50))
            .build()
      );

   public ItemHighlight() {
      super(Categories.Render, "item-highlight", "Highlights selected items when in guis");
   }

   public int getColor(ItemStack stack) {
      return stack != null && this.items.get().contains(stack.getItem()) && this.isActive() ? this.color.get().getPacked() : -1;
   }
}
