package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class Burrow extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Burrow.Block> block = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("block-to-use"))
                  .description("The block to use for Burrow."))
               .defaultValue(Burrow.Block.EChest))
            .build()
      );
   private final Setting<Boolean> instant = this.sgGeneral
      .add(new BoolSetting.Builder().name("instant").description("Jumps with packets rather than vanilla jump.").defaultValue(true).build());
   private final Setting<Boolean> automatic = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("automatic").description("Automatically burrows on activate rather than waiting for jump.").defaultValue(true).build()
      );
   private final Setting<Double> triggerHeight = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("trigger-height")
            .description("How high you have to jump before a rubberband is triggered.")
            .defaultValue(1.12)
            .range(0.01, 1.4)
            .sliderRange(0.01, 1.4)
            .build()
      );
   private final Setting<Double> rubberbandHeight = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rubberband-height")
            .description("How far to attempt to cause rubberband.")
            .defaultValue(12.0)
            .sliderMin(-30.0)
            .sliderMax(30.0)
            .build()
      );
   private final Setting<Double> timer = this.sgGeneral
      .add(new DoubleSetting.Builder().name("timer").description("Timer override.").defaultValue(1.0).min(0.01).sliderRange(0.01, 10.0).build());
   private final Setting<Boolean> onlyInHole = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-in-holes").description("Stops you from burrowing when not in a hole.").defaultValue(false).build());
   private final Setting<Boolean> center = this.sgGeneral
      .add(new BoolSetting.Builder().name("center").description("Centers you to the middle of the block before burrowing.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Faces the block you place server-side.").defaultValue(true).build());
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private boolean shouldBurrow;

   public Burrow() {
      super(Categories.Combat, "burrow", "Attempts to clip you into a block.");
   }

   @Override
   public void onActivate() {
      if (!this.mc.level.getBlockState(this.mc.player.blockPosition()).canBeReplaced()) {
         this.error("Already burrowed, disabling.");
         this.toggle();
      } else if (!PlayerUtils.isInHole(false) && this.onlyInHole.get()) {
         this.error("Not in a hole, disabling.");
         this.toggle();
      } else if (!this.checkHead()) {
         this.error("Not enough headroom to burrow, disabling.");
         this.toggle();
      } else {
         FindItemResult result = this.getItem();
         if (!result.isHotbar() && !result.isOffhand()) {
            this.error("No burrow block found, disabling.");
            this.toggle();
         } else {
            this.blockPos.set(this.mc.player.blockPosition());
            Modules.get().get(Timer.class).setOverride(this.timer.get());
            this.shouldBurrow = false;
            if (this.automatic.get()) {
               if (this.instant.get()) {
                  this.shouldBurrow = true;
               } else {
                  this.mc.player.jumpFromGround();
               }
            } else {
               this.info("Waiting for manual jump.");
            }
         }
      }
   }

   @Override
   public void onDeactivate() {
      Modules.get().get(Timer.class).setOverride(1.0);
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (!this.instant.get()) {
         this.shouldBurrow = this.mc.player.getY() > this.blockPos.getY() + this.triggerHeight.get();
      }

      if (!this.shouldBurrow && this.instant.get()) {
         this.blockPos.set(this.mc.player.blockPosition());
      }

      if (this.shouldBurrow) {
         if (this.rotate.get()) {
            Rotations.rotate(Rotations.getYaw(this.mc.player.blockPosition()), Rotations.getPitch(this.mc.player.blockPosition()), 50, this::burrow);
         } else {
            this.burrow();
         }

         this.toggle();
      }
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      if (this.instant.get() && !this.shouldBurrow) {
         if (event.action == KeyAction.Press && this.mc.options.keyJump.matches(event.input)) {
            this.shouldBurrow = true;
         }

         this.blockPos.set(this.mc.player.blockPosition());
      }
   }

   private void burrow() {
      if (this.center.get()) {
         PlayerUtils.centerPlayer();
      }

      if (this.instant.get()) {
         this.mc
            .player
            .connection
            .send(new Pos(this.mc.player.getX(), this.mc.player.getY() + 0.4, this.mc.player.getZ(), false, this.mc.player.horizontalCollision));
         this.mc
            .player
            .connection
            .send(new Pos(this.mc.player.getX(), this.mc.player.getY() + 0.75, this.mc.player.getZ(), false, this.mc.player.horizontalCollision));
         this.mc
            .player
            .connection
            .send(new Pos(this.mc.player.getX(), this.mc.player.getY() + 1.01, this.mc.player.getZ(), false, this.mc.player.horizontalCollision));
         this.mc
            .player
            .connection
            .send(new Pos(this.mc.player.getX(), this.mc.player.getY() + 1.15, this.mc.player.getZ(), false, this.mc.player.horizontalCollision));
      }

      FindItemResult block = this.getItem();
      if (this.mc.player.getInventory().getItem(block.slot()).getItem() instanceof BlockItem) {
         InvUtils.swap(block.slot(), true);
         this.mc
            .gameMode
            .useItemOn(this.mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Utils.vec3(this.blockPos), Direction.UP, this.blockPos, false));
         this.mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
         InvUtils.swapBack();
         if (this.instant.get()) {
            this.mc
               .player
               .connection
               .send(
                  new Pos(
                     this.mc.player.getX(),
                     this.mc.player.getY() + this.rubberbandHeight.get(),
                     this.mc.player.getZ(),
                     false,
                     this.mc.player.horizontalCollision
                  )
               );
         } else {
            this.mc.player.absSnapTo(this.mc.player.getX(), this.mc.player.getY() + this.rubberbandHeight.get(), this.mc.player.getZ());
         }
      }
   }

   private FindItemResult getItem() {
      return switch ((Burrow.Block)this.block.get()) {
         case EChest -> InvUtils.findInHotbar(Items.ENDER_CHEST);
         default -> InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN);
         case Anvil -> InvUtils.findInHotbar(itemStack -> net.minecraft.world.level.block.Block.byItem(itemStack.getItem()) instanceof AnvilBlock);
         case Held -> new FindItemResult(this.mc.player.getInventory().getSelectedSlot(), this.mc.player.getMainHandItem().getCount());
      };
   }

   private boolean checkHead() {
      BlockState blockState1 = this.mc
         .level
         .getBlockState(this.blockPos.set(this.mc.player.getX() + 0.3, this.mc.player.getY() + 2.3, this.mc.player.getZ() + 0.3));
      BlockState blockState2 = this.mc
         .level
         .getBlockState(this.blockPos.set(this.mc.player.getX() + 0.3, this.mc.player.getY() + 2.3, this.mc.player.getZ() - 0.3));
      BlockState blockState3 = this.mc
         .level
         .getBlockState(this.blockPos.set(this.mc.player.getX() - 0.3, this.mc.player.getY() + 2.3, this.mc.player.getZ() - 0.3));
      BlockState blockState4 = this.mc
         .level
         .getBlockState(this.blockPos.set(this.mc.player.getX() - 0.3, this.mc.player.getY() + 2.3, this.mc.player.getZ() + 0.3));
      boolean air1 = blockState1.canBeReplaced();
      boolean air2 = blockState2.canBeReplaced();
      boolean air3 = blockState3.canBeReplaced();
      boolean air4 = blockState4.canBeReplaced();
      return air1 && air2 && air3 && air4;
   }

   public enum Block {
      EChest,
      Obsidian,
      Anvil,
      Held;
   }
}
