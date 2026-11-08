package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ExtraElytra extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> instantFly = this.sgGeneral
      .add(new BoolSetting.Builder().name("instant-fly").description("Jump to fly, no weird double-jump needed!").defaultValue(true).build());
   private final Setting<Boolean> speedCtrl = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("speed-ctrl")
            .description("Control your speed with the Forward and Back keys.\n(default: W and S)\nNo fireworks needed!")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> heightCtrl = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("height-ctrl")
            .description("Control your height with the Jump and Sneak keys.\n(default: Spacebar and Shift)\nNo fireworks needed!")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> stopInWater = this.sgGeneral
      .add(new BoolSetting.Builder().name("stop-in-water").description("Stop flying in water.").defaultValue(true).build());
   private int jumpTimer;

   public ExtraElytra() {
      super(Categories.Movement, "extra-elytra", "Easier elytra.");
   }

   @Override
   public void onActivate() {
      this.jumpTimer = 0;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (Utils.canUpdate()) {
         if (this.jumpTimer > 0) {
            this.jumpTimer--;
         }

         ItemStack chest = this.mc.player.getItemBySlot(EquipmentSlot.CHEST);
         if (chest.is(Items.ELYTRA)) {
            if (this.mc.player.isFallFlying()) {
               if (this.stopInWater.get() && this.mc.player.isInWater()) {
                  this.sendStartStopPacket();
               } else {
                  this.controlSpeed();
                  this.controlHeight();
               }
            } else {
               if (chest.getDamageValue() < chest.getMaxDamage() - 1 && this.mc.options.keyJump.isDown()) {
                  this.doInstantFly();
               }
            }
         }
      }
   }

   private void sendStartStopPacket() {
      if (this.mc.getConnection() != null) {
         this.mc.getConnection().send(new ServerboundPlayerCommandPacket(this.mc.player, Action.START_FALL_FLYING));
      }
   }

   private void controlHeight() {
      if (this.heightCtrl.get()) {
         Vec3 velocity = this.mc.player.getDeltaMovement();
         if (this.mc.options.keyJump.isDown()) {
            this.mc.player.setDeltaMovement(velocity.x, velocity.y + 0.08, velocity.z);
         } else if (this.mc.options.keyShift.isDown()) {
            this.mc.player.setDeltaMovement(velocity.x, velocity.y - 0.04, velocity.z);
         }
      }
   }

   private void controlSpeed() {
      if (this.speedCtrl.get()) {
         float yaw = (float)Math.toRadians(this.mc.player.getYRot());
         Vec3 forward = new Vec3(-Math.sin(yaw) * 0.05, 0.0, Math.cos(yaw) * 0.05);
         Vec3 velocity = this.mc.player.getDeltaMovement();
         if (this.mc.options.keyUp.isDown()) {
            this.mc.player.setDeltaMovement(velocity.add(forward));
         } else if (this.mc.options.keyDown.isDown()) {
            this.mc.player.setDeltaMovement(velocity.subtract(forward));
         }
      }
   }

   private void doInstantFly() {
      if (this.instantFly.get()) {
         if (this.jumpTimer <= 0) {
            this.jumpTimer = 20;
            this.mc.player.setJumping(false);
            this.mc.player.setSprinting(true);
            this.mc.player.jumpFromGround();
         }

         this.sendStartStopPacket();
      }
   }
}
