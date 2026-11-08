package meteordevelopment.meteorclient.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.utils.misc.BetterF2Utils;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
   @ModifyVariable(
      method = "grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V",
      at = @At("HEAD"),
      argsOnly = true,
      index = 2
   )
   private static Consumer<Component> onGrabDefault(Consumer<Component> original) {
      return BetterF2Utils.wrapScreenshotCallback(original);
   }

   @ModifyVariable(
      method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
      at = @At("HEAD"),
      argsOnly = true,
      index = 4
   )
   private static Consumer<Component> onGrabNamed(Consumer<Component> original) {
      return BetterF2Utils.wrapScreenshotCallback(original);
   }

   @ModifyVariable(
      method = "takeScreenshot(Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
      at = @At("HEAD"),
      argsOnly = true,
      index = 2
   )
   private static Consumer<NativeImage> onTakeScreenshot(Consumer<NativeImage> original) {
      return BetterF2Utils.wrapNativeImageCallback(original);
   }
}
