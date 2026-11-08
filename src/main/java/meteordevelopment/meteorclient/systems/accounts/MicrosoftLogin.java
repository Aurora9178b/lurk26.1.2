package meteordevelopment.meteorclient.systems.accounts;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.util.Tuple;
import net.minecraft.util.Util;

public class MicrosoftLogin {
   private static final String CLIENT_ID = "4673b348-3efa-4f6a-bbb6-34e141cdc638";
   private static final int PORT = 9675;
   private static volatile HttpServer server;
   private static volatile Consumer<String> callback;

   private MicrosoftLogin() {
   }

   public static String getRefreshToken(Consumer<String> callback) {
      MicrosoftLogin.callback = callback;
      startServer();
      String url = "https://login.live.com/oauth20_authorize.srf?client_id=4673b348-3efa-4f6a-bbb6-34e141cdc638&response_type=code&redirect_uri=http://127.0.0.1:9675&scope=XboxLive.signin%20offline_access&prompt=select_account";
      Util.getPlatform().openUri(url);
      return url;
   }

   public static MicrosoftLogin.LoginData login(String refreshToken) {
      MicrosoftLogin.AuthTokenResponse res = Http.post("https://login.live.com/oauth20_token.srf")
         .bodyForm(
            "client_id=4673b348-3efa-4f6a-bbb6-34e141cdc638&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=http://127.0.0.1:9675"
         )
         .sendJson(MicrosoftLogin.AuthTokenResponse.class);
      if (res == null) {
         return new MicrosoftLogin.LoginData();
      } else {
         String accessToken = res.access_token;
         refreshToken = res.refresh_token;
         MicrosoftLogin.XblXstsResponse xblRes = Http.post("https://user.auth.xboxlive.com/user/authenticate")
            .bodyJson(
               "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d="
                  + accessToken
                  + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}"
            )
            .sendJson(MicrosoftLogin.XblXstsResponse.class);
         if (xblRes == null) {
            return new MicrosoftLogin.LoginData();
         } else {
            MicrosoftLogin.XblXstsResponse xstsRes = Http.post("https://xsts.auth.xboxlive.com/xsts/authorize")
               .bodyJson(
                  "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""
                     + xblRes.Token
                     + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}"
               )
               .sendJson(MicrosoftLogin.XblXstsResponse.class);
            if (xstsRes == null) {
               return new MicrosoftLogin.LoginData();
            } else {
               MicrosoftLogin.McResponse mcRes = Http.post("https://api.minecraftservices.com/authentication/login_with_xbox")
                  .bodyJson("{\"identityToken\":\"XBL3.0 x=" + xblRes.DisplayClaims.xui[0].uhs + ";" + xstsRes.Token + "\"}")
                  .sendJson(MicrosoftLogin.McResponse.class);
               if (mcRes == null) {
                  return new MicrosoftLogin.LoginData();
               } else {
                  MicrosoftLogin.GameOwnershipResponse gameOwnershipRes = Http.get("https://api.minecraftservices.com/entitlements/mcstore")
                     .bearer(mcRes.access_token)
                     .sendJson(MicrosoftLogin.GameOwnershipResponse.class);
                  if (gameOwnershipRes != null && gameOwnershipRes.hasGameOwnership()) {
                     MicrosoftLogin.ProfileResponse profileRes = Http.get("https://api.minecraftservices.com/minecraft/profile")
                        .bearer(mcRes.access_token)
                        .sendJson(MicrosoftLogin.ProfileResponse.class);
                     return profileRes == null
                        ? new MicrosoftLogin.LoginData()
                        : new MicrosoftLogin.LoginData(mcRes.access_token, refreshToken, profileRes.id, profileRes.name);
                  } else {
                     return new MicrosoftLogin.LoginData();
                  }
               }
            }
         }
      }
   }

   private static void startServer() {
      if (server == null) {
         try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 9675), 0);
            server.createContext("/", MicrosoftLogin::handleRequest);
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
         } catch (IOException e) {
            MeteorClient.LOG.error("Error starting Microsoft login server", e);
            stopServer();
         }
      }
   }

   public static void stopServer() {
      if (server != null) {
         server.stop(0);
         server = null;
         callback = null;
      }
   }

   private static void handleRequest(HttpExchange req) throws IOException {
      if (req.getRequestMethod().equals("GET")) {
         List<Tuple<String, String>> query = parseURL(req.getRequestURI().getRawQuery());
         boolean ok = false;

         for (Tuple<String, String> pair : query) {
            if (pair.getA().equals("code")) {
               handleCode(pair.getB());
               ok = true;
               break;
            }
         }

         if (!ok) {
            writeText(req, "Cannot authenticate.");
            callback.accept(null);
         } else {
            writeText(req, "You may now close this page.");
         }
      }

      stopServer();
   }

   private static void handleCode(String code) {
      MicrosoftLogin.AuthTokenResponse res = Http.post("https://login.live.com/oauth20_token.srf")
         .bodyForm("client_id=4673b348-3efa-4f6a-bbb6-34e141cdc638&code=" + code + "&grant_type=authorization_code&redirect_uri=http://127.0.0.1:9675")
         .sendJson(MicrosoftLogin.AuthTokenResponse.class);
      if (res == null) {
         callback.accept(null);
      } else {
         callback.accept(res.refresh_token);
      }
   }

   private static void writeText(HttpExchange req, String text) throws IOException {
      byte[] responseBody = text.getBytes(StandardCharsets.UTF_8);
      req.sendResponseHeaders(200, responseBody.length);

      try (OutputStream out = req.getResponseBody()) {
         out.write(responseBody);
      }
   }

   private static List<Tuple<String, String>> parseURL(String string) {
      List<Tuple<String, String>> query = new ArrayList<>();
      char[] buf = string.toCharArray();
      int i = 0;

      while (i < buf.length) {
         StringBuilder name = new StringBuilder();
         StringBuilder value = new StringBuilder();

         while (i < buf.length && buf[i] != '&' && buf[i] != ';' && buf[i] != '=') {
            name.append(buf[i]);
            i++;
         }

         if (i < buf.length) {
            char ch = buf[i];
            i++;
            if (ch == '=') {
               while (i < buf.length) {
                  if (buf[i] == '&' || buf[i] == ';') {
                     i++;
                     break;
                  }

                  value.append(buf[i]);
                  i++;
               }
            }
         }

         if (!name.isEmpty()) {
            query.add(new Tuple<>(urlDecode(name.toString()), urlDecode(value.toString())));
         }
      }

      return query;
   }

   private static String urlDecode(String s) {
      if (s == null) {
         return null;
      }

      ByteBuffer bb = ByteBuffer.allocate(s.length());
      CharBuffer cb = CharBuffer.wrap(s);

      while (cb.hasRemaining()) {
         char c = cb.get();
         if (c == '%' && cb.remaining() >= 2) {
            char uc = cb.get();
            char lc = cb.get();
            int u = Character.digit(uc, 16);
            int l = Character.digit(lc, 16);
            if (u != -1 && l != -1) {
               bb.put((byte)((u << 4) + l));
            } else {
               bb.put((byte)37);
               bb.put((byte)uc);
               bb.put((byte)lc);
            }
         } else if (c == '+') {
            bb.put((byte)32);
         } else {
            bb.put((byte)c);
         }
      }

      bb.flip();
      return StandardCharsets.UTF_8.decode(bb).toString();
   }

   private static class AuthTokenResponse {
      public String access_token;
      public String refresh_token;
   }

   private static class GameOwnershipResponse {
      private MicrosoftLogin.GameOwnershipResponse.Item[] items;

      private boolean hasGameOwnership() {
         boolean hasProduct = false;
         boolean hasGame = false;

         for (MicrosoftLogin.GameOwnershipResponse.Item item : this.items) {
            if (item.name.equals("product_minecraft")) {
               hasProduct = true;
            } else if (item.name.equals("game_minecraft")) {
               hasGame = true;
            }
         }

         return hasProduct && hasGame;
      }

      private static class Item {
         private String name;
      }
   }

   public static class LoginData {
      public String mcToken;
      public String newRefreshToken;
      public String uuid;
      public String username;

      public LoginData() {
      }

      public LoginData(String mcToken, String newRefreshToken, String uuid, String username) {
         this.mcToken = mcToken;
         this.newRefreshToken = newRefreshToken;
         this.uuid = uuid;
         this.username = username;
      }

      public boolean isGood() {
         return this.mcToken != null;
      }
   }

   private static class McResponse {
      public String access_token;
   }

   private static class ProfileResponse {
      public String id;
      public String name;
   }

   private static class XblXstsResponse {
      public String Token;
      public MicrosoftLogin.XblXstsResponse.DisplayClaims DisplayClaims;

      private static class DisplayClaims {
         private MicrosoftLogin.XblXstsResponse.DisplayClaims.Claim[] xui;

         private static class Claim {
            private String uhs;
         }
      }
   }
}
