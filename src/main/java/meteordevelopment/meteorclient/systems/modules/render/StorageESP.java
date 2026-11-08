package meteordevelopment.meteorclient.systems.modules.render;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StorageBlockListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.MeshBuilderVertexConsumerProvider;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.SimpleBlockRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.render.postprocess.PostProcessShaders;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class StorageESP extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgOpened = this.settings.createGroup("Opened Rendering");
   private final Set<BlockPos> interactedBlocks = new HashSet<>();
   private final Setting<StorageESP.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Rendering mode."))
               .defaultValue(StorageESP.Mode.Shader))
            .build()
      );
   private final Setting<List<BlockEntityType<?>>> storageBlocks = this.sgGeneral
      .add(
         new StorageBlockListSetting.Builder()
            .name("storage-blocks")
            .description("Select the storage blocks to display.")
            .defaultValue(StorageBlockListSetting.STORAGE_BLOCKS)
            .build()
      );
   private final Setting<Boolean> tracers = this.sgGeneral
      .add(new BoolSetting.Builder().name("tracers").description("Draws tracers to storage blocks.").defaultValue(false).build());
   public final Setting<ShapeMode> shapeMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   public final Setting<Integer> fillOpacity = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("fill-opacity")
            .description("The opacity of the shape fill.")
            .visible(() -> this.shapeMode.get() != ShapeMode.Lines)
            .defaultValue(50)
            .range(0, 255)
            .sliderMax(255)
            .build()
      );
   public final Setting<Integer> outlineWidth = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("width")
            .description("The width of the shader outline.")
            .visible(() -> this.mode.get() == StorageESP.Mode.Shader)
            .defaultValue(1)
            .range(1, 10)
            .sliderRange(1, 5)
            .build()
      );
   public final Setting<Double> glowMultiplier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("glow-multiplier")
            .description("Multiplier for glow effect")
            .visible(() -> this.mode.get() == StorageESP.Mode.Shader)
            .decimalPlaces(3)
            .defaultValue(3.5)
            .min(0.0)
            .sliderMax(10.0)
            .build()
      );
   private final Setting<SettingColor> chest = this.sgGeneral
      .add(new ColorSetting.Builder().name("chest").description("The color of chests.").defaultValue(new SettingColor(255, 160, 0, 255)).build());
   private final Setting<SettingColor> trappedChest = this.sgGeneral
      .add(new ColorSetting.Builder().name("trapped-chest").description("The color of trapped chests.").defaultValue(new SettingColor(255, 0, 0, 255)).build());
   private final Setting<SettingColor> barrel = this.sgGeneral
      .add(new ColorSetting.Builder().name("barrel").description("The color of barrels.").defaultValue(new SettingColor(255, 160, 0, 255)).build());
   private final Setting<SettingColor> shulker = this.sgGeneral
      .add(new ColorSetting.Builder().name("shulker").description("The color of Shulker Boxes.").defaultValue(new SettingColor(255, 160, 0, 255)).build());
   private final Setting<SettingColor> enderChest = this.sgGeneral
      .add(new ColorSetting.Builder().name("ender-chest").description("The color of Ender Chests.").defaultValue(new SettingColor(120, 0, 255, 255)).build());
   private final Setting<SettingColor> other = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("other")
            .description("The color of furnaces, dispensers, droppers and hoppers.")
            .defaultValue(new SettingColor(140, 140, 140, 255))
            .build()
      );
   private final Setting<Double> fadeDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("fade-distance")
            .description("The distance at which the color will fade.")
            .defaultValue(6.0)
            .min(0.0)
            .sliderMax(12.0)
            .build()
      );
   private final Setting<Boolean> hideOpened = this.sgOpened
      .add(new BoolSetting.Builder().name("hide-opened").description("Hides opened containers.").defaultValue(false).build());
   private final Setting<SettingColor> openedColor = this.sgOpened
      .add(
         new ColorSetting.Builder()
            .name("opened-color")
            .description("Optional setting to change colors of opened chests, as opposed to not rendering. Disabled at zero opacity.")
            .defaultValue(new SettingColor(203, 90, 203, 0))
            .build()
      );
   private final Color lineColor = new Color(0, 0, 0, 0);
   private final Color sideColor = new Color(0, 0, 0, 0);
   private boolean render;
   private int count;
   private final MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.WORLD_COLORED);
   private final MeshBuilderVertexConsumerProvider vertexConsumerProvider = new MeshBuilderVertexConsumerProvider(this.mesh);

   public StorageESP() {
      super(Categories.Render, "storage-esp", "Renders all specified storage blocks.");
   }

   private void getBlockEntityColor(BlockEntity blockEntity) {
      this.render = false;
      if (this.storageBlocks.get().contains(blockEntity.getType())) {
         if (blockEntity instanceof TrappedChestBlockEntity) {
            this.lineColor.set(this.trappedChest.get());
         } else if (blockEntity instanceof ChestBlockEntity) {
            this.lineColor.set(this.chest.get());
         } else if (blockEntity instanceof BarrelBlockEntity) {
            this.lineColor.set(this.barrel.get());
         } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            this.lineColor.set(this.shulker.get());
         } else if (blockEntity instanceof EnderChestBlockEntity) {
            this.lineColor.set(this.enderChest.get());
         } else {
            if (!(blockEntity instanceof AbstractFurnaceBlockEntity)
               && !(blockEntity instanceof BrewingStandBlockEntity)
               && !(blockEntity instanceof ChiseledBookShelfBlockEntity)
               && !(blockEntity instanceof CrafterBlockEntity)
               && !(blockEntity instanceof DispenserBlockEntity)
               && !(blockEntity instanceof DecoratedPotBlockEntity)
               && !(blockEntity instanceof HopperBlockEntity)) {
               return;
            }

            this.lineColor.set(this.other.get());
         }

         this.render = true;
         if (this.shapeMode.get() == ShapeMode.Sides || this.shapeMode.get() == ShapeMode.Both) {
            this.sideColor.set(this.lineColor);
            this.sideColor.a = this.fillOpacity.get();
         }
      }
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WButton clear = list.add(theme.button("Clear Rendering Cache")).expandX().widget();
      clear.action = this.interactedBlocks::clear;
      return list;
   }

   @EventHandler
   private void onBlockInteract(InteractBlockEvent event) {
      BlockPos pos = event.result.getBlockPos();
      BlockEntity blockEntity = this.mc.level.getBlockEntity(pos);
      if (blockEntity != null) {
         this.interactedBlocks.add(pos);
         if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
            BlockState state = chestBlockEntity.getBlockState();
            ChestType chestType = state.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.LEFT || chestType == ChestType.RIGHT) {
               Direction facing = state.getValue(ChestBlock.FACING);
               BlockPos otherPartPos = pos.relative(chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
               this.interactedBlocks.add(otherPartPos);
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      this.count = 0;

      for (BlockEntity blockEntity : Utils.blockEntities()) {
         boolean interacted = this.interactedBlocks.contains(blockEntity.getBlockPos());
         if (!interacted || !this.hideOpened.get()) {
            this.getBlockEntityColor(blockEntity);
            if (interacted && this.openedColor.get().a > 0) {
               this.lineColor.set(this.openedColor.get());
               this.sideColor.set(this.openedColor.get());
               this.sideColor.a = this.fillOpacity.get();
            }

            if (this.render) {
               double dist = PlayerUtils.squaredDistanceTo(
                  blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5
               );
               double a = 1.0;
               if (dist <= this.fadeDistance.get() * this.fadeDistance.get()) {
                  a = dist / (this.fadeDistance.get() * this.fadeDistance.get());
               }

               if (!(a < 0.075)) {
                  if (this.count == 0 && this.mode.get() == StorageESP.Mode.Shader) {
                     this.mesh.begin();
                  }

                  int prevLineA = this.lineColor.a;
                  int prevSideA = this.sideColor.a;
                  this.lineColor.a = (int)(this.lineColor.a * a);
                  this.sideColor.a = (int)(this.sideColor.a * a);
                  if (this.tracers.get()) {
                     event.renderer
                        .line(
                           RenderUtils.center.x,
                           RenderUtils.center.y,
                           RenderUtils.center.z,
                           blockEntity.getBlockPos().getX() + 0.5,
                           blockEntity.getBlockPos().getY() + 0.5,
                           blockEntity.getBlockPos().getZ() + 0.5,
                           this.lineColor
                        );
                  }

                  if (this.mode.get() == StorageESP.Mode.Box) {
                     this.renderBox(event, blockEntity);
                  }

                  if (this.mode.get() == StorageESP.Mode.Shader) {
                     this.renderShader(event, blockEntity);
                  }

                  this.lineColor.a = prevLineA;
                  this.sideColor.a = prevSideA;
                  this.count++;
               }
            }
         }
      }

      if (this.mode.get() == StorageESP.Mode.Shader && this.count > 0) {
         MeshRenderer.begin()
            .attachments(PostProcessShaders.STORAGE_OUTLINE.framebuffer)
            .clearColor(Color.CLEAR)
            .pipeline(MeteorRenderPipelines.WORLD_COLORED)
            .mesh(this.mesh, event.matrices)
            .end();
         PostProcessShaders.STORAGE_OUTLINE.render();
      }
   }

   private void renderBox(Render3DEvent event, BlockEntity blockEntity) {
      double x1 = blockEntity.getBlockPos().getX();
      double y1 = blockEntity.getBlockPos().getY();
      double z1 = blockEntity.getBlockPos().getZ();
      double x2 = blockEntity.getBlockPos().getX() + 1;
      double y2 = blockEntity.getBlockPos().getY() + 1;
      double z2 = blockEntity.getBlockPos().getZ() + 1;
      int excludeDir = 0;
      if (blockEntity instanceof ChestBlockEntity) {
         BlockState state = this.mc.level.getBlockState(blockEntity.getBlockPos());
         if ((state.getBlock() == Blocks.CHEST || state.getBlock() == Blocks.TRAPPED_CHEST) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            excludeDir = Dir.get(ChestBlock.getConnectedDirection(state));
         }
      }

      if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity) {
         double a = 0.0625;
         if (Dir.isNot(excludeDir, (byte)32)) {
            x1 += a;
         }

         if (Dir.isNot(excludeDir, (byte)8)) {
            z1 += a;
         }

         if (Dir.isNot(excludeDir, (byte)64)) {
            x2 -= a;
         }

         y2 -= a * 2.0;
         if (Dir.isNot(excludeDir, (byte)16)) {
            z2 -= a;
         }
      }

      event.renderer.box(x1, y1, z1, x2, y2, z2, this.sideColor, this.lineColor, this.shapeMode.get(), excludeDir);
   }

   private void renderShader(Render3DEvent event, BlockEntity blockEntity) {
      this.vertexConsumerProvider.setColor(this.lineColor);
      SimpleBlockRenderer.renderWithBlockEntity(blockEntity, event.tickDelta, this.vertexConsumerProvider);
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.count);
   }

   public boolean isShader() {
      return this.isActive() && this.mode.get() == StorageESP.Mode.Shader;
   }

   public enum Mode {
      Box,
      Shader;
   }
}
