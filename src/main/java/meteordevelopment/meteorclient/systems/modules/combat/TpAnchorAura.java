package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.leaves.LeavesDamageUtil;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TpAnchorAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCalc = this.settings.createGroup("Calculation");
   private final SettingGroup sgProtect = this.settings.createGroup("Protection");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final SettingGroup sgPredict = this.settings.createGroup("Prediction");
   private final Setting<Set<EntityType<?>>> targetTypes = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("target-type").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to select the target."))
               .defaultValue(SortPriority.LowestHealth))
            .build()
      );
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("Range in which to look for targets.")
            .defaultValue(200.0)
            .min(1.0)
            .sliderRange(1.0, 500.0)
            .build()
      );
   private final Setting<Boolean> usingPause = this.sgGeneral
      .add(new BoolSetting.Builder().name("pause-on-use").description("Pause while player is using an item.").defaultValue(true).build());
   private final Setting<Boolean> backVar = this.sgGeneral
      .add(new BoolSetting.Builder().name("tp-back").description("Teleport back after the cycle.").defaultValue(true).build());
   private final Setting<Integer> placeDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("place-delay").description("Tick delay between cycles.").defaultValue(4).min(0).sliderRange(0, 20).build());
   private final Setting<Double> minDamage = this.sgCalc
      .add(
         new DoubleSetting.Builder()
            .name("min-target-damage")
            .description("Minimum anchor damage required to deal to the target.")
            .defaultValue(6.0)
            .min(0.0)
            .sliderRange(0.0, 36.0)
            .build()
      );
   private final Setting<Double> maxSelfDamage = this.sgCalc
      .add(
         new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("Maximum damage allowed to inflict on yourself.")
            .defaultValue(6.0)
            .min(0.0)
            .sliderRange(0.0, 36.0)
            .build()
      );
   private final Setting<Integer> scanBorder = this.sgCalc
      .add(
         new IntSetting.Builder()
            .name("scan-border")
            .description("Scan radius around target for anchor placement.")
            .defaultValue(3)
            .min(1)
            .sliderRange(1, 8)
            .build()
      );
   private final Setting<Double> hitRange = this.sgCalc
      .add(
         new DoubleSetting.Builder()
            .name("hit-range")
            .description("Maximum range from TP position to the anchor.")
            .defaultValue(6.0)
            .min(0.1)
            .sliderRange(0.1, 10.0)
            .build()
      );
   private final Setting<Boolean> airPlace = this.sgProtect
      .add(
         new BoolSetting.Builder()
            .name("air-place")
            .description("Allow placing anchor and glowstone in the air (no adjacent solid block required).")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> allowProtect = this.sgProtect
      .add(
         new BoolSetting.Builder()
            .name("allow-protect")
            .description("Place a shield block between the TP position and the anchor to reduce self-damage.")
            .defaultValue(true)
            .build()
      );
   private final Setting<TpAnchorAura.ProtectBlockType> protectBlockType = this.sgProtect
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("protect-block"))
                     .description("Block to place as the anchor shield (挡锚). GlowStone matches GCore default behaviour."))
                  .defaultValue(TpAnchorAura.ProtectBlockType.GlowStone))
               .visible(this.allowProtect::get))
            .build()
      );
   private final Setting<Boolean> onlyProtect = this.sgProtect
      .add(
         new BoolSetting.Builder()
            .name("only-protect")
            .description("Only place the shield block — do NOT charge or detonate the anchor (pure 挡锚 mode).")
            .defaultValue(false)
            .visible(this.allowProtect::get)
            .build()
      );
   private final Setting<Double> protectStep = this.sgProtect
      .add(
         new DoubleSetting.Builder()
            .name("protect-step")
            .description("Step size when searching for the shield block position.")
            .defaultValue(0.1)
            .min(0.01)
            .sliderRange(0.01, 1.0)
            .visible(this.allowProtect::get)
            .build()
      );
   private final Setting<Boolean> useGlowstoneBlock = this.sgProtect
      .add(
         new BoolSetting.Builder()
            .name("glowstone-block")
            .description("When no safe TP position exists, place glowstone between you and the anchor to shield the blast.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Integer> glowstoneBlockCount = this.sgProtect
      .add(
         new IntSetting.Builder()
            .name("glowstone-block-count")
            .description("Number of glowstone blocks to place for shielding.")
            .defaultValue(2)
            .min(1)
            .sliderRange(1, 4)
            .visible(this.useGlowstoneBlock::get)
            .build()
      );
   private final Setting<SettingColor> glowstoneBlockColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("glowstone-block-color")
            .description("Color of the glowstone shield positions.")
            .defaultValue(new SettingColor(255, 255, 0, 80))
            .build()
      );
   private final Setting<Boolean> render = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Render anchor and TP positions.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How to render the boxes."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.render::get))
            .build()
      );
   private final Setting<SettingColor> tpColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tp-color")
            .description("Color of the teleport point box.")
            .defaultValue(new SettingColor(255, 255, 255, 80))
            .visible(this.render::get)
            .build()
      );
   private final Setting<SettingColor> anchorColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("anchor-color")
            .description("Color of the anchor position box.")
            .defaultValue(new SettingColor(255, 0, 0, 80))
            .visible(this.render::get)
            .build()
      );
   private final Setting<SettingColor> protectColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("protect-color")
            .description("Color of the protect block position.")
            .defaultValue(new SettingColor(0, 0, 255, 80))
            .visible(this.render::get)
            .build()
      );
   private final Setting<TpAnchorAura.PredictionMode> predictionMode = this.sgPredict
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("prediction-mode"))
                  .description("How to predict the target's position."))
               .defaultValue(TpAnchorAura.PredictionMode.Off))
            .build()
      );
   private final Setting<Double> prevMultiplier = this.sgPredict
      .add(
         new DoubleSetting.Builder()
            .name("prev-multiplier")
            .description("Legacy prediction multiplier.")
            .defaultValue(1.0)
            .min(0.0)
            .sliderRange(0.0, 5.0)
            .visible(() -> this.predictionMode.get() == TpAnchorAura.PredictionMode.Legacy)
            .build()
      );
   private Entity target;
   private int placeTickTimer;
   private Vec3 renderTpVec;
   private BlockPos renderAnchorPos;
   private BlockPos renderProtectPos;
   private List<BlockPos> renderGlowstonePositions = new ArrayList<>();

   public TpAnchorAura() {
      super(Categories.Combat, "TpAnchorAura", "TP-based respawn anchor aura.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.placeTickTimer = this.placeDelay.get();
      this.renderTpVec = null;
      this.renderAnchorPos = null;
      this.renderProtectPos = null;
      this.renderGlowstonePositions.clear();
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.renderTpVec = null;
      this.renderAnchorPos = null;
      this.renderProtectPos = null;
      this.renderGlowstonePositions.clear();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         if (!this.usingPause.get() || !this.mc.player.isUsingItem()) {
            this.target = TargetUtils.get(this::isValidTarget, this.priority.get());
            if (this.target != null) {
               FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
               FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
               if (anchor.found() && glowstone.found()) {
                  if (this.placeTickTimer < this.placeDelay.get()) {
                     this.placeTickTimer++;
                  } else {
                     this.placeTickTimer = 0;
                     Vec3 targetPos = this.getPredictedPos(this.target);
                     TpAnchorAura.Data data = this.findBestData(targetPos);
                     if (data == null) {
                        if (this.useGlowstoneBlock.get()) {
                           this.tryGlowstoneBlock(targetPos, anchor, glowstone);
                        }
                     } else {
                        this.renderGlowstonePositions.clear();
                        this.renderAnchorPos = data.anchorPos;
                        this.renderTpVec = data.tpVec;
                        this.renderProtectPos = data.protectPos;
                        Vec3 playerPos = this.mc.player.position();
                        boolean back = this.backVar.get();
                        this.doTp(playerPos, data.tpVec);
                        this.runCycle(data, anchor, glowstone);
                        if (back) {
                           this.doTp(data.tpVec, playerPos);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void runCycle(TpAnchorAura.Data data, FindItemResult anchor, FindItemResult glowstone) {
      int old = this.mc.player.getInventory().getSelectedSlot();
      BlockState state = this.mc.level.getBlockState(data.anchorPos);
      if (!state.is(Blocks.RESPAWN_ANCHOR)) {
         if (!BlockUtils.canPlace(data.anchorPos)) {
            return;
         }

         if (InvUtils.swap(anchor.slot(), false)) {
            this.placeBlockAt(data.anchorPos, data.tpVec, InteractionHand.MAIN_HAND);
            InvUtils.swap(old, false);
         }
      }

      if (data.protectPos != null) {
         Item shieldItem = this.protectBlockType.get() == TpAnchorAura.ProtectBlockType.GlowStone ? Items.GLOWSTONE : Items.OBSIDIAN;
         FindItemResult shield = InvUtils.findInHotbar(shieldItem);
         if (shield.found() && InvUtils.swap(shield.slot(), false)) {
            this.placeBlockAt(data.protectPos, data.tpVec, InteractionHand.MAIN_HAND);
            InvUtils.swap(old, false);
         }
      }

      if (!this.onlyProtect.get()) {
         if (InvUtils.swap(glowstone.slot(), false)) {
            this.clickBlockAt(data.anchorPos, Direction.UP, data.tpVec, InteractionHand.MAIN_HAND);
            InvUtils.swap(old, false);
         }

         FindItemResult trigger = InvUtils.findInHotbar(item -> !item.getItem().equals(Items.GLOWSTONE));
         if (trigger.found() && InvUtils.swap(trigger.slot(), false)) {
            this.clickBlockAt(data.anchorPos, Direction.UP, data.tpVec, InteractionHand.MAIN_HAND);
            this.mc.level.setBlock(data.anchorPos, this.mc.level.getFluidState(data.anchorPos).createLegacyBlock(), 0);
            InvUtils.swap(old, false);
         }
      }
   }

   private boolean placeBlockAt(BlockPos pos, Vec3 tpPos, InteractionHand hand) {
      if (!this.mc.level.getBlockState(pos).canBeReplaced()) {
         return false;
      }

      Vec3 eyePos = tpPos.add(0.0, this.mc.player.getEyeHeight(this.mc.player.getPose()), 0.0);
      Direction side = BlockUtils.getClosestPlaceSide(pos, eyePos);
      if (side == null) {
         side = BlockUtils.getPlaceSide(pos);
      }

      if (side != null) {
         BlockPos neighbour = pos.relative(side);
         Vec3 hitPos = pos.getCenter().add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
         return this.sendUseOn(new BlockHitResult(hitPos, side.getOpposite(), neighbour, false), hand);
      }

      if (!this.airPlace.get()) {
         return false;
      }

      Vec3 toBlock = pos.getCenter().subtract(eyePos).normalize();
      Direction airSide = getDirectionToward(toBlock);
      Vec3 hitPos = pos.getCenter().add(airSide.getStepX() * 0.4, airSide.getStepY() * 0.4, airSide.getStepZ() * 0.4);
      return this.sendUseOn(new BlockHitResult(hitPos, airSide.getOpposite(), pos, true), hand);
   }

   private boolean clickBlockAt(BlockPos pos, Direction side, Vec3 tpPos, InteractionHand hand) {
      Vec3 hitPos = pos.getCenter().add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
      return this.sendUseOn(new BlockHitResult(hitPos, side, pos, false), hand);
   }

   private boolean sendUseOn(BlockHitResult hitResult, InteractionHand hand) {
      boolean wasSneaking = this.mc.player.isShiftKeyDown();
      this.mc.player.setShiftKeyDown(false);
      InteractionResult result = this.mc.gameMode.useItemOn(this.mc.player, hand, hitResult);
      if (result.consumesAction()) {
         this.mc.player.swing(hand);
      } else {
         this.mc.gameMode.startPrediction(this.mc.level, sequence -> new ServerboundUseItemOnPacket(hand, hitResult, sequence));
      }

      this.mc.player.setShiftKeyDown(wasSneaking);
      return true;
   }

   private static Direction getDirectionToward(Vec3 normalized) {
      Direction best = Direction.DOWN;
      double bestDot = -Double.MAX_VALUE;

      for (Direction dir : Direction.values()) {
         double dot = dir.getStepX() * normalized.x + dir.getStepY() * normalized.y + dir.getStepZ() * normalized.z;
         if (dot > bestDot) {
            bestDot = dot;
            best = dir;
         }
      }

      return best;
   }

   private TpAnchorAura.Data findBestData(Vec3 targetPos) {
      BlockPos bestAnchor = this.findBestAnchorPos(targetPos);
      if (bestAnchor == null) {
         return null;
      }

      TpAnchorAura.TpSlot tp = this.findTpSlot(bestAnchor, this.scanBorder.get());
      if (tp == null) {
         return null;
      }

      BlockPos protectPos = null;
      if (tp.selfDmg <= this.maxSelfDamage.get()) {
         if (this.allowProtect.get()) {
            protectPos = this.findProtectPos(bestAnchor, tp.vec);
         }

         return new TpAnchorAura.Data(bestAnchor, tp.vec, protectPos, tp.selfDmg);
      } else {
         if (!this.allowProtect.get()) {
            return null;
         }

         protectPos = this.findProtectPos(bestAnchor, tp.vec);
         if (protectPos == null) {
            return null;
         }

         float dmgWithShield = this.selfDamageWithProtect(tp.vec, bestAnchor.getCenter(), bestAnchor, protectPos);
         return dmgWithShield > this.maxSelfDamage.get() ? null : new TpAnchorAura.Data(bestAnchor, tp.vec, protectPos, dmgWithShield);
      }
   }

   private BlockPos findBestAnchorPos(Vec3 targetPos) {
      BlockPos targetBlock = BlockPos.containing(targetPos);
      int border = this.scanBorder.get();
      float bestDmg = 0.0F;
      BlockPos best = null;

      for (int x = -border; x <= border; x++) {
         for (int y = -border; y <= border; y++) {
            for (int z = -border; z <= border; z++) {
               BlockPos pos = targetBlock.offset(x, y, z);
               if (this.isValidAnchorPos(pos)) {
                  Vec3 anchorCenter = pos.getCenter();
                  float dmg = LeavesDamageUtil.anchorDamage((LivingEntity)this.target, anchorCenter);
                  if (!(dmg < this.minDamage.get()) && !(dmg <= bestDmg)) {
                     bestDmg = dmg;
                     best = pos;
                  }
               }
            }
         }
      }

      return best;
   }

   private void tryGlowstoneBlock(Vec3 targetPos, FindItemResult anchor, FindItemResult glowstone) {
      BlockPos anchorPos = this.findBestAnchorPos(targetPos);
      if (anchorPos != null) {
         Vec3 playerPos = this.mc.player.position();
         List<BlockPos> shieldPositions = this.findShieldPositions(anchorPos, playerPos, this.glowstoneBlockCount.get());
         if (!shieldPositions.isEmpty()) {
            this.renderAnchorPos = anchorPos;
            this.renderTpVec = playerPos;
            this.renderProtectPos = null;
            this.renderGlowstonePositions = new ArrayList<>(shieldPositions);
            int old = this.mc.player.getInventory().getSelectedSlot();

            for (BlockPos sp : shieldPositions) {
               if (InvUtils.swap(glowstone.slot(), false)) {
                  this.placeBlockAt(sp, playerPos, InteractionHand.MAIN_HAND);
                  InvUtils.swap(old, false);
               }
            }

            this.runCycle(new TpAnchorAura.Data(anchorPos, playerPos, null, 0.0F), anchor, glowstone);
         }
      }
   }

   private List<BlockPos> findShieldPositions(BlockPos anchorPos, Vec3 playerPos, int max) {
      BlockPos tp = BlockPos.containing(playerPos);
      int tx = tp.getX();
      int ty = tp.getY();
      int tz = tp.getZ();
      BlockPos[] candidates = new BlockPos[]{
         new BlockPos(tx, ty - 1, tz),
         new BlockPos(tx, ty + 2, tz),
         new BlockPos(tx + 1, ty, tz),
         new BlockPos(tx - 1, ty, tz),
         new BlockPos(tx + 1, ty + 1, tz),
         new BlockPos(tx - 1, ty + 1, tz),
         new BlockPos(tx, ty, tz + 1),
         new BlockPos(tx, ty, tz - 1),
         new BlockPos(tx, ty + 1, tz + 1),
         new BlockPos(tx, ty + 1, tz - 1)
      };
      Vec3 anchorCenter = anchorPos.getCenter();

      record Entry(BlockPos pos, float dmg) {
      }

      List<Entry> entries = new ArrayList<>();

      for (BlockPos pos : candidates) {
         if (!pos.equals(anchorPos)
            && this.mc.level.getBlockState(pos).canBeReplaced()
            && this.mc.level.getEntities(null, new AABB(pos)).isEmpty()
            && !(playerPos.distanceTo(pos.getCenter()) > this.hitRange.get())) {
            float dmg = this.selfDamageWithProtect(playerPos, anchorCenter, anchorPos, pos);
            entries.add(new Entry(pos, dmg));
         }
      }

      entries.sort((a, b) -> Float.compare(a.dmg(), b.dmg()));
      List<BlockPos> result = new ArrayList<>();

      for (Entry e : entries) {
         if (result.size() >= max) {
            break;
         }

         result.add(e.pos());
      }

      return result;
   }

   private boolean isValidAnchorPos(BlockPos pos) {
      if (this.mc.level == null) {
         return false;
      } else {
         BlockState state = this.mc.level.getBlockState(pos);
         if (state.is(Blocks.RESPAWN_ANCHOR)) {
            return true;
         } else {
            return !BlockUtils.canPlace(pos) ? false : this.airPlace.get() || BlockUtils.getPlaceSide(pos) != null;
         }
      }
   }

   private TpAnchorAura.TpSlot findTpSlot(BlockPos anchorPos, int border) {
      Vec3 anchorCenter = anchorPos.getCenter();
      Vec3 best = null;
      float minDmg = Float.MAX_VALUE;

      for (int x = -border; x <= border; x++) {
         for (int y = -border; y <= border; y++) {
            for (int z = -border; z <= border; z++) {
               BlockPos pos = anchorPos.offset(x, y, z);
               if (!pos.equals(anchorPos)) {
                  Vec3 vec = Vec3.atLowerCornerOf(pos).add(0.5, 0.0, 0.5);
                  if (this.isBlinkVec(vec) && !(vec.distanceTo(anchorCenter) > this.hitRange.get())) {
                     float dmg = this.selfDamageAt(vec, anchorCenter);
                     if (dmg < minDmg) {
                        minDmg = dmg;
                        best = vec;
                     }
                  }
               }
            }
         }
      }

      return best == null ? null : new TpAnchorAura.TpSlot(best, minDmg);
   }

   private BlockPos findProtectPos(BlockPos anchorPos, Vec3 tpVec) {
      BlockPos tp = BlockPos.containing(tpVec);
      int tx = tp.getX();
      int ty = tp.getY();
      int tz = tp.getZ();
      BlockPos[] candidates = new BlockPos[]{
         new BlockPos(tx, ty - 1, tz),
         new BlockPos(tx, ty + 2, tz),
         new BlockPos(tx + 1, ty, tz),
         new BlockPos(tx - 1, ty, tz),
         new BlockPos(tx + 1, ty + 1, tz),
         new BlockPos(tx - 1, ty + 1, tz),
         new BlockPos(tx, ty, tz + 1),
         new BlockPos(tx, ty, tz - 1),
         new BlockPos(tx, ty + 1, tz + 1),
         new BlockPos(tx, ty + 1, tz - 1)
      };
      Vec3 anchorCenter = anchorPos.getCenter();
      BlockPos best = null;
      float bestDmg = Float.MAX_VALUE;

      for (BlockPos pos : candidates) {
         if (!pos.equals(anchorPos)
            && this.mc.level.getBlockState(pos).canBeReplaced()
            && this.mc.level.getEntities(null, new AABB(pos)).isEmpty()
            && !(tpVec.distanceTo(pos.getCenter()) > this.hitRange.get())) {
            float dmg = this.selfDamageWithProtect(tpVec, anchorCenter, anchorPos, pos);
            if (dmg < bestDmg) {
               bestDmg = dmg;
               best = pos;
            }
         }
      }

      return best;
   }

   private float selfDamageWithProtect(Vec3 tpVec, Vec3 anchorCenter, BlockPos anchorPos, BlockPos protectPos) {
      AABB box = this.mc.player.getBoundingBox().move(tpVec.x - this.mc.player.getX(), tpVec.y - this.mc.player.getY(), tpVec.z - this.mc.player.getZ());
      LeavesDamageUtil.RaycastFactory factory = (context, blockPos) -> {
         BlockState state;
         if (blockPos.equals(anchorPos)) {
            state = Blocks.AIR.defaultBlockState();
         } else if (blockPos.equals(protectPos)) {
            state = Blocks.OBSIDIAN.defaultBlockState();
         } else {
            state = this.mc.level.getBlockState(blockPos);
         }

         return state.getBlock().getExplosionResistance() < 600.0F
            ? null
            : state.getCollisionShape(this.mc.level, blockPos).clip(context.start(), context.end(), blockPos);
      };
      return LeavesDamageUtil.explosionDamage(this.mc.player, tpVec, box, anchorCenter, 10.0F, factory);
   }

   private boolean isBlinkVec(Vec3 vec) {
      if (this.mc.level == null) {
         return false;
      }

      BlockPos pos = BlockPos.containing(vec);
      return this.mc.level.getBlockState(pos).isAir() && this.mc.level.getBlockState(pos.above()).isAir();
   }

   private float selfDamageAt(Vec3 tpVec, Vec3 anchorCenter) {
      AABB box = this.mc.player.getBoundingBox().move(tpVec.x - this.mc.player.getX(), tpVec.y - this.mc.player.getY(), tpVec.z - this.mc.player.getZ());
      return LeavesDamageUtil.explosionDamage(
         this.mc.player,
         tpVec,
         box,
         anchorCenter,
         10.0F,
         LeavesDamageUtil.getOverridingHitFactory(BlockPos.containing(anchorCenter), Blocks.AIR.defaultBlockState())
      );
   }

   private void doTp(Vec3 from, Vec3 to) {
      TpSetting s = TpSetting.get();
      double dist = s != null ? s.getMoveDistance() : 128.0;
      boolean useLtp = s == null || s.isLTPUtils();
      if (useLtp) {
         LTpUtils.doTp(from, to, dist);
      } else {
         QTPUtils.doTp(from, to, dist);
      }
   }

   private boolean isValidTarget(Entity entity) {
      if (entity == null || entity == this.mc.player) {
         return false;
      }

      if (entity instanceof LivingEntity le) {
         if (!this.targetTypes.get().contains(entity.getType())) {
            return false;
         }

         if (!le.isDeadOrDying() && !(le.getHealth() <= 0.0F)) {
            if (entity instanceof Player p) {
               if (p.isCreative() || p.isSpectator()) {
                  return false;
               }

               if (!Friends.get().shouldAttack(p)) {
                  return false;
               }

               if (p instanceof FakePlayerEntity fp && fp.noHit) {
                  return false;
               }
            }

            return PlayerUtils.isWithin(entity, this.targetRange.get());
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private Vec3 getPredictedPos(Entity entity) {
      Vec3 pos = entity.position();

      return switch ((TpAnchorAura.PredictionMode)this.predictionMode.get()) {
         case Legacy -> pos.add(
            (entity.getX() - entity.xOld) * this.prevMultiplier.get(),
            (entity.getY() - entity.yOld) * this.prevMultiplier.get(),
            (entity.getZ() - entity.zOld) * this.prevMultiplier.get()
         );
         case PredictUtils -> {
            Vec3 p = PredictUtil.predictEntity(entity);
            yield p != null ? p : pos;
         }
         default -> pos;
      };
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.render.get()) {
         if (this.renderTpVec != null) {
            AABB box = new AABB(
               this.renderTpVec.x - 0.3,
               this.renderTpVec.y,
               this.renderTpVec.z - 0.3,
               this.renderTpVec.x + 0.3,
               this.renderTpVec.y + 1.8,
               this.renderTpVec.z + 0.3
            );
            event.renderer.box(box, this.tpColor.get(), this.tpColor.get(), this.shapeMode.get(), 0);
         }

         if (this.renderAnchorPos != null) {
            event.renderer.box(this.renderAnchorPos, this.anchorColor.get(), this.anchorColor.get(), this.shapeMode.get(), 0);
         }

         if (this.renderProtectPos != null) {
            event.renderer.box(this.renderProtectPos, this.protectColor.get(), this.protectColor.get(), this.shapeMode.get(), 0);
         }

         for (BlockPos gPos : this.renderGlowstonePositions) {
            event.renderer.box(gPos, this.glowstoneBlockColor.get(), this.glowstoneBlockColor.get(), this.shapeMode.get(), 0);
         }
      }
   }

   @Override
   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private record Data(BlockPos anchorPos, Vec3 tpVec, BlockPos protectPos, float selfDmg) {
   }

   public enum PredictionMode {
      Off,
      Legacy,
      PredictUtils;
   }

   public enum ProtectBlockType {
      GlowStone,
      Obsidian;
   }

   private record TpSlot(Vec3 vec, float selfDmg) {
   }
}
