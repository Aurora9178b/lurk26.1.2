package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IServerboundMovePlayerPacket;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MaceKill extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgSpecial = this.settings.createGroup("Values higher than 10 only work on Paper/Spigot");
   private final Setting<MaceKill.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("Mode")).description("Bypass Mode"))
               .defaultValue(MaceKill.Mode.Normal))
            .build()
      );
   private final Setting<MaceKill.VulcanMode> vulcanMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("Vulcan Jump Bypass"))
                     .description("Bypass Mode"))
                  .defaultValue(MaceKill.VulcanMode.Jump))
               .visible(() -> this.mode.get() == MaceKill.Mode.Vulcan))
            .build()
      );
   private final Setting<Integer> fallHeight = this.sgSpecial
      .add(
         new IntSetting.Builder()
            .name("Mace Power (Fall height)")
            .description("Simulates a fall from this distance")
            .defaultValue(10)
            .sliderRange(1, 170)
            .min(1)
            .build()
      );
   private final Setting<Boolean> maxPower = this.sgSpecial
      .add(
         new BoolSetting.Builder()
            .name("Maximum Mace Power (Paper/Spigot servers only)")
            .description("Simulates a fall from the highest air gap within 170 blocks")
            .defaultValue(false)
            .visible(() -> this.mode.get() == MaceKill.Mode.Normal)
            .build()
      );
   private final Setting<Boolean> disableWhenBlocked = this.sgSpecial
      .add(
         new BoolSetting.Builder()
            .name("Disable When Blocked")
            .description("Does not send movement packets if the attack was blocked. (prevents death)")
            .defaultValue(true)
            .visible(() -> this.mode.get() == MaceKill.Mode.Normal || this.mode.get() == MaceKill.Mode.Packet)
            .build()
      );
   private final Setting<Boolean> ignorePacket = this.sgSpecial
      .add(new BoolSetting.Builder().name("Ignore Packet").description(".").defaultValue(true).build());
   private final Setting<Boolean> debug = this.sgSpecial.add(new BoolSetting.Builder().name("Debug").description(".").defaultValue(false).build());
   private ServerboundAttackPacket attackPacket;
   private ServerboundSwingPacket swingPacket;
   private boolean sendPackets;
   private int sendTimer;
   private Entity target;

   public MaceKill() {
      super(Categories.Combat, "MaceKill", "Makes the Mace powerful when swung.");
   }

   @Override
   public void onActivate() {
      this.clearState();
   }

   @Override
   public void onDeactivate() {
      this.clearState();
   }

   private void clearState() {
      this.attackPacket = null;
      this.swingPacket = null;
      this.sendPackets = false;
      this.sendTimer = 0;
      this.target = null;
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (this.isValid() && this.isHoldingMace() && event.entity instanceof LivingEntity living) {
         switch ((MaceKill.Mode)this.mode.get()) {
            case Normal:
               this.doNormal(living);
               break;
            case Packet:
               this.doPacket(living);
               break;
            case Vulcan:
               if (this.startVulcan(living, null)) {
                  event.cancel();
               }
         }
      }
   }

   @EventHandler
   private void onSendPacket(PacketEvent.Send event) {
      if (!this.isValid()) {
         this.clearState();
      } else {
         if (event.packet instanceof ServerboundAttackPacket packet) {
            if (this.ignorePacket.get() || !this.isHoldingMace()) {
               return;
            }

            if (!((this.mc.level == null ? null : this.mc.level.getEntity(packet.entityId())) instanceof LivingEntity living)) {
               return;
            }

            switch ((MaceKill.Mode)this.mode.get()) {
               case Normal:
                  this.doNormal(living);
                  break;
               case Packet:
                  this.doPacket(living);
                  break;
               case Vulcan:
                  if (this.startVulcan(living, packet)) {
                     event.cancel();
                  }
            }
         } else if (event.packet instanceof ServerboundSwingPacket packet) {
            if (this.skipCrit()) {
               return;
            }

            if (this.sendPackets && this.swingPacket == null) {
               this.swingPacket = packet;
               event.cancel();
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.sendPackets) {
         if (!this.isValid()) {
            this.clearState();
         } else if (this.sendTimer > 0) {
            this.sendTimer--;
         } else {
            this.sendPackets = false;
            this.applyVulcanFall();
            ServerboundAttackPacket delayedAttack = this.attackPacket;
            if (delayedAttack == null && this.target != null) {
               delayedAttack = new ServerboundAttackPacket(this.target.getId());
            }

            if (delayedAttack != null) {
               this.debug("sending delayed mace attack");
               this.sendPacket(delayedAttack);
               this.sendPacket(this.swingPacket != null ? this.swingPacket : new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
               this.mc.player.setSprinting(true);
            }

            this.clearState();
         }
      }
   }

   private void doNormal(LivingEntity target) {
      if (!this.disableWhenBlocked.get() || !this.isBlocked(target)) {
         Vec3 previousPos = this.mc.player.position();
         int height = this.getMaxHeightAbovePlayer();
         if (height <= 0) {
            height = Math.max(1, this.fallHeight.get());
         }

         BlockPos top = this.mc.player.blockPosition().offset(0, height, 0);
         BlockPos topAbove = top.above();
         if (this.isSafeBlock(top) && this.isSafeBlock(topAbove)) {
            int packetsRequired = Math.abs(height / 10);
            if (packetsRequired > 20) {
               packetsRequired = 1;
            }

            if (this.mc.player.isPassenger()) {
               Entity vehicle = this.mc.player.getVehicle();
               if (vehicle == null) {
                  return;
               }

               for (int i = 0; i < packetsRequired - 1; i++) {
                  this.sendVehiclePacket(vehicle);
               }

               vehicle.setPos(vehicle.getX(), vehicle.getY() + height, vehicle.getZ());
               this.sendVehiclePacket(vehicle);
               vehicle.setPos(previousPos);
               this.sendVehiclePacket(vehicle);
               vehicle.setPos(previousPos);
               this.sendVehiclePacket(vehicle);
            } else {
               for (int i = 0; i < packetsRequired - 1; i++) {
                  this.sendStatus(false);
               }

               this.sendPosition(this.mc.player.getX(), this.mc.player.getY() + height, this.mc.player.getZ(), false);
               this.sendPosition(previousPos.x, previousPos.y, previousPos.z, false);
               this.sendPosition(previousPos.x, previousPos.y, previousPos.z, false);
            }
         }
      }
   }

   private void doPacket(LivingEntity target) {
      if (!this.skipCrit()) {
         if (!this.disableWhenBlocked.get() || !this.isBlocked(target)) {
            int height = this.determineHeight();
            if (height > 0) {
               if (height > 10) {
                  int iterations = (int)Math.ceil(Math.abs(height / 10.0));

                  for (int i = 0; i < iterations; i++) {
                     this.sendStatus(false);
                  }
               } else {
                  this.sendStatus(this.mc.player.onGround());
                  this.sendStatus(this.mc.player.onGround());
               }

               this.sendPosition(this.mc.player.getX(), this.mc.player.getY() + height, this.mc.player.getZ(), false);
               this.sendPosition(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), false);
            }
         }
      }
   }

   private boolean startVulcan(LivingEntity living, ServerboundAttackPacket packet) {
      if (this.skipCrit()) {
         return false;
      }

      if (this.disableWhenBlocked.get() && this.isBlocked(living)) {
         return false;
      }

      if (this.sendPackets) {
         return false;
      }

      this.sendPackets = true;
      this.sendTimer = this.vulcanMode.get() == MaceKill.VulcanMode.MiniJump ? 2 : 4;
      this.target = living;
      this.attackPacket = packet;
      this.swingPacket = null;
      if (this.vulcanMode.get() == MaceKill.VulcanMode.MiniJump) {
         ((IVec3)this.mc.player.getDeltaMovement()).meteor$setY(0.25);
      } else {
         this.mc.player.jumpFromGround();
      }

      return true;
   }

   private void applyVulcanFall() {
      Vec3 velocity = this.mc.player.getDeltaMovement();
      if (this.mc.player.hasEffect(MobEffects.SLOW_FALLING)) {
         this.mc.player.setDeltaMovement(velocity.x, velocity.y - 0.5, velocity.z);
      } else {
         this.mc.player.setDeltaMovement(velocity.x, velocity.y - 0.37, velocity.z);
      }

      this.debug("velocity = " + this.mc.player.getDeltaMovement().x + ", " + this.mc.player.getDeltaMovement().y + ", " + this.mc.player.getDeltaMovement().z);
      this.doPacketFallback();
   }

   private void doPacketFallback() {
      int height = this.determineHeight();
      if (height > 0) {
         if (height > 10) {
            int iterations = (int)Math.ceil(Math.abs(height / 10.0));

            for (int i = 0; i < iterations; i++) {
               this.sendStatus(false);
            }
         } else {
            this.sendStatus(this.mc.player.onGround());
            this.sendStatus(this.mc.player.onGround());
         }

         this.sendPosition(this.mc.player.getX(), this.mc.player.getY() + height, this.mc.player.getZ(), false);
         this.sendPosition(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), false);
      }
   }

   private int determineHeight() {
      AABB boundingBox = this.mc.player.getBoundingBox();

      for (int i = this.fallHeight.get(); i >= 1; i--) {
         if (this.mc.level.noCollision(this.mc.player, boundingBox.move(0.0, i, 0.0))) {
            return i;
         }
      }

      return 0;
   }

   private int getMaxHeightAbovePlayer() {
      BlockPos playerPos = this.mc.player.blockPosition();
      int maxHeight = playerPos.getY() + (this.maxPower.get() ? 170 : this.fallHeight.get());

      for (int y = maxHeight; y > playerPos.getY(); y--) {
         BlockPos first = new BlockPos(playerPos.getX(), y, playerPos.getZ());
         BlockPos second = first.above();
         if (this.isSafeBlock(first) && this.isSafeBlock(second)) {
            return y - playerPos.getY();
         }
      }

      return 0;
   }

   private boolean isSafeBlock(BlockPos pos) {
      return this.mc.level.getBlockState(pos).isAir() && this.mc.level.getFluidState(pos).isEmpty() && !this.mc.level.getBlockState(pos).is(Blocks.POWDER_SNOW);
   }

   private boolean skipCrit() {
      return !this.mc.player.onGround()
         || this.mc.player.isInWater()
         || this.mc.player.isInLava()
         || this.mc.player.onClimbable()
         || this.mc.player.isFallFlying();
   }

   private boolean isBlocked(LivingEntity target) {
      return target.isBlocking();
   }

   private boolean isHoldingMace() {
      return this.mc.player != null && this.mc.player.getMainHandItem().is(Items.MACE);
   }

   private boolean isValid() {
      return this.mc.player != null && this.mc.level != null && this.mc.getConnection() != null;
   }

   private void sendStatus(boolean onGround) {
      this.sendPacket(new StatusOnly(onGround, this.mc.player.horizontalCollision));
   }

   private void sendPosition(double x, double y, double z, boolean onGround) {
      ServerboundMovePlayerPacket packet = new Pos(x, y, z, onGround, this.mc.player.horizontalCollision);
      ((IServerboundMovePlayerPacket)packet).meteor$setTag(1337);
      this.sendPacket(packet);
   }

   private void sendVehiclePacket(Entity vehicle) {
      this.sendPacket(ServerboundMoveVehiclePacket.fromEntity(vehicle));
   }

   private void sendPacket(Packet<?> packet) {
      if (this.mc.getConnection() != null) {
         this.mc.getConnection().send(packet);
      }
   }

   private void debug(String message) {
      if (this.debug.get()) {
         ChatUtils.sendMsg(ChatFormatting.GRAY, "[MaceKill] " + message);
      }
   }

   @Override
   public String getInfoString() {
      return this.mode.get().name();
   }

   public enum Mode {
      Normal,
      Packet,
      Vulcan;
   }

   public enum VulcanMode {
      Jump,
      MiniJump;
   }
}
