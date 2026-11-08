package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EChestFarmer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> selfToggle = this.sgGeneral
      .add(new BoolSetting.Builder().name("self-toggle").description("Disables when you reach the desired amount of obsidian.").defaultValue(false).build());
   private final Setting<Boolean> ignoreExisting = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("ignore-existing")
            .description("Ignores existing obsidian in your inventory and mines the total target amount.")
            .defaultValue(true)
            .visible(this.selfToggle::get)
            .build()
      );
   private final Setting<Integer> amount = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("amount")
            .description("The amount of obsidian to farm.")
            .defaultValue(64)
            .sliderMax(128)
            .range(8, 512)
            .sliderRange(8, 128)
            .visible(this.selfToggle::get)
            .build()
      );
   private final Setting<Boolean> swingHand = this.sgRender
      .add(new BoolSetting.Builder().name("swing-hand").description("Swing hand client-side.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 50))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
      );
   private final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
   private BlockPos target;
   private int startCount;

   public EChestFarmer() {
      super(Categories.World, "echest-farmer", "Places and breaks EChests to farm obsidian.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.startCount = InvUtils.find(Items.OBSIDIAN).count();
   }

   @Override
   public void onDeactivate() {
      InvUtils.swapBack();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.target == null) {
         if (this.mc.hitResult == null || this.mc.hitResult.getType() != Type.BLOCK) {
            return;
         }

         BlockPos pos = ((BlockHitResult)this.mc.hitResult).getBlockPos().above();
         BlockState state = this.mc.level.getBlockState(pos);
         if (!state.canBeReplaced() && state.getBlock() != Blocks.ENDER_CHEST) {
            return;
         }

         this.target = ((BlockHitResult)this.mc.hitResult).getBlockPos().above();
      }

      if (!PlayerUtils.isWithinReach(this.target)) {
         this.error("Target block pos out of reach.");
         this.target = null;
      } else if (this.selfToggle.get() && InvUtils.find(Items.OBSIDIAN).count() - (this.ignoreExisting.get() ? this.startCount : 0) >= this.amount.get()) {
         InvUtils.swapBack();
         this.toggle();
      } else {
         if (this.mc.level.getBlockState(this.target).getBlock() == Blocks.ENDER_CHEST) {
            double bestScore = -1.0;
            int bestSlot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack itemStack = this.mc.player.getInventory().getItem(i);
               if (!Utils.hasEnchantment(itemStack, Enchantments.SILK_TOUCH)) {
                  double score = itemStack.getDestroySpeed(Blocks.ENDER_CHEST.defaultBlockState());
                  if (score > bestScore) {
                     bestScore = score;
                     bestSlot = i;
                  }
               }
            }

            if (bestSlot == -1) {
               return;
            }

            InvUtils.swap(bestSlot, true);
            BlockUtils.breakBlock(this.target, this.swingHand.get());
         }

         if (this.mc.level.getBlockState(this.target).canBeReplaced()) {
            FindItemResult echest = InvUtils.findInHotbar(Items.ENDER_CHEST);
            if (!echest.found()) {
               this.error("No Echests in hotbar, disabling");
               this.toggle();
               return;
            }

            BlockUtils.place(this.target, echest, true, 0, true);
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.target != null && this.render.get() && !Modules.get().get(PacketMine.class).isMiningBlock(this.target)) {
         AABB box = this.SHAPE.toAabbs().getFirst();
         event.renderer
            .box(
               this.target.getX() + box.minX,
               this.target.getY() + box.minY,
               this.target.getZ() + box.minZ,
               this.target.getX() + box.maxX,
               this.target.getY() + box.maxY,
               this.target.getZ() + box.maxZ,
               this.sideColor.get(),
               this.lineColor.get(),
               this.shapeMode.get(),
               0
            );
      }
   }
}
