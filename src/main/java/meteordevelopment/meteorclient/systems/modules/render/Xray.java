package meteordevelopment.meteorclient.systems.modules.render;

import java.util.List;
import meteordevelopment.meteorclient.MixinPlugin;
import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import meteordevelopment.meteorclient.events.world.AmbientOcclusionEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.mixin.BlockEntityRenderStateAccessor;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;

public class Xray extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public static final List<Block> ORES = List.of(
      Blocks.COAL_ORE,
      Blocks.DEEPSLATE_COAL_ORE,
      Blocks.IRON_ORE,
      Blocks.DEEPSLATE_IRON_ORE,
      Blocks.GOLD_ORE,
      Blocks.DEEPSLATE_GOLD_ORE,
      Blocks.LAPIS_ORE,
      Blocks.DEEPSLATE_LAPIS_ORE,
      Blocks.REDSTONE_ORE,
      Blocks.DEEPSLATE_REDSTONE_ORE,
      Blocks.DIAMOND_ORE,
      Blocks.DEEPSLATE_DIAMOND_ORE,
      Blocks.EMERALD_ORE,
      Blocks.DEEPSLATE_EMERALD_ORE,
      Blocks.COPPER_ORE,
      Blocks.DEEPSLATE_COPPER_ORE,
      Blocks.NETHER_GOLD_ORE,
      Blocks.NETHER_QUARTZ_ORE,
      Blocks.ANCIENT_DEBRIS
   );
   private final Setting<List<Block>> blocks = this.sgGeneral
      .add(new BlockListSetting.Builder().name("whitelist").description("Which blocks to show x-rayed.").defaultValue(ORES).onChanged(var1 -> {
         if (this.isActive()) {
            this.mc.levelRenderer.allChanged();
         }
      }).build());
   public final Setting<Integer> opacity = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("opacity")
            .description("The opacity for all other blocks.")
            .defaultValue(25)
            .range(0, 255)
            .sliderMax(255)
            .onChanged(var1 -> {
               if (this.isActive()) {
                  this.mc.levelRenderer.allChanged();
               }
            })
            .build()
      );
   private final Setting<Xray.FluidOpacity> fluidOpacity = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("fluid-opacity"))
                     .description("Which fluids should use xray opacity."))
                  .defaultValue(Xray.FluidOpacity.Both))
               .onChanged(var1 -> {
                  if (this.isActive()) {
                     this.mc.levelRenderer.allChanged();
                  }
               }))
            .build()
      );
   private final Setting<Boolean> exposedOnly = this.sgGeneral
      .add(new BoolSetting.Builder().name("exposed-only").description("Show only exposed ores.").defaultValue(false).onChanged(var1 -> {
         if (this.isActive()) {
            this.mc.levelRenderer.allChanged();
         }
      }).build());

   public Xray() {
      super(Categories.Render, "xray", "Only renders specified blocks. Good for mining.");
   }

   @Override
   public void onActivate() {
      this.mc.levelRenderer.allChanged();
   }

   @Override
   public void onDeactivate() {
      this.mc.levelRenderer.allChanged();
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      return MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()
         ? theme.label("Warning: Due to shaders in use, opacity is overridden to 0.")
         : null;
   }

   @EventHandler
   private void onRenderBlockEntity(RenderBlockEntityEvent event) {
      BlockState state = ((BlockEntityRenderStateAccessor)event.blockEntityState).meteor$getBlockState();
      if (getAlpha(state, event.blockEntityState.blockPos) == 0) {
         event.cancel();
      }
   }

   @EventHandler
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      event.cancel();
   }

   @EventHandler
   private void onAmbientOcclusion(AmbientOcclusionEvent event) {
      event.lightLevel = 1.0F;
   }

   public boolean modifyDrawSide(BlockState state, BlockGetter view, BlockPos pos, Direction facing, boolean returns) {
      if (!returns && !this.isBlocked(state.getBlock(), pos)) {
         BlockPos adjPos = pos.relative(facing);
         BlockState adjState = view.getBlockState(adjPos);
         return adjState.getFaceOcclusionShape(facing.getOpposite()) != Shapes.block()
            || adjState.getBlock() != state.getBlock()
            || !adjState.isSolidRender()
            || this.isBlocked(adjState.getBlock(), adjPos);
      } else {
         return returns;
      }
   }

   public boolean isBlocked(Block block, BlockPos blockPos) {
      return !this.blocks.get().contains(block) || this.exposedOnly.get() && blockPos != null && !BlockUtils.isExposed(blockPos);
   }

   public static int getAlpha(BlockState state, BlockPos pos) {
      WallHack wallHack = Modules.get().get(WallHack.class);
      Xray xray = Modules.get().get(Xray.class);
      Block block = state.getBlock();
      if (wallHack.isActive() && wallHack.blocks.get().contains(block)) {
         if (MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()) {
            return 0;
         }

         int alpha;
         if (xray.isActive()) {
            alpha = xray.opacity.get();
         } else {
            alpha = wallHack.opacity.get();
         }

         return alpha;
      } else if (xray.isActive() && !wallHack.isActive() && xray.isBlocked(block, pos)) {
         return MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse() ? 0 : xray.opacity.get();
      } else {
         return -1;
      }
   }

   public static int getFluidAlpha(FluidState state, BlockPos pos) {
      WallHack wallHack = Modules.get().get(WallHack.class);
      Xray xray = Modules.get().get(Xray.class);
      Block fluidBlock = state.createLegacyBlock().getBlock();
      if (wallHack.isActive() && wallHack.blocks.get().contains(fluidBlock)) {
         return MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse() ? 0 : xray.isActive() ? xray.opacity.get() : wallHack.opacity.get();
      } else if (xray.isActive() && !wallHack.isActive() && xray.shouldApplyFluidOpacity(state) && xray.isBlocked(fluidBlock, pos)) {
         return MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse() ? 0 : xray.opacity.get();
      } else {
         return -1;
      }
   }

   private boolean shouldApplyFluidOpacity(FluidState state) {
      return switch ((Xray.FluidOpacity)this.fluidOpacity.get()) {
         case None -> false;
         case Water -> state.is(FluidTags.WATER);
         case Lava -> state.is(FluidTags.LAVA);
         case Both -> state.is(FluidTags.WATER) || state.is(FluidTags.LAVA);
      };
   }

   public enum FluidOpacity {
      None,
      Water,
      Lava,
      Both;
   }
}
