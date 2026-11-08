package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.math.BlockUtil;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TpMaceAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgHelper = this.settings.createGroup("Helper");
   private final SettingGroup sgMace = this.settings.createGroup("Mace");
   private final SettingGroup sgSpeed = this.settings.createGroup("Attack Speed");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<Boolean> swing = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing").description("Swings your hand when attacking.").defaultValue(true).build());
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Range is 0.0, then infinity range.")
            .min(0.0)
            .sliderRange(0.0, 256.0)
            .defaultValue(200.0)
            .build()
      );
   private final Setting<Boolean> autoSwap = this.sgHelper
      .add(new BoolSetting.Builder().name("auto-swap").description("Auto swaps item to attack target.").defaultValue(true).build());
   private final Setting<Item> swapItem = this.sgHelper
      .add(new ItemSetting.Builder().name("swap-item").description("Auto swap item used to attack target.").defaultValue(Items.NETHERITE_SWORD).build());
   private final Setting<Boolean> usePredict = this.sgHelper
      .add(new BoolSetting.Builder().name("predict").description("Uses target movement prediction.").defaultValue(false).build());
   private final Setting<Double> safeRange = this.sgHelper
      .add(
         new DoubleSetting.Builder()
            .name("safe-range")
            .description("Radius used to search a safe teleport position.")
            .min(0.0)
            .sliderRange(0.0, 5.0)
            .defaultValue(3.0)
            .build()
      );
   private final Setting<Boolean> useMace = this.sgMace
      .add(new BoolSetting.Builder().name("use-mace").description("Uses mace vertical clip attacks.").defaultValue(true).build());
   private final Setting<List<Double>> missClip = this.sgMace
      .add(
         new DoubleListSetting.Builder()
            .name("miss-clip")
            .description("Vertical clip offsets used for mace attacks.")
            .min(0.0)
            .sliderRange(0.0, 1000.0)
            .defaultValue(20.0, 40.0, 60.0, 80.0, 100.0, 120.0, 140.0, 160.0, 180.0, 200.0)
            .visible(this.useMace::get)
            .build()
      );
   private final Setting<Integer> attackTimes = this.sgSpeed
      .add(new IntSetting.Builder().name("attack-times").description("Attack times per cycle.").min(1).sliderRange(1, 128).defaultValue(1).build());
   private final Setting<Integer> attackDelay = this.sgSpeed
      .add(new IntSetting.Builder().name("attack-delay").description("Ticks to wait before attacking.").min(0).sliderRange(0, 128).defaultValue(0).build());
   private Entity target;
   private int tickTimer;

   public TpMaceAura() {
      super(Categories.Combat, "TpMaceAura", "MaceAura.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.tickTimer = this.attackDelay.get();
   }

   @Override
   public void onDeactivate() {
      this.target = null;
   }

   @EventHandler(priority = -200)
   private void onTickPost(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         Entity found = this.findTarget();
         this.target = found;
         if (this.target == null) {
            this.tickTimer = this.attackDelay.get();
         } else {
            Vec3 originalPlayerPos = this.mc.player.position();
            Vec3 targetPos = this.getTargetPosition(this.target);
            Vec3 safeTargetPos = this.getSafeTargetPosition(targetPos);
            if (this.tickTimer < this.attackDelay.get()) {
               this.tickTimer++;
            } else {
               this.tickTimer = 0;

               for (int i = 0; i < this.attackTimes.get(); i++) {
                  this.attackCycle(originalPlayerPos, safeTargetPos);
               }
            }
         }
      }
   }

   @Override
   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   private Entity findTarget() {
      return TargetUtils.get(entity -> {
         if (entity != null && entity != this.mc.player && entity != this.mc.getCameraEntity()) {
            if (!this.entities.get().contains(entity.getType())) {
               return false;
            } else if (this.range.get() > 0.0 && !this.isInRange(this.mc.player.position(), entity.getBoundingBox(), this.range.get())) {
               return false;
            } else if (entity instanceof LivingEntity living && (living.isDeadOrDying() || !living.isAlive() || living.getHealth() <= 0.0F)) {
               return false;
            } else if (!(entity instanceof Player player)) {
               return true;
            } else if (player.isCreative() || player.isSpectator()) {
               return false;
            } else {
               return !Friends.get().shouldAttack(player) ? false : !(player instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit);
            }
         } else {
            return false;
         }
      }, SortPriority.LowestDistance);
   }

   private void attackCycle(Vec3 originalPlayerPos, Vec3 targetPos) {
      this.doHorizontalTp(originalPlayerPos, targetPos, false);
      this.attackWithSwap();
      this.attackWithMace(targetPos);
      this.doHorizontalTp(targetPos, originalPlayerPos, false);
   }

   private void attackWithSwap() {
      boolean swapped = false;
      if (this.autoSwap.get()) {
         FindItemResult item = InvUtils.find(this.swapItem.get());
         if (item.found()) {
            swapped = InvUtils.swap(item.slot(), true);
         }
      }

      this.attackTarget();
      if (swapped) {
         InvUtils.swapBack();
      }
   }

   private void attackWithMace(Vec3 pos) {
      if (this.useMace.get()) {
         FindItemResult mace = InvUtils.find(Items.MACE);
         if (mace.found()) {
            boolean swapped = InvUtils.swap(mace.slot(), true);

            for (double clip : this.missClip.get()) {
               Vec3 newPos = pos.add(0.0, clip, 0.0);
               this.doNormalTp(pos, newPos, false);
               this.doNormalTp(newPos, pos, false);
               this.attackTarget();
            }

            Vec3 zeroPos = pos.add(0.0, 1.0E-4, 0.0);
            this.doNormalTp(pos, zeroPos, false);
            this.doNormalTp(zeroPos, pos, false);
            this.attackTarget();
            if (swapped) {
               InvUtils.swapBack();
            }
         }
      }
   }

   private void attackTarget() {
      if (this.target != null && this.mc.player != null && this.mc.gameMode != null) {
         this.mc.gameMode.attack(this.mc.player, this.target);
         if (this.swing.get()) {
            this.mc.player.swing(InteractionHand.MAIN_HAND);
         }
      }
   }

   private Vec3 getTargetPosition(Entity entity) {
      if (!this.usePredict.get()) {
         return entity.position();
      }

      Vec3 predicted = PredictUtil.predictEntity(entity);
      return predicted != null ? predicted : entity.position();
   }

   private Vec3 getSafeTargetPosition(Vec3 targetPos) {
      BlockPos blockTargetPos = BlockPos.containing(targetPos);
      if (this.isPlayerSafe(blockTargetPos)) {
         return targetPos;
      }

      Vec3 safePos = this.findSafePosition(blockTargetPos, this.safeRange.get());
      return safePos != null ? safePos : targetPos;
   }

   private Vec3 findSafePosition(BlockPos center, double radius) {
      int r = (int)Math.ceil(radius);
      Vec3 best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = center.offset(x, y, z);
               if (this.isPlayerSafe(pos)) {
                  Vec3 vec = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                  double distance = vec.distanceToSqr(Vec3.atLowerCornerOf(center));
                  if (distance < bestDistance) {
                     bestDistance = distance;
                     best = vec;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean isPlayerSafe(BlockPos pos) {
      return this.mc.level != null && BlockUtil.isSafeBlock(pos) && BlockUtil.isSafeBlock(pos.above());
   }

   private boolean isInRange(Vec3 point, AABB box, double range) {
      double x = this.clamp(point.x, box.minX, box.maxX);
      double y = this.clamp(point.y, box.minY, box.maxY);
      double z = this.clamp(point.z, box.minZ, box.maxZ);
      return point.distanceToSqr(x, y, z) <= range * range;
   }

   private double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   private void doNormalTp(Vec3 from, Vec3 to, boolean onGround) {
      TpSetting setting = TpSetting.get();
      double distance = this.getMoveDistance();
      if (!(distance <= 0.0)) {
         if (setting != null && setting.isLTPUtils()) {
            LTpUtils.doTp(from, to, distance);
         } else {
            QTPUtils.doTp(from, to, distance, onGround);
         }
      }
   }

   private void doHorizontalTp(Vec3 from, Vec3 to, boolean onGround) {
      double distance = from.distanceTo(to);
      if (!(distance <= 0.001)) {
         int steps = (int)Math.ceil(distance / 10.0);
         steps = Math.max(1, steps);
         TpSetting setting = TpSetting.get();
         boolean useLtp = setting == null || setting.isLTPUtils();

         for (int i = 0; i < steps; i++) {
            double progress = (i + 1.0) / steps;
            Vec3 stepPos = from.lerp(to, progress);
            if (useLtp) {
               LTpUtils.sendMovePacket(stepPos, onGround);
            } else {
               QTPUtils.sendMovePacket(stepPos, onGround);
            }
         }

         if (useLtp) {
            LTpUtils.sendMovePacket(to, onGround);
         } else {
            QTPUtils.sendMovePacket(to, onGround);
         }
      }
   }

   private double getMoveDistance() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveDistance() : 128.0;
   }
}
