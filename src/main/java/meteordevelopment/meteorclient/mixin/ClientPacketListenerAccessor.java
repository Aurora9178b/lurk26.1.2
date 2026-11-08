package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain.Encoder;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket.NodeBuilder;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
   @Accessor("serverChunkRadius")
   int meteor$getServerChunkRadius();

   @Accessor("signedMessageEncoder")
   Encoder meteor$getSignedMessageEncoder();

   @Accessor("lastSeenMessages")
   LastSeenMessagesTracker meteor$getLastSeenMessages();

   @Accessor("registryAccess")
   Frozen meteor$getRegistryAccess();

   @Accessor("enabledFeatures")
   FeatureFlagSet meteor$getEnabledFeatures();

   @Accessor("COMMAND_NODE_BUILDER")
   static NodeBuilder<ClientSuggestionProvider> meteor$getCommandNodeFactory() {
      return null;
   }
}
