package meteordevelopment.meteorclient.events.world;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

public class ServerConnectBeginEvent {
   private static final ServerConnectBeginEvent INSTANCE = new ServerConnectBeginEvent();
   public ServerAddress address;
   public ServerData info;

   public static ServerConnectBeginEvent get(ServerAddress address, ServerData info) {
      INSTANCE.address = address;
      INSTANCE.info = info;
      return INSTANCE;
   }
}
