package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Vector2d;

public class Jesus extends Module {
   private final SettingGroup sgGeneral = this.settings.createGroup("General");
   private final SettingGroup sgWater = this.settings.createGroup("Water");
   private final SettingGroup sgLava = this.settings.createGroup("Lava");
   private final Setting<Boolean> powderSnow = this.sgGeneral
      .add(new BoolSetting.Builder().name("powder-snow").description("Walk on powder snow.").defaultValue(true).build());
   private final Setting<Boolean> ncpBypass = this.sgGeneral
      .add(new BoolSetting.Builder().name("ncp-bypass").description("Whether to apply a bypass for NCP.").defaultValue(false).build());
   private final Setting<Boolean> slowDown = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("slow-down")
            .description("Further movement option to try bypassing NCP")
            .defaultValue(false)
            .visible(this.ncpBypass::get)
            .build()
      );
   private final Setting<Jesus.Mode> waterMode = this.sgWater
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("How to treat the water."))
               .defaultValue(Jesus.Mode.Solid))
            .build()
      );
   private final Setting<Boolean> dipIfBurning = this.sgWater
      .add(
         new BoolSetting.Builder()
            .name("dip-if-burning")
            .description("Lets you go into the water when you are burning.")
            .defaultValue(true)
            .visible(() -> this.waterMode.get() == Jesus.Mode.Solid)
            .build()
      );
   private final Setting<Boolean> dipOnSneakWater = this.sgWater
      .add(
         new BoolSetting.Builder()
            .name("dip-on-sneak")
            .description("Lets you go into the water when your sneak key is held.")
            .defaultValue(true)
            .visible(() -> this.waterMode.get() == Jesus.Mode.Solid)
            .build()
      );
   private final Setting<Boolean> dipOnFallWater = this.sgWater
      .add(
         new BoolSetting.Builder()
            .name("dip-on-fall")
            .description("Lets you go into the water when you fall over a certain height.")
            .defaultValue(true)
            .visible(() -> this.waterMode.get() == Jesus.Mode.Solid)
            .build()
      );
   private final Setting<Integer> dipFallHeightWater = this.sgWater
      .add(
         new IntSetting.Builder()
            .name("dip-fall-height")
            .description("The fall height at which you will go into the water.")
            .defaultValue(4)
            .range(1, 255)
            .sliderRange(3, 20)
            .visible(() -> this.waterMode.get() == Jesus.Mode.Solid && this.dipOnFallWater.get())
            .build()
      );
   private final Setting<Jesus.Mode> lavaMode = this.sgLava
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("How to treat the lava."))
               .defaultValue(Jesus.Mode.Solid))
            .build()
      );
   private final Setting<Boolean> dipIfFireResistant = this.sgLava
      .add(
         new BoolSetting.Builder()
            .name("dip-if-resistant")
            .description("Lets you go into the lava if you have Fire Resistance effect.")
            .defaultValue(true)
            .visible(() -> this.lavaMode.get() == Jesus.Mode.Solid)
            .build()
      );
   private final Setting<Boolean> dipOnSneakLava = this.sgLava
      .add(
         new BoolSetting.Builder()
            .name("dip-on-sneak")
            .description("Lets you go into the lava when your sneak key is held.")
            .defaultValue(true)
            .visible(() -> this.lavaMode.get() == Jesus.Mode.Solid)
            .build()
      );
   private final Setting<Boolean> dipOnFallLava = this.sgLava
      .add(
         new BoolSetting.Builder()
            .name("dip-on-fall")
            .description("Lets you go into the lava when you fall over a certain height.")
            .defaultValue(true)
            .visible(() -> this.lavaMode.get() == Jesus.Mode.Solid)
            .build()
      );
   private final Setting<Integer> dipFallHeightLava = this.sgLava
      .add(
         new IntSetting.Builder()
            .name("dip-fall-height")
            .description("The fall height at which you will go into the lava.")
            .defaultValue(4)
            .range(1, 255)
            .sliderRange(3, 20)
            .visible(() -> this.lavaMode.get() == Jesus.Mode.Solid && this.dipOnFallLava.get())
            .build()
      );
   private int ascending = 10;
   private int swimmingTicks = 0;
   private boolean prePathManagerWalkOnWater;
   private boolean prePathManagerWalkOnLava;
   public boolean isInBubbleColumn = false;

   public Jesus() {
      super(Categories.Movement, "jesus", "Walk on liquids and powder snow like Jesus.");
   }

   @Override
   public void onActivate() {
      this.prePathManagerWalkOnWater = PathManagers.get().getSettings().getWalkOnWater().get();
      this.prePathManagerWalkOnLava = PathManagers.get().getSettings().getWalkOnLava().get();
      PathManagers.get().getSettings().getWalkOnWater().set(this.waterMode.get() == Jesus.Mode.Solid);
      PathManagers.get().getSettings().getWalkOnLava().set(this.lavaMode.get() == Jesus.Mode.Solid);
   }

   @Override
   public void onDeactivate() {
      PathManagers.get().getSettings().getWalkOnWater().set(this.prePathManagerWalkOnWater);
      PathManagers.get().getSettings().getWalkOnLava().set(this.prePathManagerWalkOnLava);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      boolean bubbleColumn = this.isInBubbleColumn;
      this.isInBubbleColumn = false;
      if (!this.mc.player.isVisuallySwimming()) {
         if (!this.mc.player.isInWater() || this.waterShouldBeSolid()) {
            if (!this.mc.player.isInLava() || this.lavaShouldBeSolid()) {
               Entity movingEntity = this.mc.player.isPassenger() ? this.mc.player.getVehicle() : this.mc.player;
               if (bubbleColumn) {
                  if (this.mc.options.keyJump.isDown() && movingEntity.getDeltaMovement().y() < 0.11) {
                     ((IVec3)movingEntity.getDeltaMovement()).meteor$setY(0.11);
                  }
               } else if (!movingEntity.isInWater() && !movingEntity.isInLava()) {
                  BlockState blockBelowState = this.mc.level.getBlockState(movingEntity.blockPosition().below());
                  boolean waterLogged = blockBelowState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);
                  if (this.ascending == 0) {
                     ((IVec3)movingEntity.getDeltaMovement()).meteor$setY(0.11);
                  } else if (this.ascending == 1 && (blockBelowState.getBlock() == Blocks.WATER || blockBelowState.getBlock() == Blocks.LAVA || waterLogged)) {
                     ((IVec3)movingEntity.getDeltaMovement()).meteor$setY(0.0);
                  }

                  this.ascending++;
               } else {
                  ((IVec3)movingEntity.getDeltaMovement()).meteor$setY(0.11);
                  this.ascending = 0;
               }
            }
         }
      }
   }

   @EventHandler
   private void onCanWalkOnFluid(CanWalkOnFluidEvent event) {
      if (this.mc.player == null || !this.mc.player.isSwimming()) {
         if ((event.fluidState.getType() == Fluids.WATER || event.fluidState.getType() == Fluids.FLOWING_WATER) && this.waterShouldBeSolid()) {
            event.walkOnFluid = true;
         } else if ((event.fluidState.getType() == Fluids.LAVA || event.fluidState.getType() == Fluids.FLOWING_LAVA) && this.lavaShouldBeSolid()) {
            event.walkOnFluid = true;
         }
      }
   }

   @EventHandler
   private void onFluidCollisionShape(CollisionShapeEvent event) {
      if (!event.state.getFluidState().isEmpty()) {
         if ((event.state.getBlock() == Blocks.WATER || event.state.getFluidState().getType() == Fluids.WATER)
            && !this.mc.player.isInWater()
            && this.waterShouldBeSolid()
            && event.pos.getY() <= this.mc.player.getY() - 1.0) {
            event.shape = Shapes.block();
         } else if (event.state.getBlock() == Blocks.LAVA
            && !this.mc.player.isInLava()
            && this.lavaShouldBeSolid()
            && (this.isLavaDangerous() || event.pos.getY() <= this.mc.player.getY() - 1.0)) {
            event.shape = Shapes.block();
         }
      }
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundMovePlayerPacket packet) {
         if (!this.mc.player.isInWater() || this.waterShouldBeSolid()) {
            if (!this.mc.player.isInLava() || this.lavaShouldBeSolid()) {
               if (this.ncpBypass.get()) {
                  Tuple<Boolean, Boolean> overLiquid = this.isOverLiquid();
                  boolean shouldWork = overLiquid.getA() && this.waterShouldBeSolid() || overLiquid.getB() && this.lavaShouldBeSolid();
                  if (!this.mc.player.isInWater() && !this.mc.player.isInLava() && !(this.mc.player.fallDistance > 3.0) && shouldWork) {
                     ((ServerboundMovePlayerPacketAccessor)packet).meteor$setOnGround(false);
                     if (this.mc.player.onGround() && packet.hasPosition()) {
                        ((ServerboundMovePlayerPacketAccessor)packet).meteor$setY(packet.getY(0.0) - (0.02 + 1.0E-4 * this.swimmingTicks));
                     }
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onMoveEvent(PlayerMoveEvent event) {
      if (this.ncpBypass.get()) {
         Tuple<Boolean, Boolean> overLiquid = this.isOverLiquid();
         boolean water = overLiquid.getA() && this.waterShouldBeSolid();
         boolean lava = overLiquid.getB() && this.lavaShouldBeSolid();
         if (!water && !lava) {
            this.swimmingTicks = 0;
         } else if (++this.swimmingTicks < 15) {
            if (this.mc.player.onGround()) {
               Vector2d vel = meteordevelopment.meteorclient.systems.modules.movement.speed.modes.Strafe.transformStrafe(PlayerUtils.isMoving() ? 0.2873 : 0.0);
               ((IVec3)event.movement).meteor$setXZ(vel.x, vel.y);
            }
         } else {
            this.swimmingTicks = 0;
            if (this.slowDown.get()) {
               ((IVec3)event.movement).meteor$setXZ(0.0, 0.0);
            }

            ((IVec3)this.mc.player.getDeltaMovement()).meteor$setY(0.08);
         }
      }
   }

   private boolean waterShouldBeSolid() {
      if (EntityUtils.getGameMode(this.mc.player) == GameType.SPECTATOR || this.mc.player.getAbilities().flying) {
         return false;
      } else if (this.mc.player.getVehicle() != null && this.mc.player.getVehicle() instanceof AbstractBoat) {
         return false;
      } else if (Modules.get().get(Flight.class).isActive()) {
         return false;
      } else if (this.dipIfBurning.get() && this.mc.player.isOnFire()) {
         return false;
      } else if (this.dipOnSneakWater.get() && this.mc.options.keyShift.isDown()) {
         return false;
      } else {
         return this.dipOnFallWater.get() && this.mc.player.fallDistance > this.dipFallHeightWater.get().intValue()
            ? false
            : this.waterMode.get() == Jesus.Mode.Solid;
      }
   }

   private boolean lavaShouldBeSolid() {
      if (EntityUtils.getGameMode(this.mc.player) == GameType.SPECTATOR || this.mc.player.getAbilities().flying) {
         return false;
      } else if (this.mc.player.getVehicle() != null && this.mc.player.getVehicle() instanceof Strider) {
         return false;
      } else if (this.isLavaDangerous() && this.lavaMode.get() == Jesus.Mode.Solid) {
         return true;
      } else if (this.dipOnSneakLava.get() && this.mc.options.keyShift.isDown()) {
         return false;
      } else {
         return this.dipOnFallLava.get() && this.mc.player.fallDistance > this.dipFallHeightLava.get().intValue()
            ? false
            : this.lavaMode.get() == Jesus.Mode.Solid;
      }
   }

   private boolean isLavaDangerous() {
      return !this.dipIfFireResistant.get()
         ? true
         : !this.mc.player.hasEffect(MobEffects.FIRE_RESISTANCE)
            || this.mc.player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() <= 300.0 * this.mc.player.getAttributeValue(Attributes.BURNING_TIME);
   }

   private Tuple<Boolean, Boolean> isOverLiquid() {
      AABB box = this.mc.player.isPassenger()
         ? this.mc.player.getBoundingBox().minmax(this.mc.player.getVehicle().getBoundingBox())
         : this.mc.player.getBoundingBox();
      BlockState[] states = this.mc.level.getBlockStatesIfLoaded(box.move(0.0, -0.01, 0.0)).toArray(BlockState[]::new);
      boolean water = false;
      boolean lava = false;
      boolean foundSolid = false;

      for (BlockState state : states) {
         if (state.getBlock() == Blocks.WATER || state.getFluidState().getType() == Fluids.WATER) {
            water = true;
         } else if (state.getBlock() == Blocks.LAVA) {
            lava = true;
         } else if (!state.isAir()) {
            foundSolid = true;
            break;
         }
      }

      return new Tuple<>(water && !foundSolid, lava && !foundSolid);
   }

   public boolean canWalkOnPowderSnow() {
      return this.isActive() && this.powderSnow.get();
   }

   public enum Mode {
      Solid,
      Ignore;
   }
}
