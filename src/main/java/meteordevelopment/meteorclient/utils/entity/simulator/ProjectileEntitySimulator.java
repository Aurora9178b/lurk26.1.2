package meteordevelopment.meteorclient.utils.entity.simulator;

import java.util.ArrayList;
import java.util.Collection;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.CrossbowItemAccessor;
import meteordevelopment.meteorclient.mixin.ProjectileInGroundAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class ProjectileEntitySimulator {
   private final MutableBlockPos blockPos = new MutableBlockPos();
   private final Vec3 pos3d = new Vec3(0.0, 0.0, 0.0);
   private final Vec3 prevPos3d = new Vec3(0.0, 0.0, 0.0);
   public final Vector3d pos = new Vector3d();
   private final Vector3d velocity = new Vector3d();
   private Projectile simulatingEntity;
   private EntityDimensions dimensions;
   private int tickCount;
   private int pierceLevel;
   private double gravity;
   private float airDrag;
   private float waterDrag;
   private boolean isInWater;
   private static final ProjectileEntitySimulator.MotionData EGG = new ProjectileEntitySimulator.MotionData(1.5F, 0.0F, 0.03, 0.99F, 0.8F, EntityType.EGG);
   private static final ProjectileEntitySimulator.MotionData ENDER_PEARL = new ProjectileEntitySimulator.MotionData(
      1.5F, 0.0F, 0.03, 0.99F, 0.8F, EntityType.ENDER_PEARL
   );
   private static final ProjectileEntitySimulator.MotionData SNOWBALL = new ProjectileEntitySimulator.MotionData(
      1.5F, 0.0F, 0.03, 0.99F, 0.8F, EntityType.SNOWBALL
   );
   private static final ProjectileEntitySimulator.MotionData EXPERIENCE_BOTTLE = new ProjectileEntitySimulator.MotionData(
      0.7F, -20.0F, 0.07, 0.99F, 0.8F, EntityType.EXPERIENCE_BOTTLE
   );
   private static final ProjectileEntitySimulator.MotionData LINGERING_POTION = new ProjectileEntitySimulator.MotionData(
      0.5F, -20.0F, 0.05, 0.99F, 0.8F, EntityType.LINGERING_POTION
   );
   private static final ProjectileEntitySimulator.MotionData SPLASH_POTION = new ProjectileEntitySimulator.MotionData(
      0.5F, -20.0F, 0.05, 0.99F, 0.8F, EntityType.SPLASH_POTION
   );
   private static final ProjectileEntitySimulator.MotionData EXPLOSIVE = new ProjectileEntitySimulator.MotionData(0.0F, 0.0F, 0.0, 1.0F, 1.0F, null);
   private static final ProjectileEntitySimulator.MotionData WIND_CHARGE = new ProjectileEntitySimulator.MotionData(
      1.5F, 0.0F, 0.0, 1.0F, 1.0F, EntityType.WIND_CHARGE
   );
   private static final ProjectileEntitySimulator.MotionData ARROW = new ProjectileEntitySimulator.MotionData(0.0F, 0.0F, 0.05, 0.99F, 0.6F, EntityType.ARROW);
   private static final ProjectileEntitySimulator.MotionData TRIDENT = new ProjectileEntitySimulator.MotionData(
      2.5F, 0.0F, 0.05, 0.99F, 0.99F, EntityType.TRIDENT
   );
   private static final ProjectileEntitySimulator.MotionData FIREWORK_ROCKET = new ProjectileEntitySimulator.MotionData(
      0.0F, 0.0F, 0.0, 1.0F, 1.0F, EntityType.FIREWORK_ROCKET
   );
   private static final ProjectileEntitySimulator.MotionData FISHING_BOBBER = new ProjectileEntitySimulator.MotionData(
      0.0F, 0.0F, 0.03, 0.92F, 0.0F, EntityType.FISHING_BOBBER
   );
   private static final ProjectileEntitySimulator.MotionData LLAMA_SPIT = new ProjectileEntitySimulator.MotionData(
      1.5F, 0.0F, 0.06, 0.99F, 0.0F, EntityType.LLAMA_SPIT
   );

   public boolean set(Entity user, ItemStack itemStack, double angleOffset, boolean accurate, float tickDelta) {
      Item item = itemStack.getItem();
      switch (item) {
         case BowItem var14:
            if (!(user instanceof LivingEntity livingEntity)) {
               return false;
            }

            float charge = BowItem.getPowerForTime(livingEntity.getTicksUsingItem());
            if (charge <= 0.1) {
               if (user != MeteorClient.mc.player) {
                  return false;
               }

               charge = 1.0F;
            }

            this.set(user, angleOffset, accurate, tickDelta, ARROW.withPower(charge * 3.0F));
            break;
         case CrossbowItem var15:
            ChargedProjectiles projectilesComponent = itemStack.get(DataComponents.CHARGED_PROJECTILES);
            if (projectilesComponent == null) {
               return false;
            }

            float speed = CrossbowItemAccessor.meteor$getSpeed(projectilesComponent);
            if (projectilesComponent.contains(Items.FIREWORK_ROCKET)) {
               this.set(user, angleOffset, accurate, tickDelta, FIREWORK_ROCKET.withPower(speed));
            } else {
               this.set(user, angleOffset, accurate, tickDelta, ARROW.withPower(speed));
            }

            this.pierceLevel = projectilesComponent.contains(Items.FIREWORK_ROCKET) ? 0 : Utils.getEnchantmentLevel(itemStack, Enchantments.PIERCING);
            break;
         case WindChargeItem var16:
            this.set(user, angleOffset, accurate, tickDelta, WIND_CHARGE);
            break;
         case TridentItem var17:
            this.set(user, angleOffset, accurate, tickDelta, TRIDENT);
            break;
         case SnowballItem var18:
            this.set(user, angleOffset, accurate, tickDelta, SNOWBALL);
            break;
         case EggItem var19:
            this.set(user, angleOffset, accurate, tickDelta, EGG);
            break;
         case EnderpearlItem var20:
            this.set(user, angleOffset, accurate, tickDelta, ENDER_PEARL);
            break;
         case ExperienceBottleItem var21:
            this.set(user, angleOffset, accurate, tickDelta, EXPERIENCE_BOTTLE);
            break;
         case SplashPotionItem var22:
            this.set(user, angleOffset, accurate, tickDelta, SPLASH_POTION);
            break;
         case LingeringPotionItem var23:
            this.set(user, angleOffset, accurate, tickDelta, LINGERING_POTION);
            break;
         case FishingRodItem var24:
            this.setFishingBobber(user, tickDelta, FISHING_BOBBER);
            break;
         default:
            return false;
      }

      return true;
   }

   public void set(Entity user, double angleOffset, boolean accurate, float tickDelta, ProjectileEntitySimulator.MotionData data) {
      Pose pose = user.getPose();
      if (user == MeteorClient.mc.player && (Modules.get().get(NoSlow.class).airStrict() || Modules.get().get(Sneak.class).doPacket())) {
         pose = Pose.CROUCHING;
      }

      Utils.set(this.pos, user, tickDelta).add(0.0, user.getEyeHeight(pose) - 0.1F, 0.0);
      double yaw;
      double pitch;
      if (user == MeteorClient.mc.player && Rotations.rotating) {
         yaw = Rotations.serverYaw;
         pitch = Rotations.serverPitch;
      } else {
         yaw = user.getYRot(tickDelta);
         pitch = user.getXRot(tickDelta);
      }

      double x;
      double y;
      double z;
      if (angleOffset == 0.0) {
         x = -Math.sin(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
         y = -Math.sin((pitch + data.roll()) * 0.017453292);
         z = Math.cos(yaw * 0.017453292) * Math.cos(pitch * 0.017453292);
      } else {
         Vec3 oppositeRotationVec = user.getUpVector(1.0F);
         Quaterniond quaternion = new Quaterniond().setAngleAxis(angleOffset, oppositeRotationVec.x, oppositeRotationVec.y, oppositeRotationVec.z);
         Vec3 rotationVec = user.getViewVector(1.0F);
         Vector3d vector3d = new Vector3d(rotationVec.x, rotationVec.y, rotationVec.z);
         vector3d.rotate(quaternion);
         x = vector3d.x;
         y = vector3d.y;
         z = vector3d.z;
      }

      this.velocity.set(x, y, z).normalize().mul(data.power());
      if (accurate) {
         Vec3 vel = user.getKnownMovement();
         this.velocity.add(vel.x, user.onGround() ? 0.0 : vel.y, vel.z);
      }

      this.setSimulationData((Projectile)data.entity().create(MeteorClient.mc.level, null), data);
   }

   public void setFishingBobber(Entity user, float tickDelta, ProjectileEntitySimulator.MotionData data) {
      double yaw;
      double pitch;
      if (user == MeteorClient.mc.player && Rotations.rotating) {
         yaw = Rotations.serverYaw;
         pitch = Rotations.serverPitch;
      } else {
         yaw = user.getYRot(tickDelta);
         pitch = user.getXRot(tickDelta);
      }

      double h = Math.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      double i = Math.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      double j = -Math.cos(-pitch * (float) (Math.PI / 180.0));
      double k = Math.sin(-pitch * (float) (Math.PI / 180.0));
      Pose pose = user.getPose();
      if (user == MeteorClient.mc.player && (Modules.get().get(NoSlow.class).airStrict() || Modules.get().get(Sneak.class).doPacket())) {
         pose = Pose.CROUCHING;
      }

      Utils.set(this.pos, user, tickDelta).sub(i * 0.3, 0.0, h * 0.3).add(0.0, user.getEyeHeight(pose), 0.0);
      this.velocity.set(-i, Mth.clamp(-(k / j), -5.0, 5.0), -h);
      double l = this.velocity.length();
      this.velocity.mul(0.6 / l + 0.5, 0.6 / l + 0.5, 0.6 / l + 0.5);
      this.setSimulationData((Projectile)data.entity().create(MeteorClient.mc.level, null), data);
   }

   public boolean set(Entity entity) {
      if (entity instanceof ProjectileInGroundAccessor ppe && ppe.meteor$invokeIsInGround()) {
         return false;
      } else {
         switch (entity) {
            case Arrow e:
               this.set(e, ARROW);
               break;
            case SpectralArrow e:
               this.set(e, ARROW);
               break;
            case ThrownTrident e:
               this.set(e, TRIDENT);
               break;
            case ThrownEnderpearl e:
               this.set(e, ENDER_PEARL);
               break;
            case Snowball e:
               this.set(e, SNOWBALL);
               break;
            case ThrownEgg e:
               this.set(e, EGG);
               break;
            case ThrownExperienceBottle e:
               this.set(e, EXPERIENCE_BOTTLE);
               break;
            case ThrownSplashPotion e:
               this.set(e, SPLASH_POTION);
               break;
            case ThrownLingeringPotion e:
               this.set(e, LINGERING_POTION);
               break;
            case AbstractWindCharge e:
               this.set(e, WIND_CHARGE);
               break;
            case AbstractHurtingProjectile e:
               this.set(e, EXPLOSIVE);
               break;
            case LlamaSpit e:
               this.set(e, LLAMA_SPIT);
               break;
            default:
               return false;
         }

         if (entity.isNoGravity()) {
            this.gravity = 0.0;
         }

         return true;
      }
   }

   public void set(Projectile entity, ProjectileEntitySimulator.MotionData data) {
      this.pos.set(entity.getX(), entity.getY(), entity.getZ());
      double speed = entity.getDeltaMovement().length();
      this.velocity.set(entity.getDeltaMovement().x, entity.getDeltaMovement().y, entity.getDeltaMovement().z).normalize().mul(speed);
      this.setSimulationData(entity, data);
   }

   private void setSimulationData(Projectile entity, ProjectileEntitySimulator.MotionData data) {
      this.gravity = data.gravity();
      this.airDrag = data.airDrag();
      this.waterDrag = data.waterDrag();
      this.simulatingEntity = entity;
      this.dimensions = this.simulatingEntity.getDimensions(this.simulatingEntity.getPose());
      this.isInWater = this.simulatingEntity.isInWater();
      this.tickCount = this.simulatingEntity.tickCount;
      this.pierceLevel = 0;
   }

   public SimulationStep tick() {
      this.tickCount++;
      ((IVec3)this.prevPos3d).meteor$set(this.pos);
      if (this.simulatingEntity instanceof ThrowableProjectile || this.simulatingEntity instanceof AbstractHurtingProjectile) {
         this.velocity.sub(0.0, this.gravity, 0.0);
         this.velocity.mul(this.isInWater ? this.waterDrag : this.airDrag);
         this.pos.add(this.velocity);
         this.tickIsTouchingWater();
      } else if (this.simulatingEntity instanceof AbstractArrow || this.simulatingEntity instanceof LlamaSpit) {
         this.pos.add(this.velocity);
         this.velocity.mul(this.isInWater ? this.waterDrag : this.airDrag);
         this.velocity.sub(0.0, this.gravity, 0.0);
         this.tickIsTouchingWater();
      } else if (this.simulatingEntity instanceof Projectile) {
         this.tickIsTouchingWater();
         this.velocity.sub(0.0, this.gravity, 0.0);
         this.pos.add(this.velocity);
         this.velocity.mul(this.isInWater ? this.waterDrag : this.airDrag);
      }

      if (this.pos.y < MeteorClient.mc.level.getMinY()) {
         return SimulationStep.MISS;
      }

      int chunkX = SectionPos.posToSectionCoord(this.pos.x);
      int chunkZ = SectionPos.posToSectionCoord(this.pos.z);
      if (!MeteorClient.mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
         return SimulationStep.MISS;
      }

      ((IVec3)this.pos3d).meteor$set(this.pos);
      return this.pos3d.equals(this.prevPos3d) ? SimulationStep.MISS : this.getCollision();
   }

   public void tickIsTouchingWater() {
      AABB box = this.dimensions.makeBoundingBox(this.pos.x, this.pos.y, this.pos.z).deflate(0.001);
      int minX = Mth.floor(box.minX);
      int maxX = Mth.ceil(box.maxX);
      int minY = Mth.floor(box.minY);
      int maxY = Mth.ceil(box.maxY);
      int minZ = Mth.floor(box.minZ);
      int maxZ = Mth.ceil(box.maxZ);

      for (int x = minX; x < maxX; x++) {
         for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < maxZ; z++) {
               this.blockPos.set(x, y, z);
               FluidState fluidState = MeteorClient.mc.level.getFluidState(this.blockPos);
               if (fluidState.is(FluidTags.WATER)) {
                  double fluidY = y + fluidState.getHeight(MeteorClient.mc.level, this.blockPos);
                  if (fluidY >= box.minY) {
                     this.isInWater = true;
                     return;
                  }
               }
            }
         }
      }

      this.isInWater = false;
   }

   private SimulationStep getCollision() {
      HitResult blockCollision = MeteorClient.mc
         .level
         .clipIncludingBorder(
            new ClipContext(this.prevPos3d, this.pos3d, Block.COLLIDER, this.waterDrag == 0.0F ? Fluid.ANY : Fluid.NONE, this.simulatingEntity)
         );
      if (blockCollision.getType() != Type.MISS) {
         ((IVec3)this.pos3d).meteor$set(blockCollision.getLocation());
      }

      if (this.simulatingEntity instanceof AbstractArrow) {
         Collection<EntityHitResult> entityCollisions = ProjectileUtil.getManyEntityHitResult(
            MeteorClient.mc.level,
            this.simulatingEntity,
            this.prevPos3d,
            this.pos3d,
            this.dimensions.makeBoundingBox(this.prevPos3d).expandTowards(this.velocity.x, this.velocity.y, this.velocity.z).inflate(1.0),
            entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
            this.getToleranceMargin(),
            Block.COLLIDER,
            false
         );
         entityCollisions.removeIf(collision -> this.tickCount <= 1 && collision.getEntity() == MeteorClient.mc.player);
         if (entityCollisions.isEmpty()) {
            return new SimulationStep(this.hitOrDeflect(blockCollision), blockCollision);
         }

         boolean stop = false;
         ArrayList<EntityHitResult> hits = new ArrayList<>();

         for (EntityHitResult result : entityCollisions) {
            boolean hit = this.hitOrDeflect(result);
            if (!hit) {
               break;
            }

            hits.add(result);
            if (this.pierceLevel <= 0) {
               stop = true;
               break;
            }

            this.pierceLevel--;
         }

         return new SimulationStep(stop, hits.toArray(HitResult[]::new));
      } else {
         HitResult entityCollision = ProjectileUtil.getEntityHitResult(
            MeteorClient.mc.level,
            this.simulatingEntity,
            this.prevPos3d,
            this.pos3d,
            this.dimensions.makeBoundingBox(this.prevPos3d).expandTowards(this.velocity.x, this.velocity.y, this.velocity.z).inflate(1.0),
            entity -> !entity.isSpectator() && entity.isAlive() && entity.isPickable(),
            this.getToleranceMargin()
         );
         if (entityCollision == null || this.tickCount <= 1 && entityCollision instanceof EntityHitResult ehr && ehr.getEntity() == MeteorClient.mc.player) {
            return new SimulationStep(this.hitOrDeflect(blockCollision), blockCollision);
         } else {
            return this.hitOrDeflect(entityCollision) ? new SimulationStep(true, entityCollision) : new SimulationStep(false);
         }
      }
   }

   private boolean hitOrDeflect(HitResult hitResult) {
      if (!(hitResult instanceof EntityHitResult entityHitResult)) {
         if (hitResult instanceof BlockHitResult bhr) {
            Utils.set(this.pos, bhr.getLocation());
            if (this.simulatingEntity.shouldBounceOnWorldBorder() && bhr.isWorldBorderHit()) {
               this.velocity.mul(-0.5).mul(0.2);
               return false;
            } else {
               return bhr.getType() != Type.MISS;
            }
         } else {
            return false;
         }
      } else {
         Entity entity = entityHitResult.getEntity();
         Utils.set(this.pos, entityHitResult.getLocation());
         if ((!(entity instanceof Breeze) || this.simulatingEntity instanceof AbstractWindCharge)
            && entity.deflection(this.simulatingEntity) != ProjectileDeflection.REVERSE) {
            if (entity instanceof LivingEntity livingEntity && livingEntity.isBlocking() && this.simulatingEntity instanceof AbstractArrow) {
               this.velocity.mul(-0.5).mul(0.2);
               return this.velocity.lengthSquared() < 1.0E-7;
            } else {
               return true;
            }
         } else {
            this.velocity.mul(-0.5);
            return false;
         }
      }
   }

   private float getToleranceMargin() {
      return Math.clamp((this.tickCount - 2) / 20.0F, 0.0F, 0.3F);
   }

   public record MotionData(float power, float roll, double gravity, float airDrag, float waterDrag, EntityType<?> entity) {
      public ProjectileEntitySimulator.MotionData withPower(float power) {
         return new ProjectileEntitySimulator.MotionData(power, this.roll(), this.gravity(), this.airDrag(), this.waterDrag(), this.entity());
      }
   }
}
