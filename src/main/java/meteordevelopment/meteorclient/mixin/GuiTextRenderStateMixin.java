package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.Font.PreparedText;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiTextRenderState.class)
public abstract class GuiTextRenderStateMixin {
   @Shadow
   @Final
   public FormattedCharSequence text;
   @Shadow
   private PreparedText preparedText;

   @Inject(method = "ensurePrepared", at = @At("HEAD"))
   private void refreshAnimatedPrefix(CallbackInfoReturnable<PreparedText> info) {
      if (ChatUtils.shouldAnimatePrefix(this.text)) {
         this.preparedText = null;
      }
   }

   @ModifyArg(
      method = "ensurePrepared",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Font;prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
      ),
      index = 0
   )
   private FormattedCharSequence animatePrefix(FormattedCharSequence text) {
      return ChatUtils.applyAnimatedPrefix(text);
   }
}
