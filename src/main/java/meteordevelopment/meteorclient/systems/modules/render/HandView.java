package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Vector3dSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.InteractionHand;
import org.joml.Vector3d;

public class HandView extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgMainHand = this.settings.createGroup("Main Hand");
   private final SettingGroup sgOffHand = this.settings.createGroup("Off Hand");
   private final SettingGroup sgArm = this.settings.createGroup("Arm");
   private final Setting<Boolean> followRotations = this.sgGeneral
      .add(new BoolSetting.Builder().name("server-rotations").description("Makes your hands follow your serverside rotations.").defaultValue(false).build());
   public final Setting<Boolean> oldAnimations = this.sgGeneral
      .add(new BoolSetting.Builder().name("old-animations").description("Changes hit animations to those like 1.8").defaultValue(false).build());
   public final Setting<Boolean> skipSwapping = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("skip-swapping-animation")
            .description("Whether or not to skip the item swapping animation")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> disableFoodAnimation = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("disable-eating-animation")
            .description("Disables the eating animation. Potentially desirable if it goes offscreen.")
            .defaultValue(false)
            .build()
      );
   public final Setting<Boolean> swordSlash = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("sword-slash")
            .description("Replaces the sword swing animation with the offhand idle animation.")
            .defaultValue(false)
            .build()
      );
   public final Setting<HandView.SwingMode> swingMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("swing-mode"))
                  .description("Modifies your client & server hand swinging."))
               .defaultValue(HandView.SwingMode.None))
            .build()
      );
   public final Setting<Integer> swingSpeed = this.sgGeneral
      .add(new IntSetting.Builder().name("swing-speed").description("The swing speed of your hands.").defaultValue(6).range(0, 20).sliderMax(20).build());
   public final Setting<Double> mainSwing = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("main-hand-progress")
            .description("The swing progress of your main hand.")
            .defaultValue(0.0)
            .range(0.0, 1.0)
            .sliderMax(1.0)
            .build()
      );
   public final Setting<Double> offSwing = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("off-hand-progress")
            .description("The swing progress of your off hand.")
            .defaultValue(0.0)
            .range(0.0, 1.0)
            .sliderMax(1.0)
            .build()
      );
   private final Setting<Vector3d> scaleMain = this.sgMainHand
      .add(
         new Vector3dSetting.Builder()
            .name("scale")
            .description("The scale of your main hand.")
            .defaultValue(1.0, 1.0, 1.0)
            .sliderMax(5.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Vector3d> posMain = this.sgMainHand
      .add(
         new Vector3dSetting.Builder()
            .name("position")
            .description("The position of your main hand.")
            .defaultValue(0.0, 0.0, 0.0)
            .sliderRange(-3.0, 3.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Vector3d> rotMain = this.sgMainHand
      .add(
         new Vector3dSetting.Builder()
            .name("rotation")
            .description("The rotation of your main hand.")
            .defaultValue(0.0, 0.0, 0.0)
            .sliderRange(-180.0, 180.0)
            .decimalPlaces(0)
            .build()
      );
   private final Setting<Vector3d> scaleOff = this.sgOffHand
      .add(
         new Vector3dSetting.Builder()
            .name("scale")
            .description("The scale of your off hand.")
            .defaultValue(1.0, 1.0, 1.0)
            .sliderMax(5.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Vector3d> posOff = this.sgOffHand
      .add(
         new Vector3dSetting.Builder()
            .name("position")
            .description("The position of your off hand.")
            .defaultValue(0.0, 0.0, 0.0)
            .sliderRange(-3.0, 3.0)
            .decimalPlaces(1)
            .build()
      );
   private final Setting<Vector3d> rotOff = this.sgOffHand
      .add(
         new Vector3dSetting.Builder()
            .name("rotation")
            .description("The rotation of your off hand.")
            .defaultValue(0.0, 0.0, 0.0)
            .sliderRange(-180.0, 180.0)
            .decimalPlaces(0)
            .build()
      );
   private final Setting<Vector3d> scaleArm = this.sgArm
      .add(new Vector3dSetting.Builder().name("scale").defaultValue(1.0, 1.0, 1.0).sliderMax(5.0).decimalPlaces(1).build());
   private final Setting<Vector3d> posArm = this.sgArm
      .add(new Vector3dSetting.Builder().name("position").defaultValue(0.0, 0.0, 0.0).sliderRange(-3.0, 3.0).decimalPlaces(1).build());
   private final Setting<Vector3d> rotArm = this.sgArm
      .add(new Vector3dSetting.Builder().name("rotation").defaultValue(0.0, 0.0, 0.0).sliderRange(-180.0, 180.0).decimalPlaces(0).build());

   public HandView() {
      super(Categories.Render, "hand-view", "Alters the way items are rendered in your hands.");
   }

   @EventHandler
   private void onHeldItemRender(HeldItemRendererEvent event) {
      if (Rotations.rotating && this.followRotations.get()) {
         this.applyServerRotations(event.matrix);
      }

      if (event.hand == InteractionHand.MAIN_HAND) {
         this.rotate(event.matrix, this.rotMain.get());
         this.scale(event.matrix, this.scaleMain.get());
         this.translate(event.matrix, this.posMain.get());
      } else {
         this.rotate(event.matrix, this.rotOff.get());
         this.scale(event.matrix, this.scaleOff.get());
         this.translate(event.matrix, this.posOff.get());
      }
   }

   @EventHandler
   private void onRenderArm(ArmRenderEvent event) {
      this.rotate(event.matrix, this.rotArm.get());
      this.scale(event.matrix, this.scaleArm.get());
      this.translate(event.matrix, this.posArm.get());
   }

   private void rotate(PoseStack matrix, Vector3d rotation) {
      matrix.mulPose(Axis.XP.rotationDegrees((float)rotation.x));
      matrix.mulPose(Axis.YP.rotationDegrees((float)rotation.y));
      matrix.mulPose(Axis.ZP.rotationDegrees((float)rotation.z));
   }

   private void scale(PoseStack matrix, Vector3d scale) {
      matrix.scale((float)scale.x, (float)scale.y, (float)scale.z);
   }

   private void translate(PoseStack matrix, Vector3d translation) {
      matrix.translate((float)translation.x, (float)translation.y, (float)translation.z);
   }

   private void applyServerRotations(PoseStack matrix) {
      matrix.mulPose(Axis.XP.rotationDegrees(this.mc.player.getXRot() - Rotations.serverPitch));
      matrix.mulPose(Axis.YP.rotationDegrees(this.mc.player.getYRot() - Rotations.serverYaw));
   }

   public boolean oldAnimations() {
      return this.isActive() && this.oldAnimations.get();
   }

   public boolean skipSwapping() {
      return this.isActive() && this.skipSwapping.get();
   }

   public boolean disableFoodAnimation() {
      return this.isActive() && this.disableFoodAnimation.get();
   }

   public boolean swordSlash() {
      return this.isActive() && this.swordSlash.get();
   }

   public enum SwingMode {
      Offhand,
      Mainhand,
      None;
   }
}
