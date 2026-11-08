package meteordevelopment.meteorclient.systems.modules.movement;

import com.google.common.collect.Streams;
import java.util.OptionalDouble;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Step extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Double> height = this.sgGeneral
      .add(new DoubleSetting.Builder().name("height").description("Step height.").defaultValue(1.25).min(0.0).build());
   private final Setting<Step.ActiveWhen> activeWhen = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("active-when"))
                  .description("Step is active when you meet these requirements."))
               .defaultValue(Step.ActiveWhen.Always))
            .build()
      );
   private final Setting<Boolean> safeStep = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("safe-step")
            .description("Doesn't let you step out of a hole if you are low on health or there is a crystal nearby.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Integer> stepHealth = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("step-health")
            .description("The health you stop being able to step at.")
            .defaultValue(5)
            .range(1, 36)
            .sliderRange(1, 36)
            .visible(this.safeStep::get)
            .build()
      );
   private float prevStepHeight;
   private boolean prevPathManagerStep;

   public Step() {
      super(Categories.Movement, "step", "Allows you to walk up full blocks instantly.");
   }

   @Override
   public void onActivate() {
      this.prevStepHeight = this.mc.player.maxUpStep();
      this.prevPathManagerStep = PathManagers.get().getSettings().getStep().get();
      PathManagers.get().getSettings().getStep().set(true);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      boolean work = this.activeWhen.get() == Step.ActiveWhen.Always
         || this.activeWhen.get() == Step.ActiveWhen.Sneaking && this.mc.player.isShiftKeyDown()
         || this.activeWhen.get() == Step.ActiveWhen.NotSneaking && !this.mc.player.isShiftKeyDown();
      double height = this.getMaxSafeHeight();
      if (work && height > 0.0) {
         this.mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(height);
      } else {
         this.mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(this.prevStepHeight);
      }
   }

   @Override
   public void onDeactivate() {
      this.mc.player.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(this.prevStepHeight);
      PathManagers.get().getSettings().getStep().set(this.prevPathManagerStep);
   }

   private float getHealth() {
      return this.mc.player.getHealth() + this.mc.player.getAbsorptionAmount();
   }

   private double getExplosionDamage() {
      OptionalDouble crystalDamage = Streams.stream(this.mc.level.entitiesForRendering())
         .filter(EndCrystal.class::isInstance)
         .filter(Entity::isAlive)
         .mapToDouble(entity -> DamageUtils.crystalDamage(this.mc.player, entity.position()))
         .max();
      return crystalDamage.orElse(0.0);
   }

   private boolean isSafe() {
      return this.getHealth() > this.stepHealth.get().intValue() && this.getHealth() - this.getExplosionDamage() > this.stepHealth.get().intValue();
   }

   private boolean isSaferThanWith(double damage) {
      return this.isSafe() || this.getExplosionDamage() - damage <= 0.0;
   }

   private double getMaxSafeHeight() {
      if (!this.safeStep.get()) {
         return this.height.get();
      }

      double max = this.height.get();
      double h = 0.0;
      double currentDamage = this.getExplosionDamage();
      AABB initial = this.mc.player.getBoundingBox();
      Vec3 inputOffset = this.mc.player.getLookAngle();
      Vec2 input = this.mc.player.input.getMoveVector();
      ((IVec3)inputOffset).meteor$setY(0.0);
      inputOffset = inputOffset.normalize().scale(1.2);
      double zdot = inputOffset.z;
      double xdot = inputOffset.x;
      inputOffset = new Vec3(input.y * xdot + input.x * zdot, 0.0, input.x * xdot + input.y * zdot);

      for (int i = 1; i < max; i++) {
         this.mc.player.setBoundingBox(initial.move(0.0, i, 0.0));
         if (!this.isSaferThanWith(currentDamage)) {
            this.mc.player.setBoundingBox(initial);
            return h;
         }

         this.mc.player.setBoundingBox(this.mc.player.getBoundingBox().move(inputOffset));
         if (!this.isSaferThanWith(currentDamage)) {
            this.mc.player.setBoundingBox(initial);
            return h;
         }

         h++;
      }

      this.mc.player.setBoundingBox(initial.move(0.0, max, 0.0));
      if (this.isSaferThanWith(currentDamage)) {
         h = max;
      }

      this.mc.player.setBoundingBox(initial);
      return h;
   }

   public enum ActiveWhen {
      Always,
      Sneaking,
      NotSneaking;
   }
}
