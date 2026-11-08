package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.misc.FakeClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
   @Inject(method = "getPlayerInfo", at = @At("HEAD"), cancellable = true)
   private void onGetPlayerListEntry(CallbackInfoReturnable<PlayerInfo> cir) {
      if (MeteorClient.mc.getConnection() == null) {
         cir.setReturnValue(FakeClientPlayer.getPlayerListEntry());
      }
   }
}
