package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.gui.screens.MeteorTitleMenuScreen;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.player.TitleScreenCredits;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
   private static boolean meteor$openingCustomTitleScreen;

   public TitleScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("HEAD"), cancellable = true)
   private void meteor$openCustomTitleScreen(CallbackInfo ci) {
      if (!meteor$openingCustomTitleScreen && this.minecraft != null) {
         meteor$openingCustomTitleScreen = true;
         this.minecraft.setScreen(new MeteorTitleMenuScreen());
         meteor$openingCustomTitleScreen = false;
         ci.cancel();
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
      if (Config.get().titleScreenCredits.get()) {
         TitleScreenCredits.render(graphics);
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
   private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
      if (Config.get().titleScreenCredits.get() && event.button() == 0 && TitleScreenCredits.onClicked(event.x(), event.y())) {
         cir.setReturnValue(true);
      }
   }
}
