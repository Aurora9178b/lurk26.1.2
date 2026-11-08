package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockSelection extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> advanced = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("advanced").description("Shows a more advanced outline on different types of shape blocks.").defaultValue(true).build()
      );
   private final Setting<Boolean> oneSide = this.sgGeneral
      .add(new BoolSetting.Builder().name("single-side").description("Only renders the side you are looking at.").defaultValue(false).build());
   private final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 255, 255, 50)).build());
   private final Setting<SettingColor> lineColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private final Setting<Boolean> hideInside = this.sgGeneral
      .add(new BoolSetting.Builder().name("hide-when-inside-block").description("Hide selection when inside target block.").defaultValue(true).build());

   public BlockSelection() {
      super(Categories.Render, "block-selection", "Modifies how your block selection is rendered.");
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.hitResult != null && this.mc.hitResult instanceof BlockHitResult result && result.getType() != Type.MISS) {
         if (!this.hideInside.get() || !result.isInside()) {
            BlockPos bp = result.getBlockPos();
            Direction side = result.getDirection();
            VoxelShape shape = this.mc.level.getBlockState(bp).getShape(this.mc.level, bp);
            if (!shape.isEmpty()) {
               AABB box = shape.bounds();
               if (this.oneSide.get()) {
                  switch (side) {
                     case UP:
                     case DOWN:
                        event.renderer
                           .sideHorizontal(
                              bp.getX() + box.minX,
                              bp.getY() + (side == Direction.DOWN ? box.minY : box.maxY),
                              bp.getZ() + box.minZ,
                              bp.getX() + box.maxX,
                              bp.getZ() + box.maxZ,
                              this.sideColor.get(),
                              this.lineColor.get(),
                              this.shapeMode.get()
                           );
                        break;
                     case SOUTH:
                     case NORTH:
                        double z = side == Direction.NORTH ? box.minZ : box.maxZ;
                        event.renderer
                           .sideVertical(
                              bp.getX() + box.minX,
                              bp.getY() + box.minY,
                              bp.getZ() + z,
                              bp.getX() + box.maxX,
                              bp.getY() + box.maxY,
                              bp.getZ() + z,
                              this.sideColor.get(),
                              this.lineColor.get(),
                              this.shapeMode.get()
                           );
                        break;
                     case EAST:
                     case WEST:
                        double x = side == Direction.WEST ? box.minX : box.maxX;
                        event.renderer
                           .sideVertical(
                              bp.getX() + x,
                              bp.getY() + box.minY,
                              bp.getZ() + box.minZ,
                              bp.getX() + x,
                              bp.getY() + box.maxY,
                              bp.getZ() + box.maxZ,
                              this.sideColor.get(),
                              this.lineColor.get(),
                              this.shapeMode.get()
                           );
                  }
               } else if (this.advanced.get()) {
                  if (this.shapeMode.get() == ShapeMode.Both || this.shapeMode.get() == ShapeMode.Lines) {
                     shape.forAllEdges(
                        (minX, minY, minZ, maxX, maxY, maxZ) -> event.renderer
                           .line(
                              bp.getX() + minX, bp.getY() + minY, bp.getZ() + minZ, bp.getX() + maxX, bp.getY() + maxY, bp.getZ() + maxZ, this.lineColor.get()
                           )
                     );
                  }

                  if (this.shapeMode.get() == ShapeMode.Both || this.shapeMode.get() == ShapeMode.Sides) {
                     for (AABB b : shape.toAabbs()) {
                        this.render(event, bp, b);
                     }
                  }
               } else {
                  this.render(event, bp, box);
               }
            }
         }
      }
   }

   private void render(Render3DEvent event, BlockPos bp, AABB box) {
      event.renderer
         .box(
            bp.getX() + box.minX,
            bp.getY() + box.minY,
            bp.getZ() + box.minZ,
            bp.getX() + box.maxX,
            bp.getY() + box.maxY,
            bp.getZ() + box.maxZ,
            this.sideColor.get(),
            this.lineColor.get(),
            this.shapeMode.get(),
            0
         );
   }
}
