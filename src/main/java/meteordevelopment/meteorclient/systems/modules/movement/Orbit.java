package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.Vec3;

public class Orbit extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> orbitRange = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("orbit-range")
            .description("The range of the orbit in blocks.")
            .defaultValue(50.0)
            .min(0.1)
            .sliderRange(1.0, 128.0)
            .build()
      );
   private final Setting<Double> orbitSpeed = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("orbit-speed")
            .description("The speed of the orbit in blocks per second.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 50.0)
            .build()
      );
   private final Setting<Boolean> velocityMove = this.sgGeneral
      .add(new BoolSetting.Builder().name("velocity-move").description("Uses velocity movement instead of packet teleporting.").defaultValue(true).build());
   private Vec3 center;
   private double orbitAngle;

   public Orbit() {
      super(Categories.Movement, "Orbit", "Orbits around your activation position.");
   }

   @Override
   public void onActivate() {
      if (this.mc.player != null && this.mc.level != null) {
         this.center = this.mc.player.position();
         this.orbitAngle = Math.toRadians(this.mc.player.getYRot());
      } else {
         this.toggle();
      }
   }

   @Override
   public void onDeactivate() {
      this.center = null;
      this.orbitAngle = 0.0;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.center != null) {
         double radius = Math.max(0.1, this.orbitRange.get());
         this.orbitAngle = this.normalizeRadians(this.orbitAngle + this.orbitSpeed.get() / radius / 20.0);
         Vec3 target = new Vec3(this.center.x + radius * Math.cos(this.orbitAngle), this.center.y, this.center.z + radius * Math.sin(this.orbitAngle));
         Vec3 playerPos = this.mc.player.position();
         if (this.velocityMove.get()) {
            this.moveWithVelocity(playerPos, target);
         } else {
            this.doTp(playerPos, target);
            this.mc.player.setPos(target);
         }
      }
   }

   private void moveWithVelocity(Vec3 from, Vec3 to) {
      double speed = this.getMoveModeSpeed();
      double distance = from.distanceTo(to);
      Vec3 velocity = to.subtract(from);
      if (speed > 0.0 && distance > speed) {
         velocity = velocity.normalize().scale(speed);
      }

      this.mc.player.setDeltaMovement(velocity);
   }

   private void doTp(Vec3 from, Vec3 to) {
      TpSetting setting = TpSetting.get();
      double moveDistance = this.getMoveDistance();
      if (setting != null && setting.isLTPUtils()) {
         LTpUtils.doTp(from, to, moveDistance, false);
      } else {
         QTPUtils.doTp(from, to, moveDistance, false);
      }
   }

   private double getMoveDistance() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveDistance() : 128.0;
   }

   private double getMoveModeSpeed() {
      TpSetting setting = TpSetting.get();
      return setting != null ? setting.getMoveModeSpeed() : 5.0;
   }

   private double normalizeRadians(double angle) {
      double twoPi = Math.PI * 2;
      angle %= twoPi;
      return angle < 0.0 ? angle + twoPi : angle;
   }
}
