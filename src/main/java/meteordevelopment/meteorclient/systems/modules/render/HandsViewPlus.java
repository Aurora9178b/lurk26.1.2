package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public class HandsViewPlus extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> disableSwapMain = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-swap-main").description("Removes the main hand swap animation.").defaultValue(true).build());
   public final Setting<Boolean> disableSwapOff = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-swap-off").description("Removes the offhand swap animation.").defaultValue(true).build());
   public final Setting<Boolean> blockingAnimation = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("blocking-animation").description("Renders old blocking animations while holding a weapon.").defaultValue(true).build()
      );
   public final Setting<HandsViewPlus.BlockMode> blockingMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("blocking-mode"))
                     .description("Blocking animation style."))
                  .defaultValue(HandsViewPlus.BlockMode.Pushdown))
               .visible(this.blockingAnimation::get))
            .build()
      );
   public final Setting<HandsViewPlus.SwingMode> swingMode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("swing-mode"))
                  .description("First-person swing animation mode."))
               .defaultValue(HandsViewPlus.SwingMode.Vanilla))
            .build()
      );
   public final Setting<Boolean> onlyWeapon = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-weapon")
            .description("Only applies Flux swing to weapons.")
            .defaultValue(true)
            .visible(() -> this.swingMode.get() == HandsViewPlus.SwingMode.Flux)
            .build()
      );
   public final Setting<Integer> swingSpeed = this.sgGeneral
      .add(new IntSetting.Builder().name("swing-speed").description("The swing speed of your hands.").defaultValue(6).range(0, 20).sliderMax(20).build());
   public final Setting<Boolean> swingWhileUsing = this.sgGeneral
      .add(new BoolSetting.Builder().name("visual-swing-on-use").description("Allows client-side swing while using an item.").defaultValue(true).build());
   public final Setting<Boolean> onlyOnBlock = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-on-block")
            .description("Only swings while using when looking at a block.")
            .defaultValue(true)
            .visible(this.swingWhileUsing::get)
            .build()
      );

   public HandsViewPlus() {
      super(Categories.Render, "hands-view+", "Adds custom hand animations.", "handsview+", "hands-view-plus", "handsviewplus");
   }

   public boolean shouldDisableSwapMain() {
      return this.isActive() && this.disableSwapMain.get();
   }

   public boolean shouldDisableSwapOff() {
      return this.isActive() && this.disableSwapOff.get();
   }

   public boolean shouldVisualSwingOnUse() {
      return this.isActive() && this.swingWhileUsing.get();
   }

   public boolean isFluxSwing() {
      return this.isActive() && this.swingMode.get() == HandsViewPlus.SwingMode.Flux;
   }

   public boolean shouldApplyFluxSwing(ItemStack stack) {
      return !this.isFluxSwing() ? false : !this.onlyWeapon.get() || this.isFluxWeapon(stack);
   }

   public boolean isBlocking() {
      KillAura killAuraPlus = Modules.get().get(KillAura.class);
      return killAuraPlus != null && killAuraPlus.isActive() && killAuraPlus.target != null
         ? true
         : this.mc.player != null && this.mc.options.keyUse.isDown() && this.isBlockingWeapon(this.mc.player.getMainHandItem());
   }

   public boolean shouldApplyBlockingAnimation(InteractionHand hand, ItemStack itemStack) {
      return this.isActive()
         && this.blockingAnimation.get()
         && hand == InteractionHand.MAIN_HAND
         && !itemStack.isEmpty()
         && this.mc.player != null
         && !this.mc.player.isUsingItem()
         && this.isBlocking()
         && this.isBlockingWeapon(itemStack);
   }

   public boolean shouldApplyThirdPersonBlockingAnimation(Avatar avatar, HumanoidArm arm) {
      if (this.mc.player == null) {
         return false;
      } else if (avatar == this.mc.player && this.isActive() && this.blockingAnimation.get() && avatar.getMainArm() == arm) {
         ItemStack itemStack = avatar.getItemInHand(InteractionHand.MAIN_HAND);
         return !itemStack.isEmpty() && this.isBlocking() && this.isBlockingWeapon(itemStack);
      } else {
         return false;
      }
   }

   public void applyBlockingTransform(PoseStack poseStack, HumanoidArm arm, float attack) {
      switch ((HandsViewPlus.BlockMode)this.blockingMode.get()) {
         case V1_7:
            poseStack.translate(arm == HumanoidArm.RIGHT ? -0.1F : 0.1F, 0.1F, 0.0F);
            this.applyAttackTransform(poseStack, arm, attack, 0.9F);
            this.applyBlockPose(poseStack, arm, 0.0F, 0.0F, 0.0F, -102.25F, 13.365F, 78.05F);
            break;
         case Pushdown:
            poseStack.translate(arm == HumanoidArm.RIGHT ? -0.1F : 0.1F, 0.1F, 0.0F);
            float swing = Mth.sin(Mth.sqrt(attack) * (float) Math.PI);
            poseStack.mulPose(Axis.ZP.rotationDegrees((arm == HumanoidArm.RIGHT ? 1 : -1) * swing * 10.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(swing * -35.0F));
            this.applyBlockPose(poseStack, arm, 0.0F, 0.0F, 0.0F, -102.25F, 13.365F, 78.05F);
            break;
         case Scale:
            poseStack.translate(arm == HumanoidArm.RIGHT ? -0.16F : 0.16F, 0.12F, 0.16F);
            poseStack.scale(0.85F, 0.85F, 0.85F);
            this.applyAttackTransform(poseStack, arm, attack, 0.35F);
            this.applyBlockPose(poseStack, arm, 0.0F, 0.0F, 0.0F, -100.0F, 18.0F, 72.0F);
            break;
         case Leaked:
            this.applyAttackTransform(poseStack, arm, attack, 0.6F);
            this.applyBlockPose(poseStack, arm, -0.18F, 0.18F, 0.1F, -96.0F, 24.0F, 68.0F);
            break;
         case Ninja:
            this.applyAttackTransform(poseStack, arm, attack, 0.25F);
            this.applyBlockPose(poseStack, arm, -0.05F, 0.22F, 0.2F, -88.0F, 35.0F, 82.0F);
            break;
         case Down:
            this.applyAttackTransform(poseStack, arm, attack, 0.2F);
            this.applyBlockPose(poseStack, arm, -0.25F, -0.02F, 0.05F, -120.0F, 8.0F, 72.0F);
      }
   }

   private void applyAttackTransform(PoseStack poseStack, HumanoidArm arm, float attack, float scale) {
      int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
      float ySwingRotation = Mth.sin(attack * attack * (float) Math.PI);
      poseStack.mulPose(Axis.YP.rotationDegrees(invert * (45.0F + ySwingRotation * -20.0F * scale)));
      float xzSwingRotation = Mth.sin(Mth.sqrt(attack) * (float) Math.PI);
      poseStack.mulPose(Axis.ZP.rotationDegrees(invert * xzSwingRotation * -20.0F * scale));
      poseStack.mulPose(Axis.XP.rotationDegrees(xzSwingRotation * -80.0F * scale));
      poseStack.mulPose(Axis.YP.rotationDegrees(invert * -45.0F));
   }

   private void applyBlockPose(
      PoseStack poseStack, HumanoidArm arm, float translateX, float translateY, float translateZ, float rotateX, float rotateY, float rotateZ
   ) {
      int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
      poseStack.translate(invert * translateX, translateY, translateZ);
      poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
      poseStack.mulPose(Axis.YP.rotationDegrees(invert * rotateY));
      poseStack.mulPose(Axis.ZP.rotationDegrees(invert * rotateZ));
   }

   private boolean isBlockingWeapon(ItemStack stack) {
      return stack.is(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE);
   }

   private boolean isFluxWeapon(ItemStack stack) {
      return stack.is(net.minecraft.tags.ItemTags.SWORDS) || stack.is(net.minecraft.tags.ItemTags.AXES);
   }

   public enum BlockMode {
      V1_7,
      Pushdown,
      Scale,
      Leaked,
      Ninja,
      Down;
   }

   public enum SwingMode {
      Vanilla,
      Flux;
   }
}
