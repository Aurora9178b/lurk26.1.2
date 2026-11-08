package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.List;
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
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;

public class VoidESP extends Module {
   private static final Direction[] SIDES = new Direction[]{Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.WEST};
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> airOnly = this.sgGeneral
      .add(new BoolSetting.Builder().name("air-only").description("Checks bedrock only for air blocks.").defaultValue(false).build());
   private final Setting<Integer> horizontalRadius = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("horizontal-radius")
            .description("Horizontal radius in which to search for holes.")
            .defaultValue(64)
            .min(0)
            .sliderMax(256)
            .build()
      );
   private final Setting<Integer> holeHeight = this.sgGeneral
      .add(new IntSetting.Builder().name("hole-height").description("The minimum hole height to be rendered.").defaultValue(1).min(1).sliderRange(1, 5).build());
   private final Setting<Boolean> netherRoof = this.sgGeneral
      .add(new BoolSetting.Builder().name("nether-roof").description("Check for holes in nether roof.").defaultValue(true).build());
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
            .name("fill-color")
            .description("The color that fills holes in the void.")
            .defaultValue(new SettingColor(225, 25, 25, 50))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The color to draw lines of holes to the void.")
            .defaultValue(new SettingColor(225, 25, 255))
            .build()
      );
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private final Pool<VoidESP.Void> voidHolePool = new Pool<>(() -> new VoidESP.Void());
   private final List<VoidESP.Void> voidHoles = new ArrayList<>();

   public VoidESP() {
      super(Categories.Render, "void-esp", "Renders holes in bedrock layers that lead to the void.");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      this.voidHoles.clear();
      if (!this.mc.level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END)) {
         int px = this.mc.player.blockPosition().getX();
         int pz = this.mc.player.blockPosition().getZ();
         int radius = this.horizontalRadius.get();

         for (int x = px - radius; x <= px + radius; x++) {
            for (int z = pz - radius; z <= pz + radius; z++) {
               this.blockPos.set(x, this.mc.level.getMinY(), z);
               if (this.isHole(this.blockPos, false)) {
                  this.voidHoles.add(this.voidHolePool.get().set(this.blockPos.set(x, this.mc.level.getMinY(), z), false));
               }

               if (this.netherRoof.get() && this.mc.level.dimensionTypeRegistration().is(BuiltinDimensionTypes.NETHER)) {
                  this.blockPos.set(x, 127, z);
                  if (this.isHole(this.blockPos, true)) {
                     this.voidHoles.add(this.voidHolePool.get().set(this.blockPos.set(x, 127, z), true));
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      for (VoidESP.Void voidHole : this.voidHoles) {
         voidHole.render(event);
      }
   }

   private boolean isBlockWrong(BlockPos blockPos) {
      ChunkAccess chunk = this.mc.level.getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, false);
      if (chunk == null) {
         return true;
      }

      Block block = chunk.getBlockState(blockPos).getBlock();
      return this.airOnly.get() ? block != Blocks.AIR : block == Blocks.BEDROCK;
   }

   private boolean isHole(MutableBlockPos blockPos, boolean nether) {
      for (int i = 0; i < this.holeHeight.get(); i++) {
         blockPos.setY(nether ? 127 - i : this.mc.level.getMinY() + i);
         if (this.isBlockWrong(blockPos)) {
            return false;
         }
      }

      return true;
   }

   private class Void {
      private int x;
      private int y;
      private int z;
      private int excludeDir;

      public VoidESP.Void set(MutableBlockPos blockPos, boolean nether) {
         this.x = blockPos.getX();
         this.y = blockPos.getY();
         this.z = blockPos.getZ();
         this.excludeDir = 0;

         for (Direction side : VoidESP.SIDES) {
            blockPos.set(this.x + side.getStepX(), this.y, this.z + side.getStepZ());
            if (VoidESP.this.isHole(blockPos, nether)) {
               this.excludeDir = this.excludeDir | Dir.get(side);
            }
         }

         return this;
      }

      public void render(Render3DEvent event) {
         event.renderer
            .box(
               this.x,
               this.y,
               this.z,
               this.x + 1,
               this.y + 1,
               this.z + 1,
               VoidESP.this.sideColor.get(),
               VoidESP.this.lineColor.get(),
               VoidESP.this.shapeMode.get(),
               this.excludeDir
            );
      }
   }
}
