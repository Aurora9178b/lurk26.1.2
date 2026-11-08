package meteordevelopment.meteorclient.mixin;

import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundPingPacket.class)
public interface ClientboundPingPacketAccessor {
   @Mutable
   @Accessor("id")
   void meteor$setId(int var1);
}
