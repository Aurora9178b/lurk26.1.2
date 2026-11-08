package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemHud extends HudElement {
   public static final HudElementInfo<ItemHud> INFO = new HudElementInfo<>(Hud.GROUP, "item", "Displays the item count.", ItemHud::new);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Item> item = this.sgGeneral
      .add(new ItemSetting.Builder().name("item").description("Item to display").defaultValue(Items.TOTEM_OF_UNDYING).build());
   private final Setting<ItemHud.NoneMode> noneMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("none-mode"))
                  .description("How to render the item when you don't have the specified item in your inventory."))
               .defaultValue(ItemHud.NoneMode.HideCount))
            .build()
      );
   public final Setting<Boolean> customScale = this.sgScale
      .add(
         new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this hud element.")
            .defaultValue(false)
            .onChanged(var1 -> this.calculateSize())
            .build()
      );
   public final Setting<Double> scale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(this.customScale::get)
            .defaultValue(2.0)
            .onChanged(var1 -> this.calculateSize())
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   public final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   public final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );

   private ItemHud() {
      super(INFO);
      this.calculateSize();
   }

   private void calculateSize() {
      this.setSize(17.0F * this.getScale(), 17.0F * this.getScale());
   }

   @Override
   public void render(HudRenderer renderer) {
      ItemStack itemStack = DisplayItemUtils.toStack(this.item.get(), InvUtils.find(this.item.get()).count());
      if (this.noneMode.get() != ItemHud.NoneMode.HideItem || !itemStack.isEmpty()) {
         renderer.post(() -> this.render(renderer, itemStack, this.x, this.y));
      } else if (this.isInEditor()) {
         renderer.line(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), Color.GRAY);
         renderer.line(this.x, this.y + this.getHeight(), this.x + this.getWidth(), this.y, Color.GRAY);
      }

      if (this.background.get()) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      }
   }

   private void render(HudRenderer renderer, ItemStack itemStack, int x, int y) {
      if (this.noneMode.get() == ItemHud.NoneMode.HideItem) {
         renderer.item(itemStack, x, y, this.getScale(), true);
      } else {
         String countOverride = null;
         boolean resetToZero = false;
         if (itemStack.isEmpty()) {
            if (this.noneMode.get() == ItemHud.NoneMode.ShowCount) {
               countOverride = "0";
            }

            itemStack.setCount(1);
            resetToZero = true;
         }

         renderer.item(itemStack, x, y, this.getScale(), true, countOverride);
         if (resetToZero) {
            itemStack.setCount(0);
         }
      }
   }

   private float getScale() {
      return this.customScale.get() ? this.scale.get().floatValue() : this.scale.getDefaultValue().floatValue();
   }

   public enum NoneMode {
      HideItem,
      HideCount,
      ShowCount;

      @Override
      public String toString() {
         return switch (this) {
            case HideItem -> "Hide Item";
            case HideCount -> "Hide Count";
            case ShowCount -> "Show Count";
         };
      }
   }
}
