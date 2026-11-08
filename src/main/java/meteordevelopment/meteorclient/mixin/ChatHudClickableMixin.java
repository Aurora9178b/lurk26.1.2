package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.BetterChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$ClickableTextOnlyGraphicsAccess")
public abstract class ChatHudClickableMixin {
   @Unique
   private static BetterChat betterChat;

   @ModifyArg(
      method = "handleMessage",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/util/FormattedCharSequence;)V"
      ),
      index = 1
   )
   private int modifyX(int x) {
      return getBetterChat().modifyChatWidth(x);
   }

   @Unique
   private static BetterChat getBetterChat() {
      if (betterChat == null) {
         betterChat = Modules.get().get(BetterChat.class);
      }

      return betterChat;
   }
}
