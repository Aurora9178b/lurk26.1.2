package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class ChargeJump extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> rotationSpeed = this.sgGeneral
      .add(new IntSetting.Builder().name("rotation-speed").description("How fast to rotate upward.").defaultValue(10).range(1, 10).sliderRange(1, 10).build());
   private final Setting<Integer> delayTicks = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("delay-ticks")
            .description("Ticks to wait between pearl and wind charge use.")
            .defaultValue(1)
            .range(0, 100)
            .sliderRange(0, 100)
            .build()
      );
   private ChargeJump.Stage stage;
   private int timer;
   private boolean swapped;
   private meteordevelopment.meteorclient.utils.rotation.Rotation launchRotation;

   public ChargeJump() {
      super(Categories.Movement, "ChargeJump", "Uses an ender pearl and wind charge with your current angle.", "charge-jump");
   }

   @Override
   public void onActivate() {
      this.stage = ChargeJump.Stage.Pearl;
      this.timer = 0;
      this.swapped = false;
      this.launchRotation = null;
   }

   @Override
   public void onDeactivate() {
      if (this.swapped) {
         SlotHelper.INSTANCE.stopNextTick();
         this.swapped = false;
      }

      RotationManager.INSTANCE.reset();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate() && this.mc.gameMode != null) {
         meteordevelopment.meteorclient.utils.rotation.Rotation rotation = this.getUpwardRotation();
         RotationManager.INSTANCE.setRotations(rotation, this.rotationSpeed.get().intValue(), Priority.High);
         if (this.timer > 0) {
            this.timer--;
         } else {
            switch (this.stage) {
               case Pearl:
                  this.usePearl();
                  break;
               case Charge:
                  this.useCharge();
                  break;
               case Finished:
                  this.toggle();
            }
         }
      }
   }

   private void usePearl() {
      FindItemResult pearl = InvUtils.findInHotbar(Items.ENDER_PEARL);
      FindItemResult charge = InvUtils.findInHotbar(Items.WIND_CHARGE);
      if (pearl.found() && charge.found()) {
         this.launchRotation = this.getUpwardRotation();
         if (this.useItem(pearl, this.launchRotation)) {
            this.stage = ChargeJump.Stage.Charge;
            this.timer = this.delayTicks.get();
         }
      } else {
         this.stage = ChargeJump.Stage.Finished;
      }
   }

   private void useCharge() {
      FindItemResult charge = InvUtils.findInHotbar(Items.WIND_CHARGE);
      if (!charge.found()) {
         this.stage = ChargeJump.Stage.Finished;
      } else {
         if (this.useItem(charge, this.launchRotation != null ? this.launchRotation : this.getUpwardRotation())) {
            this.stage = ChargeJump.Stage.Finished;
         }
      }
   }

   private boolean useItem(FindItemResult item, meteordevelopment.meteorclient.utils.rotation.Rotation rotation) {
      InteractionHand hand = item.getHand();
      if (hand == null) {
         SlotHelper.setCurrentItem(item.slot(), true);
         this.swapped = true;
         hand = InteractionHand.MAIN_HAND;
      }

      InteractionHand finalHand = hand;
      this.mc.gameMode.startPrediction(this.mc.level, sequence -> new ServerboundUseItemPacket(finalHand, sequence, rotation.getYaw(), rotation.getPitch()));
      this.mc.player.swing(hand);
      return true;
   }

   private meteordevelopment.meteorclient.utils.rotation.Rotation getUpwardRotation() {
      return new meteordevelopment.meteorclient.utils.rotation.Rotation(this.mc.player.getYRot(), -90.0F);
   }

   private enum Stage {
      Pearl,
      Charge,
      Finished;
   }
}
