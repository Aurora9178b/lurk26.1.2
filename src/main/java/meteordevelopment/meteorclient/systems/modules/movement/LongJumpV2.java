package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.BypassSetting;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class LongJumpV2 extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<LongJumpV2.JumpMode> jumpMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The method of jumping."))
               .defaultValue(LongJumpV2.JumpMode.Vanilla))
            .build()
      );
   private final Setting<Double> boostFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("boost-factor")
            .description("The horizontal boost amount.")
            .defaultValue(1.261)
            .min(0.0)
            .sliderMax(5.0)
            .visible(() -> this.jumpMode.get() != LongJumpV2.JumpMode.Aiglos)
            .build()
      );
   private final Setting<Double> boostFactorY = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("boost-factor-y")
            .description("The vertical boost amount.")
            .defaultValue(1.261)
            .min(0.0)
            .sliderMax(5.0)
            .visible(() -> this.jumpMode.get() != LongJumpV2.JumpMode.Aiglos)
            .build()
      );
   private final Setting<Double> vanillaBoostFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("vanilla-boost-factor")
            .description("The amount by which to boost vanilla jumping.")
            .defaultValue(1.261)
            .min(0.0)
            .sliderMax(5.0)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Vanilla)
            .build()
      );
   private final Setting<Double> burstInitialSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("burst-initial-speed")
            .description("The initial speed of the runup.")
            .defaultValue(6.0)
            .min(0.0)
            .sliderMax(20.0)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Burst)
            .build()
      );
   private final Setting<Double> burstBoostFactor = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("burst-boost-factor")
            .description("The amount by which to boost the jump.")
            .defaultValue(2.149)
            .min(0.0)
            .sliderMax(20.0)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Burst)
            .build()
      );
   private final Setting<Boolean> onlyOnGround = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Only performs the burst jump if you are on the ground.")
            .defaultValue(true)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Burst)
            .build()
      );
   private final Setting<Boolean> onJump = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("on-jump")
            .description("Whether the player needs to jump first or not.")
            .defaultValue(false)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Burst)
            .build()
      );
   private final Setting<Double> glideMultiplier = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("glide-multiplier")
            .description("The amount by which to multiply the glide velocity.")
            .defaultValue(1.0)
            .min(0.0)
            .sliderMax(5.0)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Glide)
            .build()
      );
   public final Setting<Double> timer = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("timer")
            .description("Timer override.")
            .defaultValue(1.0)
            .min(0.01)
            .sliderMin(0.01)
            .visible(() -> this.jumpMode.get() != LongJumpV2.JumpMode.Aiglos)
            .build()
      );
   private final Setting<Boolean> autoDisable = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-disable")
            .description("Automatically disables the module after jumping.")
            .defaultValue(true)
            .visible(() -> this.jumpMode.get() != LongJumpV2.JumpMode.Vanilla)
            .build()
      );
   private final Setting<Boolean> disableOnRubberband = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-on-rubberband").description("Disables the module when you get lagged back.").defaultValue(true).build());
   private final Setting<Boolean> autoUse = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("auto-use")
            .description("Uses the selected explosion item when enabling.")
            .defaultValue(false)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Explosion)
            .build()
      );
   private final Setting<Boolean> stopMotion = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("stop-motion")
            .description("Stops horizontal motion while charging Aiglos.")
            .defaultValue(false)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Aiglos)
            .build()
      );
   public final Setting<Integer> chargeTicks = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("charge-ticks")
            .description("Ticks to charge Aiglos before releasing.")
            .defaultValue(15)
            .sliderRange(0, 200)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Aiglos)
            .build()
      );
   private final Setting<Double> pitch = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("pitch")
            .description("Aiglos release pitch.")
            .defaultValue(75.0)
            .sliderRange(0.0, 200.0)
            .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Aiglos)
            .build()
      );
   public final Setting<LongJumpV2.UseMode> useMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("use-mode"))
                     .description("Explosion item to use."))
                  .defaultValue(LongJumpV2.UseMode.FireBall))
               .visible(() -> this.jumpMode.get() == LongJumpV2.JumpMode.Explosion && this.autoUse.get()))
            .build()
      );
   private int stage;
   private double moveSpeed;
   private boolean jumping;
   private int airTicks;
   private int groundTicks;
   private boolean jumped;
   private boolean start = true;
   private boolean explosionSwapBack;
   private long explosionSwapBackTime;

   public LongJumpV2() {
      super(Categories.Movement, "LongJumpV2", "Allows you to jump further than normal.");
   }

   @Override
   public void onActivate() {
      this.stage = 0;
      this.jumping = false;
      this.airTicks = 0;
      this.groundTicks = -5;
      this.explosionSwapBack = false;
      this.explosionSwapBackTime = 0L;
      if (Utils.canUpdate() && this.jumpMode.get() == LongJumpV2.JumpMode.Explosion && this.autoUse.get()) {
         this.useExplosionItem();
      }
   }

   @Override
   public void onDeactivate() {
      this.start = true;
      this.mc.options.keyUse.setDown(false);
      if (this.explosionSwapBack) {
         SlotHelper.INSTANCE.stopNextTick();
         this.explosionSwapBack = false;
      }

      if (this.jumpMode.get() == LongJumpV2.JumpMode.Aiglos) {
         InvUtils.swapBack();
      }

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

      switch ((LongJumpV2.JumpMode)this.jumpMode.get()) {
         case Vanilla:
            this.vanilla(event);
            break;
         case Burst:
            this.burst(event);
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         this.trySwapBackExplosionItem();
         switch ((LongJumpV2.JumpMode)this.jumpMode.get()) {
            case Glide:
               this.glide();
               break;
            case TakaDamage:
               if (this.mc.player.hurtTime > 0) {
                  this.jump();
                  this.toggle();
               }
               break;
            case TakaSlime:
               if (this.mc.level.getBlockState(this.mc.player.blockPosition().below()).is(Blocks.SLIME_BLOCK)) {
                  this.jump();
                  this.toggle();
               }
               break;
            case TakaBoat:
               this.takaBoat();
               break;
            case MatrixWater:
               if (!this.mc.player.horizontalCollision || !this.mc.player.isInWater()) {
                  return;
               }

               Vec3 wallVelocity = this.mc.player.getDeltaMovement();
               if (wallVelocity.y >= 0.2) {
                  return;
               }

               this.mc.player.setDeltaMovement(wallVelocity.x, this.boostFactor.get(), wallVelocity.z);
               break;
            case VulcanWall:
               if (!this.mc.player.horizontalCollision) {
                  return;
               }

               Vec3 velocity = this.mc.player.getDeltaMovement();
               if (velocity.y >= 0.2) {
                  return;
               }

               this.mc.player.setDeltaMovement(velocity.x, this.boostFactor.get(), velocity.z);
            case GrimContainer:
            case Explosion:
            case Boost:
            default:
               break;
            case Aiglos:
               this.aiglos();
         }
      }
   }

   @EventHandler
   private void onPacketEvent(PacketEvent.Receive event) {
      if (Utils.canUpdate()) {
         if (this.jumpMode.get() == LongJumpV2.JumpMode.Explosion) {
            if (event.packet instanceof ClientboundExplodePacket packet && this.mc.player.distanceToSqr(packet.center()) <= 100.0) {
               this.applyVelocityBoost();
               this.toggle();
            } else if (event.packet instanceof ClientboundLevelParticlesPacket packet
               && this.mc.player.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) <= 100.0
               && packet.getParticle().getType() == ParticleTypes.EXPLOSION) {
               this.applyVelocityBoost();
               this.toggle();
            }
         }

         if (this.jumpMode.get() == LongJumpV2.JumpMode.Boost
            && event.packet instanceof ClientboundSetEntityMotionPacket packet
            && packet.id() == this.mc.player.getId()) {
            this.applyVelocityBoost();
            this.toggle();
         }

         if (this.jumpMode.get() == LongJumpV2.JumpMode.GrimContainer && this.mc.player.containerMenu instanceof ShulkerBoxMenu) {
            this.updateY(this.boostFactorY.get());
            this.toggle();
         }
      }
   }

   private void vanilla(PlayerMoveEvent event) {
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
   }

   private void burst(PlayerMoveEvent event) {
      if (this.stage != 0 && !this.mc.player.onGround() && this.autoDisable.get()) {
         this.jumping = true;
      }

      if (this.jumping && this.mc.player.getY() - (int)this.mc.player.getY() < 0.01) {
         this.jumping = false;
         this.toggle();
         this.info("Disabling after jump.");
      }

      if (!this.onlyOnGround.get() || this.mc.player.onGround() || this.stage != 0) {
         double xDist = this.mc.player.getX() - this.mc.player.xo;
         double zDist = this.mc.player.getZ() - this.mc.player.zo;
         double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);
         if (PlayerUtils.isMoving() && (!this.onJump.get() || this.mc.options.keyJump.isDown()) && !this.mc.player.isInLava() && !this.mc.player.isInWater()) {
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

   private void aiglos() {
      if (this.stopMotion.get()) {
         this.mc.player.setDeltaMovement(0.0, this.mc.player.getDeltaMovement().y, 0.0);
      }

      float yaw = this.mc.player.getYRot();
      float oppositeYaw = (yaw + 180.0F) % 360.0F;
      if (oppositeYaw < 0.0F) {
         oppositeYaw += 360.0F;
      }

      float releasePitch = this.pitch.get().floatValue();
      Rotations.rotate(oppositeYaw, releasePitch, -100, null);
      int slot = this.findItemByName("Aiglos");
      if (slot >= 0 && InvUtils.swap(slot, true)) {
         this.mc.player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, oppositeYaw, releasePitch));
         this.mc.options.keyUse.setDown(true);
         if (this.mc.player.getTicksUsingItem() >= this.chargeTicks.get()) {
            if (this.mc.player.onGround()) {
               this.mc.player.jumpFromGround();
            }

            BlockPos pos = BlockPos.containing(this.mc.player.pick(3.0, this.mc.getDeltaTracker().getGameTimeDeltaPartialTick(false), false).getLocation());
            this.mc.player.connection.send(new ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, pos, Direction.UP));
            this.mc.options.keyUse.setDown(false);
            InvUtils.swapBack();
            this.toggle();
         }
      }
   }

   private void takaBoat() {
      if (this.mc.player.isPassenger() && this.isBoat(this.mc.player)) {
         this.start = false;
      }

      if (!this.mc.player.isPassenger() && !this.start) {
         this.jump();
         this.toggle();
      }
   }

   private void glide() {
      if (PlayerUtils.isMoving()) {
         float yaw = this.mc.player.getYRot() + 90.0F;
         double forward = this.mc.player.zza != 0.0F ? (this.mc.player.zza > 0.0F ? 1 : -1) : 0.0;
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

   private boolean isBoat(Player player) {
      return player.getVehicle() instanceof AbstractBoat;
   }

   private void jump() {
      float yaw = this.mc.player.getYRot();
      double radian = Math.toRadians(yaw);
      Vec3 direction = new Vec3(-Math.sin(radian), this.boostFactorY.get(), Math.cos(radian)).normalize().scale(this.boostFactor.get());
      this.mc.player.setDeltaMovement(direction);
      this.mc.player.hurtMarked = true;
   }

   private void applyVelocityBoost() {
      this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
      this.updateY(this.boostFactorY.get());
      this.strafe(this.boostFactor.get().floatValue());
   }

   private void useExplosionItem() {
      FindItemResult item = switch ((LongJumpV2.UseMode)this.useMode.get()) {
         case FireBall -> InvUtils.findInHotbar(Items.FIRE_CHARGE);
         case WindCharge -> InvUtils.findInHotbar(Items.WIND_CHARGE);
      };
      if (item.found()) {
         InteractionHand hand = item.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (!item.isOffhand()) {
            SlotHelper.setCurrentItem(item.slot(), true);
            this.explosionSwapBack = true;
            this.explosionSwapBackTime = System.currentTimeMillis() + BypassSetting.getSwitchDelay();
         }

         this.mc.player.connection.send(new ServerboundUseItemPacket(hand, 0, this.mc.player.getYRot(), 90.0F));
      }
   }

   private void trySwapBackExplosionItem() {
      if (this.explosionSwapBack && System.currentTimeMillis() >= this.explosionSwapBackTime) {
         SlotHelper.INSTANCE.stop();
         this.explosionSwapBack = false;
      }
   }

   private int findItemByName(String name) {
      for (int i = 0; i < this.mc.player.getInventory().getContainerSize(); i++) {
         ItemStack stack = this.mc.player.getInventory().getItem(i);
         if (!stack.isEmpty() && stack.getHoverName().getString().equalsIgnoreCase(name)) {
            return i;
         }
      }

      return -1;
   }

   private void strafe(float speed) {
      if (PlayerUtils.isMoving()) {
         double dir = this.getDir();
         this.mc
            .player
            .setDeltaMovement(Math.cos(Math.toRadians(dir + 90.0)) * speed, this.mc.player.getDeltaMovement().y, Math.sin(Math.toRadians(dir + 90.0)) * speed);
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
               yaw += forward > 0.0 ? -45.0F : 45.0F;
            } else if (strafe < 0.0) {
               yaw += forward > 0.0 ? 45.0F : -45.0F;
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

   @Override
   public String getInfoString() {
      return this.jumpMode.get().name();
   }

   public enum JumpMode {
      Vanilla,
      Burst,
      Glide,
      TakaDamage,
      TakaSlime,
      TakaBoat,
      MatrixWater,
      VulcanWall,
      GrimContainer,
      Explosion,
      Boost,
      Aiglos;
   }

   public enum UseMode {
      FireBall,
      WindCharge;
   }
}
