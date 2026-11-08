package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.ArrayList;
import java.util.List;
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
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class GTPBot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Set<EntityType<?>>> targetEntities = this.sgGeneral
      .add(
         new EntityTypeListSetting.Builder()
            .name("target-entities")
            .description("Entities to teleport to.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
      );
   private final Setting<Double> triggerRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Maximum distance to target to trigger teleport (0 = unlimited).")
            .defaultValue(128.0)
            .min(0.0)
            .max(1024.0)
            .sliderRange(0.0, 1024.0)
            .build()
      );
   private final Setting<Boolean> onlyXZ = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-xz").description("Only teleport X and Z position").defaultValue(false).build());
   private final Setting<Boolean> onlyName = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-name").description("Only target players in the name list.").defaultValue(false).build());
   private final Setting<List<String>> nameList = this.sgGeneral
      .add(new StringListSetting.Builder().name("name-list").description("List of player names to target.").visible(this.onlyName::get).build());
   private final Setting<Double> keepY = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("keep-y")
            .description("Keep Y offset from target.")
            .defaultValue(0.0)
            .min(0.0)
            .max(150.0)
            .sliderRange(0.0, 150.0)
            .visible(() -> !this.onlyXZ.get())
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("Teleport delay in milliseconds.")
            .defaultValue(40)
            .min(1)
            .max(2000)
            .sliderMin(1)
            .sliderMax(2000)
            .build()
      );
   private final Setting<Double> prev = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("prev")
            .description("Prediction offset based on previous position.")
            .defaultValue(0.0)
            .min(0.0)
            .max(5.0)
            .sliderRange(0.0, 5.0)
            .build()
      );
   private final Setting<Boolean> random = this.sgGeneral
      .add(new BoolSetting.Builder().name("random").description("Randomly select target from list.").defaultValue(false).build());
   private Entity target;
   private long lastTpTime = 0L;

   public GTPBot() {
      super(Categories.Movement, "GTPBot", "Teleport to player");
   }

   private void teleport(Vec3 from, Vec3 to, double maxDist, boolean onGround) {
      double distance = from.distanceTo(to);
      if (!(distance <= 0.001)) {
         int steps = (int)Math.ceil(distance / maxDist);
         if (steps <= 0) {
            steps = 1;
         }

         for (int i = 0; i < steps; i++) {
            double progress = (i + 1.0) / steps;
            Vec3 stepPos = from.lerp(to, progress);
            LTpUtils.sendMovePacket(stepPos, onGround);
         }

         LTpUtils.sendMovePacket(to, onGround);
      }
   }

   private void updateTarget() {
      if (this.random.get()) {
         List<Entity> targets = new ArrayList<>();
         TargetUtils.getList(targets, entity -> {
            if (entity == this.mc.player) {
               return false;
            } else if (entity instanceof LivingEntity living && living.isDeadOrDying() || !entity.isAlive()) {
               return false;
            } else {
               if (!this.targetEntities.get().contains(entity.getType())) {
                  return false;
               }

               if (entity instanceof Player player) {
                  if (!Friends.get().shouldAttack(player)) {
                     return false;
                  }

                  if (this.onlyName.get() && !this.isInNameList(player.getName().getString())) {
                     return false;
                  }
               }

               return true;
            }
         }, SortPriority.LowestDistance, Integer.MAX_VALUE);
         if (!targets.isEmpty()) {
            this.target = targets.get((int)Math.floor(Math.random() * targets.size()));
         } else {
            this.target = null;
         }
      } else {
         this.target = TargetUtils.get(entity -> {
            if (entity == this.mc.player) {
               return false;
            } else if (entity instanceof LivingEntity living && living.isDeadOrDying() || !entity.isAlive()) {
               return false;
            } else {
               if (!this.targetEntities.get().contains(entity.getType())) {
                  return false;
               }

               if (entity instanceof Player player) {
                  if (!Friends.get().shouldAttack(player)) {
                     return false;
                  }

                  if (this.onlyName.get() && !this.isInNameList(player.getName().getString())) {
                     return false;
                  }
               }

               return true;
            }
         }, SortPriority.LowestDistance);
      }
   }

   private boolean isInNameList(String playerName) {
      for (String name : this.nameList.get()) {
         if (playerName.equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.target != null && !(this.target instanceof LivingEntity living && living.isDeadOrDying()) && this.target.isAlive()) {
         Vec3 tPos = this.target.position();
         if (tPos != null && this.mc.player != null) {
            double range = this.triggerRange.get();
            if (!(range > 0.0) || !(this.mc.player.position().distanceTo(tPos) > range)) {
               if (this.target instanceof Player && this.prev.get() > 0.0) {
                  double motionX = this.target.getX() - this.target.xOld;
                  double motionY = this.target.getY() - this.target.yOld;
                  double motionZ = this.target.getZ() - this.target.zOld;
                  tPos = tPos.add(motionX * this.prev.get(), motionY * this.prev.get(), motionZ * this.prev.get());
               }

               Vec3 keepYPos = new Vec3(tPos.x, this.onlyXZ.get() ? this.mc.player.getY() : tPos.y + this.keepY.get(), tPos.z);
               if (!this.onlyXZ.get() && !BlockUtil.isSafeBlock(BlockPos.containing(keepYPos))) {
                  keepYPos = tPos;
               }

               double targetX = tPos.x;
               double targetY = keepYPos.y;
               double targetZ = tPos.z;
               if (System.currentTimeMillis() - this.lastTpTime >= this.delay.get().intValue()) {
                  Vec3 newP = new Vec3(targetX, targetY, targetZ);
                  this.teleport(this.mc.player.position(), newP, this.getMoveDistance(), this.mc.player.onGround());
                  this.mc.player.setPos(newP);
                  this.lastTpTime = System.currentTimeMillis();
               }
            }
         }
      } else {
         this.updateTarget();
      }
   }

   @Override
   public void onActivate() {
      this.updateTarget();
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      if (this.mc.player != null) {
         LTpUtils.sendMovePacket(this.mc.player.position(), this.mc.player.onGround());
      }
   }

   @Override
   public String getInfoString() {
      return EntityUtils.getName(this.target);
   }

   private double getMoveDistance() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveDistance() : 128.0;
   }
}
