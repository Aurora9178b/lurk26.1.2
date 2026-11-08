package meteordevelopment.meteorclient.systems.modules.movement;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.entity.EntityMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.phys.Vec3;

public class EntityControl extends Module {
   private final SettingGroup sgControl = this.settings.createGroup("Control");
   private final SettingGroup sgSpeed = this.settings.createGroup("Speed");
   private final SettingGroup sgFlight = this.settings.createGroup("Flight");
   private final List<EntityType<?>> list = BuiltInRegistries.ENTITY_TYPE
      .stream()
      .filter(
         entityType -> EntityUtils.isRideable((EntityType<?>)entityType)
            && entityType != EntityType.MINECART
            && entityType != EntityType.LLAMA
            && entityType != EntityType.TRADER_LLAMA
      )
      .toList();
   private final Setting<Set<EntityType<?>>> entities = this.sgControl
      .add(
         new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Target entities.")
            .filter(
               entityType -> EntityUtils.isRideable(entityType)
                  && entityType != EntityType.MINECART
                  && entityType != EntityType.LLAMA
                  && entityType != EntityType.TRADER_LLAMA
            )
            .defaultValue(this.list.toArray(EntityType[]::new))
            .build()
      );
   private final Setting<Boolean> spoofSaddle = this.sgControl
      .add(
         new BoolSetting.Builder()
            .name("spoof-saddle*")
            .description("Lets you control rideable entities without them being saddled. Only works on older server versions.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> maxJump = this.sgControl
      .add(new BoolSetting.Builder().name("max-jump").description("Sets jump power to maximum.").defaultValue(true).build());
   public final Setting<Boolean> lockYaw = this.sgControl
      .add(new BoolSetting.Builder().name("lock-yaw").description("Locks the Entity's yaw.").defaultValue(true).build());
   private final Setting<Boolean> cancelServerPackets = this.sgControl
      .add(
         new BoolSetting.Builder()
            .name("cancel-server-packets")
            .description("Cancels incoming vehicle move packets. WILL desync you from the server if you make an invalid movement.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> speed = this.sgSpeed
      .add(new BoolSetting.Builder().name("speed").description("Makes you go faster horizontally when riding entities.").defaultValue(false).build());
   private final Setting<Double> horizontalSpeed = this.sgSpeed
      .add(
         new DoubleSetting.Builder()
            .name("horizontal-speed")
            .description("Horizontal speed in blocks per second.")
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(50.0)
            .visible(this.speed::get)
            .build()
      );
   private final Setting<Boolean> onlyOnGround = this.sgSpeed
      .add(
         new BoolSetting.Builder()
            .name("only-on-ground")
            .description("Use speed only when standing on a block.")
            .defaultValue(false)
            .visible(this.speed::get)
            .build()
      );
   private final Setting<Boolean> inWater = this.sgSpeed
      .add(new BoolSetting.Builder().name("in-water").description("Use speed when in water.").defaultValue(true).visible(this.speed::get).build());
   private final Setting<Boolean> flight = this.sgFlight
      .add(new BoolSetting.Builder().name("fly").description("Allows you to fly with entities.").defaultValue(false).build());
   private final Setting<Double> verticalSpeed = this.sgFlight
      .add(
         new DoubleSetting.Builder()
            .name("vertical-speed")
            .description("Vertical speed in blocks per second.")
            .defaultValue(6.0)
            .min(0.0)
            .sliderMax(20.0)
            .visible(this.flight::get)
            .build()
      );
   private final Setting<Double> fallSpeed = this.sgFlight
      .add(
         new DoubleSetting.Builder()
            .name("fall-speed")
            .description("How fast you will fall in blocks per second. Set to a small value to prevent fly kicks.")
            .defaultValue(0.0)
            .min(0.0)
            .visible(this.flight::get)
            .build()
      );
   private final Setting<Boolean> antiKick = this.sgFlight
      .add(
         new BoolSetting.Builder()
            .name("anti-fly-kick")
            .description("Whether to prevent the server from kicking you for flying.")
            .defaultValue(true)
            .visible(this.flight::get)
            .build()
      );
   private final Setting<Integer> delay = this.sgFlight
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("The amount of delay, in ticks, between flying down a bit and return to original position")
            .defaultValue(40)
            .min(1)
            .sliderMax(80)
            .visible(() -> this.flight.get() && this.antiKick.get())
            .build()
      );
   private int delayLeft;
   private double lastPacketY = Double.MAX_VALUE;
   private boolean sentPacket = false;

   public EntityControl() {
      super(Categories.Movement, "entity-control", "Lets you control rideable entities without a saddle.", "entity-speed", "entity-fly", "boat-fly");
   }

   @Override
   public void onActivate() {
      this.delayLeft = this.delay.get();
      this.sentPacket = false;
      this.lastPacketY = Double.MAX_VALUE;
   }

   @EventHandler
   private void onPreTick(TickEvent.Pre event) {
      if (this.sentPacket && this.mc.player.getVehicle() != null) {
         ServerboundMoveVehiclePacket packet = ServerboundMoveVehiclePacket.fromEntity(this.mc.player.getVehicle());
         ((IVec3)packet.position()).meteor$setY(this.lastPacketY);
         this.mc.getConnection().send(packet);
         this.sentPacket = false;
      }

      this.delayLeft--;
   }

   @EventHandler
   private void onEntityMove(EntityMoveEvent event) {
      Entity entity = event.entity;
      if (event.entity.getControllingPassenger() == this.mc.player && this.entities.get().contains(entity.getType())) {
         double velX = entity.getDeltaMovement().x;
         double velY = entity.getDeltaMovement().y;
         double velZ = entity.getDeltaMovement().z;
         if (this.speed.get() && (!this.onlyOnGround.get() || entity.onGround() || entity.isFlyingVehicle()) && (this.inWater.get() || !entity.isInWater())) {
            Vec3 vel = PlayerUtils.getHorizontalVelocity(this.horizontalSpeed.get());
            velX = vel.x;
            velZ = vel.z;
         }

         if (this.flight.get()) {
            velY = 0.0;
            if (Input.isPressed(this.mc.options.keyJump)) {
               velY += this.verticalSpeed.get() / 20.0;
            }

            if (Input.isPressed(this.mc.options.keySprint)) {
               velY -= this.verticalSpeed.get() / 20.0;
            } else {
               velY -= this.fallSpeed.get() / 20.0;
            }
         }

         if (this.lockYaw.get()) {
            entity.setYRot(this.mc.player.getYRot());
         }

         ((IVec3)event.movement).meteor$set(velX, velY, velZ);
      }
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundMoveVehiclePacket packet && this.antiKick.get()) {
         double currentY = packet.position().y;
         if (this.delayLeft <= 0
            && !this.sentPacket
            && this.shouldFlyDown(currentY)
            && EntityUtils.isOnAir(this.mc.player.getVehicle())
            && !this.mc.player.getVehicle().isFlyingVehicle()) {
            ((IVec3)packet.position()).meteor$setY(this.lastPacketY - 0.0313);
            this.sentPacket = true;
            this.delayLeft = this.delay.get();
         }

         this.lastPacketY = currentY;
      }
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundMoveVehiclePacket && this.cancelServerPackets.get()) {
         event.cancel();
      }
   }

   private boolean shouldFlyDown(double currentY) {
      return currentY >= this.lastPacketY ? true : this.lastPacketY - currentY < 0.0313;
   }

   public boolean spoofSaddle() {
      return this.isActive() && this.spoofSaddle.get();
   }

   public boolean maxJump() {
      return this.isActive() && this.maxJump.get();
   }

   public boolean cancelJump() {
      return !(this.mc.player.getVehicle() instanceof PlayerRideableJumping)
         ? false
         : this.isActive() && this.entities.get().contains(this.mc.player.getVehicle().getType()) && this.flight.get();
   }
}
