package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.MoveInputEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerTickMovementEvent;
import meteordevelopment.meteorclient.events.entity.player.TravelEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public class ElytraFlyPlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<ElytraFlyPlus.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("Elytra flight mode."))
               .defaultValue(ElytraFlyPlus.Mode.Control))
            .build()
      );
   private final Setting<ElytraFlyPlus.SwapMode> swapMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("swap-mode"))
                     .description("How rockets are selected."))
                  .defaultValue(ElytraFlyPlus.SwapMode.InvSwitch))
               .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control))
            .build()
      );
   private final Setting<Boolean> armored = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("armored")
            .description("Temporarily equips elytra from inventory.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control)
            .build()
      );
   private final Setting<Boolean> noSprint = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("no-sprint")
            .description("Stops sprinting while armored control mode is active.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control && this.armored.get())
            .build()
      );
   private final Setting<Boolean> useFireworks = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("use-fireworks")
            .description("Automatically uses fireworks while flying.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control)
            .build()
      );
   private final Setting<Integer> boostDelay = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("boost-delay")
            .description("Ticks between firework boosts.")
            .defaultValue(20)
            .min(2)
            .sliderRange(2, 50)
            .visible(() -> this.mode.get() == ElytraFlyPlus.Mode.Control && this.useFireworks.get())
            .build()
      );
   private boolean hasFirstFirework;
   private boolean shouldJump;
   private long lastBoostTime;

   public ElytraFlyPlus() {
      super(
         Categories.Movement,
         "elytra-fly+",
         "Epsilon-style elytra fly with control, armor swap and firework boosts.",
         "elytrafly+",
         "elytra-fly-plus",
         "elytraflyplus"
      );
   }

   @Override
   public void onActivate() {
      this.hasFirstFirework = false;
      this.shouldJump = false;
      this.lastBoostTime = 0L;
   }

   @Override
   public void onDeactivate() {
      this.hasFirstFirework = false;
      this.shouldJump = false;
   }

   @EventHandler
   private void onPlayerTick(PlayerTickMovementEvent event) {
      if (this.mc.player != null) {
         this.redirectRotation();
         switch ((ElytraFlyPlus.Mode)this.mode.get()) {
            case Control:
               this.updateControl();
         }
      }
   }

   @EventHandler
   private void onTravel(TravelEvent event) {
      if (this.mode.get() == ElytraFlyPlus.Mode.Control) {
         if (!this.hasMoveInput() && (!this.useFireworks.get() || this.hasFirstFirework)) {
            this.mc.player.setDeltaMovement(0.0, 0.02, 0.0);
         }
      }
   }

   @EventHandler
   private void onMoveInput(MoveInputEvent event) {
      if (this.mode.get() == ElytraFlyPlus.Mode.Control) {
         if (this.noSprint.get()) {
            event.setSprint(false);
         }

         if (this.shouldJump) {
            event.setJump(true);
            this.shouldJump = false;
         }
      }
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (event.button() == 1 && event.action == KeyAction.Press) {
         event.setCancelled(true);
      }
   }

   @Override
   public String getInfoString() {
      return this.mode.get().name();
   }

   private void updateControl() {
      if (!this.noSprint.get() || !this.mc.player.isSprinting()) {
         FindItemResult elytra = InvUtils.find(Items.ELYTRA);
         boolean inventoryElytra = elytra.found() && !elytra.isArmor() && !elytra.isOffhand();
         if (this.canGlide(elytra.found()) && !this.mc.player.onGround()) {
            if (this.armored.get() && inventoryElytra) {
               if (this.canFFlying()) {
                  this.jiaFei(elytra.slot());
               }
            } else {
               if (this.canFFlying()) {
                  this.startFFlying();
               }

               this.useFirework();
            }
         } else {
            this.shouldJump = true;
            this.hasFirstFirework = false;
         }
      }
   }

   private boolean canFFlying() {
      return !this.mc.player.isFallFlying() && !this.mc.player.isInWater();
   }

   private boolean startFFlying() {
      if (this.mc.player.tryToStartFallFlying()) {
         this.shouldJump = true;
         this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_FALL_FLYING));
         return true;
      } else {
         return false;
      }
   }

   private boolean canGlide(boolean hasElytra) {
      if (this.armored.get() && hasElytra) {
         return true;
      }

      for (EquipmentSlot slot : EquipmentSlot.VALUES) {
         if (LivingEntity.canGlideUsing(this.mc.player.getItemBySlot(slot), slot)) {
            return true;
         }
      }

      return false;
   }

   private void useFirework() {
      if (this.useFireworks.get() && System.currentTimeMillis() - this.lastBoostTime >= this.boostDelay.get().intValue() * 50L) {
         FindItemResult rocket = this.swapMode.get() == ElytraFlyPlus.SwapMode.Silent
            ? InvUtils.findInHotbar(Items.FIREWORK_ROCKET)
            : InvUtils.find(Items.FIREWORK_ROCKET);
         if (rocket.found()) {
            InteractionHand hand = rocket.getHand();
            boolean swapped = false;
            boolean moved = false;
            int selectedSlot = this.mc.player.getInventory().getSelectedSlot();
            if (hand == null) {
               if (this.swapMode.get() == ElytraFlyPlus.SwapMode.Silent) {
                  if (!InvUtils.swap(rocket.slot(), true)) {
                     return;
                  }

                  swapped = true;
               } else {
                  InvUtils.move().from(rocket.slot()).toHotbar(selectedSlot);
                  moved = true;
               }

               hand = InteractionHand.MAIN_HAND;
            }

            InteractionResult result = this.mc.gameMode.useItem(this.mc.player, hand);
            if (result.consumesAction()) {
               this.hasFirstFirework = true;
               this.lastBoostTime = System.currentTimeMillis();
               this.mc.player.swing(hand);
            }

            if (swapped) {
               InvUtils.swapBack();
            } else if (moved) {
               InvUtils.move().fromHotbar(selectedSlot).to(rocket.slot());
            }
         }
      }
   }

   private void jiaFei(int elytraSlot) {
      int containerSlot = elytraSlot < 9 ? elytraSlot + 36 : elytraSlot;
      this.swapArmor(containerSlot);
      this.startFFlying();
      this.useFirework();
      this.swapArmor(containerSlot);
   }

   private void swapArmor(int containerSlot) {
      int containerId = this.mc.player.containerMenu.containerId;
      this.mc.gameMode.handleContainerInput(containerId, containerSlot, 0, ContainerInput.PICKUP, this.mc.player);
      this.mc.gameMode.handleContainerInput(containerId, 6, 0, ContainerInput.PICKUP, this.mc.player);
      this.mc.gameMode.handleContainerInput(containerId, containerSlot, 0, ContainerInput.PICKUP, this.mc.player);
   }

   private boolean hasMoveInput() {
      return this.mc.options.keyUp.isDown()
         || this.mc.options.keyDown.isDown()
         || this.mc.options.keyLeft.isDown()
         || this.mc.options.keyRight.isDown()
         || this.mc.options.keyJump.isDown()
         || this.mc.options.keyShift.isDown();
   }

   private void redirectRotation() {
      RotationManager.INSTANCE.setRotations(new Rotation(this.calcYaw(), this.calcPitch()), 10.0, Priority.Highest);
   }

   private float calcYaw() {
      float yaw = this.mc.player.getYRot();
      boolean forward = this.mc.options.keyUp.isDown();
      boolean back = this.mc.options.keyDown.isDown();
      boolean left = this.mc.options.keyLeft.isDown();
      boolean right = this.mc.options.keyRight.isDown();
      if (forward && !back) {
         if (left && !right) {
            yaw -= 45.0F;
         } else if (right && !left) {
            yaw += 45.0F;
         }
      } else if (back && !forward) {
         yaw += 180.0F;
         if (left && !right) {
            yaw += 45.0F;
         } else if (right && !left) {
            yaw -= 45.0F;
         }
      } else if (left && !right) {
         yaw -= 90.0F;
      } else if (right && !left) {
         yaw += 90.0F;
      }

      return Mth.wrapDegrees(yaw);
   }

   private float calcPitch() {
      float pitch = this.mc.player.getXRot();
      boolean jump = this.mc.options.keyJump.isDown();
      boolean sneak = this.mc.options.keyShift.isDown();
      boolean moving = PlayerUtils.isMoving();
      if (sneak && jump) {
         pitch = -3.0F;
      } else if (jump) {
         pitch = moving ? -45.0F : -90.0F;
      } else if (sneak) {
         pitch = moving ? 45.0F : 90.0F;
      } else if (moving) {
         pitch = -1.9F;
      }

      return Mth.clamp(pitch, -90.0F, 90.0F);
   }

   private enum Mode {
      Control;
   }

   private enum SwapMode {
      Silent,
      InvSwitch;
   }
}
