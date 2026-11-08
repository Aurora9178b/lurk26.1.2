package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;

public class AntiBed extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> placeStringTop = this.sgGeneral
      .add(new BoolSetting.Builder().name("place-string-top").description("Places string above you.").defaultValue(false).build());
   private final Setting<Boolean> placeStringMiddle = this.sgGeneral
      .add(new BoolSetting.Builder().name("place-string-middle").description("Places string in your upper hitbox.").defaultValue(true).build());
   private final Setting<Boolean> placeStringBottom = this.sgGeneral
      .add(new BoolSetting.Builder().name("place-string-bottom").description("Places string at your feet.").defaultValue(false).build());
   private final Setting<Boolean> onlyInHole = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-in-hole").description("Only functions when you are standing in a hole.").defaultValue(true).build());
   private boolean breaking;

   public AntiBed() {
      super(Categories.Combat, "anti-bed", "Places string to prevent beds being placed on you.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (!this.onlyInHole.get() || PlayerUtils.isInHole(true)) {
         BlockPos head = this.mc.player.blockPosition().above();
         if (this.mc.level.getBlockState(head).getBlock() instanceof BedBlock && !this.breaking) {
            Rotations.rotate(Rotations.getYaw(head), Rotations.getPitch(head), 50, () -> this.sendMinePackets(head));
            this.breaking = true;
         } else if (this.breaking) {
            Rotations.rotate(Rotations.getYaw(head), Rotations.getPitch(head), 50, () -> this.sendStopPackets(head));
            this.breaking = false;
         }

         if (this.placeStringTop.get()) {
            this.place(this.mc.player.blockPosition().above(2));
         }

         if (this.placeStringMiddle.get()) {
            this.place(this.mc.player.blockPosition().above(1));
         }

         if (this.placeStringBottom.get()) {
            this.place(this.mc.player.blockPosition());
         }
      }
   }

   private void place(BlockPos blockPos) {
      if (this.mc.level.getBlockState(blockPos).getBlock().asItem() != Items.STRING) {
         BlockUtils.place(blockPos, InvUtils.findInHotbar(Items.STRING), 50, false);
      }
   }

   private void sendMinePackets(BlockPos blockPos) {
      this.mc
         .gameMode
         .startPrediction(this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, Direction.UP, sequence));
      this.mc
         .gameMode
         .startPrediction(this.mc.level, sequence -> new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP, sequence));
   }

   private void sendStopPackets(BlockPos blockPos) {
      this.mc.getConnection().send(new ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, blockPos, Direction.UP));
      this.mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
   }
}
