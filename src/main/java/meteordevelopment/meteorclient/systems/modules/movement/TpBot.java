package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.List;
import java.util.Random;
import java.util.Set;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.math.BlockUtil;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class TpBot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgTweaks = this.settings.createGroup("Tweaks");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Entities to target.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<Boolean> useX = this.sgGeneral.add(new BoolSetting.Builder().name("use-x").defaultValue(true).build());
   private final Setting<Boolean> randomX = this.sgGeneral.add(new BoolSetting.Builder().name("random-x").defaultValue(false).visible(this.useX::get).build());
   private final Setting<Double> relativeX = this.sgGeneral
      .add(new DoubleSetting.Builder().name("relative-x").sliderRange(-25.0, 25.0).defaultValue(0.0).visible(this.useX::get).build());
   private final Setting<Boolean> useY = this.sgGeneral.add(new BoolSetting.Builder().name("use-y").defaultValue(true).build());
   private final Setting<Boolean> randomY = this.sgGeneral.add(new BoolSetting.Builder().name("random-y").defaultValue(false).visible(this.useY::get).build());
   private final Setting<Double> relativeY = this.sgGeneral
      .add(new DoubleSetting.Builder().name("relative-y").sliderRange(-25.0, 25.0).defaultValue(0.0).visible(this.useY::get).build());
   private final Setting<Boolean> useZ = this.sgGeneral.add(new BoolSetting.Builder().name("use-z").defaultValue(true).build());
   private final Setting<Boolean> randomZ = this.sgGeneral.add(new BoolSetting.Builder().name("random-z").defaultValue(false).visible(this.useZ::get).build());
   private final Setting<Double> relativeZ = this.sgGeneral
      .add(new DoubleSetting.Builder().name("relative-z").sliderRange(-25.0, 25.0).defaultValue(0.0).visible(this.useZ::get).build());
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to target. 0 means unlimited.")
            .min(0.0)
            .max(256.0)
            .sliderRange(0.0, 256.0)
            .defaultValue(0.0)
            .build()
      );
   private final Setting<Boolean> predict = this.sgGeneral
      .add(new BoolSetting.Builder().name("predict").description("Uses predicted target position.").defaultValue(false).build());
   private final Setting<Integer> delay = this.sgTweaks.add(new IntSetting.Builder().name("delay").min(0).sliderRange(0, 20).defaultValue(0).build());
   private final Setting<Boolean> ignoreFriends = this.sgTweaks.add(new BoolSetting.Builder().name("ignore-friends").defaultValue(true).build());
   private final Setting<Boolean> ignoreCreative = this.sgTweaks.add(new BoolSetting.Builder().name("ignore-creative").defaultValue(true).build());
   private final Setting<Double> diffLength = this.sgTweaks
      .add(
         new DoubleSetting.Builder()
            .name("diff-length")
            .description("Vertical search distance for a safe Y position.")
            .min(0.0)
            .sliderRange(0.0, 256.0)
            .defaultValue(40.0)
            .build()
      );
   private final Setting<Boolean> velocityMove = this.sgTweaks.add(new BoolSetting.Builder().name("velocity-move").defaultValue(true).build());
   private final Setting<Boolean> lowDistanceTp = this.sgTweaks
      .add(
         new BoolSetting.Builder()
            .name("low-distance-tp")
            .description("Uses direct TP when close to target in move mode.")
            .defaultValue(false)
            .visible(this.velocityMove::get)
            .build()
      );
   private final Setting<Double> tpDistance = this.sgTweaks
      .add(
         new DoubleSetting.Builder()
            .name("tp-distance")
            .description("Direct TP distance threshold.")
            .min(0.0)
            .max(100.0)
            .sliderRange(0.0, 100.0)
            .defaultValue(15.0)
            .visible(this.lowDistanceTp::get)
            .build()
      );
   private final Setting<Boolean> onlyName = this.sgTweaks
      .add(new BoolSetting.Builder().name("only-name").description("Only targets players in the name list.").defaultValue(false).build());
   private final Setting<List<String>> nameList = this.sgTweaks
      .add(new StringListSetting.Builder().name("name-list").description("Player names to target.").visible(this.onlyName::get).build());
   private final Random random = new Random();
   private Entity target;
   private int tickTimer;
   private Vec3 realPos;

   public TpBot() {
      super(Categories.Movement, "TPBot+", "Auto teleport to nearest player.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.realPos = null;
      this.tickTimer = this.delay.get();
   }

   @EventHandler(priority = 1199)
   private void onTickPre(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         Entity foundTarget = this.findTarget();
         if (this.target == null || this.isInvalidTarget(this.target)) {
            this.target = foundTarget;
         } else if (foundTarget != null && this.shouldReplaceTarget(foundTarget)) {
            this.target = foundTarget;
         }

         if (this.target == null) {
            this.tickTimer = this.delay.get();
            this.realPos = null;
         } else {
            Vec3 playerPos = this.mc.player.position();
            Vec3 targetPos = this.predict.get() ? PredictUtil.predictEntity(this.target) : this.target.position();
            Vec3 desiredPos = this.getDesiredPosition(playerPos, targetPos);
            if (this.tickTimer < this.delay.get()) {
               this.tickTimer++;
            } else {
               this.tickTimer = 0;
               this.realPos = desiredPos;
            }

            if (this.realPos == null) {
               this.realPos = desiredPos;
            }

            this.realPos = this.adjustY(playerPos, this.realPos);
            if (this.velocityMove.get()) {
               this.moveWithVelocity(playerPos, this.realPos);
            } else if (!this.samePos(playerPos, this.realPos)) {
               this.doTp(playerPos, this.realPos);
               this.mc.player.setPos(this.realPos.x, this.realPos.y, this.realPos.z);
            }
         }
      }
   }

   private Entity findTarget() {
      return TargetUtils.get(
         entity -> {
            if (entity == this.mc.player) {
               return false;
            } else if (!this.entities.get().contains(entity.getType())) {
               return false;
            } else if (this.range.get() > 0.0 && !PlayerUtils.isWithin(entity, this.range.get())) {
               return false;
            } else if (entity instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0.0F)) {
               return false;
            } else if (!(entity instanceof Player player)) {
               return !this.onlyName.get();
            } else if (this.ignoreFriends.get() && !Friends.get().shouldAttack(player)) {
               return false;
            } else {
               return this.ignoreCreative.get() && EntityUtils.getGameMode(player) == GameType.CREATIVE
                  ? false
                  : !this.onlyName.get() || this.isInNameList(player.getName().getString());
            }
         },
         SortPriority.LowestDistance
      );
   }

   private boolean isInvalidTarget(Entity entity) {
      if (!entity.isAlive()) {
         return true;
      } else if (entity instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0.0F)) {
         return true;
      } else if (!this.entities.get().contains(entity.getType())) {
         return true;
      } else if (this.range.get() > 0.0 && !PlayerUtils.isWithin(entity, this.range.get())) {
         return true;
      } else if (!(entity instanceof Player player)) {
         return this.onlyName.get();
      } else if (this.ignoreFriends.get() && !Friends.get().shouldAttack(player)) {
         return true;
      } else {
         return this.ignoreCreative.get() && EntityUtils.getGameMode(player) == GameType.CREATIVE
            ? true
            : this.onlyName.get() && !this.isInNameList(player.getName().getString());
      }
   }

   private boolean shouldReplaceTarget(Entity foundTarget) {
      return this.mc.player.distanceTo(foundTarget) < this.mc.player.distanceTo(this.target);
   }

   private Vec3 getDesiredPosition(Vec3 playerPos, Vec3 targetPos) {
      double x = this.getOffset(this.relativeX.get(), this.randomX.get());
      double y = this.getOffset(this.relativeY.get(), this.randomY.get());
      double z = this.getOffset(this.relativeZ.get(), this.randomZ.get());
      return new Vec3(
         this.useX.get() ? targetPos.x + x : playerPos.x, this.useY.get() ? targetPos.y + y : playerPos.y, this.useZ.get() ? targetPos.z + z : playerPos.z
      );
   }

   private double getOffset(double offset, boolean randomize) {
      return !randomize ? offset : offset - this.random.nextDouble() * offset * 2.0;
   }

   private Vec3 adjustY(Vec3 playerPos, Vec3 pos) {
      if (this.useY.get() && !this.isPlayerSafePos(BlockPos.containing(pos))) {
         Double y = this.findSafeY(BlockPos.containing(playerPos), BlockPos.containing(pos));
         return y == null ? pos : new Vec3(pos.x, y, pos.z);
      } else {
         return pos;
      }
   }

   private boolean isPlayerSafePos(BlockPos pos) {
      return BlockUtil.isSafeBlock(pos) && BlockUtil.isSafeBlock(pos.above());
   }

   private Double findSafeY(BlockPos from, BlockPos target) {
      int max = Math.max(0, this.diffLength.get().intValue());

      for (int i = 0; i <= max; i++) {
         BlockPos up = new BlockPos(target.getX(), target.getY() + i, target.getZ());
         if (this.isPlayerSafePos(up)) {
            return (double)up.getY();
         }

         BlockPos down = new BlockPos(target.getX(), target.getY() - i, target.getZ());
         if (down.getY() != up.getY() && this.isPlayerSafePos(down)) {
            return (double)down.getY();
         }
      }

      return this.isPlayerSafePos(from) ? (double)from.getY() : null;
   }

   private void moveWithVelocity(Vec3 playerPos, Vec3 pos) {
      double distance = this.getMoveModeSpeed();
      double playerToTargetDist = playerPos.distanceTo(pos);
      if (this.lowDistanceTp.get() && playerToTargetDist <= this.tpDistance.get()) {
         if (!this.samePos(playerPos, pos)) {
            this.doTp(playerPos, pos);
            this.mc.player.setPos(pos.x, pos.y, pos.z);
         }
      } else {
         Vec3 velocity = pos.subtract(playerPos);
         if (distance > 0.0 && playerToTargetDist > distance) {
            velocity = velocity.normalize().scale(distance);
         }

         this.mc.player.setDeltaMovement(velocity);
      }
   }

   private double getMoveModeSpeed() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveModeSpeed() : 5.0;
   }

   private double getMoveDistance() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveDistance() : 128.0;
   }

   private void doTp(Vec3 from, Vec3 to) {
      TpSetting setting = TpSetting.get();
      double moveDistance = this.getMoveDistance();
      if (setting != null && setting.isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDistance);
      } else {
         QTPUtils.doTp(from, to, moveDistance);
      }
   }

   private boolean samePos(Vec3 a, Vec3 b) {
      return a.distanceToSqr(b) < 1.0E-6;
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.realPos = null;
      this.tickTimer = this.delay.get();
   }

   @Override
   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   private boolean isInNameList(String playerName) {
      for (String name : this.nameList.get()) {
         if (playerName.equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }
}
