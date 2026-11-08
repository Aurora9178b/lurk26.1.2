package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.MobEffectInstanceAccessor;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;

public class Fullbright extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Fullbright.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                     .description("The mode to use for Fullbright."))
                  .defaultValue(Fullbright.Mode.Gamma))
               .onChanged(mode -> {
                  if (this.isActive()) {
                     if (mode != Fullbright.Mode.Potion) {
                        this.disableNightVision();
                     }

                     if (this.mc.levelRenderer != null) {
                        this.mc.levelRenderer.allChanged();
                     }
                  }
               }))
            .build()
      );
   public final Setting<LightLayer> lightType = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder()
                           .name("light-type"))
                        .description("Which type of light to use for Luminance mode."))
                     .defaultValue(LightLayer.BLOCK))
                  .visible(() -> this.mode.get() == Fullbright.Mode.Luminance))
               .onChanged(var1 -> {
                  if (this.mc.levelRenderer != null && this.isActive()) {
                     this.mc.levelRenderer.allChanged();
                  }
               }))
            .build()
      );
   private final Setting<Integer> minimumLightLevel = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("minimum-light-level")
            .description("Minimum light level when using Luminance mode.")
            .visible(() -> this.mode.get() == Fullbright.Mode.Luminance)
            .defaultValue(8)
            .range(0, 15)
            .sliderMax(15)
            .onChanged(var1 -> {
               if (this.mc.levelRenderer != null && this.isActive()) {
                  this.mc.levelRenderer.allChanged();
               }
            })
            .build()
      );

   public Fullbright() {
      super(Categories.Render, "fullbright", "Lights up your world!");
   }

   @Override
   public void onActivate() {
      if (this.mode.get() == Fullbright.Mode.Luminance) {
         this.mc.levelRenderer.allChanged();
      }
   }

   @Override
   public void onDeactivate() {
      if (this.mode.get() == Fullbright.Mode.Luminance) {
         this.mc.levelRenderer.allChanged();
      } else if (this.mode.get() == Fullbright.Mode.Potion) {
         this.disableNightVision();
      }
   }

   public int getLuminance(LightLayer type) {
      return this.isActive() && this.mode.get() == Fullbright.Mode.Luminance && type == this.lightType.get() ? this.minimumLightLevel.get() : 0;
   }

   public boolean getGamma() {
      return this.isActive() && this.mode.get() == Fullbright.Mode.Gamma;
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mode.get().equals(Fullbright.Mode.Potion)) {
         if (this.mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()))) {
            MobEffectInstance instance = this.mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()));
            if (instance != null && instance.getDuration() < 420) {
               ((MobEffectInstanceAccessor)instance).meteor$setDuration(420);
            }
         } else {
            this.mc.player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()), 420, 0));
         }
      }
   }

   private void disableNightVision() {
      if (this.mc.player != null) {
         if (this.mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()))) {
            this.mc.player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(MobEffects.NIGHT_VISION.value()));
         }
      }
   }

   public enum Mode {
      Gamma,
      Potion,
      Luminance;
   }
}
