package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ArmorHud extends HudElement {
   public static final HudElementInfo<ArmorHud> INFO = new HudElementInfo<>(Hud.GROUP, "armor", "Displays your armor.", ArmorHud::new);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgDurability = this.settings.createGroup("Durability");
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<ArmorHud.Orientation> orientation = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("orientation"))
                     .description("How to display armor."))
                  .defaultValue(ArmorHud.Orientation.Horizontal))
               .onChanged(var1 -> this.calculateSize()))
            .build()
      );
   private final Setting<Boolean> flipOrder = this.sgGeneral
      .add(new BoolSetting.Builder().name("flip-order").description("Flips the order of armor items.").defaultValue(true).build());
   private final Setting<Boolean> showEmpty = this.sgGeneral
      .add(new BoolSetting.Builder().name("show-empty").description("Renders barrier icons for empty slots.").defaultValue(false).build());
   private final Setting<ArmorHud.Durability> durability = this.sgDurability
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("durability"))
                     .description("How to display armor durability."))
                  .defaultValue(ArmorHud.Durability.Bar))
               .onChanged(var1 -> this.calculateSize()))
            .build()
      );
   private final Setting<SettingColor> durabilityColor = this.sgDurability
      .add(
         new ColorSetting.Builder()
            .name("durability-color")
            .description("Color of the text.")
            .visible(() -> this.durability.get() == ArmorHud.Durability.Total || this.durability.get() == ArmorHud.Durability.Percentage)
            .defaultValue(new SettingColor())
            .build()
      );
   private final Setting<Boolean> durabilityShadow = this.sgDurability
      .add(
         new BoolSetting.Builder()
            .name("durability-shadow")
            .description("Text shadow.")
            .visible(() -> this.durability.get() == ArmorHud.Durability.Total || this.durability.get() == ArmorHud.Durability.Percentage)
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> customScale = this.sgScale
      .add(
         new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this hud element.")
            .defaultValue(false)
            .onChanged(var1 -> this.calculateSize())
            .build()
      );
   private final Setting<Double> scale = this.sgScale
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
   private final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );

   public ArmorHud() {
      super(INFO);
      this.calculateSize();
   }

   private void calculateSize() {
      switch ((ArmorHud.Orientation)this.orientation.get()) {
         case Horizontal:
            this.setSize(72.0F * this.getScale(), 16.0F * this.getScale());
            break;
         case Vertical:
            this.setSize(16.0F * this.getScale(), 72.0F * this.getScale());
      }
   }

   @Override
   public void render(HudRenderer renderer) {
      int emptySlots = 0;
      ItemStack[] armor = this.flipOrder.get()
         ? new ItemStack[]{
            this.getItem(EquipmentSlot.HEAD), this.getItem(EquipmentSlot.CHEST), this.getItem(EquipmentSlot.LEGS), this.getItem(EquipmentSlot.FEET)
         }
         : new ItemStack[]{
            this.getItem(EquipmentSlot.FEET), this.getItem(EquipmentSlot.LEGS), this.getItem(EquipmentSlot.CHEST), this.getItem(EquipmentSlot.HEAD)
         };

      for (ItemStack stack : armor) {
         if (stack.isEmpty()) {
            emptySlots++;
         }
      }

      if (this.background.get() && emptySlots < 4) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      }

      renderer.post(
         () -> {
            double x = this.x;
            double y = this.y;

            for (int position = 0; position < 4; position++) {
               ItemStack itemStack = armor[position];
               double armorX;
               double armorY;
               if (this.orientation.get() == ArmorHud.Orientation.Vertical) {
                  armorX = x;
                  armorY = y + position * 18 * this.getScale();
               } else {
                  armorX = x + position * 18 * this.getScale();
                  armorY = y;
               }

               renderer.item(
                  itemStack, (int)armorX, (int)armorY, this.getScale(), itemStack.isDamageableItem() && this.durability.get() == ArmorHud.Durability.Bar
               );
               if (itemStack.isDamageableItem() && this.durability.get() != ArmorHud.Durability.Bar && this.durability.get() != ArmorHud.Durability.None) {
                  String message = switch ((ArmorHud.Durability)this.durability.get()) {
                     case Total -> Integer.toString(itemStack.getMaxDamage() - itemStack.getDamageValue());
                     case Percentage -> Integer.toString(
                        Math.round((itemStack.getMaxDamage() - itemStack.getDamageValue()) * 100.0F / itemStack.getMaxDamage())
                     );
                     default -> "err";
                  };
                  double messageWidth = renderer.textWidth(message);
                  if (this.orientation.get() == ArmorHud.Orientation.Vertical) {
                     armorX = x + 8.0F * this.getScale() - messageWidth / 2.0;
                     armorY = y + 18 * position * this.getScale() + (18.0F * this.getScale() - renderer.textHeight());
                  } else {
                     armorX = x + 18 * position * this.getScale() + 8.0F * this.getScale() - messageWidth / 2.0;
                     armorY = y + (this.getHeight() - renderer.textHeight());
                  }

                  TextRenderer.get().render(message, armorX, armorY, this.durabilityColor.get(), this.durabilityShadow.get());
               }
            }
         }
      );
   }

   private ItemStack getItem(EquipmentSlot slot) {
      if (this.isInEditor()) {
         return switch (slot.getIndex()) {
            case 1 -> DisplayItemUtils.toStack(Items.NETHERITE_LEGGINGS);
            case 2 -> DisplayItemUtils.toStack(Items.NETHERITE_CHESTPLATE);
            case 3 -> DisplayItemUtils.toStack(Items.NETHERITE_HELMET);
            default -> DisplayItemUtils.toStack(Items.NETHERITE_BOOTS);
         };
      } else {
         ItemStack stack = MeteorClient.mc.player.getItemBySlot(slot);
         return stack.isEmpty() && this.showEmpty.get() ? DisplayItemUtils.toStack(Items.BARRIER) : stack;
      }
   }

   private float getScale() {
      return this.customScale.get() ? this.scale.get().floatValue() : this.scale.getDefaultValue().floatValue();
   }

   public enum Durability {
      None,
      Bar,
      Total,
      Percentage;
   }

   public enum Orientation {
      Horizontal,
      Vertical;
   }
}
