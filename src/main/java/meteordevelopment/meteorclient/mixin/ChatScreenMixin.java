package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.ClientSetting;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import meteordevelopment.meteorclient.utils.render.ChatBackgroundRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatScreen.class, priority = 1001)
public abstract class ChatScreenMixin {
   @Shadow
   protected EditBox input;

   @Inject(method = "init", at = @At("RETURN"))
   private void onInit(CallbackInfo ci) {
      if (Modules.get().get(BetterChat.class).isInfiniteChatBox()) {
         this.input.setMaxLength(Integer.MAX_VALUE);
      }
   }

   @Inject(method = "extractRenderState", at = @At("HEAD"))
   private void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
      SettingColor color = ClientSetting.getChatBackgroundColor();
      ChatBackgroundRenderer.request(2.0, ((ChatScreen)(Object)(Object)this).height - 14, ((ChatScreen)(Object)(Object)this).width - 4, 12.0, 4.0, color);
   }

   @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getBackgroundColor(I)I"))
   private int onExtractRenderState$getBackgroundColor(int color) {
      return 0;
   }
}
