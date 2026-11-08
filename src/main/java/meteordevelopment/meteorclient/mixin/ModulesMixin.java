package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.elements.DynamicIslandHud;
import meteordevelopment.meteorclient.systems.hud.elements.DynamicIslandV2;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.SoundStorage;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Modules.class)
public class ModulesMixin {
   @Inject(
      method = "addActive",
      at = @At(value = "INVOKE", target = "Lmeteordevelopment/orbit/IEventBus;post(Ljava/lang/Object;)Ljava/lang/Object;"),
      remap = false
   )
   private void addActive(Module module, CallbackInfo ci) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         MeteorClient.mc
            .level
            .playSound(MeteorClient.mc.player, MeteorClient.mc.player.blockPosition(), SoundStorage.SOUND_ENABLE, SoundSource.MASTER, 1.0F, 1.0F);
      }

      DynamicIslandHud.notifyModuleToggle(module.title, true);
      DynamicIslandV2.notifyModuleToggle(module.title, true);
   }

   @Inject(
      method = "removeActive",
      at = @At(value = "INVOKE", target = "Lmeteordevelopment/orbit/IEventBus;post(Ljava/lang/Object;)Ljava/lang/Object;"),
      remap = false
   )
   private void removeActive(Module module, CallbackInfo ci) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         MeteorClient.mc
            .level
            .playSound(MeteorClient.mc.player, MeteorClient.mc.player.blockPosition(), SoundStorage.SOUND_DISABLE, SoundSource.MASTER, 1.0F, 1.0F);
      }

      DynamicIslandHud.notifyModuleToggle(module.title, false);
      DynamicIslandV2.notifyModuleToggle(module.title, false);
   }
}
