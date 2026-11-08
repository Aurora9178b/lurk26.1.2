package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
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
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.LiquidGlassUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryHud extends HudElement {
   public static final HudElementInfo<InventoryHud> INFO = new HudElementInfo<>(Hud.GROUP, "inventory", "Displays your inventory.", InventoryHud::new);
   private static final double BASE_WIDTH = 162.0;
   private static final double BASE_HEIGHT = 54.0;
   private static final double DEFAULT_CORNER_RADIUS = 4.0;
   private static final double GRID_X = 1.0;
   private static final double GRID_Y = 1.0;
   private static final double SLOT_SPACING = 18.0;
   private ItemStack previewPickaxe;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<Boolean> containers = this.sgGeneral
      .add(new BoolSetting.Builder().name("containers").description("Shows the contents of a container when holding them.").defaultValue(false).build());
   public final Setting<Double> scale = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the inventory HUD.")
            .defaultValue(2.0)
            .onChanged(var1 -> this.calculateSize())
            .min(1.0)
            .sliderRange(1.0, 5.0)
            .build()
      );
   private final Setting<InventoryHud.BackgroundMode> backgroundMode = this.sgBackground
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("background-mode"))
                  .description("The render mode used for the inventory background."))
               .defaultValue(InventoryHud.BackgroundMode.LiquidGlass))
            .build()
      );
   private final Setting<Double> backgroundRadius = this.sgBackground
      .add(
         new DoubleSetting.Builder()
            .name("background-radius")
            .description("Radius of the background corners.")
            .defaultValue(4.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .defaultValue(new SettingColor(92, 106, 142, 178))
            .build()
      );
   private final Setting<Boolean> enableGlow = this.sgBackground
      .add(new BoolSetting.Builder().name("enable-glow").description("Renders glow around the inventory background.").defaultValue(true).build());
   private final ItemStack[] containerItems = new ItemStack[27];

   private InventoryHud() {
      super(INFO);
      this.calculateSize();
   }

   @Override
   public void render(HudRenderer renderer) {
      double x = this.x;
      double y = this.y;
      double s = this.getScale();
      double width = 162.0 * s;
      double height = 54.0 * s;
      double radius = this.backgroundRadius.get() * s;
      ItemStack container = this.getContainer();
      boolean hasContainer = this.containers.get() && container != null;
      if (hasContainer) {
         Utils.getItemsInContainerItem(container, this.containerItems);
      }

      this.setSize(width, height);
      this.drawBackground(renderer, x, y, width, height, radius, hasContainer ? Utils.getShulkerColor(container) : this.backgroundColor.get());
      renderer.post(() -> {
         for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
               int index = row * 9 + i;
               ItemStack stack;
               if (MeteorClient.mc.player == null) {
                  if (this.previewPickaxe == null) {
                     this.previewPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                  }

                  stack = index == 26 ? this.previewPickaxe : ItemStack.EMPTY;
               } else {
                  stack = hasContainer ? this.containerItems[index] : MeteorClient.mc.player.getInventory().getItem(index + 9);
               }

               if (stack != null && !stack.isEmpty()) {
                  int itemX = (int)Math.round(x + (1.0 + i * 18.0) * s);
                  int itemY = (int)Math.round(y + (1.0 + row * 18.0) * s);
                  renderer.item(stack, itemX, itemY, (float)s, true);
               }
            }
         }
      });
   }

   private void calculateSize() {
      this.setSize(162.0 * this.getScale(), 54.0 * this.getScale());
   }

   private void drawBackground(HudRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
      if (this.backgroundMode.get() == InventoryHud.BackgroundMode.LiquidGlass) {
         LiquidGlassUtil.render(renderer, x, y, width, height, radius, color);
      } else {
         BlurUtil.render((int)Math.round(x), (int)Math.round(y), (int)Math.ceil(width), (int)Math.ceil(height), (float)radius);
         Render2DUtil.drawRoundedRect(renderer, x, y, width, height, radius, color);
      }

      if (this.enableGlow.get()) {
         Render2DUtil.drawRoundedGlow(x, y, width, height, radius);
      }
   }

   private ItemStack getContainer() {
      if (!this.isInEditor() && MeteorClient.mc.player != null) {
         ItemStack stack = MeteorClient.mc.player.getOffhandItem();
         if (!Utils.hasItems(stack) && stack.getItem() != Items.ENDER_CHEST) {
            stack = MeteorClient.mc.player.getMainHandItem();
            return !Utils.hasItems(stack) && stack.getItem() != Items.ENDER_CHEST ? null : stack;
         } else {
            return stack;
         }
      } else {
         return null;
      }
   }

   private double getScale() {
      return this.scale.get();
   }

   public enum BackgroundMode {
      Blur,
      LiquidGlass;
   }
}
