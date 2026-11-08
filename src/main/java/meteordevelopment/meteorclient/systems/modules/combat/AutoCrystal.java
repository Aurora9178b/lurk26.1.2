package meteordevelopment.meteorclient.systems.modules.combat;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AutoCrystal extends Module {
   public static AutoCrystal INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("TargetRange").description("Target range.").defaultValue(12.0).sliderRange(1.0, 20.0).build());
   private final Setting<Double> placeRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("PlaceRange").description("Place range.").defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Double> breakRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("BreakRange").description("Break range.").defaultValue(4.5).sliderRange(1.0, 6.0).build());
   private final Setting<Integer> placeDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("PlaceDelay").description("Place delay.").defaultValue(50).sliderRange(0, 500).build());
   private final Setting<Integer> breakDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("BreakDelay").description("Break delay.").defaultValue(50).sliderRange(0, 500).build());
   private final Setting<AutoCrystal.CalcMode> calcMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("CalcMode")).description("Damage calculation mode."))
               .defaultValue(AutoCrystal.CalcMode.AlienV4))
            .build()
      );
   private final Setting<Double> minDamage = this.sgGeneral
      .add(new DoubleSetting.Builder().name("MinDamage").description("Minimum target damage.").defaultValue(4.0).sliderRange(1.0, 36.0).build());
   private final Setting<Double> maxSelfDmg = this.sgGeneral
      .add(new DoubleSetting.Builder().name("MaxSelfDmg").description("Maximum self damage.").defaultValue(12.0).sliderRange(1.0, 36.0).build());
   private final Setting<Integer> predict = this.sgGeneral
      .add(new IntSetting.Builder().name("Predict").description("Movement prediction ticks.").defaultValue(4).sliderRange(0, 12).build());
   public final Setting<AutoCrystal.PreferMode> preferMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("PreferMode")).description("Priority mode."))
               .defaultValue(AutoCrystal.PreferMode.PreferAnchor))
            .build()
      );
   private final Setting<Boolean> autoBase = this.sgGeneral
      .add(new BoolSetting.Builder().name("AutoBase").description("Places obsidian bases.").defaultValue(true).build());
   private final Setting<Integer> baseDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("BaseDelay").description("Base place delay.").defaultValue(500).sliderRange(0, 1000).build());
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(new BoolSetting.Builder().name("UsingPause").description("Pause while using items.").defaultValue(true).build());
   private final Setting<Boolean> onlyMain = this.sgGeneral
      .add(new BoolSetting.Builder().name("OnlyMain").description("Only pause for main hand use.").defaultValue(true).visible(this.usingPause::get).build());
   private final Setting<Boolean> noSuicide = this.sgGeneral
      .add(new BoolSetting.Builder().name("NoSuicide").description("Prevents lethal self damage.").defaultValue(true).build());
   private final Setting<Boolean> inventory = this.sgGeneral
      .add(new BoolSetting.Builder().name("InventorySwap").description("Uses inventory swap.").defaultValue(true).build());
   private final Setting<Boolean> placeAfterBreak = this.sgGeneral
      .add(new BoolSetting.Builder().name("PlaceAfterBreak").description("Places after breaking.").defaultValue(true).build());
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("Rotate").description("Keeps rotating to the current crystal action.").defaultValue(true).build());
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("Render").description("Render placement.").defaultValue(true).build());
   private final Setting<Boolean> renderDmg = this.sgRender
      .add(new BoolSetting.Builder().name("RenderDmg").description("Render damage text.").defaultValue(false).build());
   private final Setting<SettingColor> dmgColor = this.sgRender
      .add(new ColorSetting.Builder().name("DamageColor").description("Damage text color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).description("Render shape mode."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(new ColorSetting.Builder().name("side-color").description("Side color.").defaultValue(new SettingColor(255, 255, 255, 50)).build());
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(new ColorSetting.Builder().name("line-color").description("Line color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private final Setting<Double> renderSpeed = this.sgRender
      .add(new DoubleSetting.Builder().name("RenderSpeed").description("Render animation speed.").defaultValue(0.05).sliderRange(0.0, 1.0).build());
   private final Setting<Double> renderH = this.sgRender
      .add(new DoubleSetting.Builder().name("RenderHeight").description("Render height.").defaultValue(0.1).sliderRange(0.0, 1.0).build());
   private final LeavesTimer placeTimer = new LeavesTimer();
   private final LeavesTimer breakTimer = new LeavesTimer();
   private final LeavesTimer baseTimer = new LeavesTimer();
   private int dmg;
   private Player target;
   public BlockPos crystalPos;
   public BlockPos lastBestPos;
   private final AutoCrystal.RenderPos renderPos = new AutoCrystal.RenderPos();
   private boolean holdingRotation;

   public AutoCrystal() {
      super(Categories.Combat, "AutoCrystal", "Leaves-style auto crystal.");
      INSTANCE = this;
   }

   @Override
   public String getInfoString() {
      return this.target == null ? null : "[" + this.target.getName().getString() + "]";
   }

   @Override
   public void onDeactivate() {
      this.crystalPos = null;
      this.lastBestPos = null;
      this.releaseRotation();
   }

   @Override
   public void onActivate() {
      this.breakTimer.setMs(9999999L);
      this.placeTimer.setMs(9999999L);
      this.baseTimer.setMs(9999999L);
      this.renderPos.x = 0.0;
      this.renderPos.y = 0.0;
      this.renderPos.z = 0.0;
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.render.get()) {
         if (this.crystalPos == null) {
            this.renderPos.x = 0.0;
            this.renderPos.y = 0.0;
            this.renderPos.z = 0.0;
         } else {
            if (this.renderPos.x == 0.0 && this.renderPos.y == 0.0 && this.renderPos.z == 0.0) {
               this.renderPos.x = this.mc.player.getX();
               this.renderPos.y = this.mc.player.getY();
               this.renderPos.z = this.mc.player.getZ();
            }

            this.renderPos.x = this.renderPos.x + (this.crystalPos.getX() - this.renderPos.x) * this.renderSpeed.get();
            this.renderPos.y = this.renderPos.y + (this.crystalPos.getY() - 1.0 - this.renderPos.y) * this.renderSpeed.get();
            this.renderPos.z = this.renderPos.z + (this.crystalPos.getZ() - this.renderPos.z) * this.renderSpeed.get();
            AABB box = new AABB(
               this.renderPos.x,
               this.renderPos.y + (1.0 - this.renderH.get()),
               this.renderPos.z,
               this.renderPos.x + 1.0,
               this.renderPos.y + 1.0,
               this.renderPos.z + 1.0
            );
            event.renderer.box(box, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (this.crystalPos != null && !this.within(this.crystalPos.getCenter(), this.placeRange.get())) {
            this.crystalPos = null;
            this.lastBestPos = null;
         }

         if (this.crystalPos != null && !this.within(this.crystalPos.getCenter(), this.breakRange.get())) {
            this.crystalPos = null;
            this.lastBestPos = null;
         }

         if (this.crystalPos != null
            && (
               LeavesBlockUtil.getBlock(this.crystalPos.below()) != Blocks.OBSIDIAN && LeavesBlockUtil.getBlock(this.crystalPos.below()) != Blocks.BEDROCK
                  || !this.mc.level.isEmptyBlock(this.crystalPos)
            )) {
            this.crystalPos = null;
            this.lastBestPos = null;
         }

         this.target = LeavesCombatUtil.getClosestEnemy(this.targetRange.get());
         if (this.target == null) {
            this.crystalPos = null;
            this.lastBestPos = null;
            this.releaseRotation();
         } else if (this.shouldPause()) {
            this.holdAutoRotation();
         } else {
            int crystalSlot = this.inventory.get()
               ? LeavesInventoryUtil.findItemInventorySlot(Items.END_CRYSTAL)
               : LeavesInventoryUtil.findItem(Items.END_CRYSTAL);
            if (crystalSlot == -1) {
               this.crystalPos = null;
               this.releaseRotation();
            } else {
               if (this.breakTimer.passedMs(this.breakDelay.get().intValue())) {
                  this.breakCrystal();
                  this.breakTimer.reset();
               }

               this.findBestPos();
               this.holdAutoRotation();
               if (this.crystalPos != null && this.placeTimer.passedMs(this.placeDelay.get().intValue())) {
                  if (!LeavesBlockUtil.hasCrystal(this.crystalPos)) {
                     this.placeCrystal(this.crystalPos, crystalSlot);
                     this.baseTimer.reset();
                     this.placeTimer.reset();
                  } else {
                     this.holdCrystalRotation(this.crystalPos);
                     LeavesCombatUtil.attackCrystal(this.crystalPos, false, false);
                  }
               }
            }
         }
      }
   }

   private void findBestPos() {
      Player predictTarget = this.predictTarget(this.target);
      float bestDamage = 0.0F;
      BlockPos best = null;
      ArrayList<BlockPos> placeList = new ArrayList<>();

      for (BlockPos pos : LeavesBlockUtil.getSphere(this.placeRange.get())) {
         if (this.autoBase.get()
            && !LeavesBlockUtil.canPlaceCrystal(pos)
            && LeavesBlockUtil.canPlace(pos.below())
            && !LeavesBlockUtil.hasEntity(pos, true)
            && this.mc.level.isEmptyBlock(pos)
            && pos.getY() <= this.target.getY()
            && this.baseTimer.passedMs(this.baseDelay.get().intValue())) {
            placeList.add(pos);
         }

         if (LeavesBlockUtil.canPlaceCrystal(pos) || LeavesBlockUtil.hasCrystalPlace(pos) && this.mc.level.isEmptyBlock(pos)) {
            placeList.add(pos);
         }
      }

      if (!placeList.isEmpty()) {
         for (BlockPos pos : placeList) {
            if (this.autoBase.get()) {
               LeavesCombatUtil.modifyPos = pos.below();
               LeavesCombatUtil.modifyBlockState = Blocks.OBSIDIAN.defaultBlockState();
            }

            Vec3 vec = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            float targetDamage = LeavesDamageUtil.calculateDamage(vec, predictTarget);
            float self = LeavesDamageUtil.calculateDamage(vec, this.mc.player);
            if (this.autoBase.get()) {
               LeavesCombatUtil.modifyPos = null;
            }

            if (!(targetDamage < this.minDamage.get())
               && !(self > this.maxSelfDmg.get())
               && (!this.noSuicide.get() || !(self > this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount()))
               && targetDamage > bestDamage) {
               bestDamage = targetDamage;
               best = pos;
            }
         }

         if (this.lastBestPos != null) {
            if (this.autoBase.get()) {
               LeavesCombatUtil.modifyPos = this.lastBestPos.below();
               LeavesCombatUtil.modifyBlockState = Blocks.OBSIDIAN.defaultBlockState();
            }

            Vec3 vec = new Vec3(this.lastBestPos.getX() + 0.5, this.lastBestPos.getY(), this.lastBestPos.getZ() + 0.5);
            float last = LeavesDamageUtil.calculateDamage(vec, predictTarget);
            float lastSelf = LeavesDamageUtil.calculateDamage(vec, this.mc.player);
            if (this.autoBase.get()) {
               LeavesCombatUtil.modifyPos = null;
            }

            if (best != null
               && last >= bestDamage * 0.95
               && lastSelf < this.maxSelfDmg.get()
               && (!this.noSuicide.get() || lastSelf < this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount())) {
               this.crystalPos = this.lastBestPos;
               this.dmg = (int)last;
               return;
            }

            if (best == null && last >= this.minDamage.get()) {
               this.crystalPos = this.lastBestPos;
               this.dmg = (int)last;
               return;
            }
         }

         if (best != null && this.autoBase.get() && LeavesBlockUtil.canPlace(best.below()) && this.baseTimer.passedMs(this.baseDelay.get().intValue())) {
            this.doBase(best.below());
         }

         this.lastBestPos = best;
         this.crystalPos = best;
         this.dmg = (int)bestDamage;
      }
   }

   private Player predictTarget(Player target) {
      if (this.predict.get() > 0 && this.mc.level != null) {
         int ticks = this.predict.get();
         double predictX = target.getX() + (target.getX() - target.xOld) * ticks;
         double predictY = target.getY() + (target.getY() - target.yOld) * ticks;
         double predictZ = target.getZ() + (target.getZ() - target.zOld) * ticks;
         Player fake = new Player(this.mc.level, new GameProfile(UUID.randomUUID(), "Predict")) {
            @Override
            public GameType gameMode() {
               return GameType.SURVIVAL;
            }

            @Override
            public boolean isSpectator() {
               return false;
            }

            @Override
            public boolean isCreative() {
               return false;
            }
         };
         fake.setPos(predictX, predictY, predictZ);
         fake.setYRot(target.getYRot());
         fake.setXRot(target.getXRot());
         fake.setPose(target.getPose());
         fake.setOnGround(target.onGround());
         fake.setDeltaMovement(target.getDeltaMovement());
         fake.setHealth(target.getHealth());
         fake.setAbsorptionAmount(target.getAbsorptionAmount());

         for (MobEffectInstance effect : target.getActiveEffects()) {
            fake.addEffect(new MobEffectInstance(effect));
         }

         return fake;
      } else {
         return target;
      }
   }

   private void doBase(BlockPos pos) {
      int old = this.mc.player.getInventory().getSelectedSlot();
      int obsSlot = this.inventory.get() ? LeavesInventoryUtil.findItemInventorySlot(Items.OBSIDIAN) : LeavesInventoryUtil.findItem(Items.OBSIDIAN);
      Direction side = LeavesBlockUtil.getPlaceSide(pos, null);
      if (obsSlot != -1 && side != null) {
         this.doSwap(obsSlot);
         this.holdPlaceRotation(pos, side);
         LeavesBlockUtil.placeBlock(pos, side, false);
         this.doSwap(this.inventory.get() ? obsSlot : old);
         this.breakTimer.reset();
      }
   }

   private void placeCrystal(BlockPos pos, int slot) {
      BlockPos base = pos.below();
      Direction side = LeavesBlockUtil.getClickSide(base);
      if (side != null) {
         int old = this.mc.player.getInventory().getSelectedSlot();
         boolean holdItem = this.checkItem(old);
         if (!holdItem) {
            this.doSwap(slot);
         }

         this.holdClickRotation(base, side);
         LeavesBlockUtil.clickBlock(base, side, false);
         if (!holdItem) {
            this.doSwap(this.inventory.get() ? slot : old);
         }
      }
   }

   private boolean checkItem(int old) {
      return this.mc.player.getInventory().getItem(old).is(Items.END_CRYSTAL);
   }

   private boolean shouldPause() {
      AutoAnchor anchor = AutoAnchor.INSTANCE;
      return this.preferMode.get() == AutoCrystal.PreferMode.PreferAnchor && anchor != null
         ? anchor.currentPos != null
         : !this.usingPause.get() || this.checkPause(this.onlyMain.get());
   }

   public boolean checkPause(boolean onlyMain) {
      return (this.mc.options.keyUse.isDown() || this.mc.player.isUsingItem()) && (!onlyMain || this.mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND);
   }

   private void breakCrystal() {
      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal crystal && LeavesCombatUtil.isValid(entity, this.breakRange.get())) {
            Player predictTarget = this.predictTarget(this.target);
            float damage = LeavesDamageUtil.calculateDamage(crystal.position(), predictTarget);
            float self = LeavesDamageUtil.calculateDamage(crystal.position(), this.mc.player);
            if (!(damage < this.minDamage.get())
               && !(self > this.maxSelfDmg.get())
               && (!this.noSuicide.get() || !(self > this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount()))) {
               this.holdCrystalRotation(crystal);
               LeavesCombatUtil.attackCrystal(crystal, false, false);
               if (this.crystalPos != null && LeavesBlockUtil.hasCrystal(this.crystalPos) && this.placeAfterBreak.get()) {
                  int crystalSlot = this.inventory.get()
                     ? LeavesInventoryUtil.findItemInventorySlot(Items.END_CRYSTAL)
                     : LeavesInventoryUtil.findItem(Items.END_CRYSTAL);
                  this.placeCrystal(this.crystalPos, crystalSlot);
               }

               return;
            }
         }
      }
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

   private boolean within(Vec3 vec, double range) {
      return this.mc.player.getEyePosition().distanceTo(vec) <= range;
   }

   private void holdAutoRotation() {
      if (!this.rotate.get()) {
         this.releaseRotation();
      } else {
         if (this.crystalPos != null) {
            if (LeavesBlockUtil.hasCrystal(this.crystalPos)) {
               this.holdCrystalRotation(this.crystalPos);
               return;
            }

            BlockPos base = this.crystalPos.below();
            Direction side = LeavesBlockUtil.getClickSide(base);
            if (side != null) {
               this.holdClickRotation(base, side);
               return;
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

   private void holdCrystalRotation(BlockPos pos) {
      Iterator var2 = LeavesBlockUtil.getEndCrystals(new AABB(pos)).iterator();
      if (var2.hasNext()) {
         EndCrystal crystal = (EndCrystal)var2.next();
         this.holdCrystalRotation(crystal);
      } else {
         this.holdRotation(pos.getCenter());
      }
   }

   private void holdCrystalRotation(EndCrystal crystal) {
      this.holdRotation(new Vec3(crystal.getX(), crystal.getY() + 0.25, crystal.getZ()));
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

   public enum CalcMode {
      AlienV4,
      Meteor;
   }

   public enum PreferMode {
      PreferCrystal,
      PreferAnchor;
   }

   private static class RenderPos {
      double x;
      double y;
      double z;
   }
}
