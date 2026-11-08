package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.List;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.world.level.block.Block;

public class Slippy extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Double> friction = this.sgGeneral
      .add(
         new DoubleSetting.Builder().name("friction").description("The base friction level.").range(0.01, 1.1).sliderRange(0.01, 1.1).defaultValue(1.0).build()
      );
   public final Setting<Slippy.ListMode> listMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("list-mode"))
                  .description("The mode to select blocks."))
               .defaultValue(Slippy.ListMode.Blacklist))
            .build()
      );
   public final Setting<List<Block>> ignoredBlocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("ignored-blocks")
            .description("Decide which blocks not to slip on")
            .visible(() -> this.listMode.get() == Slippy.ListMode.Blacklist)
            .build()
      );
   public final Setting<List<Block>> allowedBlocks = this.sgGeneral
      .add(
         new BlockListSetting.Builder()
            .name("allowed-blocks")
            .description("Decide which blocks to slip on")
            .visible(() -> this.listMode.get() == Slippy.ListMode.Whitelist)
            .build()
      );

   public Slippy() {
      super(Categories.Movement, "slippy", "Changes the base friction level of blocks.");
   }

   public enum ListMode {
      Whitelist,
      Blacklist;
   }
}
