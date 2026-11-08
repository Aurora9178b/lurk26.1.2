package meteordevelopment.meteorclient.systems.modules.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.simulator.ProjectileEntitySimulator;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ArrowDodge extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgMovement = this.settings.createGroup("Movement");
   private final Setting<ArrowDodge.MoveType> moveType = this.sgMovement
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("move-type"))
                  .description("The way you are moved by this module."))
               .defaultValue(ArrowDodge.MoveType.Velocity))
            .build()
      );
   private final Setting<Double> moveSpeed = this.sgMovement
      .add(
         new DoubleSetting.Builder()
            .name("move-speed")
            .description("How fast should you be when dodging arrow.")
            .defaultValue(1.0)
            .min(0.01)
            .sliderRange(0.01, 5.0)
            .build()
      );
   private final Setting<Double> distanceCheck = this.sgMovement
      .add(
         new DoubleSetting.Builder()
            .name("distance-check")
            .description("How far should an arrow be from the player to be considered not hitting.")
            .defaultValue(1.0)
            .min(0.01)
            .sliderRange(0.01, 5.0)
            .build()
      );
   private final Setting<Boolean> groundCheck = this.sgGeneral
      .add(new BoolSetting.Builder().name("ground-check").description("Tries to prevent you from falling to your death.").defaultValue(true).build());
   private final Setting<Boolean> allProjectiles = this.sgGeneral
      .add(new BoolSetting.Builder().name("all-projectiles").description("Dodge all projectiles, not only arrows.").defaultValue(false).build());
   private final Setting<Boolean> ignoreOwn = this.sgGeneral
      .add(new BoolSetting.Builder().name("ignore-own").description("Ignore your own projectiles.").defaultValue(true).build());
   public final Setting<Integer> simulationSteps = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("simulation-steps")
            .description("How many steps to simulate projectiles. Zero for no limit.")
            .defaultValue(500)
            .sliderMax(5000)
            .build()
      );
   private final List<Vec3> possibleMoveDirections = Arrays.asList(
      new Vec3(1.0, 0.0, 1.0),
      new Vec3(0.0, 0.0, 1.0),
      new Vec3(-1.0, 0.0, 1.0),
      new Vec3(1.0, 0.0, 0.0),
      new Vec3(-1.0, 0.0, 0.0),
      new Vec3(1.0, 0.0, -1.0),
      new Vec3(0.0, 0.0, -1.0),
      new Vec3(-1.0, 0.0, -1.0)
   );
   private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
   private final Pool<Vector3d> vec3s = new Pool<>(Vector3d::new);
   private final List<Vector3d> points = new ArrayList<>();

   public ArrowDodge() {
      super(Categories.Combat, "arrow-dodge", "Tries to dodge arrows coming at you.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.vec3s.freeAll(this.points);
      this.points.clear();
      Iterator speed = this.mc.level.entitiesForRendering().iterator();
      Projectile projectile = null;

      while (true) {
         while (true) {
            if (!speed.hasNext()) {
               if (this.isValid(Vec3.ZERO, false)) {
                  return;
               }

               double speedx = this.moveSpeed.get();

               for (int i = 0; i < 500; i++) {
                  boolean didMove = false;
                  Collections.shuffle(this.possibleMoveDirections);

                  for (Vec3 direction : this.possibleMoveDirections) {
                     Vec3 velocity = direction.scale(speedx);
                     if (this.isValid(velocity, true)) {
                        this.move(velocity);
                        didMove = true;
                        break;
                     }
                  }

                  if (didMove) {
                     break;
                  }

                  speedx += this.moveSpeed.get();
               }

               return;
            }

            Entity e = (Entity)speed.next();
            if (e instanceof Projectile candidate && (this.allProjectiles.get() || candidate instanceof Arrow || candidate instanceof SpectralArrow)) {
               if (!this.ignoreOwn.get()) {
                  projectile = candidate;
                  break;
               }

               Entity owner = candidate.getOwner();
               if (owner == null || !owner.getUUID().equals(this.mc.player.getUUID())) {
                  projectile = candidate;
                  break;
               }
            }
         }

         if (this.simulator.set(projectile)) {
            for (int i = 0; i < (this.simulationSteps.get() > 0 ? this.simulationSteps.get() : Integer.MAX_VALUE); i++) {
               this.points.add(this.vec3s.get().set(this.simulator.pos));
               if (this.simulator.tick().shouldStop) {
                  break;
               }
            }
         }
      }
   }

   private void move(Vec3 vel) {
      this.move(vel.x, vel.y, vel.z);
   }

   private void move(double velX, double velY, double velZ) {
      switch ((ArrowDodge.MoveType)this.moveType.get()) {
         case Velocity:
            this.mc.player.setDeltaMovement(velX, velY, velZ);
            break;
         case Packet:
            Vec3 newPos = this.mc.player.position().add(velX, velY, velZ);
            this.mc.player.connection.send(new Pos(newPos.x, newPos.y, newPos.z, false, this.mc.player.horizontalCollision));
            this.mc.player.connection.send(new Pos(newPos.x, newPos.y - 0.01, newPos.z, true, this.mc.player.horizontalCollision));
      }
   }

   private boolean isValid(Vec3 velocity, boolean checkGround) {
      Vec3 playerPos = this.mc.player.position().add(velocity);
      Vec3 headPos = playerPos.add(0.0, 1.0, 0.0);

      for (Vector3d pos : this.points) {
         Vec3 projectilePos = new Vec3(pos.x, pos.y, pos.z);
         if (projectilePos.closerThan(playerPos, this.distanceCheck.get())) {
            return false;
         }

         if (projectilePos.closerThan(headPos, this.distanceCheck.get())) {
            return false;
         }
      }

      if (checkGround) {
         BlockPos blockPos = this.mc.player.blockPosition().offset(BlockPos.containing(velocity.x, velocity.y, velocity.z));
         if (!this.mc.level.getBlockState(blockPos).getCollisionShape(this.mc.level, blockPos).isEmpty()) {
            return false;
         }

         if (!this.mc.level.getBlockState(blockPos.above()).getCollisionShape(this.mc.level, blockPos.above()).isEmpty()) {
            return false;
         }

         if (this.groundCheck.get()) {
            return !this.mc.level.getBlockState(blockPos.below()).getCollisionShape(this.mc.level, blockPos.below()).isEmpty();
         }
      }

      return true;
   }

   public enum MoveType {
      Velocity,
      Packet;
   }
}
