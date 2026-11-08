package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LocalPlayerAccessor;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.phys.Vec3;

public class Flight extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgAntiKick = this.settings.createGroup("Anti Kick");
   private final Setting<Flight.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                     .description("The mode for Flight."))
                  .defaultValue(Flight.Mode.Abilities))
               .onChanged(mode -> {
                  if (this.isActive() && Utils.canUpdate()) {
                     this.abilitiesOff();
                  }
               }))
            .build()
      );
   private final Setting<Double> speed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("speed").description("Your speed when flying.").defaultValue(0.1).min(0.0).build());
   private final Setting<Boolean> verticalSpeedMatch = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("vertical-speed-match")
            .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noSneak = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("no-sneak")
            .description("Prevents you from sneaking while flying.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == Flight.Mode.Velocity)
            .build()
      );
   private final Setting<Flight.AntiKickMode> antiKickMode = this.sgAntiKick
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("The mode for anti kick."))
               .defaultValue(Flight.AntiKickMode.Packet))
            .build()
      );
   private final Setting<Integer> delay = this.sgAntiKick
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("The amount of delay, in ticks, between flying down a bit and return to original position")
            .defaultValue(20)
            .min(1)
            .sliderMax(200)
            .build()
      );
   private final Setting<Integer> offTime = this.sgAntiKick
      .add(
         new IntSetting.Builder()
            .name("off-time")
            .description("The amount of delay, in ticks, to fly down a bit to reset floating ticks.")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 20)
            .build()
      );
   private int delayLeft = this.delay.get();
   private int offLeft = this.offTime.get();
   private boolean flip;
   private float lastYaw;
   private double lastPacketY = Double.MAX_VALUE;

   public Flight() {
      super(Categories.Movement, "flight", "FLYYYY! No Fall is recommended with this module.");
   }

   @Override
   public void onActivate() {
      if (this.mode.get() == Flight.Mode.Abilities && !this.mc.player.isSpectator()) {
         this.mc.player.getAbilities().flying = true;
         if (this.mc.player.getAbilities().instabuild) {
            return;
         }

         this.mc.player.getAbilities().mayfly = true;
      }
   }

   @Override
   public void onDeactivate() {
      if (this.mode.get() == Flight.Mode.Abilities && !this.mc.player.isSpectator()) {
         this.abilitiesOff();
      }
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      float currentYaw = this.mc.player.getYRot();
      if (this.mc.player.fallDistance >= 3.0 && currentYaw == this.lastYaw && this.mc.player.getDeltaMovement().length() < 0.003) {
         this.mc.player.setYRot(currentYaw + (this.flip ? 1 : -1));
         this.flip = !this.flip;
      }

      this.lastYaw = currentYaw;
   }

   @EventHandler
   private void onPostTick(TickEvent.Post event) {
      if (this.delayLeft > 0) {
         this.delayLeft--;
      }

      if (this.offLeft <= 0 && this.delayLeft <= 0) {
         this.delayLeft = this.delay.get();
         this.offLeft = this.offTime.get();
         if (this.antiKickMode.get() == Flight.AntiKickMode.Packet) {
            ((LocalPlayerAccessor)this.mc.player).meteor$setPositionReminder(20);
         }
      } else if (this.delayLeft <= 0) {
         boolean shouldReturn = false;
         if (this.antiKickMode.get() == Flight.AntiKickMode.Normal) {
            if (this.mode.get() == Flight.Mode.Abilities) {
               this.abilitiesOff();
               shouldReturn = true;
            }
         } else if (this.antiKickMode.get() == Flight.AntiKickMode.Packet && this.offLeft == this.offTime.get()) {
            ((LocalPlayerAccessor)this.mc.player).meteor$setPositionReminder(20);
         }

         this.offLeft--;
         if (shouldReturn) {
            return;
         }
      }

      if (this.mc.player.getYRot() != this.lastYaw) {
         this.mc.player.setYRot(this.lastYaw);
      }

      switch ((Flight.Mode)this.mode.get()) {
         case Abilities:
            if (this.mc.player.isSpectator()) {
               return;
            }

            this.mc.player.getAbilities().setFlyingSpeed(this.speed.get().floatValue());
            this.mc.player.getAbilities().flying = true;
            if (this.mc.player.getAbilities().instabuild) {
               return;
            }

            this.mc.player.getAbilities().mayfly = true;
            break;
         case Velocity:
            this.mc.player.getAbilities().flying = false;
            this.mc.player.setDeltaMovement(0.0, 0.0, 0.0);
            Vec3 playerVelocity = this.mc.player.getDeltaMovement();
            if (this.mc.options.keyJump.isDown()) {
               playerVelocity = playerVelocity.add(0.0, this.speed.get() * (this.verticalSpeedMatch.get() ? 10.0F : 5.0F), 0.0);
            }

            if (this.mc.options.keyShift.isDown()) {
               playerVelocity = playerVelocity.subtract(0.0, this.speed.get() * (this.verticalSpeedMatch.get() ? 10.0F : 5.0F), 0.0);
            }

            this.mc.player.setDeltaMovement(playerVelocity);
            if (this.noSneak.get()) {
               this.mc.player.setOnGround(false);
            }
      }
   }

   private void antiKickPacket(ServerboundMovePlayerPacket packet, double currentY) {
      if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE && this.shouldFlyDown(currentY, this.lastPacketY) && EntityUtils.isOnAir(this.mc.player)) {
         ((ServerboundMovePlayerPacketAccessor)packet).meteor$setY(this.lastPacketY - 0.0313);
      } else {
         this.lastPacketY = currentY;
      }
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundMovePlayerPacket packet && this.antiKickMode.get() == Flight.AntiKickMode.Packet) {
         double currentY = packet.getY(Double.MAX_VALUE);
         if (currentY != Double.MAX_VALUE) {
            this.antiKickPacket(packet, currentY);
         } else {
            ServerboundMovePlayerPacket fullPacket;
            if (packet.hasRotation()) {
               fullPacket = new PosRot(
                  this.mc.player.getX(),
                  this.mc.player.getY(),
                  this.mc.player.getZ(),
                  packet.getYRot(0.0F),
                  packet.getXRot(0.0F),
                  packet.isOnGround(),
                  this.mc.player.horizontalCollision
               );
            } else {
               fullPacket = new Pos(
                  this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), packet.isOnGround(), this.mc.player.horizontalCollision
               );
            }

            event.cancel();
            this.antiKickPacket(fullPacket, this.mc.player.getY());
            this.mc.getConnection().send(fullPacket);
         }
      }
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundPlayerAbilitiesPacket packet && this.mode.get() == Flight.Mode.Abilities) {
         event.cancel();
         this.mc.player.getAbilities().invulnerable = packet.isInvulnerable();
         this.mc.player.getAbilities().instabuild = packet.canInstabuild();
         this.mc.player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
      }
   }

   private boolean shouldFlyDown(double currentY, double lastY) {
      return currentY >= lastY ? true : lastY - currentY < 0.0313;
   }

   private void abilitiesOff() {
      this.mc.player.getAbilities().flying = false;
      this.mc.player.getAbilities().setFlyingSpeed(0.05F);
      if (!this.mc.player.getAbilities().instabuild) {
         this.mc.player.getAbilities().mayfly = false;
      }
   }

   public float getFlyingSpeed() {
      return this.isActive() && this.mode.get() == Flight.Mode.Velocity
         ? this.speed.get().floatValue() * (this.mc.player.isSprinting() ? 15.0F : 10.0F)
         : -1.0F;
   }

   public boolean noSneak() {
      return this.isActive() && this.mode.get() == Flight.Mode.Velocity && this.noSneak.get();
   }

   public enum AntiKickMode {
      Normal,
      Packet,
      None;
   }

   public enum Mode {
      Abilities,
      Velocity;
   }
}
