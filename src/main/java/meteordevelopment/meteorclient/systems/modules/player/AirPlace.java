package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AirPlace extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRange = this.settings.createGroup("Range");
   private final Setting<Boolean> render = this.sgGeneral
      .add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
      );
   private final Setting<Boolean> customRange = this.sgRange
      .add(new BoolSetting.Builder().name("custom-range").description("Use custom range for air place.").defaultValue(false).build());
   private final Setting<Double> range = this.sgRange
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Custom range to place at.")
            .visible(this.customRange::get)
            .defaultValue(5.0)
            .min(0.0)
            .sliderMax(6.0)
            .build()
      );
   private HitResult hitResult;

   public AirPlace() {
      super(Categories.Player, "air-place", "Places a block where your crosshair is pointing at.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (InvUtils.testInHands(this::placeable)) {
         if (this.mc.hitResult == null || this.mc.hitResult.getType() == Type.MISS) {
            double r = this.customRange.get() ? this.range.get() : this.mc.player.blockInteractionRange();
            this.hitResult = this.mc.getCameraEntity().pick(r, 0.0F, false);
         }
      }
   }

   @EventHandler
   private void onInteractItem(InteractItemEvent event) {
      if (this.hitResult instanceof BlockHitResult bhr && this.placeable(this.mc.player.getItemInHand(event.hand))) {
         Block toPlace = Blocks.OBSIDIAN;
         Item i = this.mc.player.getItemInHand(event.hand).getItem();
         if (i instanceof BlockItem blockItem) {
            toPlace = blockItem.getBlock();
         }

         if (BlockUtils.canPlaceBlock(bhr.getBlockPos(), i instanceof ArmorStandItem || i instanceof BlockItem, toPlace)) {
            Vec3 hitPos = Vec3.atCenterOf(bhr.getBlockPos());
            BlockHitResult b = new BlockHitResult(hitPos, this.mc.player.getMotionDirection().getOpposite(), bhr.getBlockPos(), false);
            BlockUtils.interact(b, event.hand, true);
            event.toReturn = InteractionResult.SUCCESS;
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.hitResult instanceof BlockHitResult bhr
         && (this.mc.hitResult == null || this.mc.hitResult.getType() == Type.MISS)
         && this.mc.level.getBlockState(bhr.getBlockPos()).canBeReplaced()
         && InvUtils.testInHands(this::placeable)
         && this.render.get()) {
         event.renderer.box(bhr.getBlockPos(), this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      }
   }

   private boolean placeable(ItemStack stack) {
      Item i = stack.getItem();
      return i instanceof BlockItem || i instanceof SpawnEggItem || i instanceof FireworkRocketItem || i instanceof ArmorStandItem;
   }
}
