package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.BlockBehaviourAccessor;
import meteordevelopment.meteorclient.mixin.DirectionAccessor;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Vector3dSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class AutoWasp extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> horizontalSpeed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("horizontal-speed").description("Horizontal elytra speed.").defaultValue(2.0).build());
   private final Setting<Double> verticalSpeed = this.sgGeneral
      .add(new DoubleSetting.Builder().name("vertical-speed").description("Vertical elytra speed.").defaultValue(3.0).build());
   private final Setting<Boolean> avoidLanding = this.sgGeneral
      .add(new BoolSetting.Builder().name("avoid-landing").description("Will try to avoid landing if your target is on the ground.").defaultValue(true).build());
   private final Setting<Boolean> predictMovement = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("predict-movement")
            .description("Tries to predict the targets position according to their movement.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> onlyFriends = this.sgGeneral
      .add(new BoolSetting.Builder().name("only-friends").description("Will only follow friends.").defaultValue(false).build());
   private final Setting<AutoWasp.Action> action = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("action-on-target-loss"))
                  .description("What to do if you lose the target."))
               .defaultValue(AutoWasp.Action.TOGGLE))
            .build()
      );
   private final Setting<Vector3d> offset = this.sgGeneral
      .add(new Vector3dSetting.Builder().name("offset").description("How many blocks offset to wasp at from the target.").defaultValue(0.0, 0.0, 0.0).build());
   public Player target;
   private int jumpTimer = 0;
   private boolean incrementJumpTimer = false;

   public AutoWasp() {
      super(Categories.Movement, "auto-wasp", "Wasps for you. Unable to traverse around blocks, assumes a clear straight line to the target.");
   }

   @Override
   public void onActivate() {
      if (this.target == null || this.target.isRemoved()) {
         this.target = (Player)TargetUtils.get(entity -> {
            if (!(entity instanceof Player player && entity != this.mc.player)) {
               return false;
            } else {
               return !player.isDeadOrDying() && !(player.getHealth() <= 0.0F) ? !this.onlyFriends.get() || Friends.get().get(player) != null : false;
            }
         }, SortPriority.LowestDistance);
         if (this.target == null) {
            this.error("No valid targets.");
            this.toggle();
            return;
         }

         this.info(this.target.getName().getString() + " set as target.");
      }

      this.jumpTimer = 0;
      this.incrementJumpTimer = false;
   }

   @Override
   public void onDeactivate() {
      this.target = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.target.isRemoved()) {
         this.warning("Lost target!");
         switch ((AutoWasp.Action)this.action.get()) {
            case TOGGLE:
               this.toggle();
               break;
            case CHOOSE_NEW_TARGET:
               this.onActivate();
               break;
            case DISCONNECT:
               this.mc
                  .player
                  .connection
                  .handleDisconnect(
                     new ClientboundDisconnectPacket(
                        Component.literal("%s[%sAuto Wasp%s] Lost target.".formatted(ChatFormatting.GRAY, ChatFormatting.BLUE, ChatFormatting.GRAY))
                     )
                  );
         }

         if (!this.isActive()) {
            return;
         }
      }

      if (this.mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) {
         if (this.incrementJumpTimer) {
            this.jumpTimer++;
         }

         if (!this.mc.player.isFallFlying()) {
            if (!this.incrementJumpTimer) {
               this.incrementJumpTimer = true;
            }

            if (this.mc.player.onGround() && this.incrementJumpTimer) {
               this.mc.player.jumpFromGround();
               return;
            }

            if (this.jumpTimer >= 4) {
               this.jumpTimer = 0;
               this.mc.player.setJumping(false);
               this.mc.player.setSprinting(true);
               this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
         } else {
            this.incrementJumpTimer = false;
            this.jumpTimer = 0;
         }
      }
   }

   @EventHandler
   private void onMove(PlayerMoveEvent event) {
      if (this.mc.player.getItemBySlot(EquipmentSlot.CHEST).has(DataComponents.GLIDER)) {
         if (this.mc.player.isFallFlying()) {
            double xVel = 0.0;
            double yVel = 0.0;
            double zVel = 0.0;
            Vec3 targetPos = this.target.position().add(this.offset.get().x, this.offset.get().y, this.offset.get().z);
            if (this.predictMovement.get()) {
               targetPos.add(
                  Entity.collideBoundingBox(
                     this.target,
                     this.target.getDeltaMovement(),
                     this.target.getBoundingBox(),
                     this.mc.level,
                     this.mc.level.getEntityCollisions(this.target, this.target.getBoundingBox().expandTowards(this.target.getDeltaMovement()))
                  )
               );
            }

            if (this.avoidLanding.get()) {
               double d = this.target.getBoundingBox().getXsize() / 2.0;

               for (Direction dir : DirectionAccessor.meteor$getHorizontal()) {
                  BlockPos pos = BlockPos.containing(targetPos.relative(dir, d).relative(dir.getClockWise(), d)).below();
                  if (((BlockBehaviourAccessor)this.mc.level.getBlockState(pos).getBlock()).meteor$isHasCollision()
                     && Math.abs(targetPos.y() - (pos.getY() + 1)) <= 0.25) {
                     targetPos = new Vec3(targetPos.x, pos.getY() + 1.25, targetPos.z);
                     break;
                  }
               }
            }

            double xDist = targetPos.x() - this.mc.player.getX();
            double zDist = targetPos.z() - this.mc.player.getZ();
            double absX = Math.abs(xDist);
            double absZ = Math.abs(zDist);
            double diag = 0.0;
            if (absX > 1.0E-5F && absZ > 1.0E-5F) {
               diag = 1.0 / Math.sqrt(absX * absX + absZ * absZ);
            }

            if (absX > 1.0E-5F) {
               if (absX < this.horizontalSpeed.get()) {
                  xVel = xDist;
               } else {
                  xVel = this.horizontalSpeed.get() * Math.signum(xDist);
               }

               if (diag != 0.0) {
                  xVel *= absX * diag;
               }
            }

            if (absZ > 1.0E-5F) {
               if (absZ < this.horizontalSpeed.get()) {
                  zVel = zDist;
               } else {
                  zVel = this.horizontalSpeed.get() * Math.signum(zDist);
               }

               if (diag != 0.0) {
                  zVel *= absZ * diag;
               }
            }

            double yDist = targetPos.y() - this.mc.player.getY();
            if (Math.abs(yDist) > 1.0E-5F) {
               if (Math.abs(yDist) < this.verticalSpeed.get()) {
                  yVel = yDist;
               } else {
                  yVel = this.verticalSpeed.get() * Math.signum(yDist);
               }
            }

            ((IVec3)event.movement).meteor$set(xVel, yVel, zVel);
         }
      }
   }

   public enum Action {
      TOGGLE,
      CHOOSE_NEW_TARGET,
      DISCONNECT;

      @Override
      public String toString() {
         return switch (this) {
            case TOGGLE -> "Toggle module";
            case CHOOSE_NEW_TARGET -> "Choose new target";
            case DISCONNECT -> "Disconnect";
         };
      }
   }
}
