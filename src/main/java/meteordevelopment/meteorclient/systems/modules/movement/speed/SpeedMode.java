package meteordevelopment.meteorclient.systems.modules.movement.speed;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class SpeedMode {
   protected final Minecraft mc;
   protected final Speed settings = Modules.get().get(Speed.class);
   private final SpeedModes type;
   protected int stage;
   protected double distance;
   protected double speed;

   public SpeedMode(SpeedModes type) {
      this.mc = Minecraft.getInstance();
      this.type = type;
      this.reset();
   }

   public void onTick() {
   }

   public void onMove(PlayerMoveEvent event) {
   }

   public void onRubberband() {
      this.reset();
   }

   public void onActivate() {
   }

   public void onDeactivate() {
   }

   protected double getDefaultSpeed() {
      double defaultSpeed = 0.2873;
      if (this.mc.player.hasEffect(MobEffects.SPEED)) {
         int amplifier = this.mc.player.getEffect(MobEffects.SPEED).getAmplifier();
         defaultSpeed *= 1.0 + 0.2 * (amplifier + 1);
      }

      if (this.mc.player.hasEffect(MobEffects.SLOWNESS)) {
         int amplifier = this.mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
         defaultSpeed /= 1.0 + 0.2 * (amplifier + 1);
      }

      return defaultSpeed;
   }

   protected void reset() {
      this.stage = 0;
      this.distance = 0.0;
      this.speed = 0.2873;
   }

   protected double getHop(double height) {
      MobEffectInstance jumpBoost = this.mc.player.hasEffect(MobEffects.JUMP_BOOST) ? this.mc.player.getEffect(MobEffects.JUMP_BOOST) : null;
      if (jumpBoost != null) {
         height += (jumpBoost.getAmplifier() + 1) * 0.1F;
      }

      return height;
   }

   public String getHudString() {
      return this.type.name();
   }
}
