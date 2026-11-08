package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.managers.RotationManager;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.BypassSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.SlotHelper;
import meteordevelopment.meteorclient.utils.rotation.Priority;
import meteordevelopment.meteorclient.utils.rotation.Rotation;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class AutoWindCharge extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> pitch = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("pitch")
            .description("Pitch used when throwing the wind charge.")
            .defaultValue(90.0)
            .range(-90.0, 90.0)
            .sliderRange(-90.0, 90.0)
            .build()
      );
   private final Setting<Double> rotationSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotation-speed")
            .description("How fast to rotate before using the wind charge.")
            .defaultValue(10.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<Double> rotationBackSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("rotation-back-speed")
            .description("How fast to return render rotation after using the wind charge.")
            .defaultValue(5.0)
            .min(0.0)
            .sliderRange(0.0, 20.0)
            .build()
      );
   private final Setting<Integer> maxRotateTicks = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("max-rotate-ticks")
            .description("Maximum ticks to wait for rotation before throwing.")
            .defaultValue(8)
            .min(0)
            .sliderRange(0, 20)
            .build()
      );
   private Rotation targetRotation;
   private InteractionHand hand;
   private boolean swapBack;
   private long swapBackTime;
   private int rotateTicks;
   private boolean used;

   public AutoWindCharge() {
      super(
         Categories.Movement, "auto-wind-charge", "Automatically uses a wind charge without applying extra long-jump velocity.", "autowindcharge", "auto-wind"
      );
   }

   @Override
   public void onActivate() {
      this.targetRotation = null;
      this.hand = null;
      this.swapBack = false;
      this.swapBackTime = 0L;
      this.rotateTicks = 0;
      this.used = false;
   }

   @Override
   public void onDeactivate() {
      if (this.swapBack) {
         SlotHelper.INSTANCE.stopNextTick();
      }

      RotationManager.INSTANCE.reset();
      this.swapBack = false;
      this.targetRotation = null;
      this.hand = null;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         if (this.used) {
            this.trySwapBack();
         } else if (this.targetRotation == null && !this.prepare()) {
            this.toggle();
         } else {
            RotationManager.INSTANCE.setRotations(this.targetRotation, this.rotationSpeed.get(), this.rotationBackSpeed.get(), Priority.High);
            this.rotateTicks++;
            Rotation rotation = RotationManager.INSTANCE.getRotation();
            boolean ready = RotationManager.INSTANCE.isDone() || this.rotateTicks >= this.maxRotateTicks.get() || this.isClose(rotation, this.targetRotation);
            if (ready) {
               this.useWindCharge(rotation);
            }
         }
      }
   }

   private boolean prepare() {
      FindItemResult windCharge = InvUtils.findInHotbar(Items.WIND_CHARGE);
      if (!windCharge.found()) {
         this.warning("No wind charge found in hotbar.");
         return false;
      }

      this.hand = windCharge.isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
      if (!windCharge.isOffhand() && !windCharge.isMainHand()) {
         SlotHelper.setCurrentItem(windCharge.slot(), true);
         this.swapBack = true;
         this.swapBackTime = System.currentTimeMillis() + BypassSetting.getSwitchDelay();
      }

      this.targetRotation = new Rotation(this.mc.player.getYRot(), this.pitch.get().floatValue());
      return true;
   }

   private void useWindCharge(Rotation rotation) {
      if (this.hand != null && this.mc.getConnection() != null) {
         float yaw = rotation == null ? this.targetRotation.getYaw() : rotation.getYaw();
         float usePitch = rotation == null ? this.targetRotation.getPitch() : rotation.getPitch();
         this.mc.getConnection().send(new ServerboundUseItemPacket(this.hand, 0, yaw, usePitch));
         this.used = true;
         if (!this.swapBack) {
            this.toggle();
         } else if (this.swapBackTime <= 0L) {
            this.swapBackTime = System.currentTimeMillis() + BypassSetting.getSwitchDelay();
         }
      }
   }

   private void trySwapBack() {
      if (this.swapBack && System.currentTimeMillis() >= this.swapBackTime) {
         SlotHelper.INSTANCE.stop();
         this.swapBack = false;
         this.toggle();
      }
   }

   private boolean isClose(Rotation current, Rotation target) {
      return current != null && target != null
         ? Math.abs(Mth.wrapDegrees(current.getYaw() - target.getYaw())) <= 1.0F && Math.abs(current.getPitch() - target.getPitch()) <= 1.0F
         : false;
   }
}
