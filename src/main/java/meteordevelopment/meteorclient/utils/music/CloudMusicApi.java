package meteordevelopment.meteorclient.utils.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class CloudMusicApi {
   private static final String HOST = "https://music.163.com";
   private static final String PLAYER_HOST = "https://interface3.music.163.com";
   private static final String BASE_COOKIE = "appver=2.7.1.198277; os=pc;";
   private static final String IMAGE_ID_MAGIC = "3go8&$8*3*3h0k(2)2";
   private static final int MAX_PLAYLIST_TRACKS = 300;
   private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).followRedirects(Redirect.ALWAYS).build();
   private static final Map<String, String> HEADERS = Map.of(
      "Accept",
      "*/*",
      "Accept-Language",
      "zh-CN,zh;q=0.9,en;q=0.8",
      "Content-Type",
      "application/x-www-form-urlencoded",
      "Referer",
      "http://music.163.com",
      "User-Agent",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36"
   );
   private static final Map<String, String> IMAGE_HEADERS = Map.of(
      "Accept",
      "image/png,image/jpeg,image/jpg,image/*;q=0.8,*/*;q=0.5",
      "Accept-Language",
      "zh-CN,zh;q=0.9,en;q=0.8",
      "Referer",
      "https://music.163.com/",
      "User-Agent",
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
   );

   private CloudMusicApi() {
   }

   public static String qrKey() throws IOException, InterruptedException {
      JsonObject json = postJson("/api/login/qrcode/unikey", Map.of("type", "1"));
      if (json != null && json.has("unikey")) {
         return json.get("unikey").getAsString();
      } else {
         throw new IOException("Failed to request qr key.");
      }
   }

   public static CloudMusicApi.LoginPollResult checkQr(String key) throws IOException, InterruptedException {
      HttpResponse<String> response = post("/api/login/qrcode/client/login", Map.of("key", key, "type", "1"));
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      int code = json.has("code") ? json.get("code").getAsInt() : -1;
      String cookie = joinCookies(response.headers().allValues("set-cookie"));
      if (cookie.isBlank() && json.has("cookie") && !json.get("cookie").isJsonNull()) {
         cookie = json.get("cookie").getAsString();
      }

      return new CloudMusicApi.LoginPollResult(code, cookie);
   }

   public static InputStream qrImage(String key) throws IOException, InterruptedException {
      String qrUrl = "https://music.163.com/login?codekey=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
      String url = "https://api.qrserver.com/v1/create-qr-code/?size=360x360&data=" + URLEncoder.encode(qrUrl, StandardCharsets.UTF_8);
      return image(url, Duration.ofSeconds(5L));
   }

   public static NativeImage qrImageLocal(String key) throws IOException {
      String qrUrl = "https://music.163.com/login?codekey=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
      return generateQrImage(qrUrl, 360);
   }

   public static InputStream image(String url) throws IOException, InterruptedException {
      return image(url, Duration.ofSeconds(8L));
   }

   private static InputStream image(String url, Duration timeout) throws IOException, InterruptedException {
      Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET();
      IMAGE_HEADERS.forEach(builder::header);
      HttpResponse<InputStream> response = CLIENT.send(builder.build(), BodyHandlers.ofInputStream());
      if (response.statusCode() != 200) {
         response.body().close();
         throw new IOException("Failed to download image: " + response.statusCode());
      } else {
         return response.body();
      }
   }

   public static List<CloudMusicApi.Song> searchSongs(String query, String cookie, int limit) throws IOException, InterruptedException {
      JsonObject json = postJson(
         "https://music.163.com",
         "/api/cloudsearch/pc",
         Map.of("s", query, "type", "1", "limit", Integer.toString(limit), "offset", "0", "total", "true"),
         cookie
      );
      if (json.has("result") && json.get("result").isJsonObject()) {
         JsonObject result = json.getAsJsonObject("result");
         if (result.has("songs") && result.get("songs").isJsonArray()) {
            List<CloudMusicApi.Song> songs = new ArrayList<>();

            for (JsonElement element : result.getAsJsonArray("songs")) {
               if (element.isJsonObject()) {
                  JsonObject song = element.getAsJsonObject();
                  long id = getLong(song, "id", -1L);
                  String name = getString(song, "name", "Unknown");
                  long durationMs = getLong(song, "dt", getLong(song, "duration", 0L));
                  String artist = readArtists(song);
                  String coverUrl = readCoverUrl(song);
                  if (id > 0L) {
                     songs.add(new CloudMusicApi.Song(id, name, artist, durationMs, coverUrl));
                  }
               }
            }

            return fillMissingCovers(cookie, songs);
         } else {
            return List.of();
         }
      } else {
         return List.of();
      }
   }

   public static CloudMusicApi.Account account(String cookie) throws IOException, InterruptedException {
      JsonObject json = postJson("https://music.163.com", "/api/w/nuser/account/get", Map.of(), cookie);
      if (json.has("profile") && json.get("profile").isJsonObject()) {
         JsonObject profile = json.getAsJsonObject("profile");
         long userId = getLong(profile, "userId", -1L);
         String nickname = getString(profile, "nickname", "Cloud Music");
         String avatarUrl = readAvatarUrl(profile);
         if (userId > 0L) {
            try {
               JsonObject detail = postJson("https://music.163.com", "/api/v1/user/detail/" + userId, Map.of(), cookie);
               if (detail.has("profile") && detail.get("profile").isJsonObject()) {
                  JsonObject detailProfile = detail.getAsJsonObject("profile");
                  if (nickname.equals("Cloud Music")) {
                     nickname = getString(detailProfile, "nickname", nickname);
                  }

                  String detailAvatarUrl = readAvatarUrl(detailProfile);
                  if (!detailAvatarUrl.isBlank()) {
                     avatarUrl = detailAvatarUrl;
                  }
               }
            } catch (IOException var10) {
            }
         }

         return new CloudMusicApi.Account(userId, nickname, avatarUrl);
      } else {
         throw new IOException("Missing account profile.");
      }
   }

   public static List<CloudMusicApi.Song> dailySongs(String cookie) throws IOException, InterruptedException {
      JsonObject json = postJson("https://music.163.com", "/api/v3/discovery/recommend/songs", Map.of(), cookie);
      if (json.has("data") && json.get("data").isJsonObject()) {
         JsonObject data = json.getAsJsonObject("data");
         return data.has("dailySongs") && data.get("dailySongs").isJsonArray()
            ? fillMissingCovers(cookie, readSongs(data.getAsJsonArray("dailySongs")))
            : List.of();
      } else {
         return List.of();
      }
   }

   public static List<CloudMusicApi.Playlist> userPlaylists(String cookie, long userId, int limit) throws IOException, InterruptedException {
      JsonObject json = postJson(
         "https://music.163.com",
         "/api/user/playlist",
         Map.of("uid", Long.toString(userId), "limit", Integer.toString(limit), "offset", "0", "total", "true"),
         cookie
      );
      if (json.has("playlist") && json.get("playlist").isJsonArray()) {
         List<CloudMusicApi.Playlist> playlists = new ArrayList<>();

         for (JsonElement element : json.getAsJsonArray("playlist")) {
            if (element.isJsonObject()) {
               JsonObject playlist = element.getAsJsonObject();
               long id = getLong(playlist, "id", -1L);
               String name = getString(playlist, "name", "Playlist");
               int count = (int)getLong(playlist, "trackCount", 0L);
               if (id > 0L) {
                  playlists.add(new CloudMusicApi.Playlist(id, name, count));
               }
            }
         }

         return playlists;
      } else {
         return List.of();
      }
   }

   public static List<CloudMusicApi.Song> likedSongs(String cookie) throws IOException, InterruptedException {
      CloudMusicApi.Account account = account(cookie);
      JsonObject json = postJson(
         "https://music.163.com", "/api/user/playlist", Map.of("uid", Long.toString(account.id()), "limit", "100", "offset", "0", "total", "true"), cookie
      );
      if (json.has("playlist") && json.get("playlist").isJsonArray()) {
         JsonArray playlists = json.getAsJsonArray("playlist");
         long likedId = -1L;
         long firstId = -1L;

         for (JsonElement element : playlists) {
            if (element.isJsonObject()) {
               JsonObject playlist = element.getAsJsonObject();
               long id = getLong(playlist, "id", -1L);
               if (id > 0L) {
                  if (firstId <= 0L) {
                     firstId = id;
                  }

                  String name = getString(playlist, "name", "");
                  int specialType = (int)getLong(playlist, "specialType", -1L);
                  if (specialType == 5 || name.contains("喜欢")) {
                     likedId = id;
                     break;
                  }
               }
            }
         }

         long playlistId = likedId > 0L ? likedId : firstId;
         return playlistId <= 0L ? List.of() : playlistSongs(cookie, playlistId);
      } else {
         return List.of();
      }
   }

   public static List<CloudMusicApi.Song> playlistSongs(String cookie, long playlistId) throws IOException, InterruptedException {
      JsonObject json = postJson(
         "https://music.163.com", "/api/v6/playlist/detail", Map.of("id", Long.toString(playlistId), "n", Integer.toString(300)), cookie
      );
      if (json.has("playlist") && json.get("playlist").isJsonObject()) {
         JsonObject playlist = json.getAsJsonObject("playlist");
         if (playlist.has("tracks") && playlist.get("tracks").isJsonArray() && playlist.getAsJsonArray("tracks").size() > 0) {
            return fillMissingCovers(cookie, readSongs(playlist.getAsJsonArray("tracks"), 300));
         } else {
            return playlist.has("trackIds") && playlist.get("trackIds").isJsonArray()
               ? fillMissingCovers(cookie, songDetails(cookie, playlist.getAsJsonArray("trackIds"), 300))
               : List.of();
         }
      } else {
         return List.of();
      }
   }

   public static List<CloudMusicApi.Artist> subscribedArtists(String cookie, int limit) throws IOException, InterruptedException {
      JsonObject json = postJson(
         "https://music.163.com", "/api/artist/sublist", Map.of("limit", Integer.toString(limit), "offset", "0", "total", "true"), cookie
      );
      if (json.has("data") && json.get("data").isJsonArray()) {
         List<CloudMusicApi.Artist> artists = new ArrayList<>();

         for (JsonElement element : json.getAsJsonArray("data")) {
            if (element.isJsonObject()) {
               JsonObject artist = element.getAsJsonObject();
               long id = getLong(artist, "id", -1L);
               String name = getString(artist, "name", "Artist");
               int songCount = (int)getLong(artist, "musicSize", getLong(artist, "songSize", 0L));
               if (id > 0L) {
                  artists.add(new CloudMusicApi.Artist(id, name, songCount));
               }
            }
         }

         return artists;
      } else {
         return List.of();
      }
   }

   public static List<CloudMusicApi.Song> artistTopSongs(String cookie, long artistId) throws IOException, InterruptedException {
      JsonObject json = postJson("https://music.163.com", "/api/artist/top/song", Map.of("id", Long.toString(artistId)), cookie);
      return json.has("songs") && json.get("songs").isJsonArray() ? fillMissingCovers(cookie, readSongs(json.getAsJsonArray("songs"))) : List.of();
   }

   public static String playUrl(long id, String cookie) throws IOException, InterruptedException {
      JsonObject json = postJsonWithFallback(
         "https://interface3.music.163.com",
         "https://music.163.com",
         "/api/song/enhance/player/url/v1",
         Map.of("ids", "[" + id + "]", "level", "standard", "encodeType", "mp3"),
         cookie
      );
      if (json.has("data") && json.get("data").isJsonArray()) {
         JsonArray data = json.getAsJsonArray("data");
         if (data.size() != 0 && data.get(0).isJsonObject()) {
            JsonObject first = data.get(0).getAsJsonObject();
            if (first.has("url") && !first.get("url").isJsonNull()) {
               return first.get("url").getAsString();
            } else {
               throw new IOException("Song has no playable url.");
            }
         } else {
            throw new IOException("Empty player url data.");
         }
      } else {
         throw new IOException("Missing player url data.");
      }
   }

   private static List<CloudMusicApi.Song> songDetails(String cookie, JsonArray trackIds, int limit) throws IOException, InterruptedException {
      List<CloudMusicApi.Song> songs = new ArrayList<>();
      StringJoiner c = new StringJoiner(",", "[", "]");
      int batchSize = 0;

      for (JsonElement element : trackIds) {
         if (songs.size() + batchSize >= limit) {
            break;
         }

         if (element.isJsonObject()) {
            long id = getLong(element.getAsJsonObject(), "id", -1L);
            if (id > 0L) {
               c.add("{\"id\":" + id + "}");
               if (++batchSize >= 300) {
                  songs.addAll(songDetailsBatch(cookie, c.toString()));
                  c = new StringJoiner(",", "[", "]");
                  batchSize = 0;
               }
            }
         }
      }

      if (batchSize > 0) {
         songs.addAll(songDetailsBatch(cookie, c.toString()));
      }

      return songs;
   }

   private static List<CloudMusicApi.Song> songDetailsBatch(String cookie, String c) throws IOException, InterruptedException {
      JsonObject json = postJson("https://music.163.com", "/api/v3/song/detail", Map.of("c", c), cookie);
      return json.has("songs") && json.get("songs").isJsonArray() ? readSongs(json.getAsJsonArray("songs")) : List.of();
   }

   private static List<CloudMusicApi.Song> fillMissingCovers(String cookie, List<CloudMusicApi.Song> songs) throws IOException, InterruptedException {
      if (songs.isEmpty()) {
         return songs;
      }

      StringJoiner c = new StringJoiner(",", "[", "]");
      int missing = 0;
      int batchSize = 0;
      List<CloudMusicApi.Song> details = new ArrayList<>();

      for (CloudMusicApi.Song song : songs) {
         if (song.coverUrl().isBlank()) {
            c.add("{\"id\":" + song.id() + "}");
            missing++;
            if (++batchSize >= 300) {
               details.addAll(songDetailsBatch(cookie, c.toString()));
               c = new StringJoiner(",", "[", "]");
               batchSize = 0;
            }
         }
      }

      if (missing == 0) {
         return songs;
      }

      if (batchSize > 0) {
         details.addAll(songDetailsBatch(cookie, c.toString()));
      }

      if (details.isEmpty()) {
         return songs;
      }

      List<CloudMusicApi.Song> filled = new ArrayList<>(songs.size());

      for (CloudMusicApi.Song song : songs) {
         if (!song.coverUrl().isBlank()) {
            filled.add(song);
         } else {
            CloudMusicApi.Song detail = findSong(details, song.id());
            filled.add(detail != null && !detail.coverUrl().isBlank() ? song.withCoverUrl(detail.coverUrl()) : song);
         }
      }

      return filled;
   }

   private static CloudMusicApi.Song findSong(List<CloudMusicApi.Song> songs, long id) {
      for (CloudMusicApi.Song song : songs) {
         if (song.id() == id) {
            return song;
         }
      }

      return null;
   }

   private static JsonObject postJson(String path, Map<String, String> data) throws IOException, InterruptedException {
      return postJson("https://music.163.com", path, data, "");
   }

   private static JsonObject postJson(String host, String path, Map<String, String> data, String cookie) throws IOException, InterruptedException {
      HttpResponse<String> response = post(host, path, data, cookie);
      if (response.statusCode() != 200) {
         throw new IOException("HTTP " + response.statusCode());
      } else {
         return JsonParser.parseString(response.body()).getAsJsonObject();
      }
   }

   private static JsonObject postJsonWithFallback(String primaryHost, String fallbackHost, String path, Map<String, String> data, String cookie) throws IOException, InterruptedException {
      try {
         return postJson(primaryHost, path, data, cookie);
      } catch (IOException primary) {
         try {
            return postJson(fallbackHost, path, data, cookie);
         } catch (IOException fallback) {
            primary.addSuppressed(fallback);
            throw primary;
         }
      }
   }

   private static HttpResponse<String> post(String path, Map<String, String> data) throws IOException, InterruptedException {
      return post("https://music.163.com", path, data, "");
   }

   private static HttpResponse<String> post(String host, String path, Map<String, String> data, String cookie) throws IOException, InterruptedException {
      Builder builder = HttpRequest.newBuilder(URI.create(host + path)).timeout(Duration.ofSeconds(8L)).POST(BodyPublishers.ofString(form(data)));
      HEADERS.forEach(builder::header);
      builder.header("Cookie", cookieHeader(cookie));
      return CLIENT.send(builder.build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
   }

   private static String form(Map<String, String> data) {
      StringJoiner joiner = new StringJoiner("&");
      data.forEach((key, value) -> joiner.add(URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)));
      return joiner.toString();
   }

   private static String cookieHeader(String cookie) {
      return cookie != null && !cookie.isBlank() ? "appver=2.7.1.198277; os=pc; " + cookie : "appver=2.7.1.198277; os=pc;";
   }

   private static String readArtists(JsonObject song) {
      JsonArray artists = null;
      if (song.has("ar") && song.get("ar").isJsonArray()) {
         artists = song.getAsJsonArray("ar");
      } else if (song.has("artists") && song.get("artists").isJsonArray()) {
         artists = song.getAsJsonArray("artists");
      }

      if (artists != null && artists.size() != 0) {
         List<String> names = new ArrayList<>();

         for (JsonElement element : artists) {
            if (element.isJsonObject()) {
               JsonObject artist = element.getAsJsonObject();
               names.add(getString(artist, "name", "Unknown"));
            }
         }

         return names.isEmpty() ? "Unknown" : String.join(" / ", names);
      } else {
         return "Unknown";
      }
   }

   private static List<CloudMusicApi.Song> readSongs(JsonArray array) {
      return readSongs(array, Integer.MAX_VALUE);
   }

   private static List<CloudMusicApi.Song> readSongs(JsonArray array, int limit) {
      List<CloudMusicApi.Song> songs = new ArrayList<>();

      for (JsonElement element : array) {
         if (songs.size() >= limit) {
            break;
         }

         if (element.isJsonObject()) {
            CloudMusicApi.Song song = readSong(element.getAsJsonObject());
            if (song != null) {
               songs.add(song);
            }
         }
      }

      return songs;
   }

   private static CloudMusicApi.Song readSong(JsonObject object) {
      JsonObject song = object;
      if (song.has("song") && song.get("song").isJsonObject()) {
         song = song.getAsJsonObject("song");
      } else if (song.has("songInfo") && song.get("songInfo").isJsonObject()) {
         song = song.getAsJsonObject("songInfo");
      } else if (song.has("simpleSong") && song.get("simpleSong").isJsonObject()) {
         song = song.getAsJsonObject("simpleSong");
      }

      long id = getLong(song, "id", -1L);
      if (id <= 0L) {
         return null;
      }

      String name = getString(song, "name", "Unknown");
      long durationMs = getLong(song, "dt", getLong(song, "duration", 0L));
      String artist = readArtists(song);
      String coverUrl = readCoverUrl(song);
      return new CloudMusicApi.Song(id, name, artist, durationMs, coverUrl);
   }

   private static String readCoverUrl(JsonObject song) {
      String coverUrl = getString(song, "picUrl", "");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = getString(song, "coverUrl", "");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = getString(song, "coverImgUrl", "");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = getString(song, "blurPicUrl", "");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = readCoverUrl(song, "album");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = readCoverUrl(song, "al");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = readCoverUrl(song, "pic");
      if (!coverUrl.isBlank()) {
         return coverUrl;
      }

      coverUrl = readCoverUrl(song, "originSongSimpleData");
      return !coverUrl.isBlank() ? coverUrl : readCoverUrl(song, "resource");
   }

   private static String readCoverUrl(JsonObject song, String key) {
      if (song.has(key) && song.get(key).isJsonObject()) {
         JsonObject album = song.getAsJsonObject(key);
         String coverUrl = getString(album, "picUrl", "");
         if (!coverUrl.isBlank()) {
            return coverUrl;
         }

         coverUrl = getString(album, "coverUrl", "");
         if (!coverUrl.isBlank()) {
            return coverUrl;
         }

         coverUrl = getString(album, "coverImgUrl", "");
         if (!coverUrl.isBlank()) {
            return coverUrl;
         }

         coverUrl = getString(album, "blurPicUrl", "");
         if (!coverUrl.isBlank()) {
            return coverUrl;
         }

         coverUrl = getString(album, "img1v1Url", "");
         if (!coverUrl.isBlank()) {
            return coverUrl;
         }

         coverUrl = getString(album, "url", "");
         if (!coverUrl.isBlank()) {
            return coverUrl;
         }

         String picStr = getString(album, "pic_str", "");
         if (!picStr.isBlank()) {
            return coverUrlFromPicId(picStr);
         }

         String pic = getString(album, "pic", "");
         if (!pic.isBlank() && !pic.equals("0")) {
            return coverUrlFromPicId(pic);
         }

         String picId = getString(album, "picId", "");
         return !picId.isBlank() && !picId.equals("0") ? coverUrlFromPicId(picId) : "";
      } else {
         return "";
      }
   }

   private static String coverUrlFromPicId(String picId) {
      if (picId == null) {
         return "";
      }

      picId = picId.trim();
      return !picId.isBlank() && !picId.equals("0") ? "https://p1.music.126.net/" + encryptedImageId(picId) + "/" + picId + ".jpg" : "";
   }

   private static String encryptedImageId(String imageId) {
      byte[] magic = "3go8&$8*3*3h0k(2)2".getBytes(StandardCharsets.UTF_8);
      byte[] bytes = imageId.getBytes(StandardCharsets.UTF_8);

      for (int i = 0; i < bytes.length; i++) {
         bytes[i] ^= magic[i % magic.length];
      }

      try {
         return Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(bytes)).replace('/', '_').replace('+', '-');
      } catch (NoSuchAlgorithmException ignored) {
         return imageId;
      }
   }

   private static String readAvatarUrl(JsonObject profile) {
      String avatarUrl = getString(profile, "avatarUrl", "");
      if (!avatarUrl.isBlank()) {
         return avatarUrl;
      }

      avatarUrl = getString(profile, "avatarImgUrl", "");
      if (!avatarUrl.isBlank()) {
         return avatarUrl;
      }

      avatarUrl = getString(profile, "avatarDetailUrl", "");
      if (!avatarUrl.isBlank()) {
         return avatarUrl;
      }

      String avatarId = getString(profile, "avatarImgIdStr", "");
      if (avatarId.isBlank()) {
         avatarId = getString(profile, "avatarImgId", "");
      }

      return !avatarId.isBlank() && !avatarId.equals("0") ? coverUrlFromPicId(avatarId) : "";
   }

   private static String getString(JsonObject object, String key, String fallback) {
      return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
   }

   private static long getLong(JsonObject object, String key, long fallback) {
      return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : fallback;
   }

   private static String joinCookies(List<String> cookies) {
      if (cookies.isEmpty()) {
         return "";
      }

      List<String> values = new ArrayList<>(cookies.size());

      for (String cookie : cookies) {
         int semicolon = cookie.indexOf(59);
         values.add(semicolon == -1 ? cookie : cookie.substring(0, semicolon));
      }

      return String.join("; ", values);
   }

   private static NativeImage generateQrImage(String text, int size) throws IOException {
      Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.MARGIN, 1);

      try {
         BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
         NativeImage image = new NativeImage(Format.RGBA, size, size, false);

         for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
               image.setPixelABGR(x, y, matrix.get(x, y) ? -16777216 : -1);
            }
         }

         return image;
      } catch (WriterException e) {
         throw new IOException("Failed to generate qr image.", e);
      }
   }

   public record Account(long id, String nickname, String avatarUrl) {
   }

   public record Artist(long id, String name, int songCount) {
   }

   public record LoginPollResult(int code, String cookie) {
   }

   public record Playlist(long id, String name, int count) {
   }

   public record Song(long id, String name, String artist, long durationMs, String coverUrl) {
      public CloudMusicApi.Song withCoverUrl(String coverUrl) {
         return new CloudMusicApi.Song(this.id, this.name, this.artist, this.durationMs, coverUrl);
      }
   }
}
