package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.math.BlockUtil;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TpWeb extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPrediction = this.settings.createGroup("Prediction");
   private final Setting<Double> targetRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("target-range")
            .description("The maximum distance to target players.")
            .defaultValue(40.0)
            .min(0.0)
            .max(128.0)
            .sliderMax(128.0)
            .build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to select the target."))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Boolean> doubles = this.sgGeneral
      .add(new BoolSetting.Builder().name("doubles").description("Places a second web above the target.").defaultValue(false).build());
   private final Setting<Boolean> inventorySwap = this.sgGeneral
      .add(new BoolSetting.Builder().name("inventory-swap").description("Uses webs from the inventory.").defaultValue(true).build());
   private final Setting<TpWeb.PredictionMode> predictionMode = this.sgPrediction
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("prediction-mode"))
                  .description("Target position prediction mode."))
               .defaultValue(TpWeb.PredictionMode.Off))
            .build()
      );
   private final Setting<Double> prevMultiplier = this.sgPrediction
      .add(
         new DoubleSetting.Builder()
            .name("prev")
            .description("Multiplier for legacy prediction.")
            .defaultValue(1.0)
            .min(0.0)
            .max(5.0)
            .sliderRange(0.0, 5.0)
            .visible(() -> this.predictionMode.get() == TpWeb.PredictionMode.Legacy)
            .build()
      );
   private Player target;

   public TpWeb() {
      super(Categories.Combat, "tp-web", "Automatically places webs on other players with TP.");
   }

   @Override
   public void onActivate() {
      this.target = null;
   }

   @Override
   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
            this.target = TargetUtils.getPlayerTarget(this.targetRange.get(), this.priority.get());
            if (TargetUtils.isBadTarget(this.target, this.targetRange.get())) {
               return;
            }
         }

         Vec3 predictedTargetPos = this.getPredictedTargetPos(this.target);
         if (predictedTargetPos != null) {
            BlockPos webPos = BlockPos.containing(predictedTargetPos);
            Vec3 playerPos = this.mc.player.position();
            Vec3 tpPos = this.findNearestPositionToPlaceWeb(webPos, playerPos);
            if (tpPos != null && BlockUtil.checkCanMove(playerPos, tpPos)) {
               this.doTp(playerPos, tpPos, this.mc.player.onGround());
               this.placeWebs(webPos, tpPos);
               this.doTp(tpPos, playerPos, this.mc.player.onGround());
            }
         }
      }
   }

   private Vec3 getPredictedTargetPos(Player player) {
      Vec3 pos = player.position();

      return switch ((TpWeb.PredictionMode)this.predictionMode.get()) {
         case Off -> pos;
         case Legacy -> pos.add(
            (player.getX() - player.xOld) * this.prevMultiplier.get(),
            (player.getY() - player.yOld) * this.prevMultiplier.get(),
            (player.getZ() - player.zOld) * this.prevMultiplier.get()
         );
         case PredictUtils -> PredictUtil.predictEntity(player);
      };
   }

   private Vec3 findNearestPositionToPlaceWeb(BlockPos targetPos, Vec3 playerPos) {
      Vec3 best = null;
      double bestDistance = Double.MAX_VALUE;
      Vec3 targetVec = Vec3.atLowerCornerOf(targetPos);

      for (int x = -1; x <= 1; x++) {
         for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
               if (x != 0 || y != 0 || z != 0) {
                  BlockPos offsetPos = targetPos.offset(x, y, z);
                  if (this.mc.level.getBlockState(offsetPos).isAir()) {
                     Vec3 tpPos = new Vec3(offsetPos.getX() + 0.5, offsetPos.getY(), offsetPos.getZ() + 0.5);
                     if (BlockUtil.isBlinkVec(tpPos) && BlockUtil.checkCanMove(playerPos, tpPos)) {
                        double distance = offsetPos.getCenter().distanceToSqr(targetVec);
                        if (distance < bestDistance) {
                           bestDistance = distance;
                           best = tpPos;
                        }
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private void placeWebs(BlockPos webPos, Vec3 tpPos) {
      FindItemResult webs = this.inventorySwap.get() ? InvUtils.find(Items.COBWEB) : InvUtils.findInHotbar(Items.COBWEB);
      if (webs.found()) {
         if (this.inventorySwap.get() && !webs.isHotbar() && !webs.isOffhand()) {
            int selectedSlot = this.mc.player.getInventory().getSelectedSlot();
            InvUtils.move().from(webs.slot()).toHotbar(selectedSlot);
            if (this.mc.player.getInventory().getItem(selectedSlot).is(Items.COBWEB)) {
               this.placeSelectedWebs(webPos, tpPos, InteractionHand.MAIN_HAND);
            }

            InvUtils.move().fromHotbar(selectedSlot).to(webs.slot());
         } else {
            this.placeWebs(webPos, webs, tpPos);
         }
      }
   }

   private void placeWebs(BlockPos webPos, FindItemResult webs, Vec3 tpPos) {
      if (webs.isOffhand()) {
         this.placeSelectedWebs(webPos, tpPos, InteractionHand.OFF_HAND);
      } else {
         int oldSlot = this.mc.player.getInventory().getSelectedSlot();
         if (InvUtils.swap(webs.slot(), false)) {
            this.placeSelectedWebs(webPos, tpPos, InteractionHand.MAIN_HAND);
            InvUtils.swap(oldSlot, false);
         }
      }
   }

   private void placeSelectedWebs(BlockPos webPos, Vec3 tpPos, InteractionHand hand) {
      this.placeBlock(webPos, tpPos, hand);
      if (this.doubles.get()) {
         this.placeBlock(webPos.above(), tpPos, hand);
      }
   }

   private boolean placeBlock(BlockPos pos, Vec3 tpPos, InteractionHand hand) {
      if (!this.mc.level.getBlockState(pos).canBeReplaced()) {
         return false;
      }

      Vec3 eyePos = tpPos.add(0.0, this.mc.player.getEyeHeight(this.mc.player.getPose()), 0.0);
      Direction side = BlockUtils.getClosestPlaceSide(pos, eyePos);
      if (side == null) {
         side = BlockUtils.getPlaceSide(pos);
      }

      if (side == null) {
         return false;
      }

      BlockPos neighbour = pos.relative(side);
      Vec3 hitPos = pos.getCenter().add(side.getStepX() * 0.5, side.getStepY() * 0.5, side.getStepZ() * 0.5);
      BlockHitResult hitResult = new BlockHitResult(hitPos, side.getOpposite(), neighbour, false);
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

   private void doTp(Vec3 from, Vec3 to, boolean onGround) {
      TpSetting setting = TpSetting.get();
      double moveDistance = setting != null ? setting.getMoveDistance() : 128.0;
      if (setting != null && setting.isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDistance);
      } else {
         QTPUtils.doTp(from, to, moveDistance, onGround);
      }
   }

   public enum PredictionMode {
      Off,
      Legacy,
      PredictUtils;
   }
}
