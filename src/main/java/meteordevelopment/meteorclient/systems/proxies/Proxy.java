package meteordevelopment.meteorclient.systems.proxies;

import com.google.common.net.InetAddresses;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class Proxy implements ISerializable<Proxy> {
   public final Settings settings = new Settings();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgOptional = this.settings.createGroup("Optional");
   public Setting<String> name = this.sgGeneral.add(new StringSetting.Builder().name("name").description("The name of the proxy.").build());
   public Setting<ProxyType> type = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("type")).description("The type of proxy."))
               .defaultValue(ProxyType.Socks5))
            .build()
      );
   public Setting<String> address = this.sgGeneral
      .add(new StringSetting.Builder().name("address").description("The ip address of the proxy.").filter(Utils::ipFilter).build());
   public Setting<Integer> port = this.sgGeneral
      .add(new IntSetting.Builder().name("port").description("The port of the proxy.").defaultValue(0).range(0, 65535).sliderMax(65535).noSlider().build());
   public Setting<Boolean> enabled = this.sgGeneral
      .add(new BoolSetting.Builder().name("enabled").description("Whether the proxy is enabled.").defaultValue(true).build());
   public Setting<String> username = this.sgOptional.add(new StringSetting.Builder().name("username").description("The username of the proxy.").build());
   public Setting<String> password = this.sgOptional
      .add(
         new StringSetting.Builder().name("password").description("The password of the proxy.").visible(() -> this.type.get().equals(ProxyType.Socks5)).build()
      );
   public Proxy.Status status = Proxy.Status.UNCHECKED;
   public long latency;

   private Proxy() {
   }

   public Proxy(Tag tag) {
      this.fromTag((CompoundTag)tag);
   }

   public boolean resolveAddress() {
      return Utils.resolveAddress(this.address.get(), this.port.get());
   }

   public int checkStatus() {
      if (this.status == Proxy.Status.CHECKING) {
         return 0;
      }

      this.status = Proxy.Status.CHECKING;
      boolean timeout = false;

      try {
         Instant before = Instant.now();
         if (this.isSocks4()) {
            this.status = Proxy.Status.ALIVE;
            this.latency = Duration.between(before, Instant.now()).toMillis();
            return 1;
         }
      } catch (SocketTimeoutException var5) {
         timeout = true;
      } catch (IOException var6) {
      }

      try {
         Instant before = Instant.now();
         if (this.isSocks5()) {
            this.status = Proxy.Status.ALIVE;
            this.latency = Duration.between(before, Instant.now()).toMillis();
            return 1;
         }
      } catch (SocketTimeoutException var3) {
         timeout = true;
      } catch (IOException var4) {
      }

      this.status = Proxy.Status.DEAD;
      return timeout ? 3 : 2;
   }

   private boolean isSocks4() throws IOException {
      byte[] u = this.username.get().getBytes();
      ByteBuffer bb;
      if (InetAddresses.isInetAddress(this.address.get())) {
         bb = ByteBuffer.allocate(9 + u.length)
            .put((byte)4)
            .put((byte)1)
            .putShort(this.port.get().shortValue())
            .putInt(InetAddress.getByName(this.address.get()).hashCode())
            .put(u)
            .put((byte)0);
      } else {
         byte[] addr = this.address.get().getBytes();
         bb = ByteBuffer.allocate(10 + u.length + addr.length)
            .put((byte)4)
            .put((byte)1)
            .putShort(this.port.get().shortValue())
            .put(new byte[]{0, 0, 0, 1})
            .put(u)
            .put((byte)0)
            .put(addr)
            .put((byte)0);
      }

      byte[] data = this.sendData(bb.array(), 8);
      return data.length < 2 ? false : data[0] == 0 && data[1] == 90;
   }

   private boolean isSocks5() throws IOException {
      ByteBuffer bb = ByteBuffer.allocate(4).put((byte)5).put((byte)2).put((byte)0).put((byte)2);
      byte[] data = this.sendData(bb.array(), 2);
      return data.length < 2 ? false : data[0] == 5 && (data[1] == 0 || data[1] == 2);
   }

   private byte[] sendData(byte[] data, int read) throws IOException {
      try (Socket s = new Socket()) {
         s.setSoTimeout(Proxies.get().timeout.get());
         s.connect(new InetSocketAddress(this.address.get(), this.port.get()), Proxies.get().timeout.get());
         OutputStream out = s.getOutputStream();
         out.write(data);
         return s.getInputStream().readNBytes(read);
      }
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.put("settings", this.settings.toTag());
      return tag;
   }

   public Proxy fromTag(CompoundTag tag) {
      tag.getCompound("settings").ifPresent(this.settings::fromTag);
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Proxy proxy = (Proxy)o;
         return Objects.equals(proxy.address.get(), this.address.get()) && Objects.equals(proxy.port.get(), this.port.get());
      } else {
         return false;
      }
   }

   public static class Builder {
      protected ProxyType type = ProxyType.Socks5;
      protected String address = "";
      protected int port = 0;
      protected String name = "";
      protected String username = "";
      protected String password = "";
      protected boolean enabled = false;

      public Proxy.Builder type(ProxyType type) {
         this.type = type;
         return this;
      }

      public Proxy.Builder address(String address) {
         this.address = address;
         return this;
      }

      public Proxy.Builder port(int port) {
         this.port = port;
         return this;
      }

      public Proxy.Builder name(String name) {
         this.name = name;
         return this;
      }

      public Proxy.Builder username(String username) {
         this.username = username;
         return this;
      }

      public Proxy.Builder password(String password) {
         this.password = password;
         return this;
      }

      public Proxy.Builder enabled(boolean enabled) {
         this.enabled = enabled;
         return this;
      }

      public Proxy build() {
         Proxy proxy = new Proxy();
         if (!this.type.equals(proxy.type.getDefaultValue())) {
            proxy.type.set(this.type);
         }

         if (!this.address.equals(proxy.address.getDefaultValue())) {
            proxy.address.set(this.address);
         }

         if (this.port != proxy.port.getDefaultValue()) {
            proxy.port.set(this.port);
         }

         if (!this.name.equals(proxy.name.getDefaultValue())) {
            proxy.name.set(this.name);
         }

         if (!this.username.equals(proxy.username.getDefaultValue())) {
            proxy.username.set(this.username);
         }

         if (!this.password.equals(proxy.password.getDefaultValue())) {
            proxy.password.set(this.password);
         }

         if (this.enabled != proxy.enabled.getDefaultValue()) {
            proxy.enabled.set(this.enabled);
         }

         return proxy;
      }
   }

   public enum Status {
      UNCHECKED,
      CHECKING,
      DEAD,
      ALIVE;

      @Override
      public String toString() {
         return switch (this) {
            case UNCHECKED -> "";
            case CHECKING -> "...";
            case DEAD -> "X";
            case ALIVE -> "O";
         };
      }

      public Color getColor() {
         return switch (this) {
            case UNCHECKED, CHECKING -> Color.GRAY;
            case DEAD -> Color.RED;
            case ALIVE -> Color.GREEN;
         };
      }
   }
}
