package meteordevelopment.meteorclient.systems.modules.render;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class KillEffects extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<KillEffects.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("Effect rendered where enemies die."))
               .defaultValue(KillEffects.Mode.LightningBolt))
            .build()
      );
   private final Setting<Boolean> playSound = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("play-sound")
            .description("Plays the effect sound.")
            .defaultValue(true)
            .visible(() -> this.mode.get() != KillEffects.Mode.FallingLava)
            .build()
      );
   private final Map<UUID, KillEffects.DeathEffect> effects = new ConcurrentHashMap<>();
   private final Map<UUID, Long> lightningCooldowns = new ConcurrentHashMap<>();

   public KillEffects() {
      super(Categories.Render, "KillEffects", "Renders effects where enemies die.");
   }

   @Override
   public void onDeactivate() {
      this.effects.clear();
      this.lightningCooldowns.clear();
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.level != null && this.mc.player != null) {
         long now = System.currentTimeMillis();
         this.lightningCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);

         for (Player player : this.mc.level.players()) {
            if (player != this.mc.player
               && !this.effects.containsKey(player.getUUID())
               && !this.lightningCooldowns.containsKey(player.getUUID())
               && !player.isAlive()
               && player.getHealth() <= 0.0F) {
               this.effects.put(player.getUUID(), new KillEffects.DeathEffect(player.position(), player.getBbWidth(), player.getBbHeight()));
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.mc.level != null && this.mc.player != null) {
         for (Iterator<Entry<UUID, KillEffects.DeathEffect>> iterator = this.effects.entrySet().iterator(); iterator.hasNext(); iterator.remove()) {
            Entry<UUID, KillEffects.DeathEffect> entry = iterator.next();
            KillEffects.DeathEffect effect = entry.getValue();
            switch ((KillEffects.Mode)this.mode.get()) {
               case FallingLava:
                  this.spawnFallingLava(effect);
                  break;
               case LightningBolt:
                  this.spawnLightning(entry.getKey(), effect);
            }
         }
      }
   }

   private void spawnFallingLava(KillEffects.DeathEffect effect) {
      for (int i = 0; i < effect.height() * 10.0F; i++) {
         for (int j = 0; j < effect.width() * 10.0F; j++) {
            for (int k = 0; k < effect.width() * 10.0F; k++) {
               this.mc
                  .level
                  .addParticle(ParticleTypes.FALLING_LAVA, effect.pos().x + j * 0.1, effect.pos().y + i * 0.1, effect.pos().z + k * 0.1, 0.0, 0.0, 0.0);
            }
         }
      }
   }

   private void spawnLightning(UUID uuid, KillEffects.DeathEffect effect) {
      LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.mc.level, EntitySpawnReason.TRIGGERED);
      if (lightning != null) {
         lightning.setPos(effect.pos().x, effect.pos().y, effect.pos().z);
         this.mc.level.addEntity(lightning);
      }

      if (this.playSound.get()) {
         this.mc
            .level
            .playSound(this.mc.player, effect.pos().x, effect.pos().y, effect.pos().z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F, 0.16F);
         this.mc
            .level
            .playSound(this.mc.player, effect.pos().x, effect.pos().y, effect.pos().z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F, 0.1F);
      }

      this.lightningCooldowns.put(uuid, System.currentTimeMillis());
   }

   private record DeathEffect(Vec3 pos, float width, float height) {
   }

   private enum Mode {
      FallingLava,
      LightningBolt;
   }
}
