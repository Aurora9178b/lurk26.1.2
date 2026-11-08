package meteordevelopment.meteorclient.mixin;

import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
   protected GameMenuScreenMixin(Component title) {
      super(title);
   }

   @Inject(method = "init", at = @At("TAIL"))
   private void meteor$addReconnectButton(CallbackInfo ci) {
      if (!MeteorClient.mc.hasSingleplayerServer() && MeteorClient.mc.getCurrentServer() != null) {
         this.addRenderableWidget(
            new Builder(Component.literal("Reconnect"), var1x -> this.meteor$tryConnecting()).pos(this.width / 2 - 102, 340).size(204, 20).build()
         );
      }
   }

   @Unique
   private void meteor$tryConnecting() {
      ServerData lastServer = MeteorClient.mc.getCurrentServer();
      if (lastServer == null) {
         ChatUtils.sendMsg(Component.literal("Failed to reconnect server! You might be in Singleplayer?"));
         MeteorClient.mc.setScreen(null);
      } else {
         ServerAddress address = ServerAddress.parseString(lastServer.ip);
         Modules.get().get(AutoReconnect.class).lastServerConnection = new ObjectObjectImmutablePair<>(address, lastServer);
         if (MeteorClient.mc.getConnection() != null) {
            MeteorClient.mc.getConnection().getConnection().disconnect(Component.literal("Reconnecting..."));
         }

         ConnectScreen.startConnecting(new TitleScreen(), MeteorClient.mc, address, lastServer, false, null);
      }
   }
}
