package meteordevelopment.meteorclient.systems.modules.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Breaker extends Module {
   private static final Direction[] SURROUNDING_DIRECTIONS = new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Breaker.SwingMode> swingMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("swing-mode"))
                  .description("How to swing when breaking blocks."))
               .defaultValue(Breaker.SwingMode.Client))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Range to search and break beds.")
            .defaultValue(4.5)
            .range(0.5, 6.0)
            .sliderRange(0.5, 6.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Boolean> breakSurroundings = this.sgGeneral
      .add(new BoolSetting.Builder().name("break-surroundings").description("Breaks blocks around beds before breaking the bed.").defaultValue(true).build());
   private final Setting<Boolean> autoSwitch = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-switch").description("Switches to the fastest hotbar tool for the target block.").defaultValue(true).build());
   private final Setting<Double> rotationSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("Rotation speed.")
            .defaultValue(10.0)
            .range(1.0, 10.0)
            .sliderRange(1.0, 10.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Double> rotationBackSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotation-back-speed")
            .description("Rotation return speed.")
            .defaultValue(3.0)
            .range(1.0, 10.0)
            .sliderRange(1.0, 10.0)
            .decimalPlaces(1)
            .build()
      );
   private Breaker.BlockTarget currentTarget;

   public Breaker() {
      super(Categories.World, "breaker", "Breaks nearby beds and their surrounding blocks.");
   }

   @Override
   public void onDeactivate() {
      this.currentTarget = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         this.updateTargetBlock();
         if (this.currentTarget != null) {
            BlockPos pos = this.currentTarget.candidate.pos;
            BlockState state = this.mc.level.getBlockState(pos);
            if (!BlockUtils.canBreak(pos, state)) {
               this.currentTarget = null;
            } else {
               Direction side = RotationUtil.getClickSide(pos);
               Rotation rotation = RotationUtil.calculate(pos, side);
               RotationManager.INSTANCE.setRotationsWithNoise(rotation, this.rotationSpeed.get(), this.rotationBackSpeed.get(), r -> true, Priority.Medium);
               if (this.autoSwitch.get()) {
                  int slot = this.getBestSlot(state);
                  if (slot != -1 && slot != this.mc.player.getInventory().getSelectedSlot()) {
                     SlotHelper.setCurrentItem(slot);
                  }
               }

               BlockUtils.breakBlock(pos, this.swingMode.get() == Breaker.SwingMode.Client);
            }
         }
      }
   }

   private void updateTargetBlock() {
      this.currentTarget = null;
      Breaker.BlockCandidate closestBed = this.findClosestBed();
      if (closestBed != null) {
         if (!this.breakSurroundings.get()) {
            this.currentTarget = new Breaker.BlockTarget(closestBed, 0.01);
         } else {
            Breaker.BlockCandidate surrounding = this.findSurroundingTarget(closestBed);
            this.currentTarget = surrounding == null
               ? new Breaker.BlockTarget(closestBed, 0.01)
               : new Breaker.BlockTarget(surrounding, this.getResistance(surrounding));
         }
      }
   }

   private Breaker.BlockCandidate findClosestBed() {
      Vec3 eyePos = this.mc.player.getEyePosition();
      double rangeValue = this.range.get();
      double rangeSq = rangeValue * rangeValue;
      int fromX = (int)Math.floor(eyePos.x - rangeValue - 1.0);
      int fromY = (int)Math.floor(eyePos.y - rangeValue - 1.0);
      int fromZ = (int)Math.floor(eyePos.z - rangeValue - 1.0);
      int toX = (int)Math.ceil(eyePos.x + rangeValue + 1.0);
      int toY = (int)Math.ceil(eyePos.y + rangeValue + 1.0);
      int toZ = (int)Math.ceil(eyePos.z + rangeValue + 1.0);
      Breaker.BlockCandidate closest = null;

      for (int x = fromX; x <= toX; x++) {
         for (int y = fromY; y <= toY; y++) {
            for (int z = fromZ; z <= toZ; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               BlockState state = this.mc.level.getBlockState(pos);
               if (state.getBlock() instanceof BedBlock) {
                  Breaker.BlockCandidate candidate = new Breaker.BlockCandidate(pos);
                  if (!(candidate.distanceSq > rangeSq) && (closest == null || candidate.distanceSq < closest.distanceSq)) {
                     closest = candidate;
                  }
               }
            }
         }
      }

      return closest;
   }

   private Breaker.BlockCandidate findSurroundingTarget(Breaker.BlockCandidate bedCandidate) {
      List<Breaker.BlockCandidate> candidates = this.getSurroundingCandidates(bedCandidate);

      for (Breaker.BlockCandidate candidate : candidates) {
         BlockState state = this.mc.level.getBlockState(candidate.pos);
         if (state.isAir() || !state.getFluidState().isEmpty()) {
            return null;
         }
      }

      return candidates.stream().filter(candidatex -> {
         BlockState statex = this.mc.level.getBlockState(candidatex.pos);
         return !(statex.getBlock() instanceof BedBlock) && BlockUtils.canBreak(candidatex.pos, statex);
      }).min(Comparator.comparingDouble(this::getResistance)).orElse(null);
   }

   private List<Breaker.BlockCandidate> getSurroundingCandidates(Breaker.BlockCandidate bedCandidate) {
      List<Breaker.BlockCandidate> candidates = new ArrayList<>();
      this.addSurroundingCandidates(candidates, bedCandidate.pos);
      BlockState bedState = this.mc.level.getBlockState(bedCandidate.pos);
      if (bedState.getBlock() instanceof BedBlock) {
         BlockPos otherPart = bedCandidate.pos.relative(BedBlock.getConnectedDirection(bedState));
         if (this.mc.level.getBlockState(otherPart).getBlock() instanceof BedBlock) {
            this.addSurroundingCandidates(candidates, otherPart);
         }
      }

      candidates.sort(Comparator.comparingDouble(candidate -> candidate.distanceSq));
      return candidates;
   }

   private void addSurroundingCandidates(List<Breaker.BlockCandidate> candidates, BlockPos origin) {
      double rangeSq = this.range.get() * this.range.get();

      for (Direction direction : SURROUNDING_DIRECTIONS) {
         Breaker.BlockCandidate candidate = new Breaker.BlockCandidate(origin.relative(direction));
         if (candidate.distanceSq <= rangeSq && candidates.stream().noneMatch(existing -> existing.pos.equals(candidate.pos))) {
            candidates.add(candidate);
         }
      }
   }

   private double getResistance(Breaker.BlockCandidate candidate) {
      BlockState state = this.mc.level.getBlockState(candidate.pos);
      double speed = Math.max(1.0, this.getBestDestroySpeed(state));
      return Math.max(0.01, state.getDestroySpeed(this.mc.level, candidate.pos)) / speed;
   }

   private double getBestDestroySpeed(BlockState state) {
      double bestSpeed = this.mc.player.getMainHandItem().getDestroySpeed(state);

      for (int i = 0; i < 9; i++) {
         bestSpeed = Math.max(bestSpeed, this.mc.player.getInventory().getItem(i).getDestroySpeed(state));
      }

      return bestSpeed;
   }

   private int getBestSlot(BlockState state) {
      double bestSpeed = this.mc.player.getMainHandItem().getDestroySpeed(state);
      int bestSlot = -1;

      for (int i = 0; i < 9; i++) {
         double speed = this.mc.player.getInventory().getItem(i).getDestroySpeed(state);
         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = i;
         }
      }

      return bestSlot;
   }

   @Override
   public String getInfoString() {
      return this.currentTarget == null ? null : this.currentTarget.candidate.pos.toShortString();
   }

   private class BlockCandidate {
      private final BlockPos pos;
      private final double distanceSq;

      private BlockCandidate(BlockPos pos) {
         this.pos = pos.immutable();
         this.distanceSq = Breaker.this.mc.player.getEyePosition().distanceToSqr(pos.getCenter());
      }
   }

   private record BlockTarget(Breaker.BlockCandidate candidate, double resistance) {
   }

   private enum SwingMode {
      Client,
      Server;
   }
}
