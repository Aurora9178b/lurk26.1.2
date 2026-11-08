package meteordevelopment.meteorclient.systems.modules.combat;

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
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.leaves.LeavesBlockUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesCombatUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesDamageUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesInventoryUtil;
import meteordevelopment.meteorclient.utils.leaves.LeavesTimer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoAnchor extends Module {
   public static AutoAnchor INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("TargetRange").description("Target range.").defaultValue(12.0).sliderRange(1.0, 20.0).build());
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Action range.").defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay-ms").description("Action delay.").defaultValue(50).sliderRange(0, 500).build());
   private final Setting<Double> minDamage = this.sgGeneral
      .add(new DoubleSetting.Builder().name("MinDamage").description("Minimum target damage.").defaultValue(4.0).sliderRange(1.0, 36.0).build());
   private final Setting<Double> maxSelfDmg = this.sgGeneral
      .add(new DoubleSetting.Builder().name("MaxSelfDmg").description("Maximum self damage.").defaultValue(12.0).sliderRange(1.0, 36.0).build());
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(new BoolSetting.Builder().name("UsingPause").description("Pause while using items.").defaultValue(true).build());
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(new BoolSetting.Builder().name("OnlyMain").description("Only pause for main hand use.").defaultValue(true).visible(this.usingPause::get).build());
   private final Setting<Boolean> preferHead = this.sgGeneral
      .add(new BoolSetting.Builder().name("PreferHead").description("Prefer placing at head.").defaultValue(true).build());
   private final Setting<Boolean> placeHelper = this.sgGeneral
      .add(new BoolSetting.Builder().name("PlaceHelper").description("Places helper blocks.").defaultValue(true).build());
   private final Setting<Boolean> noSuicide = this.sgGeneral
      .add(new BoolSetting.Builder().name("NoSuicide").description("Prevents lethal self damage.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Rotates to actions.").defaultValue(true).build());
   private final Setting<Boolean> inventory = this.sgGeneral
      .add(new BoolSetting.Builder().name("InventorySwap").description("Uses inventory swap.").defaultValue(true).build());
   private final Setting<Boolean> renderDmg = this.sgRender
      .add(new BoolSetting.Builder().name("RenderDmg").description("Render damage text.").defaultValue(false).build());
   private final Setting<SettingColor> dmgColor = this.sgRender
      .add(new ColorSetting.Builder().name("DamageColor").description("Damage text color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("Shape Mode")).description("Render shape mode."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(new ColorSetting.Builder().name("Line").description("Line color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(new ColorSetting.Builder().name("Side").description("Side color.").defaultValue(new SettingColor(255, 255, 255, 10)).build());
   private final Setting<Double> renderSpeed = this.sgRender
      .add(new DoubleSetting.Builder().name("RenderSpeed").description("Render animation speed.").defaultValue(0.1).sliderRange(0.0, 1.0).build());
   public Player target;
   public BlockPos currentPos;
   public int dmg;
   public final LeavesTimer placeTimer = new LeavesTimer();
   private AutoAnchor.PosEntry renderPosEntry = new AutoAnchor.PosEntry();
   private boolean holdingRotation;

   public AutoAnchor() {
      super(Categories.Combat, "AutoAnchor", "Leaves-style auto anchor.");
      INSTANCE = this;
   }

   @Override
   public void onDeactivate() {
      this.currentPos = null;
      this.releaseRotation();
   }

   @Override
   public void onActivate() {
      this.placeTimer.setMs(9999999L);
      this.renderPosEntry = new AutoAnchor.PosEntry();
   }

   @Override
   public String getInfoString() {
      return this.target == null ? null : "[" + this.target.getName().getString() + "]";
   }

   @EventHandler
   public void onRender3D(Render3DEvent event) {
      if (this.currentPos != null) {
         if (this.renderDmg.get()) {
         }

         if (this.renderPosEntry.x == 0.0 && this.renderPosEntry.y == 0.0 && this.renderPosEntry.z == 0.0) {
            this.renderPosEntry.x = this.mc.player.getX();
            this.renderPosEntry.y = this.mc.player.getY();
            this.renderPosEntry.z = this.mc.player.getZ();
         }

         this.renderPosEntry.x = this.renderPosEntry.x + (this.currentPos.getX() - this.renderPosEntry.x) * this.renderSpeed.get();
         this.renderPosEntry.y = this.renderPosEntry.y + (this.currentPos.getY() - this.renderPosEntry.y) * this.renderSpeed.get();
         this.renderPosEntry.z = this.renderPosEntry.z + (this.currentPos.getZ() - this.renderPosEntry.z) * this.renderSpeed.get();
         AABB renderBox = new AABB(
            this.renderPosEntry.x,
            this.renderPosEntry.y,
            this.renderPosEntry.z,
            this.renderPosEntry.x + 1.0,
            this.renderPosEntry.y + 1.0,
            this.renderPosEntry.z + 1.0
         );
         event.renderer.box(renderBox, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      } else {
         this.renderPosEntry = new AutoAnchor.PosEntry();
      }
   }

   @EventHandler
   public void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         this.target = LeavesCombatUtil.getClosestEnemy(this.targetRange.get());
         if (this.target == null) {
            this.currentPos = null;
            this.releaseRotation();
         } else if (this.shouldPause()) {
            this.holdAutoRotation();
         } else {
            int anchor = this.inventory.get()
               ? LeavesInventoryUtil.findItemInventorySlot(Items.RESPAWN_ANCHOR)
               : LeavesInventoryUtil.findItem(Items.RESPAWN_ANCHOR);
            int glow = this.inventory.get() ? LeavesInventoryUtil.findItemInventorySlot(Items.GLOWSTONE) : LeavesInventoryUtil.findItem(Items.GLOWSTONE);
            if (anchor != -1 && glow != -1) {
               this.updatePos(this.target);
               this.holdAutoRotation();
               if (this.placeTimer.passedMs(this.delay.get().intValue())) {
                  this.doAnchor(anchor, glow);
               }
            } else {
               this.currentPos = null;
               this.releaseRotation();
            }
         }
      }
   }

   private void doAnchor(int anchor, int glow) {
      if (this.currentPos != null && this.mc.player != null) {
         if (!this.noSuicide.get()
            || !(LeavesDamageUtil.anchorDamage(this.mc.player, this.currentPos.getCenter()) > this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount())
            )
          {
            if (this.mc.player.getEyePosition().distanceTo(this.currentPos.getCenter()) > this.range.get()
               || !LeavesBlockUtil.canPlace(this.currentPos) && LeavesBlockUtil.getBlock(this.currentPos) != Blocks.RESPAWN_ANCHOR) {
               this.updatePos(this.target);
            }

            if (LeavesBlockUtil.getBlock(this.currentPos) != Blocks.RESPAWN_ANCHOR) {
               Direction side = LeavesBlockUtil.getPlaceSide(this.currentPos, null);
               if (side == null) {
                  return;
               }

               int old = this.mc.player.getInventory().getSelectedSlot();
               this.doSwap(anchor);
               this.holdPlaceRotation(this.currentPos, side);
               LeavesBlockUtil.placeBlock(this.currentPos, side, false);
               this.doSwap(this.inventory.get() ? anchor : old);
               this.placeTimer.reset();
            } else {
               int old = this.mc.player.getInventory().getSelectedSlot();
               Direction side = LeavesBlockUtil.getClickSide(this.currentPos);
               if (this.mc.level.getBlockState(this.currentPos).getValue(RespawnAnchorBlock.CHARGE) > 0) {
                  this.holdClickRotation(this.currentPos, side);
                  LeavesBlockUtil.clickBlock(this.currentPos, side, false);
                  this.placeTimer.reset();
                  return;
               }

               this.doSwap(glow);
               this.holdClickRotation(this.currentPos, side);
               LeavesBlockUtil.clickBlock(this.currentPos, side, false);
               this.mc.player.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 5.0F, 1.0F);
               this.doSwap(this.inventory.get() ? glow : old);
               this.placeTimer.reset();
            }
         }
      }
   }

   private void updatePos(Player target) {
      if (this.preferHead.get()) {
         BlockPos head = target.blockPosition().above(2);
         if (LeavesDamageUtil.anchorDamage(target, head.getCenter()) > this.minDamage.get()) {
            if (LeavesBlockUtil.canPlace(head) || LeavesBlockUtil.getBlock(head) == Blocks.RESPAWN_ANCHOR) {
               this.currentPos = head;
               return;
            }

            if (this.placeHelper.get()) {
               for (Direction dir : Plane.HORIZONTAL) {
                  BlockPos temp = head.relative(dir);
                  if (LeavesBlockUtil.canPlace(temp) && LeavesBlockUtil.isGrimDirection(temp.relative(dir), dir.getOpposite())) {
                     this.placeHelper(temp);
                     return;
                  }
               }
            }
         }
      }

      float bestDmg = Float.MIN_VALUE;
      BlockPos bestPos = null;

      for (BlockPos pos : LeavesBlockUtil.getSphere(this.range.get())) {
         if (LeavesBlockUtil.canPlace(pos) || LeavesBlockUtil.getBlock(pos) == Blocks.RESPAWN_ANCHOR) {
            float targetDmg = LeavesDamageUtil.anchorDamage(target, pos.getCenter());
            float selfDmg = LeavesDamageUtil.anchorDamage(this.mc.player, pos.getCenter());
            if (targetDmg > bestDmg && targetDmg > this.minDamage.get() && selfDmg < this.maxSelfDmg.get()) {
               bestDmg = targetDmg;
               bestPos = pos;
            }
         }
      }

      if (bestPos != null) {
         this.currentPos = bestPos;
         this.dmg = (int)bestDmg;
      }
   }

   private void placeHelper(BlockPos pos) {
      Direction side = LeavesBlockUtil.getPlaceSide(pos, null);
      if (side != null) {
         int old = this.mc.player.getInventory().getSelectedSlot();
         int anchor = this.inventory.get()
            ? LeavesInventoryUtil.findItemInventorySlot(Items.RESPAWN_ANCHOR)
            : LeavesInventoryUtil.findItem(Items.RESPAWN_ANCHOR);
         this.doSwap(anchor);
         this.holdPlaceRotation(pos, side);
         LeavesBlockUtil.placeBlock(pos, side, false);
         this.doSwap(this.inventory.get() ? anchor : old);
      }
   }

   private boolean shouldPause() {
      AutoCrystal crystal = AutoCrystal.INSTANCE;
      return crystal != null && crystal.isActive() && crystal.preferMode.get() == AutoCrystal.PreferMode.PreferCrystal
         ? crystal.crystalPos != null
         : !this.usingPause.get() || this.checkPause(this.onlyMain.get());
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

   private void holdAutoRotation() {
      if (!this.rotate.get()) {
         this.releaseRotation();
      } else {
         if (this.currentPos != null) {
            if (LeavesBlockUtil.getBlock(this.currentPos) == Blocks.RESPAWN_ANCHOR) {
               Direction side = LeavesBlockUtil.getClickSide(this.currentPos);
               if (side != null) {
                  this.holdClickRotation(this.currentPos, side);
                  return;
               }
            } else {
               Direction side = LeavesBlockUtil.getPlaceSide(this.currentPos, null);
               if (side != null) {
                  this.holdPlaceRotation(this.currentPos, side);
                  return;
               }
            }
         }

         this.releaseRotation();
      }
   }

   private void holdPlaceRotation(BlockPos pos, Direction side) {
      if (side != null) {
         this.holdClickRotation(pos.relative(side), side.getOpposite());
      }
   }

   private void holdClickRotation(BlockPos pos, Direction side) {
      if (pos != null && side != null) {
         this.holdRotation(pos.getCenter().add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5));
      }
   }

   private void holdRotation(Vec3 vec) {
      if (this.rotate.get() && vec != null) {
         RotationManager.INSTANCE.setRotations(RotationUtil.calculate(vec), 100.0, Priority.High);
         this.holdingRotation = true;
      } else {
         this.releaseRotation();
      }
   }

   private void releaseRotation() {
      if (this.holdingRotation) {
         RotationManager.INSTANCE.reset();
         this.holdingRotation = false;
      }
   }

   private static class PosEntry {
      double x;
      double y;
      double z;
   }
}
