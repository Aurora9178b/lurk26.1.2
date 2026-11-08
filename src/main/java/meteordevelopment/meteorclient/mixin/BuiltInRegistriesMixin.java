package meteordevelopment.meteorclient.mixin;

import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BuiltInRegistries.class)
public abstract class BuiltInRegistriesMixin {
   @Redirect(
      method = "internalRegister(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/WritableRegistry;Lnet/minecraft/core/registries/BuiltInRegistries$RegistryBootstrap;)Lnet/minecraft/core/WritableRegistry;",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;checkBootstrapCalled(Ljava/util/function/Supplier;)V")
   )
   private static void ignoreBootstrap(Supplier<String> location) {
   }
}
