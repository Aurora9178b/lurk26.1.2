package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1", remap = false)
public abstract class ChatHudLineConsumerMixin {
   @Inject(method = "accept", at = @At("HEAD"))
   private void setLine(Line line, int lineIndex, float alpha, CallbackInfo ci) {
      Modules.get().get(BetterChat.class).line = line;
   }

   @ModifyExpressionValue(
      method = "accept",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;tag()Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;")
   )
   private GuiMessageTag onRender_modifyIndicator(GuiMessageTag indicator) {
      return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : indicator;
   }
}
