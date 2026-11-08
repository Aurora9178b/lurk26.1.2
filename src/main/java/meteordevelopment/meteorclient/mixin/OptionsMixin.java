package meteordevelopment.meteorclient.mixin;

import java.io.File;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ChangePerspectiveEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {
   @Shadow
   @Final
   @Mutable
   public KeyMapping[] keyMappings;

   @Inject(
      method = "<init>",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyMappings:[Lnet/minecraft/client/KeyMapping;", opcode = 181, shift = Shift.AFTER)
   )
   private void onInitAfterKeysAll(Minecraft minecraft, File workingDirectory, CallbackInfo ci) {
      this.keyMappings = KeyBinds.apply(this.keyMappings);
   }

   @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true)
   private void setPerspective(CameraType cameraType, CallbackInfo ci) {
      if (Modules.get() != null) {
         ChangePerspectiveEvent event = (ChangePerspectiveEvent)MeteorClient.EVENT_BUS.post(ChangePerspectiveEvent.get(cameraType));
         if (event.isCancelled()) {
            ci.cancel();
         }

         if (Modules.get().isActive(Freecam.class)) {
            ci.cancel();
         }
      }
   }
}
