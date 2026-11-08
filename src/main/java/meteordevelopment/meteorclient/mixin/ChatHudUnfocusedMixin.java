package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.ActiveTextCollector.Parameters;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public abstract class ChatHudUnfocusedMixin {
   @Unique
   private static BetterChat betterChat;
   @Final
   @Shadow
   private GuiGraphicsExtractor graphics;

   @ModifyArg(
      method = "handleMessage",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"
      ),
      index = 1
   )
   private int modifyX(int x) {
      return getBetterChat().modifyChatWidth(x);
   }

   @ModifyArg(method = "fill", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"), index = 4)
   private int removeVanillaBackground(int color) {
      return this.isBlackBackground(color) ? 0 : color;
   }

   @ModifyReceiver(
      method = "handleMessage",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V"
      )
   )
   private ActiveTextCollector onRender_beforeDrawText(
      ActiveTextCollector instance, TextAlignment alignment, int x, int y, Parameters transformation, FormattedCharSequence orderedText
   ) {
      getBetterChat().beforeDrawMessage(this.graphics, y, ARGB.white(transformation.opacity()));
      return instance;
   }

   @Inject(method = "handleMessage", at = @At("TAIL"))
   private void onRender_afterDrawText(int textTop, float opacity, FormattedCharSequence message, CallbackInfoReturnable<Boolean> cir) {
      getBetterChat().afterDrawMessage();
   }

   @Unique
   private static BetterChat getBetterChat() {
      if (betterChat == null) {
         betterChat = Modules.get().get(BetterChat.class);
      }

      return betterChat;
   }

   @Unique
   private boolean isBlackBackground(int color) {
      return (color & 16777215) == 0;
   }
}
