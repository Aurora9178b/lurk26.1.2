package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.effect.MobEffects;

public class LongJump extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<LongJump.JumpMode> jumpMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The method of jumping."))
               .defaultValue(LongJump.JumpMode.Vanilla))
            .build()
      );
   private final Setting<Double> vanillaBoostFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("vanilla-boost-factor")
            .description("The amount by which to boost the jump.")
            .visible(() -> this.jumpMode.get() == LongJump.JumpMode.Vanilla)
            .defaultValue(1.261)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   private final Setting<Double> burstInitialSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("burst-initial-speed")
            .description("The initial speed of the runup.")
            .visible(() -> this.jumpMode.get() == LongJump.JumpMode.Burst)
            .defaultValue(6.0)
            .min(0.0)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Double> burstBoostFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("burst-boost-factor")
            .description("The amount by which to boost the jump.")
            .visible(() -> this.jumpMode.get() == LongJump.JumpMode.Burst)
            .defaultValue(2.149)
            .min(0.0)
            .sliderMax(20.0)
            .build()
      );
   private final Setting<Boolean> onlyOnGround = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only performs the jump if you are on the ground.")
            .visible(() -> this.jumpMode.get() == LongJump.JumpMode.Burst)
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> onJump = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("on-jump")
            .description("Whether the player needs to jump first or not.")
            .visible(() -> this.jumpMode.get() == LongJump.JumpMode.Burst)
            .defaultValue(false)
            .build()
      );
   private final Setting<Double> glideMultiplier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("glide-multiplier")
            .description("The amount by to multiply the glide velocity.")
            .visible(() -> this.jumpMode.get() == LongJump.JumpMode.Glide)
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(5.0)
            .build()
      );
   public final Setting<Double> timer = this.sgGeneral
      .add(new DoubleSetting.Builder().name("timer").description("Timer override.").defaultValue(1.0).min(0.01).sliderMin(0.01).build());
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-disable")
            .description("Automatically disabled the module after jumping.")
            .visible(() -> this.jumpMode.get() != LongJump.JumpMode.Vanilla)
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> disableOnRubberband = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-on-rubberband").description("Disables the module when you get lagged back.").defaultValue(true).build());
   private int stage;
   private double moveSpeed;
   private boolean jumping = false;
   private int airTicks;
   private int groundTicks;
   private boolean jumped = false;

   public LongJump() {
      super(Categories.Movement, "long-jump", "Allows you to jump further than normal.");
   }

   @Override
   public void onActivate() {
      this.stage = 0;
      this.jumping = false;
      this.airTicks = 0;
      this.groundTicks = -5;
   }

   @Override
   public void onDeactivate() {
      Modules.get().get(Timer.class).setOverride(1.0);
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerPositionPacket && this.disableOnRubberband.get()) {
         this.info("Rubberband detected! Disabling...");
         this.toggle();
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.timer.get() != 1.0) {
         Modules.get().get(Timer.class).setOverride(PlayerUtils.isMoving() ? this.timer.get() : 1.0);
      }

      switch ((LongJump.JumpMode)this.jumpMode.get()) {
         case Vanilla:
            if (PlayerUtils.isMoving() && this.mc.options.keyJump.isDown()) {
               double dir = this.getDir();
               double xDir = Math.cos(Math.toRadians(dir + 90.0));
               double zDir = Math.sin(Math.toRadians(dir + 90.0));
               if (!this.mc.level.noCollision(this.mc.player.getBoundingBox().move(0.0, this.mc.player.getDeltaMovement().y, 0.0))
                  || this.mc.player.verticalCollision) {
                  ((IVec3)event.movement).meteor$setXZ(xDir * 0.29F, zDir * 0.29F);
               }

               if (event.movement.y() == 0.33319999363422365) {
                  ((IVec3)event.movement).meteor$setXZ(xDir * this.vanillaBoostFactor.get(), zDir * this.vanillaBoostFactor.get());
               }
            }
            break;
         case Burst:
            if (this.stage != 0 && !this.mc.player.onGround() && this.autoDisable.get()) {
               this.jumping = true;
            }

            if (this.jumping && this.mc.player.getY() - (int)this.mc.player.getY() < 0.01) {
               this.jumping = false;
               this.toggle();
               this.info("Disabling after jump.");
            }

            if (this.onlyOnGround.get() && !this.mc.player.onGround() && this.stage == 0) {
               return;
            }

            double xDist = this.mc.player.getX() - this.mc.player.xo;
            double zDist = this.mc.player.getZ() - this.mc.player.zo;
            double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
            if (PlayerUtils.isMoving() && (!this.onJump.get() || this.mc.options.keyJump.isDown()) && !this.mc.player.isInLava() && !this.mc.player.isInWater()
               )
             {
               switch (this.stage) {
                  case 0:
                     this.moveSpeed = this.getMoveSpeed() * this.burstInitialSpeed.get();
                     break;
                  case 1:
                     ((IVec3)event.movement).meteor$setY(0.42);
                     this.moveSpeed = this.moveSpeed * this.burstBoostFactor.get();
                     break;
                  case 2:
                     double difference = lastDist - this.getMoveSpeed();
                     this.moveSpeed = lastDist - difference;
                     break;
                  default:
                     this.moveSpeed = lastDist - lastDist / 159.0;
               }

               this.setMoveSpeed(event, this.moveSpeed = Math.max(this.getMoveSpeed(), this.moveSpeed));
               if (!this.mc.player.verticalCollision
                  && !this.mc.level.noCollision(this.mc.player.getBoundingBox().move(0.0, this.mc.player.getDeltaMovement().y, 0.0))
                  && !this.mc.level.noCollision(this.mc.player.getBoundingBox().move(0.0, -0.4, 0.0))) {
                  ((IVec3)event.movement).meteor$setY(-0.001);
               }

               this.stage++;
            }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate() && this.jumpMode.get() == LongJump.JumpMode.Glide) {
         if (!PlayerUtils.isMoving()) {
            return;
         }

         float yaw = this.mc.player.getYRot() + 90.0F;
         double forward = this.mc.player.zza != 0.0F ? (this.mc.player.zza > 0.0F ? 1 : -1) : 0;
         float[] motion = new float[]{
            0.4206065F,
            0.4179245F,
            0.41525924F,
            0.41261F,
            0.409978F,
            0.407361F,
            0.404761F,
            0.402178F,
            0.399611F,
            0.39706F,
            0.394525F,
            0.392F,
            0.3894F,
            0.38644F,
            0.383655F,
            0.381105F,
            0.37867F,
            0.37625F,
            0.37384F,
            0.37145F,
            0.369F,
            0.3666F,
            0.3642F,
            0.3618F,
            0.35945F,
            0.357F,
            0.354F,
            0.351F,
            0.348F,
            0.345F,
            0.342F,
            0.339F,
            0.336F,
            0.333F,
            0.33F,
            0.327F,
            0.324F,
            0.321F,
            0.318F,
            0.315F,
            0.312F,
            0.309F,
            0.307F,
            0.305F,
            0.303F,
            0.3F,
            0.297F,
            0.295F,
            0.293F,
            0.291F,
            0.289F,
            0.287F,
            0.285F,
            0.283F,
            0.281F,
            0.279F,
            0.277F,
            0.275F,
            0.273F,
            0.271F,
            0.269F,
            0.267F,
            0.265F,
            0.263F,
            0.261F,
            0.259F,
            0.257F,
            0.255F,
            0.253F,
            0.251F,
            0.249F,
            0.247F,
            0.245F,
            0.243F,
            0.241F,
            0.239F,
            0.237F
         };
         float[] glide = new float[]{0.3425F, 0.5445F, 0.65425F, 0.685F, 0.675F, 0.2F, 0.895F, 0.719F, 0.76F};
         double cos = Math.cos(Math.toRadians(yaw));
         double sin = Math.sin(Math.toRadians(yaw));
         if (!this.mc.player.verticalCollision && !this.mc.player.onGround()) {
            this.jumped = true;
            this.airTicks++;
            this.groundTicks = -5;
            double velocityY = this.mc.player.getDeltaMovement().y;
            if (this.airTicks - 6 >= 0 && this.airTicks - 6 < glide.length) {
               this.updateY(velocityY * glide[this.airTicks - 6] * this.glideMultiplier.get());
            }

            if (velocityY < -0.2 && velocityY > -0.24) {
               this.updateY(velocityY * 0.7 * this.glideMultiplier.get());
            } else if (velocityY < -0.25 && velocityY > -0.32) {
               this.updateY(velocityY * 0.8 * this.glideMultiplier.get());
            } else if (velocityY < -0.35 && velocityY > -0.8) {
               this.updateY(velocityY * 0.98 * this.glideMultiplier.get());
            }

            if (this.airTicks - 1 >= 0 && this.airTicks - 1 < motion.length) {
               this.mc
                  .player
                  .setDeltaMovement(
                     forward * motion[this.airTicks - 1] * 3.0 * cos * this.glideMultiplier.get(),
                     this.mc.player.getDeltaMovement().y,
                     forward * motion[this.airTicks - 1] * 3.0 * sin * this.glideMultiplier.get()
                  );
            } else {
               this.mc.player.setDeltaMovement(0.0, this.mc.player.getDeltaMovement().y, 0.0);
            }
         } else {
            if (this.autoDisable.get() && this.jumped) {
               this.jumped = false;
               this.toggle();
               this.info("Disabling after jump.");
            }

            this.airTicks = 0;
            this.groundTicks++;
            if (this.groundTicks <= 2) {
               this.mc
                  .player
                  .setDeltaMovement(
                     forward * 0.01F * cos * this.glideMultiplier.get(),
                     this.mc.player.getDeltaMovement().y,
                     forward * 0.01F * sin * this.glideMultiplier.get()
                  );
            } else {
               this.mc.player.setDeltaMovement(forward * 0.3F * cos * this.glideMultiplier.get(), 0.424F, forward * 0.3F * sin * this.glideMultiplier.get());
            }
         }
      }
   }

   private void updateY(double amount) {
      this.mc.player.setDeltaMovement(this.mc.player.getDeltaMovement().x, amount, this.mc.player.getDeltaMovement().z);
   }

   private double getDir() {
      double dir = 0.0;
      if (Utils.canUpdate()) {
         dir = this.mc.player.getYRot() + (this.mc.player.zza < 0.0F ? 180 : 0);
         if (this.mc.player.xxa > 0.0F) {
            dir += -90.0F * (this.mc.player.zza < 0.0F ? -0.5F : (this.mc.player.zza > 0.0F ? 0.5F : 1.0F));
         } else if (this.mc.player.xxa < 0.0F) {
            dir += 90.0F * (this.mc.player.zza < 0.0F ? -0.5F : (this.mc.player.zza > 0.0F ? 0.5F : 1.0F));
         }
      }

      return dir;
   }

   private double getMoveSpeed() {
      double base = 0.2873;
      if (this.mc.player.hasEffect(MobEffects.SPEED)) {
         base *= 1.0 + 0.2 * (this.mc.player.getEffect(MobEffects.SPEED).getAmplifier() + 1);
      }

      return base;
   }

   private void setMoveSpeed(PlayerMoveEvent event, double speed) {
      double forward = this.mc.player.zza;
      double strafe = this.mc.player.xxa;
      float yaw = this.mc.player.getYRot();
      if (!PlayerUtils.isMoving()) {
         ((IVec3)event.movement).meteor$setXZ(0.0, 0.0);
      } else {
         if (forward != 0.0) {
            if (strafe > 0.0) {
               yaw += forward > 0.0 ? -45 : 45;
            } else if (strafe < 0.0) {
               yaw += forward > 0.0 ? 45 : -45;
            }
         }

         strafe = 0.0;
         if (forward > 0.0) {
            forward = 1.0;
         } else if (forward < 0.0) {
            forward = -1.0;
         }
      }

      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      ((IVec3)event.movement).meteor$setXZ(forward * speed * cos + strafe * speed * sin, forward * speed * sin + strafe * speed * cos);
   }

   public enum JumpMode {
      Vanilla,
      Burst,
      Glide;
   }
}
