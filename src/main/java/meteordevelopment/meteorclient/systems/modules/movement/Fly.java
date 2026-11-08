package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

public class Fly extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> vspeed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("VerticalSpeed").description(".").defaultValue(1.0).min(0.0).sliderRange(0.0, 10.0).build());
   private final Setting<Double> hspeed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("HorizontalSpeed").description(".").defaultValue(1.0).min(0.0).sliderRange(0.0, 10.0).build());
   public final Setting<Boolean> antikick = this.sgGeneral.add(new BoolSetting.Builder().name("AntiKick").description(".").defaultValue(true).build());
   private final Setting<Fly.AntiKickMode> antiKickMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("AntiKickMode"))
                     .description("The mode for anti kick."))
                  .defaultValue(Fly.AntiKickMode.Acid))
               .visible(this.antikick::get))
            .build()
      );
   private final Setting<Double> antiKickValue = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("GlideValue")
            .description(".")
            .defaultValue(-0.3)
            .sliderRange(-1.0, 1.0)
            .visible(() -> this.antikick.get() && this.antiKickMode.get() == Fly.AntiKickMode.Acid)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("Delay")
            .description("The amount of delay, in ticks, between flying down a bit and return to original position")
            .defaultValue(20)
            .min(1)
            .sliderMax(200)
            .visible(() -> this.antikick.get() && this.antiKickMode.get() == Fly.AntiKickMode.Meteor)
            .build()
      );
   private final Setting<Integer> offTime = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("OffTime")
            .description("The amount of delay, in ticks, to fly down a bit to reset floating ticks.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .visible(() -> this.antikick.get() && this.antiKickMode.get() == Fly.AntiKickMode.Meteor)
            .build()
      );
   private final Setting<Boolean> alienUp = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("AlienUp")
            .description("Allow upward movement for Alien anti-kick")
            .defaultValue(true)
            .visible(() -> this.antikick.get() && this.antiKickMode.get() == Fly.AntiKickMode.Alien)
            .build()
      );
   public final Setting<Boolean> fakeDamage = this.sgGeneral.add(new BoolSetting.Builder().name("FakeDamage").description(".").defaultValue(false).build());
   private boolean flip;
   private float lastYaw;
   int t1;
   int t2;
   private int delayLeft = 20;
   private int offLeft = 1;
   private boolean meteorFlip;
   private double lastPacketY = Double.MAX_VALUE;
   private long alienAntiKickTimer = 0L;
   private boolean alienShouldReturn = false;
   private double alienOriginalY = 0.0;
   int slot = -1;

   public Fly() {
      super(Categories.Movement, "Fly", "FLYYYY! No Fall is recommended with this module.");
   }

   @Override
   public void onActivate() {
      this.t1 = 0;
      this.t2 = 0;
      if (this.fakeDamage.get() && this.mc.player != null) {
         this.forceHurtCam(this.mc.player);
      }

      this.delayLeft = this.delay.get();
      this.offLeft = this.offTime.get();
      this.alienAntiKickTimer = System.currentTimeMillis();
      this.alienShouldReturn = false;
      this.alienOriginalY = 0.0;
   }

   @Override
   public void onDeactivate() {
      this.t1 = 0;
      this.t2 = 0;
      this.alienAntiKickTimer = 0L;
      this.alienShouldReturn = false;
      this.alienOriginalY = 0.0;
   }

   public void forceHurtCam(Player player) {
      if (player.level() != null) {
         player.hurtTime = 10;
         player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
      }
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      float currentYaw = this.mc.player.getYHeadRot();
      if (this.mc.player.fallDistance >= 3.0 && currentYaw == this.lastYaw && this.mc.player.getDeltaMovement().length() < 0.003) {
         this.flip = !this.flip;
      }

      this.lastYaw = currentYaw;
      if (this.antikick.get() && this.antiKickMode.get() == Fly.AntiKickMode.Meteor) {
         if (this.delayLeft > 0) {
            this.delayLeft--;
         }

         if (this.offLeft <= 0 && this.delayLeft <= 0) {
            this.delayLeft = this.delay.get();
            this.offLeft = this.offTime.get();
         } else if (this.delayLeft <= 0) {
            this.offLeft--;
         }
      }
   }

   public boolean isMoving() {
      return this.mc.player != null
         && this.mc.level != null
         && this.mc.player.input != null
         && (
            this.mc.player.input.keyPresses.forward()
               || this.mc.player.input.keyPresses.backward()
               || this.mc.player.input.keyPresses.left()
               || this.mc.player.input.keyPresses.right()
         );
   }

   public double[] forward(double d) {
      float forward = 0.0F;
      float sideways = 0.0F;
      if (this.mc.player.input.keyPresses.forward()) {
         forward++;
      }

      if (this.mc.player.input.keyPresses.backward()) {
         forward--;
      }

      if (this.mc.player.input.keyPresses.left()) {
         sideways++;
      }

      if (this.mc.player.input.keyPresses.right()) {
         sideways--;
      }

      float yaw = this.mc.player.getYHeadRot();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45 : 45;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45 : -45;
         }

         sideways = 0.0F;
         if (forward > 0.0F) {
            forward = 1.0F;
         } else if (forward < 0.0F) {
            forward = -1.0F;
         }
      }

      double sin = Math.sin(Math.toRadians(yaw + 90.0F));
      double cos = Math.cos(Math.toRadians(yaw + 90.0F));
      double d4 = forward * d * cos + sideways * d * sin;
      double d5 = forward * d * sin - sideways * d * cos;
      return new double[]{d4, d5};
   }

   @EventHandler
   private void onPostTick(TickEvent.Post event) {
      if (this.isMoving()) {
         double[] dir = this.forward(this.hspeed.get());
         this.mc.player.setDeltaMovement(dir[0], 0.0, dir[1]);
      } else {
         this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
      }

      if (this.mc.options.keyJump.isDown()) {
         this.mc.player.setDeltaMovement(this.mc.player.getDeltaMovement().add(0.0, this.vspeed.get(), 0.0));
      }

      if (this.mc.options.keyShift.isDown()) {
         this.mc.player.setDeltaMovement(this.mc.player.getDeltaMovement().add(0.0, -this.vspeed.get(), 0.0));
      }

      if (this.antikick.get()) {
         if (this.antiKickMode.get() == Fly.AntiKickMode.Acid) {
            this.mc.player.setDeltaMovement(this.mc.player.getDeltaMovement().add(0.0, this.antiKickValue.get(), 0.0));
         } else if (this.antiKickMode.get() == Fly.AntiKickMode.Meteor) {
            if (this.offLeft <= 0 && this.delayLeft <= 0 && EntityUtils.isOnAir(this.mc.player)) {
               this.mc
                  .player
                  .connection
                  .send(new Pos(this.mc.player.getX(), this.mc.player.getY() - 0.03125, this.mc.player.getZ(), true, this.mc.player.horizontalCollision));
            }
         } else if (this.antiKickMode.get() == Fly.AntiKickMode.Alien) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.alienAntiKickTimer >= 2000L && !this.alienShouldReturn) {
               this.alienOriginalY = this.mc.player.getY();
               this.mc.player.setDeltaMovement(this.mc.player.getDeltaMovement().add(0.0, -0.5, 0.0));
               this.alienShouldReturn = true;
               this.alienAntiKickTimer = currentTime;
            }

            if (this.alienShouldReturn && currentTime - this.alienAntiKickTimer >= 100L) {
               this.mc.player.setPos(this.mc.player.getX(), this.alienOriginalY, this.mc.player.getZ());
               this.alienShouldReturn = false;
            }
         }
      }
   }

   @Override
   public String getInfoString() {
      return "Vanilla";
   }

   public enum AntiKickMode {
      Acid,
      Meteor,
      Alien;
   }
}
