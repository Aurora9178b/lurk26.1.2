package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.leaves.LeavesBlockUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesCombatUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesDamageUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesInventoryUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesRotation;
import meteordevelopment.meteorclient.utils.leaves.LeavesTimer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.phys.Vec3;

public class PistonCrystal extends Module {
   public static PistonCrystal INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("TargetRange").description("Target range.").defaultValue(6.0).sliderRange(1.0, 6.0).build());
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Action range.").defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay-ms").description("Action delay.").defaultValue(50).sliderRange(0, 500).build());
   private final Setting<Integer> breakDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("breakDelay-ms").description("Break delay.").defaultValue(300).sliderRange(0, 500).build());
   private final Setting<Double> minDamage = this.sgGeneral
      .add(new DoubleSetting.Builder().name("MinDamage").description("Minimum target damage.").defaultValue(4.0).sliderRange(1.0, 36.0).build());
   private final Setting<Double> maxSelfDmg = this.sgGeneral
      .add(new DoubleSetting.Builder().name("MaxSelfDmg").description("Maximum self damage.").defaultValue(12.0).sliderRange(1.0, 36.0).build());
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(new BoolSetting.Builder().name("UsingPause").description("Pause while using items.").defaultValue(true).build());
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(new BoolSetting.Builder().name("OnlyMain").description("Only pause for main hand use.").defaultValue(true).visible(this.usingPause::get).build());
   private final Setting<Boolean> preferCrystal = this.sgGeneral
      .add(new BoolSetting.Builder().name("PreferCrystal").description("Prefer AutoCrystal.").defaultValue(true).build());
   private final Setting<Boolean> attack = this.sgGeneral
      .add(new BoolSetting.Builder().name("AttackForPlace").description("Attacks blocking crystals.").defaultValue(true).build());
   private final Setting<Boolean> noSuicide = this.sgGeneral
      .add(new BoolSetting.Builder().name("NoSuicide").description("Prevents lethal self damage.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates to actions.").defaultValue(true).build());
   private final Setting<Boolean> inventory = this.sgGeneral
      .add(new BoolSetting.Builder().name("InventorySwap").description("Uses inventory swap.").defaultValue(true).build());
   private final Setting<Boolean> mine = this.sgGeneral
      .add(new BoolSetting.Builder().name("Mine").description("Mines stuck pistons.").defaultValue(true).build());
   private final Setting<Boolean> yawDeceive = this.sgGeneral
      .add(new BoolSetting.Builder().name("YawDeceive").description("Limits piston facing to player yaw.").defaultValue(true).build());
   private final Setting<PistonCrystal.RedstoneMode> redStoneMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("redStone")).description("Redstone mode."))
               .defaultValue(PistonCrystal.RedstoneMode.Block))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Render positions.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("Render shape mode."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> crystalColor = this.sgRender
      .add(new ColorSetting.Builder().name("Crystal").description("Crystal color.").defaultValue(new SettingColor(255, 0, 0, 80)).build());
   private final Setting<SettingColor> pistonColor = this.sgRender
      .add(new ColorSetting.Builder().name("Piston").description("Piston color.").defaultValue(new SettingColor(255, 255, 255, 80)).build());
   private final Setting<SettingColor> redstoneColor = this.sgRender
      .add(new ColorSetting.Builder().name("RedStone").description("Redstone color.").defaultValue(new SettingColor(255, 100, 0, 80)).build());
   private long lastAction;
   private BlockPos crystalPos;
   private BlockPos pistonPos;
   private BlockPos redstonePos;
   private BlockPos lastPiston;
   private Direction face;
   private final LeavesTimer breakTimer = new LeavesTimer();
   private Player target;

   public PistonCrystal() {
      super(Categories.Combat, "PistonCrystal", "Leaves-style piston crystal.");
      INSTANCE = this;
   }

   @Override
   public void onActivate() {
      this.breakTimer.setMs(99999999L);
   }

   @Override
   public String getInfoString() {
      return this.target == null ? null : "[" + this.target.getName().getString() + "]";
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (System.currentTimeMillis() - this.lastAction >= this.delay.get().intValue()) {
            AutoCrystal crystal = AutoCrystal.INSTANCE;
            if (!this.preferCrystal.get() || crystal == null || crystal.crystalPos == null) {
               this.target = LeavesCombatUtil.getClosestEnemy(this.targetRange.get());
               int redstone = this.findRedstone();
               int crystalSlot = this.inventory.get()
                  ? LeavesInventoryUtil.findItemInventorySlot(Items.END_CRYSTAL)
                  : LeavesInventoryUtil.findItem(Items.END_CRYSTAL);
               int piston = this.inventory.get()
                  ? LeavesInventoryUtil.findClassInventory(PistonBaseBlock.class)
                  : LeavesInventoryUtil.findClass(PistonBaseBlock.class);
               if (!this.shouldPause()) {
                  if (redstone == -1 || crystalSlot == -1 || piston == -1) {
                     this.pistonPos = null;
                     this.crystalPos = null;
                     this.redstonePos = null;
                  } else if (this.target != null) {
                     if (this.crystalPos != null
                        && this.redstonePos != null
                        && this.pistonPos != null
                        && (
                           !LeavesBlockUtil.canPlaceCrystal(this.crystalPos)
                              || !LeavesBlockUtil.canPlace(this.redstonePos)
                              || !LeavesBlockUtil.canPlace(this.pistonPos)
                        )) {
                        this.pistonPos = null;
                        this.crystalPos = null;
                        this.redstonePos = null;
                     }

                     if (this.pistonPos == null && this.crystalPos == null && this.redstonePos == null) {
                        this.doPistonCrystal(this.target);
                     }

                     if (this.lastPiston != null
                        && this.face != null
                        && LeavesBlockUtil.getBlock(this.lastPiston.relative(this.face.getOpposite())) instanceof PistonHeadBlock
                        && LeavesBlockUtil.getBlock(this.lastPiston) instanceof PistonBaseBlock
                        && this.mine.get()) {
                        Direction side = LeavesBlockUtil.getClickSide(this.lastPiston);
                        this.mc.gameMode.startDestroyBlock(this.lastPiston, side);
                        this.lastPiston = null;
                     }

                     if (this.pistonPos != null && LeavesBlockUtil.canPlace(this.pistonPos)) {
                        this.place(Items.PISTON, piston, this.pistonPos, this.face);
                        this.lastPiston = this.pistonPos;
                     }

                     if (this.redstonePos != null && LeavesBlockUtil.canPlace(this.redstonePos)) {
                        if (this.redStoneMode.get() == PistonCrystal.RedstoneMode.Torch) {
                           this.placeTorch(this.redstonePos, redstone);
                        } else {
                           this.place(Items.REDSTONE_BLOCK, redstone, this.redstonePos, null);
                        }
                     }

                     if (this.crystalPos != null && LeavesBlockUtil.canPlaceCrystal(this.crystalPos)) {
                        this.placeCrystal(this.crystalPos, crystalSlot);
                        this.lastAction = System.currentTimeMillis();
                     } else {
                        if (this.breakTimer.passedMs(this.breakDelay.get().intValue())) {
                           if (LeavesBlockUtil.hasCrystalPlace(this.target.blockPosition().above())) {
                              LeavesCombatUtil.attackCrystal(this.target.blockPosition().above(), true, false);
                              this.resetPositions();
                              this.breakTimer.reset();
                           }

                           if (LeavesBlockUtil.hasCrystalPlace(this.target.blockPosition().above(2))) {
                              LeavesCombatUtil.attackCrystal(this.target.blockPosition().above(2), true, false);
                              this.resetPositions();
                              this.breakTimer.reset();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private int findRedstone() {
      if (this.redStoneMode.get() == PistonCrystal.RedstoneMode.Torch) {
         return this.inventory.get() ? LeavesInventoryUtil.findItemInventorySlot(Items.REDSTONE_TORCH) : LeavesInventoryUtil.findItem(Items.REDSTONE_TORCH);
      } else {
         return this.inventory.get() ? LeavesInventoryUtil.findItemInventorySlot(Items.REDSTONE_BLOCK) : LeavesInventoryUtil.findItem(Items.REDSTONE_BLOCK);
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.render.get()) {
         if (this.crystalPos != null) {
            event.renderer.box(this.crystalPos, this.crystalColor.get(), this.crystalColor.get(), this.shapeMode.get(), 0);
         }

         if (this.pistonPos != null) {
            event.renderer.box(this.pistonPos, this.pistonColor.get(), this.pistonColor.get(), this.shapeMode.get(), 0);
         }

         if (this.redstonePos != null) {
            event.renderer.box(this.redstonePos, this.redstoneColor.get(), this.redstoneColor.get(), this.shapeMode.get(), 0);
         }
      }
   }

   private void doPistonCrystal(Player target) {
      BlockPos base = target.blockPosition();
      this.tryLayer(base, 1);
      if (this.crystalPos == null) {
         this.tryLayer(base, 2);
      }
   }

   private void tryLayer(BlockPos base, int up) {
      Vec3 vec = new Vec3(base.above(up).getX() + 0.5, base.above(up).getY(), base.above(up).getZ() + 0.5);
      float damage = LeavesDamageUtil.calculateDamage(vec, this.target);
      float self = LeavesDamageUtil.calculateDamage(vec, this.mc.player);
      if (!(damage <= this.minDamage.get()) && !(self > this.maxSelfDmg.get())) {
         if (!(self > this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount()) || !this.noSuicide.get()) {
            for (Direction dir : Direction.values()) {
               if (dir != Direction.UP && dir != Direction.DOWN && (!this.yawDeceive.get() || dir == this.mc.player.getDirection())) {
                  BlockPos tempCrystal = base.relative(dir).above(up);
                  if (!LeavesBlockUtil.canPlaceCrystal(tempCrystal)) {
                     if (this.attack.get() && LeavesBlockUtil.hasCrystal(tempCrystal)) {
                        LeavesCombatUtil.attackCrystal(tempCrystal.above(), this.rotate.get(), false);
                        return;
                     }
                  } else if (!(this.mc.player.getEyePosition().distanceTo(tempCrystal.getCenter()) > this.range.get())) {
                     BlockPos tempPiston = tempCrystal.relative(dir);
                     if (!this.canUsePistonPos(tempPiston)) {
                        if (this.placeHelper(tempPiston, dir)) {
                           return;
                        }
                     } else {
                        BlockPos tempRedstone = this.findRedstonePos(tempPiston, dir);
                        if (tempRedstone != null) {
                           this.face = dir;
                           this.crystalPos = tempCrystal;
                           this.pistonPos = tempPiston;
                           this.redstonePos = tempRedstone;
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean canUsePistonPos(BlockPos pos) {
      return this.mc.player.getEyePosition().distanceTo(pos.getCenter()) <= this.range.get()
         && (LeavesBlockUtil.canPlace(pos) || LeavesBlockUtil.getBlock(pos) instanceof PistonBaseBlock);
   }

   private boolean placeHelper(BlockPos pistonPos, Direction dir) {
      for (Direction help : Direction.values()) {
         if (help != dir.getOpposite()) {
            BlockPos helpPos = pistonPos.relative(help);
            if (LeavesBlockUtil.isGrimDirection(helpPos, help.getOpposite())
               && LeavesBlockUtil.canPlace(helpPos)
               && !(this.mc.player.getEyePosition().distanceTo(helpPos.getCenter()) > this.range.get())) {
               int old = this.mc.player.getInventory().getSelectedSlot();
               Direction side = LeavesBlockUtil.getPlaceSide(helpPos, null);
               if (side != null) {
                  this.doSwap(this.findRedstone());
                  LeavesBlockUtil.placeBlock(helpPos, side, this.rotate.get());
                  this.doSwap(this.inventory.get() ? this.findRedstone() : old);
                  return true;
               }
            }
         }
      }

      return false;
   }

   private BlockPos findRedstonePos(BlockPos piston, Direction pushDir) {
      for (Direction dir : Direction.values()) {
         if (dir != pushDir.getOpposite()) {
            BlockPos pos = piston.relative(dir);
            if (this.isRedstoneBlock(pos)) {
               return pos;
            }

            if (LeavesBlockUtil.canPlace(pos) && !(this.mc.player.getEyePosition().distanceTo(pos.getCenter()) > this.range.get())) {
               return pos;
            }
         }
      }

      return null;
   }

   private boolean isRedstoneBlock(BlockPos pos) {
      return this.redStoneMode.get() == PistonCrystal.RedstoneMode.Block
         ? LeavesBlockUtil.getBlock(pos) == Blocks.REDSTONE_BLOCK
         : LeavesBlockUtil.getBlock(pos) instanceof RedstoneTorchBlock || LeavesBlockUtil.getBlock(pos) instanceof RedStoneWireBlock;
   }

   private void place(Item item, int slot, BlockPos pos, Direction dir) {
      Direction side = LeavesBlockUtil.getPlaceSide(pos, var0 -> true);
      if (side != null && slot != -1) {
         int old = this.mc.player.getInventory().getSelectedSlot();
         this.doSwap(slot);
         if (this.rotate.get()) {
            LeavesRotation.snapAt(pos, side);
         }

         if (item == Items.PISTON && this.yawDeceive.get() && this.rotate.get() && dir != null) {
            pistonFacing(dir);
         }

         LeavesBlockUtil.placeBlock(pos, side, false);
         if (this.rotate.get()) {
            LeavesRotation.snapBack();
         }

         this.doSwap(this.inventory.get() ? slot : old);
      }
   }

   public static void pistonFacing(Direction direction) {
      if (direction == Direction.EAST) {
         LeavesRotation.snapAt(-90.0F, 5.0F);
      } else if (direction == Direction.WEST) {
         LeavesRotation.snapAt(90.0F, 5.0F);
      } else if (direction == Direction.NORTH) {
         LeavesRotation.snapAt(180.0F, 5.0F);
      } else if (direction == Direction.SOUTH) {
         LeavesRotation.snapAt(0.0F, 5.0F);
      }
   }

   private void placeCrystal(BlockPos pos, int slot) {
      BlockPos base = pos.below();
      Direction side = LeavesBlockUtil.getClickSide(base);
      if (side != null && slot != -1) {
         int old = this.mc.player.getInventory().getSelectedSlot();
         this.doSwap(slot);
         LeavesBlockUtil.clickBlock(base, side, this.rotate.get());
         this.doSwap(this.inventory.get() ? slot : old);
      }
   }

   private void placeTorch(BlockPos pos, int slot) {
      if (LeavesBlockUtil.canPlace(pos)) {
         ArrayList<Direction> sides = LeavesBlockUtil.getPlaceSides(pos, null);
         if (!sides.isEmpty() && slot != -1) {
            for (Direction side : sides) {
               if (!(LeavesBlockUtil.getBlock(pos.relative(side)) instanceof PistonBaseBlock) && side != Direction.UP) {
                  int old = this.mc.player.getInventory().getSelectedSlot();
                  this.doSwap(slot);
                  LeavesBlockUtil.placeBlock(pos, side, this.rotate.get());
                  this.doSwap(this.inventory.get() ? slot : old);
                  return;
               }
            }
         }
      }
   }

   private boolean shouldPause() {
      return !this.usingPause.get() || this.checkPause(this.onlyMain.get());
   }

   public boolean checkPause(boolean onlyMain) {
      return (this.mc.options.keyUse.isDown() || this.mc.player.isUsingItem()) && (!onlyMain || this.mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND);
   }

   private void doSwap(int slot) {
      if (slot != -1) {
         if (!this.inventory.get()) {
            LeavesInventoryUtil.switchToSlot(slot);
         } else {
            LeavesInventoryUtil.inventorySwap(slot, this.mc.player.getInventory().getSelectedSlot());
         }
      }
   }

   private void resetPositions() {
      this.lastAction = System.currentTimeMillis();
      this.pistonPos = null;
      this.crystalPos = null;
      this.redstonePos = null;
   }

   public enum RedstoneMode {
      Torch,
      Block;
   }
}
