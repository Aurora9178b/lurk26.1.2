package meteordevelopment.meteorclient.systems.modules.misc;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.text.RunnableClickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.OpenUrl;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Strings;

public class ServerSpoof extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> spoofBrand = this.sgGeneral
      .add(new BoolSetting.Builder().name("spoof-brand").description("Whether or not to spoof the brand.").defaultValue(true).build());
   private final Setting<String> brand = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("brand")
            .description("Specify the brand that will be send to the server.")
            .defaultValue("vanilla")
            .visible(this.spoofBrand::get)
            .build()
      );
   private final Setting<Boolean> resourcePack = this.sgGeneral
      .add(new BoolSetting.Builder().name("resource-pack").description("Spoof accepting server resource pack.").defaultValue(false).build());
   private final Setting<Boolean> blockChannels = this.sgGeneral
      .add(new BoolSetting.Builder().name("block-channels").description("Whether or not to block some channels.").defaultValue(true).build());
   private final Setting<List<String>> channels = this.sgGeneral
      .add(
         new StringListSetting.Builder()
            .name("channels")
            .description("If the channel contains the keyword, this outgoing channel will be blocked.")
            .defaultValue("fabric", "minecraft:register")
            .visible(this.blockChannels::get)
            .build()
      );
   private MutableComponent msg;
   public boolean silentAcceptResourcePack = false;

   public ServerSpoof() {
      super(Categories.Misc, "server-spoof", "Spoof client brand, resource pack and channels.");
      this.runInMainMenu = true;
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
      if (this.isActive()) {
         if (event.packet instanceof ServerboundCustomPayloadPacket customPayloadPacket) {
            Identifier id = customPayloadPacket.payload().type().id();
            if (this.blockChannels.get()) {
               for (String channel : this.channels.get()) {
                  if (Strings.CI.contains(id.toString(), channel)) {
                     event.cancel();
                     return;
                  }
               }
            }

            if (this.spoofBrand.get() && id.equals(BrandPayload.TYPE.id())) {
               ServerboundCustomPayloadPacket spoofedPacket = new ServerboundCustomPayloadPacket(new BrandPayload(this.brand.get()));
               event.sendSilently(spoofedPacket);
               event.cancel();
            }
         }

         if (this.silentAcceptResourcePack && event.packet instanceof ServerboundResourcePackPacket) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (this.isActive() && this.resourcePack.get()) {
         if (event.packet instanceof ClientboundResourcePackPushPacket packet) {
            event.cancel();
            event.connection.send(new ServerboundResourcePackPacket(packet.id(), Action.ACCEPTED));
            event.connection.send(new ServerboundResourcePackPacket(packet.id(), Action.DOWNLOADED));
            event.connection.send(new ServerboundResourcePackPacket(packet.id(), Action.SUCCESSFULLY_LOADED));
            this.msg = Component.literal("This server has ");
            this.msg.append(packet.required() ? "a required " : "an optional ").append("resource pack. ");
            MutableComponent link = Component.literal("[Open URL]");
            link.setStyle(
               link.getStyle()
                  .withColor(ChatFormatting.BLUE)
                  .withUnderlined(true)
                  .withClickEvent(new OpenUrl(URI.create(packet.url())))
                  .withHoverEvent(new ShowText(Component.literal("Click to open the pack url")))
            );
            MutableComponent acceptance = Component.literal("[Accept Pack]");
            acceptance.setStyle(acceptance.getStyle().withColor(ChatFormatting.DARK_GREEN).withUnderlined(true).withClickEvent(new RunnableClickEvent(() -> {
               URL url = getParsedResourcePackUrl(packet.url());
               if (url == null) {
                  this.error("Invalid resource pack URL: " + packet.url());
               } else {
                  this.silentAcceptResourcePack = true;
                  this.mc.getDownloadedPackSource().pushPack(packet.id(), url, packet.hash());
               }
            })).withHoverEvent(new ShowText(Component.literal("Click to accept and apply the pack."))));
            this.msg.append(link).append(" ");
            this.msg.append(acceptance).append(".");
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.isActive() && Utils.canUpdate() && this.msg != null) {
         this.info(this.msg);
         this.msg = null;
      }
   }

   private static URL getParsedResourcePackUrl(String url) {
      try {
         URL uRL = new URI(url).toURL();
         String string = uRL.getProtocol();
         return !"http".equals(string) && !"https".equals(string) ? null : uRL;
      } catch (MalformedURLException | URISyntaxException var3) {
         return null;
      }
   }
}
