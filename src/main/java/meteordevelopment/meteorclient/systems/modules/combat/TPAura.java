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
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TPAura extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final SettingGroup sgPrediction = this.settings.createGroup("Prediction");
   private final SettingGroup sgMace = this.settings.createGroup("Mace");
   private final SettingGroup sgSpeed = this.settings.createGroup("Attack Speed");
   private final Setting<Set<EntityType<?>>> targetTypes = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("target-type").description("Entities to attack.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("Entity sorting priority."))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Double> range = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("range")
            .description("Max distance of a target.")
            .defaultValue(128.0)
            .min(1.0)
            .max(200.0)
            .sliderRange(1.0, 200.0)
            .build()
      );
   private final Setting<Boolean> swingHand = this.sgGeneral
      .add(new BoolSetting.Builder().name("swing-hand").description("Swings client-side after attacking.").defaultValue(false).build());
   private final Setting<Boolean> useMace = this.sgGeneral
      .add(new BoolSetting.Builder().name("use-mace").description("Switches to mace before attacking.").defaultValue(false).build());
   private final Setting<Integer> attackTimes = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("attack-times")
            .description("Number of attacks per teleport.")
            .defaultValue(1)
            .min(1)
            .max(200)
            .sliderRange(1, 200)
            .build()
      );
   private final Setting<Integer> attackDelay = this.sgSpeed
      .add(new IntSetting.Builder().name("attack-delay").description("Ticks to wait between attacks.").defaultValue(2).min(0).sliderRange(0, 128).build());
   private final Setting<Double> vclipHeight = this.sgMace
      .add(
         new DoubleSetting.Builder()
            .name("vclip")
            .description("Vertical offset for mace attacks.")
            .defaultValue(10.0)
            .min(0.0)
            .max(200.0)
            .sliderRange(0.0, 200.0)
            .visible(this.useMace::get)
            .build()
      );
   private final Setting<TPAura.PredictionMode> predictionMode = this.sgPrediction
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("prediction-mode"))
                  .description("Target position prediction mode."))
               .defaultValue(TPAura.PredictionMode.Off))
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
            .visible(() -> this.predictionMode.get() == TPAura.PredictionMode.Legacy)
            .build()
      );
   private final Setting<Boolean> render3d = this.sgRender
      .add(new BoolSetting.Builder().name("render-3d").description("Renders TPAura visuals.").defaultValue(true).build());
   private final Setting<Boolean> renderTarget = this.sgRender
      .add(
         new BoolSetting.Builder().name("render-target").description("Renders the target bounding box.").defaultValue(true).visible(this.render3d::get).build()
      );
   private final Setting<Boolean> renderPrediction = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("render-prediction")
            .description("Renders the predicted target position.")
            .defaultValue(true)
            .visible(this.render3d::get)
            .build()
      );
   private final Setting<Boolean> renderTp = this.sgRender
      .add(new BoolSetting.Builder().name("render-tp").description("Renders the teleport position.").defaultValue(false).visible(this.render3d::get).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How boxes are rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(this.render3d::get))
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("target-color")
            .description("Target box color.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .visible(() -> this.render3d.get() && this.renderTarget.get())
            .build()
      );
   private final Setting<SettingColor> predictedColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("predicted-color")
            .description("Predicted box color.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(() -> this.render3d.get() && this.renderPrediction.get())
            .build()
      );
   private final Setting<SettingColor> tpBoxColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tp-box-color")
            .description("Teleport box side color.")
            .defaultValue(new SettingColor(255, 255, 0, 80))
            .visible(() -> this.render3d.get() && this.renderTp.get() && this.shapeMode.get().sides())
            .build()
      );
   private final Setting<SettingColor> tpPathColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tp-path-color")
            .description("Teleport box line color.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .visible(() -> this.render3d.get() && this.renderTp.get() && this.shapeMode.get().lines())
            .build()
      );
   private Entity target;
   private int tickTimer;
   private Vec3 renderTpPos;

   public TPAura() {
      super(Categories.Combat, "TPAura", "Tp KillAura.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.tickTimer = 0;
      this.renderTpPos = null;
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.renderTpPos = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null && this.mc.getConnection() != null) {
         this.updateTarget();
         if (this.target != null && !(this.mc.player.distanceTo(this.target) > this.range.get())) {
            if (this.tickTimer < this.attackDelay.get()) {
               this.tickTimer++;
            } else {
               this.tickTimer = 0;
               Vec3 predictedPos = this.getPredictedTargetPos(this.target);
               if (predictedPos != null) {
                  this.doNormalAttack(this.mc.player.position(), predictedPos);
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (this.mc.player != null && this.mc.level != null && this.target != null) {
         if (event.packet instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35) {
            Entity entity = packet.getEntity(this.mc.level);
            if (entity instanceof Player && entity == this.target) {
               this.doPopAttack(this.mc.player.position());
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.render3d.get()) {
         if (this.renderTarget.get() && this.target != null) {
            event.renderer.box(this.target.getBoundingBox(), this.targetColor.get(), this.targetColor.get(), this.shapeMode.get(), 0);
         }

         if (this.renderPrediction.get() && this.predictionMode.get() != TPAura.PredictionMode.Off) {
            AABB predictedBox = this.getPredictedBoundingBox();
            if (predictedBox != null) {
               event.renderer.box(predictedBox, this.predictedColor.get(), this.predictedColor.get(), this.shapeMode.get(), 0);
            }
         }

         if (this.renderTp.get() && this.renderTpPos != null) {
            AABB box = new AABB(
               this.renderTpPos.x - 0.3,
               this.renderTpPos.y,
               this.renderTpPos.z - 0.3,
               this.renderTpPos.x + 0.3,
               this.renderTpPos.y + 1.8,
               this.renderTpPos.z + 0.3
            );
            if (this.shapeMode.get().lines()) {
               event.renderer.box(box, this.tpPathColor.get(), this.tpPathColor.get(), ShapeMode.Lines, 0);
            }

            if (this.shapeMode.get().sides()) {
               event.renderer.box(box, this.tpBoxColor.get(), this.tpBoxColor.get(), ShapeMode.Sides, 0);
            }
         }
      }
   }

   @Override
   public String getInfoString() {
      return this.target != null ? this.target.getName().getString() : null;
   }

   private Vec3 getPredictedTargetPos(Entity entity) {
      Vec3 nowPos = entity.position();

      return switch ((TPAura.PredictionMode)this.predictionMode.get()) {
         case Off -> nowPos;
         case Legacy -> nowPos.add(
            (entity.getX() - entity.xOld) * this.prevMultiplier.get(),
            (entity.getY() - entity.yOld) * this.prevMultiplier.get(),
            (entity.getZ() - entity.zOld) * this.prevMultiplier.get()
         );
         case PredictUtils -> PredictUtil.predictEntity(entity);
      };
   }

   private AABB getPredictedBoundingBox() {
      if (this.target == null) {
         return null;
      } else {
         Vec3 predictedPos = this.getPredictedTargetPos(this.target);
         if (predictedPos == null) {
            return null;
         } else {
            return predictedPos.equals(this.target.position()) && this.predictionMode.get() == TPAura.PredictionMode.Off
               ? null
               : this.target.getBoundingBox().move(predictedPos.subtract(this.target.position()));
         }
      }
   }

   private void performVClip(Vec3 playerPos) {
      if (this.useMace.get()) {
         Vec3 clipPos = playerPos.add(0.0, this.vclipHeight.get(), 0.0);
         this.doVerticalTp(playerPos, clipPos, false);
         this.doVerticalTp(clipPos, playerPos, false);
      }
   }

   private void performAttacks() {
      if (this.target != null && this.mc.player != null && this.mc.gameMode != null) {
         boolean swapped = false;
         if (this.useMace.get()) {
            FindItemResult mace = InvUtils.find(Items.MACE);
            if (mace.found()) {
               InvUtils.swap(mace.slot(), true);
               swapped = true;
            }
         }

         for (int i = 0; i < this.attackTimes.get(); i++) {
            this.mc.gameMode.attack(this.mc.player, this.target);
         }

         if (this.swingHand.get()) {
            this.mc.player.swing(InteractionHand.MAIN_HAND);
         }

         if (swapped) {
            InvUtils.swapBack();
         }
      }
   }

   private void doNormalAttack(Vec3 playerPos, Vec3 targetPos) {
      this.performVClip(playerPos);
      this.renderTpPos = targetPos;
      this.doHorizontalTp(playerPos, targetPos, this.mc.player.onGround());
      this.performAttacks();
      this.doHorizontalTp(targetPos, playerPos, this.mc.player.onGround());
   }

   private void doPopAttack(Vec3 playerPos) {
      Vec3 upPos = playerPos.add(0.0, 20.0, 0.0);
      this.doVerticalTp(playerPos, upPos, false);
      this.performAttacks();
      this.doVerticalTp(upPos, playerPos, false);
   }

   private boolean targetCheck(Entity entity) {
      if (entity != null && entity != this.mc.player && entity != this.mc.getCameraEntity()) {
         if (!this.targetTypes.get().contains(entity.getType())) {
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
   }

   private void updateTarget() {
      this.target = TargetUtils.get(this::targetCheck, this.priority.get());
   }

   private void doVerticalTp(Vec3 from, Vec3 to, boolean onGround) {
      TpSetting setting = TpSetting.get();
      double moveDistance = setting != null ? setting.getMoveDistance() : 128.0;
      if (setting != null && setting.isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDistance);
      } else {
         QTPUtils.doTp(from, to, moveDistance, onGround);
      }
   }

   private void doHorizontalTp(Vec3 from, Vec3 to, boolean onGround) {
      double totalDistance = from.distanceTo(to);
      if (!(totalDistance <= 0.001)) {
         double maxStep = 10.0;
         int steps = (int)Math.ceil(totalDistance / maxStep);
         if (steps <= 0) {
            steps = 1;
         }

         for (int i = 0; i < steps; i++) {
            double progress = (i + 1.0) / steps;
            LTpUtils.sendMovePacket(from.lerp(to, progress), onGround);
         }
      }
   }

   public enum PredictionMode {
      Off,
      Legacy,
      PredictUtils;
   }
}
