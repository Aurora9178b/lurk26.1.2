package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

public class AntiAnchor extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Makes you rotate when placing.").defaultValue(true).build());
   private final Setting<Boolean> swing = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing").description("Swings your hand when placing.").defaultValue(true).build());

   public AntiAnchor() {
      super(Categories.Combat, "anti-anchor", "Automatically prevents Anchor Aura by placing a slab on your head.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.level.getBlockState(this.mc.player.blockPosition().above(2)).getBlock() == Blocks.RESPAWN_ANCHOR
         && this.mc.level.getBlockState(this.mc.player.blockPosition().above()).getBlock() == Blocks.AIR) {
         BlockUtils.place(
            this.mc.player.blockPosition().offset(0, 1, 0),
            InvUtils.findInHotbar(itemStack -> Block.byItem(itemStack.getItem()) instanceof SlabBlock),
            this.rotate.get(),
            15,
            this.swing.get(),
            false,
            true
         );
      }
   }
}
