package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ClientboundSetEntityMotionPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Flight;
import meteordevelopment.meteorclient.systems.modules.movement.Fly;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TargetStrafe extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> jump = this.sgGeneral.add(new BoolSetting.Builder().name("jump").defaultValue(true).build());
   private final Setting<Boolean> onlyMoving = this.sgGeneral.add(new BoolSetting.Builder().name("OnlyMoving").defaultValue(true).build());
   private final Setting<Double> jumpRange = this.sgGeneral
      .add(new DoubleSetting.Builder().name("JumpRange").defaultValue(2.0).min(0.0).sliderRange(0.0, 8.0).visible(this.jump::get).build());
   private final Setting<Boolean> lowHop = this.sgGeneral.add(new BoolSetting.Builder().name("LowHop").defaultValue(false).visible(this.jump::get).build());
   private final Setting<Boolean> pauseWhenFall = this.sgGeneral.add(new BoolSetting.Builder().name("PauseWhenFall").defaultValue(false).build());
   private final Setting<Integer> fallTicks = this.sgGeneral
      .add(new IntSetting.Builder().name("FallTicks").defaultValue(10).sliderRange(0, 200).visible(this.pauseWhenFall::get).build());
   private final Setting<Double> motion = this.sgGeneral
      .add(new DoubleSetting.Builder().name("Motion").defaultValue(0.1).min(0.0).max(7.0).visible(this.lowHop::get).build());
   private final Setting<Double> distance = this.sgGeneral.add(new DoubleSetting.Builder().name("distance").defaultValue(1.3).min(0.2).max(7.0).build());
   private final Setting<Integer> onGroundTicks = this.sgGeneral
      .add(new IntSetting.Builder().name("On Ground Ticks").defaultValue(2).min(0).sliderRange(0, 500).build());
   private final Setting<TargetStrafe.Boost> boost = this.sgGeneral
      .add(((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("boost")).defaultValue(TargetStrafe.Boost.None)).build());
   private final Setting<Double> setSpeed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("speed").defaultValue(1.3).min(0.0).max(2.0).visible(() -> this.boost.get() == TargetStrafe.Boost.Elytra).build());
   private final Setting<Double> velReduction = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("reduction")
            .defaultValue(6.0)
            .min(0.1)
            .max(10.0)
            .visible(() -> this.boost.get() == TargetStrafe.Boost.Damage)
            .build()
      );
   private final Setting<Double> maxVelocitySpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("max-velocity")
            .defaultValue(0.8)
            .min(0.1)
            .max(2.0)
            .visible(() -> this.boost.get() == TargetStrafe.Boost.Damage)
            .build()
      );
   private final Setting<Double> speedFactor = this.sgGeneral.add(new DoubleSetting.Builder().name("speedFactor").defaultValue(1.0).min(0.0).max(5.0).build());
   private final Setting<Double> flightSpeedFactor = this.sgGeneral
      .add(new DoubleSetting.Builder().name("flightSpeedFactor").defaultValue(1.0).min(0.0).max(5.0).build());
   private final Setting<Double> onGroundSpeedFactor = this.sgGeneral
      .add(new DoubleSetting.Builder().name("OnGroundSpeedFactor").defaultValue(1.0).min(0.0).max(5.0).build());
   private final Setting<Double> onGroundFrictionFactor = this.sgGeneral
      .add(new DoubleSetting.Builder().name("OnGroundFrictionFactor").defaultValue(1.0).min(0.0).max(5.0).build());
   private final Setting<Integer> points = this.sgGeneral
      .add(new IntSetting.Builder().name("points").description("Ignores yourself drawing the shader.").defaultValue(30).sliderRange(1, 100).build());
   private final Setting<Integer> colorOffset = this.sgGeneral
      .add(new IntSetting.Builder().name("colorOffset").description("Ignores yourself drawing the shader.").defaultValue(30).sliderRange(0, 100).build());
   private final Setting<SettingColor> color = this.sgGeneral
      .add(
         new ColorSetting.Builder()
            .name("color")
            .description("Ignores yourself drawing the shader.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
      );
   public static double oldSpeed;
   public static double contextFriction;
   public static double fovval;
   public static boolean needSwap;
   public static boolean needSprintState;
   public static boolean skip;
   public static boolean switchDir;
   public static boolean disabled;
   public static int noSlowTicks;
   public static int jumpTicks;
   public static int waterTicks;
   private static long disableTime;
   private static TargetStrafe instance;
   private int ticks;
   private int fallCounter;

   public TargetStrafe() {
      super(Categories.Combat, "TargetStrafe", "");
      instance = this;
   }

   @Override
   public void onActivate() {
      oldSpeed = 0.0;
      contextFriction = 1.0;
      fovval = this.mc.options.fovEffectScale().get();
      this.mc.options.fovEffectScale().set(0.0);
      needSwap = false;
      needSprintState = false;
      skip = true;
      switchDir = false;
      disabled = false;
      noSlowTicks = 0;
      jumpTicks = 0;
      waterTicks = 0;
      this.ticks = 0;
      this.fallCounter = 0;
   }

   @Override
   public void onDeactivate() {
      this.mc.options.fovEffectScale().set(fovval);
   }

   @EventHandler
   private void onRender3DEvent(Render3DEvent event) {
      LivingEntity target = this.getTarget();
      if (target != null) {
         int segments = Math.max(3, this.points.get());
         double radius = this.distance.get();
         double x = Mth.lerp(event.tickDelta, target.xo, target.getX());
         double y = Mth.lerp(event.tickDelta, target.yo, target.getY()) + 0.05;
         double z = Mth.lerp(event.tickDelta, target.zo, target.getZ());
         double lastX = x + radius;
         double lastZ = z;

         for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2) * i / segments;
            double nextX = x + Math.cos(angle) * radius;
            double nextZ = z + Math.sin(angle) * radius;
            event.renderer.line(lastX, y, lastZ, nextX, y, nextZ, this.circleColor(i));
            lastX = nextX;
            lastZ = nextZ;
         }
      }
   }

   @EventHandler
   private void onMove(PlayerMoveEvent event) {
      if (this.mc.player != null && this.mc.level != null && event.type == MoverType.SELF) {
         if (!this.pauseWhenFall.get() || this.fallCounter < this.fallTicks.get()) {
            int elytraSlot = this.getElytra();
            if (this.boost.get() == TargetStrafe.Boost.Elytra
               && elytraSlot != -1
               && PlayerUtils.isMoving()
               && !this.mc.player.onGround()
               && !this.mc.level.noCollision(this.mc.player, this.mc.player.getBoundingBox().move(0.0, event.movement.y(), 0.0))
               && disabled) {
               oldSpeed = this.setSpeed.get();
            }

            if (!this.canStrafe()) {
               oldSpeed = 0.0;
            } else {
               LivingEntity target = this.getTarget();
               if (target != null) {
                  double targetDistance = Math.sqrt(this.mc.player.distanceToSqr(target));
                  if (!(targetDistance < 1.0E-4)) {
                     double speed = this.calculateSpeed(event);
                     double wrap = Math.atan2(this.mc.player.getZ() - target.getZ(), this.mc.player.getX() - target.getX());
                     wrap += switchDir ? speed / targetDistance : -(speed / targetDistance);
                     double x = target.getX() + this.distance.get() * Math.cos(wrap);
                     double z = target.getZ() + this.distance.get() * Math.sin(wrap);
                     if (this.needToSwitch(x, z)) {
                        switchDir = !switchDir;
                        wrap += 2.0 * (switchDir ? speed / targetDistance : -(speed / targetDistance));
                        x = target.getX() + this.distance.get() * Math.cos(wrap);
                        z = target.getZ() + this.distance.get() * Math.sin(wrap);
                     }

                     double direction = Math.toRadians(this.wrapDS(x, z));
                     ((IVec3)event.movement).meteor$setXZ(speed * -Math.sin(direction), speed * Math.cos(direction));
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void updateValues(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.level != null) {
         oldSpeed = Math.hypot(this.mc.player.getX() - this.mc.player.xo, this.mc.player.getZ() - this.mc.player.zo) * contextFriction;
         if (this.mc.player.onGround()) {
            this.fallCounter = 0;
         } else if (this.mc.player.fallDistance > 0.0) {
            this.fallCounter++;
         }

         LivingEntity target = this.getTarget();
         if (this.mc.player.onGround() && target != null && this.jump.get() && this.ticks <= 0) {
            if (this.mc.player.distanceTo(target) <= this.jumpRange.get()) {
               if (this.lowHop.get()) {
                  Vec3 velocity = this.mc.player.getDeltaMovement();
                  this.mc.player.setDeltaMovement(velocity.x, this.motion.get(), velocity.z);
               } else {
                  this.mc.player.jumpFromGround();
               }
            }

            this.ticks = this.onGroundTicks.get();
         }

         if (this.ticks > 0) {
            this.ticks--;
         }

         if (this.mc.player.isUnderWater()) {
            waterTicks = 10;
         } else {
            waterTicks--;
         }

         if (this.boost.get() == TargetStrafe.Boost.Elytra
            && this.getElytra() != -1
            && !this.mc.player.onGround()
            && this.mc.player.fallDistance > 0.0
            && !disabled) {
            this.disabler(this.getElytra());
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (this.mc.player != null) {
         if (event.packet instanceof ClientboundPlayerPositionPacket) {
            oldSpeed = 0.0;
         }

         if (event.packet instanceof ClientboundSetEntityMotionPacket packet
            && packet.id() == this.mc.player.getId()
            && this.boost.get() == TargetStrafe.Boost.Damage) {
            if (this.mc.player.onGround()) {
               return;
            }

            Vec3 movement = packet.movement();
            double vX = Math.abs(movement.x);
            double vZ = Math.abs(movement.z);
            oldSpeed = (vX + vZ) / this.velReduction.get();
            oldSpeed = Math.min(oldSpeed, this.maxVelocitySpeed.get());
            ((ClientboundSetEntityMotionPacketAccessor)(Object)packet).meteor$setMovement(Vec3.ZERO);
         }
      }
   }

   public boolean canStrafe() {
      if (this.mc.player == null) {
         return false;
      } else if (this.onlyMoving.get()
         && !this.mc.options.keyUp.isDown()
         && !this.mc.options.keyDown.isDown()
         && !this.mc.options.keyLeft.isDown()
         && !this.mc.options.keyRight.isDown()) {
         return false;
      } else if (this.mc.player.isShiftKeyDown()) {
         return false;
      } else if (this.mc.player.isInLava()) {
         return false;
      } else {
         return !this.mc.player.isUnderWater() && waterTicks <= 0 ? !this.mc.player.getAbilities().flying : false;
      }
   }

   public boolean needToSwitch(double x, double z) {
      if (this.mc.player == null || this.mc.level == null) {
         return false;
      }

      if (!this.mc.player.horizontalCollision && (!this.mc.options.keyLeft.isDown() && !this.mc.options.keyRight.isDown() || jumpTicks > 0)) {
         for (int y = (int)(this.mc.player.getY() + 4.0); y >= this.mc.level.getMinY(); y--) {
            BlockPos playerPos = BlockPos.containing(x, y, z);
            if (this.mc.level.getBlockState(playerPos).is(Blocks.LAVA) || this.mc.level.getBlockState(playerPos).is(Blocks.FIRE)) {
               return true;
            }

            if (!this.mc.level.isEmptyBlock(playerPos)) {
               return false;
            }
         }

         return false;
      } else {
         jumpTicks = 10;
         return true;
      }
   }

   public double calculateSpeed(PlayerMoveEvent move) {
      jumpTicks--;
      float speedAttributes = this.getAIMoveSpeed();
      BlockPos frictionPos = BlockPos.containing(this.mc.player.getX(), this.getBoundingBox().minY - move.movement.y(), this.mc.player.getZ());
      float frictionFactor = this.mc.level.getBlockState(frictionPos).getBlock().getFriction() * 0.91F;
      float n6 = this.mc.player.hasEffect(MobEffects.JUMP_BOOST) && this.mc.player.isUsingItem()
         ? 0.88F
         : (float)(oldSpeed > 0.32 && this.mc.player.isUsingItem() ? 0.88 : 0.91F);
      if (this.mc.player.onGround()) {
         n6 = frictionFactor * this.factor(this.onGroundFrictionFactor.get());
      }

      float n7 = (float)(0.1631F / Math.pow(n6, 3.0));
      float n8;
      if (this.mc.player.onGround()) {
         n8 = speedAttributes * n7 * this.factor(this.onGroundSpeedFactor.get());
         if (move.movement.y() > 0.0) {
            n8 += this.boost.get() == TargetStrafe.Boost.Elytra && this.getElytra() != -1 && disabled ? 0.65F : 0.2F;
         }

         disabled = false;
      } else {
         n8 = this.isFlightActive() ? 0.0255F * this.factor(this.flightSpeedFactor.get()) : 0.0255F * this.factor(this.speedFactor.get());
      }

      boolean noslow = false;
      double max2 = oldSpeed + n8;
      double max = 0.0;
      if (this.mc.player.isUsingItem() && move.movement.y() <= 0.0) {
         double n10 = oldSpeed + n8 * 0.25;
         double motionY2 = move.movement.y();
         if (motionY2 != 0.0 && Math.abs(motionY2) < 0.08) {
            n10 += 0.055;
         }

         if (max2 > (max = Math.max(0.043, n10))) {
            noslow = true;
            noSlowTicks++;
         } else {
            noSlowTicks = Math.max(noSlowTicks - 1, 0);
         }
      } else {
         noSlowTicks = 0;
      }

      if (noSlowTicks > 3) {
         max2 = max - 0.019;
      } else {
         max2 = Math.max(noslow ? 0.0 : 0.25, max2) - (this.mc.player.tickCount % 2 == 0 ? 0.001 : 0.002);
      }

      contextFriction = n6;
      if (!this.mc.player.onGround()) {
         needSprintState = !this.mc.player.isSprinting();
         needSwap = true;
      } else {
         needSprintState = false;
      }

      return max2;
   }

   public AABB getBoundingBox() {
      return new AABB(
         this.mc.player.getX() - 0.1,
         this.mc.player.getY(),
         this.mc.player.getZ() - 0.1,
         this.mc.player.getX() + 0.1,
         this.mc.player.getY() + 1.0,
         this.mc.player.getZ() + 0.1
      );
   }

   public float getAIMoveSpeed() {
      boolean prevSprinting = this.mc.player.isSprinting();
      this.mc.player.setSprinting(false);
      float speed = this.getBaseMoveSpeed() * 1.3F;
      this.mc.player.setSprinting(prevSprinting);
      return speed;
   }

   public void disabler(int elytra) {
      if (elytra != -1 && this.mc.player != null && this.mc.getConnection() != null) {
         if (System.currentTimeMillis() - disableTime > 190L) {
            if (elytra != -2) {
               InvUtils.move().from(elytra).toArmor(2);
            }

            this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_FALL_FLYING));
            this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_FALL_FLYING));
            if (elytra != -2) {
               InvUtils.move().fromArmor(2).to(elytra);
            }

            disableTime = System.currentTimeMillis();
         }

         disabled = true;
      }
   }

   private LivingEntity getTarget() {
      KillAura aura = Modules.get().get(KillAura.class);
      return aura != null && aura.isActive() ? aura.target : null;
   }

   private int getElytra() {
      if (this.mc.player == null) {
         return -1;
      }

      if (this.isGlider(this.mc.player.getItemBySlot(EquipmentSlot.CHEST))) {
         return -2;
      }

      FindItemResult elytra = InvUtils.find(this::isGlider);
      return elytra.found() ? elytra.slot() : -1;
   }

   private boolean isGlider(ItemStack stack) {
      return stack.has(DataComponents.GLIDER) || stack.is(Items.ELYTRA);
   }

   private double wrapDS(double x, double z) {
      double diffX = x - this.mc.player.getX();
      double diffZ = z - this.mc.player.getZ();
      return Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0;
   }

   private boolean isFlightActive() {
      return Modules.get().isActive(Fly.class) || Modules.get().isActive(Flight.class);
   }

   private float getBaseMoveSpeed() {
      double speed = 0.2873;
      if (this.mc.player.hasEffect(MobEffects.SPEED)) {
         int amplifier = this.mc.player.getEffect(MobEffects.SPEED).getAmplifier();
         speed *= 1.0 + 0.2 * (amplifier + 1);
      }

      if (this.mc.player.hasEffect(MobEffects.SLOWNESS)) {
         int amplifier = this.mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
         speed /= 1.0 + 0.2 * (amplifier + 1);
      }

      return (float)speed;
   }

   private float factor(double value) {
      return (float)value;
   }

   private Color circleColor(int index) {
      SettingColor base = this.color.get();
      int offset = this.colorOffset.get();
      if (offset <= 0) {
         return base;
      }

      double wave = (Math.sin(System.currentTimeMillis() / 80.0 + (double)index / offset) + 1.0) * 0.5;
      int alpha = (int)Mth.clamp(base.a * (0.55 + wave * 0.45), 0.0, 255.0);
      return new Color(base.r, base.g, base.b, alpha);
   }

   public static TargetStrafe getInstance() {
      return instance;
   }

   private enum Boost {
      None,
      Elytra,
      Damage;
   }
}
