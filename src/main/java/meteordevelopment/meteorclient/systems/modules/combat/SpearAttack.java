package meteordevelopment.meteorclient.systems.modules.combat;

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
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.math.BlockUtil;
import meteordevelopment.meteorclient.utils.math.PredictUtil;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.meteorclient.utils.rotation.RotationUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SpearAttack extends Module {
   private static final double[] SEARCH_ORDER = new double[]{
      0.0, 1.0, -1.0, 2.0, -2.0, 3.0, -3.0, 4.0, -4.0, 5.0, -5.0, 6.0, -6.0, 7.0, -7.0, 8.0, -8.0, 9.0, -9.0
   };
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgPrediction = this.settings.createGroup("Prediction");
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Set<EntityType<?>>> entities = this.sgGeneral
      .add(new EntityTypeListSetting.Builder().name("entities").description("Entities to target.").onlyAttackable().defaultValue(EntityType.PLAYER).build());
   private final Setting<SortPriority> priority = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("target-priority"))
                  .description("How to select the target."))
               .defaultValue(SortPriority.LowestDistance))
            .build()
      );
   private final Setting<Double> spearDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("spear-distance")
            .description("Maximum target range and simulated spear movement distance.")
            .defaultValue(50.0)
            .min(0.0)
            .max(128.0)
            .sliderRange(0.0, 128.0)
            .build()
      );
   private final Setting<Double> spearMaxTp = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("spear-max-tp")
            .description("Maximum distance from the original position to the spear teleport position.")
            .defaultValue(100.0)
            .min(0.0)
            .max(256.0)
            .sliderRange(0.0, 256.0)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         new IntSetting.Builder().name("delay").description("Ticks to wait between spear movements.").defaultValue(2).min(0).max(40).sliderRange(0, 40).build()
      );
   private final Setting<Boolean> rotate = this.sgGeneral
      .add(new BoolSetting.Builder().name("rotate").description("Sends a rotation packet toward the target before moving.").defaultValue(true).build());
   private final Setting<SpearAttack.PredictionMode> predictionMode = this.sgPrediction
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("prediction-mode"))
                  .description("Target position prediction mode."))
               .defaultValue(SpearAttack.PredictionMode.PredictUtils))
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
            .visible(() -> this.predictionMode.get() == SpearAttack.PredictionMode.Legacy)
            .build()
      );
   private final Setting<Boolean> renderTarget = this.sgRender
      .add(new BoolSetting.Builder().name("render-target").description("Renders the current spear target.").defaultValue(true).build());
   private final Setting<Boolean> renderTp = this.sgRender
      .add(new BoolSetting.Builder().name("render-tp").description("Renders the spear teleport position.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                     .description("How boxes are rendered."))
                  .defaultValue(ShapeMode.Both))
               .visible(() -> this.renderTarget.get() || this.renderTp.get()))
            .build()
      );
   private final Setting<SettingColor> targetColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("target-color")
            .description("Target box color.")
            .defaultValue(new SettingColor(0, 255, 0, 255))
            .visible(this.renderTarget::get)
            .build()
      );
   private final Setting<SettingColor> tpColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("tp-color")
            .description("Teleport box color.")
            .defaultValue(new SettingColor(255, 0, 255, 255))
            .visible(this.renderTp::get)
            .build()
      );
   private Entity target;
   private Vec3 renderTpPos;
   private int cooldown;
   private boolean warnedBadConfig;

   public SpearAttack() {
      super(Categories.Combat, "spear-attack", "Moves with TpSetting while charging a spear, using PredictUtil for targets.");
   }

   @Override
   public void onActivate() {
      this.target = null;
      this.renderTpPos = null;
      this.cooldown = 0;
      this.warnedBadConfig = false;
   }

   @Override
   public void onDeactivate() {
      this.target = null;
      this.renderTpPos = null;
      this.cooldown = 0;
      this.warnedBadConfig = false;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.getConnection() != null) {
         if (!this.canSpearAttack()) {
            this.target = null;
            this.renderTpPos = null;
            this.cooldown = 0;
            this.warnedBadConfig = false;
         } else if (this.cooldown > 0) {
            this.cooldown--;
         } else if (this.spearMaxTp.get() < this.spearDistance.get()) {
            if (!this.warnedBadConfig) {
               this.warning("spear-max-tp cannot be lower than spear-distance.");
               this.warnedBadConfig = true;
            }
         } else {
            this.target = this.findTarget();
            if (this.target == null) {
               this.renderTpPos = null;
            } else {
               Vec3 playerPos = this.mc.player.position();
               Vec3 predictedTargetEye = this.getPredictedEyePos(this.target);
               Vec3 tpPos = this.findValidTpPosition(playerPos, predictedTargetEye);
               if (tpPos == null) {
                  this.renderTpPos = null;
               } else {
                  this.renderTpPos = tpPos;
                  boolean onGround = this.mc.player.onGround();
                  if (this.rotate.get()) {
                     this.sendRotationPacket(RotationUtil.calculate(this.mc.player.getEyePosition(), predictedTargetEye), onGround);
                  }

                  this.doTp(playerPos, tpPos, onGround);
                  this.doTp(tpPos, playerPos, onGround);
                  this.cooldown = this.delay.get();
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.renderTarget.get() && this.target != null) {
         SettingColor color = this.targetColor.get();
         event.renderer.box(this.target.getBoundingBox(), this.sideColor(color), color, this.shapeMode.get(), 0);
      }

      if (this.renderTp.get() && this.renderTpPos != null) {
         SettingColor color = this.tpColor.get();
         AABB box = new AABB(
            this.renderTpPos.x - 0.3,
            this.renderTpPos.y,
            this.renderTpPos.z - 0.3,
            this.renderTpPos.x + 0.3,
            this.renderTpPos.y + 1.8,
            this.renderTpPos.z + 0.3
         );
         event.renderer.box(box, this.sideColor(color), color, this.shapeMode.get(), 0);
      }
   }

   @Override
   public String getInfoString() {
      return this.target != null ? EntityUtils.getName(this.target) : null;
   }

   private boolean canSpearAttack() {
      return !this.hasSpear(this.mc.player.getMainHandItem()) && !this.hasSpear(this.mc.player.getOffhandItem())
         ? false
         : this.mc.options.keyUse.isDown() || this.mc.player.isUsingItem();
   }

   private boolean hasSpear(ItemStack stack) {
      return stack != null && stack.is(ItemTags.SPEARS);
   }

   private Entity findTarget() {
      return TargetUtils.get(
         entity -> {
            if (entity != null && entity != this.mc.player && entity != this.mc.getCameraEntity()) {
               if (!this.entities.get().contains(entity.getType())) {
                  return false;
               }

               if (this.spearDistance.get() > 0.0
                  && this.mc.player.position().distanceToSqr(entity.position()) > this.spearDistance.get() * this.spearDistance.get()) {
                  return false;
               }

               if (!(entity instanceof LivingEntity living && (living.isDeadOrDying() || !living.isAlive() || living.getHealth() <= 0.0F))) {
                  if (entity instanceof Player player) {
                     if (player.isCreative() || player.isSpectator()) {
                        return false;
                     }

                     if (!Friends.get().shouldAttack(player)) {
                        return false;
                     }

                     if (player instanceof FakePlayerEntity fakePlayer) {
                        return !fakePlayer.noHit;
                     }
                  }

                  return this.isSpearable(entity);
               } else {
                  return false;
               }
            } else {
               return false;
            }
         },
         this.priority.get()
      );
   }

   private boolean isSpearable(Entity entity) {
      Vec3 playerEye = this.mc.player.getEyePosition();
      Vec3 targetEye = entity.getEyePosition();
      double minRange = this.mc.player.entityInteractionRange();
      if (playerEye.distanceToSqr(targetEye) <= minRange * minRange) {
         return false;
      }

      HitResult hit = this.mc.level.clip(new ClipContext(playerEye, targetEye, Block.COLLIDER, Fluid.NONE, this.mc.player));
      return hit.getType() == Type.MISS;
   }

   private Vec3 getPredictedEyePos(Entity entity) {
      return this.getPredictedPos(entity).add(0.0, entity.getEyeHeight(entity.getPose()), 0.0);
   }

   private Vec3 getPredictedPos(Entity entity) {
      Vec3 pos = entity.position();

      return switch ((SpearAttack.PredictionMode)this.predictionMode.get()) {
         case Off -> pos;
         case Legacy -> pos.add(
            (entity.getX() - entity.xOld) * this.prevMultiplier.get(),
            (entity.getY() - entity.yOld) * this.prevMultiplier.get(),
            (entity.getZ() - entity.zOld) * this.prevMultiplier.get()
         );
         case PredictUtils -> PredictUtil.predictEntity(entity);
      };
   }

   private Vec3 findValidTpPosition(Vec3 playerPos, Vec3 targetEyePos) {
      Vec3 playerEye = this.mc.player.getEyePosition();
      Vec3 toTarget = targetEyePos.subtract(playerEye);
      double targetDistance = toTarget.length();
      if (!(targetDistance <= 1.0E-6) && !(targetDistance >= this.spearDistance.get())) {
         Vec3 away = toTarget.normalize().scale(-1.0);
         Vec3 side = new Vec3(-away.z, 0.0, away.x);
         if (side.lengthSqr() <= 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
         } else {
            side = side.normalize();
         }

         double maxTpSq = this.spearMaxTp.get() * this.spearMaxTp.get();

         for (double search = this.spearDistance.get(); search > targetDistance; search -= 2.0) {
            Vec3 base = playerPos.add(away.scale(search));

            for (double vertical : SEARCH_ORDER) {
               for (double sideOffset : SEARCH_ORDER) {
                  Vec3 candidate = base.add(side.scale(sideOffset)).add(0.0, vertical, 0.0);
                  if (!(candidate.distanceToSqr(playerPos) > maxTpSq) && this.isValidTpLocation(playerPos, candidate)) {
                     return candidate;
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean isValidTpLocation(Vec3 from, Vec3 to) {
      BlockPos fromPos = BlockPos.containing(from);
      BlockPos toPos = BlockPos.containing(to);
      if (!BlockUtil.isSafeBlock(fromPos) || !BlockUtil.isSafeBlock(fromPos.above())) {
         return false;
      } else if (BlockUtil.isSafeBlock(toPos) && BlockUtil.isSafeBlock(toPos.above())) {
         double eyeHeight = this.mc.player.getEyeHeight(this.mc.player.getPose());
         Vec3 fromEye = from.add(0.0, eyeHeight, 0.0);
         Vec3 toEye = to.add(0.0, eyeHeight, 0.0);
         HitResult hit = this.mc.level.clip(new ClipContext(fromEye, toEye, Block.COLLIDER, Fluid.NONE, this.mc.player));
         return hit.getType() == Type.MISS;
      } else {
         return false;
      }
   }

   private void sendRotationPacket(Rotation rotation, boolean onGround) {
      if (this.mc.getConnection() != null) {
         this.mc.getConnection().send(new Rot(rotation.getYaw(), rotation.getPitch(), onGround, this.mc.player.horizontalCollision));
      }
   }

   private void doTp(Vec3 from, Vec3 to, boolean onGround) {
      double distance = from.distanceTo(to);
      if (!(distance <= 0.001)) {
         TpSetting setting = TpSetting.get();
         double moveDistance = setting != null ? setting.getMoveDistance() : 128.0;
         boolean useLtp = setting == null || setting.isLTPUtils();
         int steps = moveDistance <= 0.0 ? 1 : Math.max(1, (int)Math.ceil(distance / moveDistance));

         for (int i = 1; i < steps; i++) {
            this.sendMovePacket(from.lerp(to, (double)i / steps), onGround, useLtp);
         }

         this.sendMovePacket(to, onGround, useLtp);
      }
   }

   private void sendMovePacket(Vec3 pos, boolean onGround, boolean useLtp) {
      if (useLtp) {
         LTpUtils.sendMovePacket(pos, onGround);
      } else {
         QTPUtils.sendMovePacket(pos, onGround);
      }
   }

   private SettingColor sideColor(SettingColor color) {
      return new SettingColor(color.r, color.g, color.b, Math.min(color.a, 45));
   }

   public enum PredictionMode {
      Off,
      Legacy,
      PredictUtils;
   }
}
