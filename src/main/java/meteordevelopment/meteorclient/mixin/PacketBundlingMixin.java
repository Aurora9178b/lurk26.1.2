package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AntiPacketKick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/network/protocol/BundlerInfo$1$1")
public abstract class PacketBundlingMixin {
   @ModifyExpressionValue(method = "addPacket", at = @At(value = "CONSTANT", args = "intValue=4096"))
   private int add(int value) {
      return Modules.get().get(AntiPacketKick.class).isActive() ? Integer.MAX_VALUE : value;
   }
}
