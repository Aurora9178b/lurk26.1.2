package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.Set;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
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
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.network.syncher.SynchedEntityData.DataValue;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BowAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<BowAura.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Which mode to use."))
               .defaultValue(BowAura.Mode.Vanilla))
            .build()
      );
   private final Setting<Set<EntityType<?>>> targetEntity = this.sgGeneral
      .add(
         new EntityTypeListSetting.Builder().name("target-entity").description("Entities to target.").onlyAttackable().defaultValue(EntityType.PLAYER).build()
      );
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to prioritize targets."))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Boolean> attackFriends = this.sgGeneral
      .add(new BoolSetting.Builder().name("attack-friends").description("Allow attacking friends.").defaultValue(false).build());
   private final Setting<Boolean> onlyTarget = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-target").description("Only accelerate when target is found.").defaultValue(false).build());
   private final Setting<Double> range = this.sgGeneral
      .add(new DoubleSetting.Builder().name("range").description("Range to find targets.").defaultValue(50.0).min(0.0).sliderMax(200.0).build());
   private final Setting<Boolean> fastBow = this.sgGeneral
      .add(new BoolSetting.Builder().name("fast-bow").description("Auto release bow after draw.").defaultValue(true).build());
   private final Setting<Integer> fastBowDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("Draw time before auto-release (ticks).")
            .defaultValue(3)
            .min(0)
            .sliderRange(0, 20)
            .visible(() -> this.fastBow.get())
            .build()
      );
   private final Setting<Boolean> autoRotate = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-rotate")
            .description("Automatically rotate towards target.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == BowAura.Mode.Vanilla)
            .build()
      );
   private final Setting<Boolean> predict = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("predict")
            .description("Predict target movement.")
            .defaultValue(false)
            .visible(() -> this.autoRotate.get() || this.mode.get() == BowAura.Mode.New)
            .build()
      );
   private final Setting<Boolean> missTotem = this.sgGeneral
      .add(new BoolSetting.Builder().name("try-miss").description("Change teleport distance on miss.").defaultValue(false).build());
   private final Setting<Integer> maxMiss = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-miss")
            .description("Max miss count before reset.")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .visible(() -> this.missTotem.get())
            .build()
      );
   private final Setting<Double> rY = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("relative-y")
            .description("Relative Y height above target for New mode.")
            .defaultValue(0.5)
            .min(0.0)
            .sliderMax(5.0)
            .visible(() -> this.missTotem.get() && this.mode.get() == BowAura.Mode.New)
            .build()
      );
   final Setting<Double> arrowDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("arrow-distance")
            .description("Distance to teleport for arrows.")
            .defaultValue(20.0)
            .min(0.0)
            .sliderMax(128.0)
            .build()
      );
   private final Setting<Integer> packetLimit = this.sgGeneral
      .add(new IntSetting.Builder().name("packet-limit").description("Maximum number of packets to send.").defaultValue(10).min(1).sliderMax(100).build());
   private final Setting<Boolean> checkHeadBlock = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("check-head-block")
            .description("Check if target has block above head in New mode.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == BowAura.Mode.New)
            .build()
      );
   private final Setting<Boolean> autoMode = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-mode")
            .description("Switch to Old mode if target has block above head.")
            .defaultValue(false)
            .visible(() -> this.checkHeadBlock.get() && this.mode.get() == BowAura.Mode.New)
            .build()
      );
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Boolean> tpRender = this.sgRender
      .add(new BoolSetting.Builder().name("tp-render").description("Render TP positions.").defaultValue(true).build());
   private final Setting<ShapeMode> tpShapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("tp-shape-mode"))
                     .description("How the TP position is rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.tpRender::get))
            .build()
      );
   private final Setting<SettingColor> tpSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tp-side-color")
            .description("The side color of the TP position rendering.")
            .defaultValue(new SettingColor(0, 255, 255, 80))
            .visible(() -> this.tpRender.get() && this.tpShapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> tpLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tp-line-color")
            .description("The line color of the TP position rendering.")
            .defaultValue(new SettingColor(0, 255, 255, 255))
            .visible(() -> this.tpRender.get() && this.tpShapeMode.get().lines())
            .build()
      );
   private final Setting<Boolean> predictRender = this.sgRender
      .add(new BoolSetting.Builder().name("predict-render").description("Render prediction box.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("predict-shape-mode"))
                     .description("How the prediction box is rendered."))
                  .defaultValue(ShapeMode.Lines))
               .visible(this.predictRender::get))
            .build()
      );
   private final Setting<SettingColor> predictColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("predict-color")
            .description("Color of the prediction box.")
            .defaultValue(new SettingColor(225, 25, 25, 255))
            .visible(this.predictRender::get)
            .build()
      );
   private int missCount = 0;
   private long lastShootTime = 0L;
   private Player lastTarget = null;
   private float lastYaw = 0.0F;
   private float lastPitch = 0.0F;
   private int drawTime = 0;
   private int sequenceId = 0;
   private Vec3 tpPosition = null;
   private Vec3 lastTpPosition = null;
   private Vec3 newModeAcceleratePos = null;
   private Vec3 newModeShootPos = null;
   private Vec3 expectedTpPosition = null;
   private boolean modifyingPacket;
   public boolean inPacketMove = false;

   public BowAura() {
      super(Categories.Combat, "BowAura", "32k.");
   }

   private boolean isUseTrue(Item item) {
      return false;
   }

   private double getDistanceForItem(Item item) {
      if (this.missTotem.get()) {
         return this.arrowDistance.get() + this.missCount * 10;
      } else {
         return item == Items.BOW ? this.arrowDistance.get() : 20.0;
      }
   }

   private Vec3 getRotationTarget(Entity target) {
      if (this.predict.get() && target instanceof Player player) {
         Vec3 predicted = PredictUtil.predictEntity(player);
         if (predicted != null) {
            return predicted.add(0.0, target.getEyeHeight(target.getPose()), 0.0);
         }
      }

      return target.getEyePosition();
   }

   private void applyRotation(Entity target) {
      if (this.autoRotate.get() || this.missTotem.get() || this.predict.get()) {
         if (target != null) {
            Vec3 predictedPos = this.getPredictedPosition(target);
            if (predictedPos != null) {
               this.lastYaw = this.mc.player.getYRot();
               this.lastPitch = this.mc.player.getXRot();
               float yaw = (float)Rotations.getYaw(predictedPos);
               float pitch = (float)Rotations.getPitch(predictedPos);
               this.mc.player.setYRot(yaw);
               this.mc.player.setXRot(pitch);
            }
         }
      }
   }

   private Vec3 getPredictedPosition(Entity target) {
      if (!this.predict.get()) {
         return target.getEyePosition();
      }

      Vec3 predicted = PredictUtil.predictEntity(target);
      return predicted != null ? predicted.add(0.0, target.getEyeHeight(), 0.0) : target.getEyePosition();
   }

   private Vec3 getLookVector(float yaw, float pitch) {
      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);
      return new Vec3(-Math.sin(yawRad) * Math.cos(pitchRad), -Math.sin(pitchRad), Math.cos(yawRad) * Math.cos(pitchRad));
   }

   private Entity getTarget() {
      return this.mc.player != null && this.mc.level != null ? TargetUtils.get(entity -> {
         if (!this.targetEntity.get().contains(entity.getType())) {
            return false;
         } else if (entity == this.mc.player) {
            return false;
         } else if (entity instanceof LivingEntity living && (living.isDeadOrDying() || living.getHealth() <= 0.0F)) {
            return false;
         } else if (!PlayerUtils.isWithin(entity, this.range.get())) {
            return false;
         } else if (entity instanceof Player player && !this.attackFriends.get() && !Friends.get().shouldAttack(player)) {
            return false;
         } else {
            return entity instanceof FakePlayerEntity fakePlayer ? !fakePlayer.noHit : true;
         }
      }, this.priority.get()) : null;
   }

   private boolean hasBlockAboveHead(Entity target) {
      if (target != null && this.mc.level != null) {
         BlockPos headPos = target.blockPosition().above(2);
         return !this.mc.level.getBlockState(headPos).isAir();
      } else {
         return false;
      }
   }

   private int calculateRequiredPackets(Vec3 from, double distance) {
      return TpSetting.get() == null ? (int)Math.ceil(distance / 128.0) : (int)Math.ceil(distance / TpSetting.get().getMoveDistance());
   }

   private void checkShootResult(Player target) {
      if (this.missTotem.get()) {
         this.missCount++;
         if (this.missCount > this.maxMiss.get()) {
            this.missCount = 1;
         }
      }
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      if (!this.fastBow.get()) {
         this.drawTime = 0;
      } else if (this.mc.player != null && this.mc.level != null) {
         if (this.drawTime >= this.fastBowDelay.get() && this.mc.options.keyUse.isDown() && this.mc.player.getMainHandItem().getItem() == Items.BOW) {
            this.mc.player.connection.send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, this.mc.player.getDirection()));
            this.mc
               .player
               .connection
               .send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, this.sequenceId, this.mc.player.getYRot(), this.mc.player.getXRot()));
         }

         this.drawTime++;
      } else {
         this.drawTime = 0;
      }
   }

   @EventHandler(priority = 200)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundSetEntityDataPacket packet && packet.id() == this.mc.player.getId()) {
         for (DataValue<?> data : packet.packedItems()) {
            if (data.id() == 8 && data.value() instanceof Number ptValue && ptValue.byteValue() == 0 && this.mc.player.getMainHandItem().getItem() == Items.BOW
               )
             {
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundUseItemPacket packet && this.mc.player.getMainHandItem().getItem() == Items.BOW) {
         this.drawTime = 0;
         this.sequenceId = packet.getSequence();
      }

      if (event.packet instanceof ServerboundPlayerActionPacket packet && packet.getAction() == Action.RELEASE_USE_ITEM) {
         Item mainItem = this.mc.player.getMainHandItem().getItem();
         Item offItem = this.mc.player.getOffhandItem().getItem();
         if (this.modifyingPacket || mainItem != Items.BOW && offItem != Items.BOW) {
            return;
         }

         if (!this.mc.options.keyUse.isDown()) {
            this.missCount = 0;
         }

         if (System.currentTimeMillis() - this.lastShootTime < 50L) {
            return;
         }

         this.lastShootTime = System.currentTimeMillis();
         Vec3 pPos = this.mc.player.position();
         Item item = mainItem == Items.BOW ? mainItem : offItem;
         double distance = this.getDistanceForItem(item);
         int requiredPackets = this.calculateRequiredPackets(pPos, distance);
         if (requiredPackets > this.packetLimit.get()) {
            ChatUtils.info("TP Packet limit exceeded: " + requiredPackets + " packets");
            return;
         }

         Entity target = this.getTarget();
         if (this.onlyTarget.get() && target == null) {
            return;
         }

         double[] angles = null;
         boolean useOldMode = this.mode.get() == BowAura.Mode.Vanilla
            || this.mode.get() == BowAura.Mode.New && this.checkHeadBlock.get() && this.autoMode.get() && target != null && this.hasBlockAboveHead(target);
         if (useOldMode
            && (this.autoRotate.get() || this.missTotem.get() || this.predict.get())
            && target != null
            && (this.autoRotate.get() || this.predict.get())) {
            Vec3 predictedPos = this.getPredictedPosition(target);
            if (predictedPos != null) {
               angles = new double[]{Rotations.getYaw(predictedPos), Rotations.getPitch(predictedPos)};
            }
         }

         if (this.missTotem.get() && target instanceof Player playerTarget) {
            this.lastTarget = playerTarget;
            this.checkShootResult(playerTarget);
         }

         Vec3 lookVec;
         if (angles != null) {
            lookVec = this.getLookVector((float)angles[0], (float)angles[1]);
         } else {
            lookVec = this.mc.player.getLookAngle();
         }

         Vec3 tP = pPos.add(lookVec.normalize().scale(-distance));
         if (this.mode.get() == BowAura.Mode.New && target != null) {
            if (this.mode.get() == BowAura.Mode.Vanilla && this.checkHeadBlock.get() && this.hasBlockAboveHead(target)) {
               if (!this.autoMode.get()) {
                  ChatUtils.info("Invalid attack position!");
                  return;
               }

               ChatUtils.info("Invalid attack position, switched to Old mode!");
               this.modifyingPacket = true;
               this.doTpWithTpSetting(pPos, tP);
               this.doTpWithTpSetting(pPos, pPos);
               this.mc.getConnection().send(packet);
               if (target != null && (this.autoRotate.get() || this.missTotem.get() || this.predict.get())) {
                  this.mc.player.setYRot(this.lastYaw);
                  this.mc.player.setXRot(this.lastPitch);
               }

               this.modifyingPacket = false;
               event.cancel();
               return;
            }

            Vec3 targetPos;
            if (this.predict.get()) {
               Vec3 predicted = this.getPredictedPosition(target);
               targetPos = new Vec3(predicted.x, target.getEyePosition().y, predicted.z);
            } else {
               targetPos = target.getEyePosition();
            }

            Vec3 aboveTarget = new Vec3(targetPos.x, targetPos.y + this.rY.get(), targetPos.z);
            double newDistance = this.getDistanceForItem(item);
            Vec3 newTP = aboveTarget.add(0.0, newDistance, 0.0);
            this.modifyingPacket = true;
            float yaw = this.mc.player.getYRot();
            float pitch = this.mc.player.getXRot();
            this.doTpWithTpSetting(pPos, aboveTarget);
            this.doTpWithTpSetting(aboveTarget, newTP);
            this.doTpWithTpSetting(aboveTarget, aboveTarget);
            this.mc.getConnection().send(packet);

            for (int i = 1; i <= 6; i++) {
               this.doTpWithTpSetting(aboveTarget, aboveTarget);
            }

            this.doTpWithTpSetting(aboveTarget, pPos);
            this.mc.player.setYRot(yaw);
            this.mc.player.setXRot(pitch);
            if (this.tpRender.get()) {
               this.tpPosition = aboveTarget;
               this.newModeAcceleratePos = aboveTarget;
               this.newModeShootPos = newTP;
            }

            this.modifyingPacket = false;
            event.cancel();
            return;
         }

         this.modifyingPacket = true;
         float originalYaw = this.mc.player.getYRot();
         float originalPitch = this.mc.player.getXRot();
         if (angles != null) {
            this.doTpWithTpSetting(pPos, tP);
            this.doTpWithTpSetting(pPos, pPos);
         } else {
            this.doTpWithTpSetting(pPos, tP);
            this.doTpWithTpSetting(pPos, pPos);
         }

         this.mc.getConnection().send(packet);
         if (angles != null) {
            this.doTpWithTpSetting(pPos, pPos);
         }

         if (this.tpRender.get()) {
            this.tpPosition = tP;
            this.lastTpPosition = tP;
         }

         this.modifyingPacket = false;
         event.cancel();
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (this.predict.get() && this.predictRender.get()) {
            Entity target = this.getTarget();
            if (target != null) {
               Vec3 predictedPos = PredictUtil.predictEntity(target);
               if (predictedPos != null) {
                  AABB originalAABB = target.getBoundingBox();
                  double width = originalAABB.getXsize();
                  double height = originalAABB.getYsize();
                  double depth = originalAABB.getZsize();
                  AABB predictedAABB = new AABB(
                     predictedPos.x - width / 2.0,
                     predictedPos.y,
                     predictedPos.z - depth / 2.0,
                     predictedPos.x + width / 2.0,
                     predictedPos.y + height,
                     predictedPos.z + depth / 2.0
                  );
                  event.renderer.box(predictedAABB, this.predictColor.get(), this.predictColor.get(), this.shapeMode.get(), 0);
               }
            }
         }

         if (this.tpRender.get() && this.tpPosition != null) {
            AABB tpAABB = new AABB(
               this.tpPosition.x - 0.3, this.tpPosition.y, this.tpPosition.z - 0.3, this.tpPosition.x + 0.3, this.tpPosition.y + 1.8, this.tpPosition.z + 0.3
            );
            event.renderer.box(tpAABB, this.tpSideColor.get(), this.tpLineColor.get(), this.tpShapeMode.get(), 0);
         }

         if (this.tpRender.get() && this.mode.get() == BowAura.Mode.New && this.newModeShootPos != null) {
            AABB shootAABB = new AABB(
               this.newModeShootPos.x - 0.3,
               this.newModeShootPos.y,
               this.newModeShootPos.z - 0.3,
               this.newModeShootPos.x + 0.3,
               this.newModeShootPos.y + 1.8,
               this.newModeShootPos.z + 0.3
            );
            event.renderer.box(shootAABB, this.tpSideColor.get(), this.tpLineColor.get(), this.tpShapeMode.get(), 0);
         }
      }
   }

   private void doTpWithTpSetting(Vec3 from, Vec3 to) {
      TpSetting setting = TpSetting.get();
      if (setting != null) {
         if (from != null && to != null) {
            double dx = Math.abs(from.x - to.x);
            double dz = Math.abs(from.z - to.z);
            if (dx < 0.001 && dz < 0.001) {
               double moveDistance = setting.getMoveDistance();
               if (setting.isLTPUtils()) {
                  LTpUtils.doTp(from, to, moveDistance);
               } else {
                  QTPUtils.doTp(from, to, moveDistance);
               }
            } else {
               double totalDistance = from.distanceTo(to);
               if (!(totalDistance <= 0.001)) {
                  int steps = (int)Math.ceil(totalDistance / 10.0);
                  steps = Math.max(1, steps);

                  for (int i = 0; i < steps; i++) {
                     double progress = (i + 1.0) / steps;
                     Vec3 stepPos = from.lerp(to, progress);
                     if (setting.isLTPUtils()) {
                        LTpUtils.sendMovePacket(stepPos, true);
                     } else {
                        QTPUtils.sendMovePacket(stepPos, true);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onActivate() {
      this.missCount = 0;
      this.lastShootTime = 0L;
      this.lastTarget = null;
      this.drawTime = 0;
      this.sequenceId = 0;
      this.tpPosition = null;
      this.lastTpPosition = null;
      this.newModeAcceleratePos = null;
      this.newModeShootPos = null;
   }

   @Override
   public void onDeactivate() {
      this.modifyingPacket = false;
      this.inPacketMove = false;
      this.missCount = 0;
      this.drawTime = 0;
      this.sequenceId = 0;
      this.tpPosition = null;
      this.lastTpPosition = null;
      this.newModeAcceleratePos = null;
      this.newModeShootPos = null;
   }

   public enum Mode {
      Vanilla,
      New;
   }
}
