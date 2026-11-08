package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.LevelRendererAccessor;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class HoleHud extends HudElement {
   public static final HudElementInfo<HoleHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "hole", "Displays information about the hole you are standing in.", HoleHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   public final Setting<List<Block>> safe = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("safe-blocks")
            .description("Which blocks to consider safe.")
            .defaultValue(Blocks.OBSIDIAN, Blocks.BEDROCK, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK)
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
   private final Color BG_COLOR = new Color(255, 25, 25, 100);
   private final Color OL_COLOR = new Color(255, 25, 25, 255);

   public HoleHud() {
      super(INFO);
      this.calculateSize();
   }

   private void calculateSize() {
      this.setSize(48.0F * this.getScale(), 48.0F * this.getScale());
   }

   @Override
   public void render(HudRenderer renderer) {
      renderer.post(() -> {
         this.drawBlock(renderer, this.get(HoleHud.Facing.Left), this.x, this.y + 16.0F * this.getScale());
         this.drawBlock(renderer, this.get(HoleHud.Facing.Front), this.x + 16.0F * this.getScale(), this.y);
         this.drawBlock(renderer, this.get(HoleHud.Facing.Right), this.x + 32.0F * this.getScale(), this.y + 16.0F * this.getScale());
         this.drawBlock(renderer, this.get(HoleHud.Facing.Back), this.x + 16.0F * this.getScale(), this.y + 32.0F * this.getScale());
      });
      if (this.background.get()) {
         renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
      }
   }

   private Direction get(HoleHud.Facing dir) {
      return this.isInEditor() ? Direction.DOWN : Direction.fromYRot(Mth.wrapDegrees(MeteorClient.mc.player.getYRot() + dir.offset));
   }

   private void drawBlock(HudRenderer renderer, Direction dir, double x, double y) {
      Block block = dir == Direction.DOWN
         ? Blocks.OBSIDIAN
         : MeteorClient.mc.level.getBlockState(MeteorClient.mc.player.blockPosition().relative(dir)).getBlock();
      if (this.safe.get().contains(block)) {
         renderer.item(DisplayItemUtils.toStack(block.asItem()), (int)x, (int)y, this.getScale(), false);
         if (dir != Direction.DOWN) {
            ((LevelRendererAccessor)MeteorClient.mc.levelRenderer).meteor$getDestroyingBlocks().values().forEach(info -> {
               if (info.getPos().equals(MeteorClient.mc.player.blockPosition().relative(dir))) {
                  this.renderBreaking(renderer, x, y, info.getProgress() / 9.0F);
               }
            });
         }
      }
   }

   private void renderBreaking(HudRenderer renderer, double x, double y, double percent) {
      renderer.quad(x, y, 16.0 * percent * this.getScale(), 16.0F * this.getScale(), this.BG_COLOR);
      renderer.quad(x, y, 16.0F * this.getScale(), 1.0F * this.getScale(), this.OL_COLOR);
      renderer.quad(x, y + 15.0F * this.getScale(), 16.0F * this.getScale(), 1.0F * this.getScale(), this.OL_COLOR);
      renderer.quad(x, y, 1.0F * this.getScale(), 16.0F * this.getScale(), this.OL_COLOR);
      renderer.quad(x + 15.0F * this.getScale(), y, 1.0F * this.getScale(), 16.0F * this.getScale(), this.OL_COLOR);
   }

   private float getScale() {
      return this.customScale.get() ? this.scale.get().floatValue() : this.scale.getDefaultValue().floatValue();
   }

   private enum Facing {
      Left(-90),
      Right(90),
      Front(0),
      Back(180);

      public final int offset;

      Facing(int offset) {
         this.offset = offset;
      }
   }
}
