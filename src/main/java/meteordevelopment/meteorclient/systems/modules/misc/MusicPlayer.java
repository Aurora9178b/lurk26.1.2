package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.MusicPlayerScreen;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.music.CloudMusicApi;
import meteordevelopment.meteorclient.utils.music.CloudMusicAudioPlayer;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;

public class MusicPlayer extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<String> cookie = this.sgGeneral
      .add(new StringSetting.Builder().name("cookie").description("NetEase Cloud Music login cookie.").defaultValue("").wide().build());
   private final CloudMusicAudioPlayer audioPlayer = new CloudMusicAudioPlayer();
   private final List<CloudMusicApi.Song> queue = new ArrayList<>();
   private final List<CloudMusicApi.Song> searchResults = new ArrayList<>();
   private final List<CloudMusicApi.Song> likedSongs = new ArrayList<>();
   private final List<CloudMusicApi.Song> dailySongs = new ArrayList<>();
   private final List<CloudMusicApi.Playlist> playlists = new ArrayList<>();
   private final List<CloudMusicApi.Song> playlistSongs = new ArrayList<>();
   private final List<CloudMusicApi.Artist> artists = new ArrayList<>();
   private final List<CloudMusicApi.Song> artistSongs = new ArrayList<>();
   private volatile boolean loadingSearch;
   private volatile boolean loadingLiked;
   private volatile boolean loadingDaily;
   private volatile boolean loadingPlaylists;
   private volatile boolean loadingPlaylistSongs;
   private volatile boolean loadingArtists;
   private volatile boolean loadingArtistSongs;
   private volatile boolean loadingAccount;
   private volatile boolean accountLoadAttempted;
   private volatile boolean avatarLoadAttempted;
   private volatile boolean loadingStream;
   private volatile int currentIndex;
   private volatile String status = "Ready";
   private volatile String searchStatus = "输入关键词搜索";
   private volatile String likedStatus = "加载喜欢歌曲";
   private volatile String dailyStatus = "加载每日推荐";
   private volatile String playlistsStatus = "加载歌单";
   private volatile String playlistSongsStatus = "选择歌单";
   private volatile String artistsStatus = "加载关注歌手";
   private volatile String artistSongsStatus = "选择歌手";
   private volatile String accountName = "Cloud Music";
   private volatile String accountAvatarUrl = "";
   private volatile long accountId = -1L;
   private volatile long lastAccountLoadAt;
   private volatile long selectedPlaylistId = -1L;
   private volatile String selectedPlaylistName = "";
   private volatile long selectedArtistId = -1L;
   private volatile String selectedArtistName = "";

   public MusicPlayer() {
      super(Categories.Misc, "MusicPlayer", "Opens a cloud music player screen.");
      this.runInMainMenu = true;
   }

   @Override
   public void onActivate() {
      this.mc.setScreen(new MusicPlayerScreen(GuiThemes.get(), this));
      this.toggle();
   }

   public boolean isLoggedIn() {
      return !this.cookie.get().isBlank();
   }

   public void setCookie(String value) {
      String oldCookie = this.cookie.get();
      String newCookie = value == null ? "" : value;
      this.cookie.set(newCookie);
      Systems.save();
      if (!newCookie.equals(oldCookie)) {
         this.accountLoadAttempted = false;
         this.avatarLoadAttempted = false;
         this.accountId = -1L;
         this.accountName = "Cloud Music";
         this.accountAvatarUrl = "";
         this.lastAccountLoadAt = 0L;
      }

      if (!this.isLoggedIn()) {
         this.stop();
         synchronized (this.queue) {
            this.queue.clear();
         }

         synchronized (this.searchResults) {
            this.searchResults.clear();
         }

         synchronized (this.likedSongs) {
            this.likedSongs.clear();
         }

         synchronized (this.dailySongs) {
            this.dailySongs.clear();
         }

         synchronized (this.playlists) {
            this.playlists.clear();
         }

         synchronized (this.playlistSongs) {
            this.playlistSongs.clear();
         }

         synchronized (this.artists) {
            this.artists.clear();
         }

         synchronized (this.artistSongs) {
            this.artistSongs.clear();
         }

         this.status = "Ready";
         this.searchStatus = "输入关键词搜索";
         this.likedStatus = "加载喜欢歌曲";
         this.dailyStatus = "加载每日推荐";
         this.playlistsStatus = "加载歌单";
         this.playlistSongsStatus = "选择歌单";
         this.artistsStatus = "加载关注歌手";
         this.artistSongsStatus = "选择歌手";
         this.accountName = "Cloud Music";
         this.accountAvatarUrl = "";
         this.accountId = -1L;
         this.accountLoadAttempted = false;
         this.avatarLoadAttempted = false;
         this.loadingAccount = false;
         this.lastAccountLoadAt = 0L;
         this.selectedPlaylistId = -1L;
         this.selectedPlaylistName = "";
         this.selectedArtistId = -1L;
         this.selectedArtistName = "";
      }
   }

   public String getCookie() {
      return this.cookie.get();
   }

   public void search(String query) {
      if (this.isLoggedIn() && !this.loadingSearch) {
         String trimmed = query == null ? "" : query.trim();
         if (trimmed.isEmpty()) {
            this.searchStatus = "请输入搜索内容";
         } else {
            this.loadingSearch = true;
            this.searchStatus = "搜索中...";
            MeteorExecutor.execute(() -> {
               try {
                  List<CloudMusicApi.Song> songs = CloudMusicApi.searchSongs(trimmed, this.getCookie(), 30);
                  synchronized (this.searchResults) {
                     this.searchResults.clear();
                     this.searchResults.addAll(songs);
                  }

                  this.searchStatus = songs.isEmpty() ? "没有搜索结果" : "找到 " + songs.size() + " 首";
               } catch (Exception e) {
                  MeteorClient.LOG.error("Failed to search cloud music.", e);
                  this.searchStatus = this.failureStatus(e, "搜索失败");
               } finally {
                  this.loadingSearch = false;
               }
            });
         }
      }
   }

   public List<CloudMusicApi.Song> getQueue() {
      synchronized (this.queue) {
         return List.copyOf(this.queue);
      }
   }

   public List<CloudMusicApi.Song> getSearchResults() {
      synchronized (this.searchResults) {
         return List.copyOf(this.searchResults);
      }
   }

   public List<CloudMusicApi.Song> getLikedSongs() {
      synchronized (this.likedSongs) {
         return List.copyOf(this.likedSongs);
      }
   }

   public List<CloudMusicApi.Song> getDailySongs() {
      synchronized (this.dailySongs) {
         return List.copyOf(this.dailySongs);
      }
   }

   public List<CloudMusicApi.Playlist> getPlaylists() {
      synchronized (this.playlists) {
         return List.copyOf(this.playlists);
      }
   }

   public List<CloudMusicApi.Song> getPlaylistSongs() {
      synchronized (this.playlistSongs) {
         return List.copyOf(this.playlistSongs);
      }
   }

   public List<CloudMusicApi.Artist> getArtists() {
      synchronized (this.artists) {
         return List.copyOf(this.artists);
      }
   }

   public List<CloudMusicApi.Song> getArtistSongs() {
      synchronized (this.artistSongs) {
         return List.copyOf(this.artistSongs);
      }
   }

   public CloudMusicApi.Song getCurrentSong() {
      synchronized (this.queue) {
         return this.queue.isEmpty() ? null : this.queue.get(Math.floorMod(this.currentIndex, this.queue.size()));
      }
   }

   public int getCurrentIndex() {
      return this.currentIndex;
   }

   public boolean isLoadingSearch() {
      return this.loadingSearch;
   }

   public boolean isLoadingLiked() {
      return this.loadingLiked;
   }

   public boolean isLoadingDaily() {
      return this.loadingDaily;
   }

   public boolean isLoadingPlaylists() {
      return this.loadingPlaylists;
   }

   public boolean isLoadingPlaylistSongs() {
      return this.loadingPlaylistSongs;
   }

   public boolean isLoadingArtists() {
      return this.loadingArtists;
   }

   public boolean isLoadingArtistSongs() {
      return this.loadingArtistSongs;
   }

   public boolean isLoadingStream() {
      return this.loadingStream;
   }

   public boolean isPlaying() {
      return this.audioPlayer.isPlaying();
   }

   public boolean isPaused() {
      return this.audioPlayer.isPaused();
   }

   public long getPositionMs() {
      return this.audioPlayer.positionMs();
   }

   public double getProgress() {
      CloudMusicApi.Song song = this.getCurrentSong();
      return song != null && song.durationMs() > 0L ? Math.max(0.0, Math.min(1.0, (double)this.getPositionMs() / song.durationMs())) : 0.0;
   }

   public String getStatus() {
      return this.status;
   }

   public String getSearchStatus() {
      return this.searchStatus;
   }

   public String getLikedStatus() {
      return this.likedStatus;
   }

   public String getDailyStatus() {
      return this.dailyStatus;
   }

   public String getPlaylistsStatus() {
      return this.playlistsStatus;
   }

   public String getPlaylistSongsStatus() {
      return this.playlistSongsStatus;
   }

   public String getArtistsStatus() {
      return this.artistsStatus;
   }

   public String getArtistSongsStatus() {
      return this.artistSongsStatus;
   }

   public String getAccountName() {
      return this.accountName;
   }

   public String getAccountAvatarUrl() {
      return this.accountAvatarUrl;
   }

   public long getAccountId() {
      return this.accountId;
   }

   public String getSelectedPlaylistName() {
      return this.selectedPlaylistName;
   }

   public long getSelectedPlaylistId() {
      return this.selectedPlaylistId;
   }

   public String getSelectedArtistName() {
      return this.selectedArtistName;
   }

   public long getSelectedArtistId() {
      return this.selectedArtistId;
   }

   public void ensureAccountLoaded() {
      if (this.isLoggedIn() && !this.loadingAccount) {
         if (this.accountId <= 0L || this.accountAvatarUrl.isBlank()) {
            long now = System.currentTimeMillis();
            if (!this.accountLoadAttempted || this.accountId > 0L || now - this.lastAccountLoadAt >= 15000L) {
               if (!this.avatarLoadAttempted || this.accountId <= 0L || !this.accountAvatarUrl.isBlank() || now - this.lastAccountLoadAt >= 15000L) {
                  this.loadingAccount = true;
                  this.accountLoadAttempted = true;
                  this.lastAccountLoadAt = now;
                  MeteorExecutor.execute(() -> {
                     try {
                        CloudMusicApi.Account account = CloudMusicApi.account(this.getCookie());
                        this.accountId = account.id();
                        this.accountName = account.nickname();
                        this.accountAvatarUrl = account.avatarUrl();
                        this.avatarLoadAttempted = this.accountAvatarUrl.isBlank();
                     } catch (Exception e) {
                        MeteorClient.LOG.error("Failed to load cloud music account.", e);
                        this.status = this.failureStatus(e, this.status);
                     } finally {
                        this.loadingAccount = false;
                     }
                  });
               }
            }
         }
      }
   }

   public void refreshAccount() {
      if (this.isLoggedIn()) {
         this.accountLoadAttempted = false;
         this.avatarLoadAttempted = false;
         this.accountId = -1L;
         this.accountAvatarUrl = "";
         this.lastAccountLoadAt = 0L;
         this.ensureAccountLoaded();
      }
   }

   public void refreshLikedSongs() {
      synchronized (this.likedSongs) {
         this.likedSongs.clear();
      }

      this.loadLikedSongs();
   }

   public void loadLikedSongs() {
      if (this.isLoggedIn() && !this.loadingLiked && this.getLikedSongs().isEmpty()) {
         this.loadingLiked = true;
         this.likedStatus = "加载中...";
         MeteorExecutor.execute(() -> {
            try {
               List<CloudMusicApi.Song> songs = CloudMusicApi.likedSongs(this.getCookie());
               synchronized (this.likedSongs) {
                  this.likedSongs.clear();
                  this.likedSongs.addAll(songs);
               }

               this.likedStatus = songs.isEmpty() ? "没有喜欢歌曲" : "喜欢 " + songs.size() + " 首";
            } catch (Exception e) {
               MeteorClient.LOG.error("Failed to load liked songs.", e);
               this.likedStatus = this.failureStatus(e, "加载失败");
            } finally {
               this.loadingLiked = false;
            }
         });
      }
   }

   public void refreshDailySongs() {
      synchronized (this.dailySongs) {
         this.dailySongs.clear();
      }

      this.loadDailySongs();
   }

   public void loadDailySongs() {
      if (this.isLoggedIn() && !this.loadingDaily && this.getDailySongs().isEmpty()) {
         this.loadingDaily = true;
         this.dailyStatus = "加载中...";
         MeteorExecutor.execute(() -> {
            try {
               List<CloudMusicApi.Song> songs = CloudMusicApi.dailySongs(this.getCookie());
               synchronized (this.dailySongs) {
                  this.dailySongs.clear();
                  this.dailySongs.addAll(songs);
               }

               this.dailyStatus = songs.isEmpty() ? "没有每日推荐" : "每日推荐 " + songs.size() + " 首";
            } catch (Exception e) {
               MeteorClient.LOG.error("Failed to load daily songs.", e);
               this.dailyStatus = this.failureStatus(e, "加载失败");
            } finally {
               this.loadingDaily = false;
            }
         });
      }
   }

   public void refreshPlaylists() {
      this.selectedPlaylistId = -1L;
      this.selectedPlaylistName = "";
      synchronized (this.playlists) {
         this.playlists.clear();
      }

      synchronized (this.playlistSongs) {
         this.playlistSongs.clear();
      }

      this.loadPlaylists();
   }

   public void loadPlaylists() {
      if (this.isLoggedIn() && !this.loadingPlaylists && this.getPlaylists().isEmpty()) {
         this.loadingPlaylists = true;
         this.playlistsStatus = "加载中...";
         MeteorExecutor.execute(
            () -> {
               try {
                  CloudMusicApi.Account account = this.accountId > 0L && !this.accountAvatarUrl.isBlank()
                     ? new CloudMusicApi.Account(this.accountId, this.accountName, this.accountAvatarUrl)
                     : CloudMusicApi.account(this.getCookie());
                  this.accountId = account.id();
                  this.accountName = account.nickname();
                  this.accountAvatarUrl = account.avatarUrl();
                  List<CloudMusicApi.Playlist> result = CloudMusicApi.userPlaylists(this.getCookie(), account.id(), 100);
                  synchronized (this.playlists) {
                     this.playlists.clear();
                     this.playlists.addAll(result);
                  }

                  this.playlistsStatus = result.isEmpty() ? "没有歌单" : "歌单 " + result.size() + " 个";
                  if (!result.isEmpty() && this.selectedPlaylistId <= 0L) {
                     this.loadPlaylistSongs(0);
                  }
               } catch (Exception e) {
                  MeteorClient.LOG.error("Failed to load playlists.", e);
                  this.playlistsStatus = this.failureStatus(e, "加载失败");
               } finally {
                  this.loadingPlaylists = false;
               }
            }
         );
      }
   }

   public void loadPlaylistSongs(int index) {
      CloudMusicApi.Playlist playlist;
      synchronized (this.playlists) {
         if (index < 0 || index >= this.playlists.size()) {
            return;
         }

         playlist = this.playlists.get(index);
      }

      this.selectedPlaylistId = playlist.id();
      this.selectedPlaylistName = playlist.name();
      this.loadingPlaylistSongs = true;
      this.playlistSongsStatus = "加载 " + playlist.name() + "...";
      synchronized (this.playlistSongs) {
         this.playlistSongs.clear();
      }

      MeteorExecutor.execute(() -> {
         try {
            List<CloudMusicApi.Song> songs = CloudMusicApi.playlistSongs(this.getCookie(), playlist.id());
            synchronized (this.playlistSongs) {
               this.playlistSongs.clear();
               this.playlistSongs.addAll(songs);
            }

            this.playlistSongsStatus = songs.isEmpty() ? "歌单为空" : "歌曲 " + songs.size() + " 首";
         } catch (Exception e) {
            MeteorClient.LOG.error("Failed to load playlist songs.", e);
            this.playlistSongsStatus = this.failureStatus(e, "加载失败");
         } finally {
            this.loadingPlaylistSongs = false;
         }
      });
   }

   public void refreshArtists() {
      this.selectedArtistId = -1L;
      this.selectedArtistName = "";
      synchronized (this.artists) {
         this.artists.clear();
      }

      synchronized (this.artistSongs) {
         this.artistSongs.clear();
      }

      this.loadArtists();
   }

   public void loadArtists() {
      if (this.isLoggedIn() && !this.loadingArtists && this.getArtists().isEmpty()) {
         this.loadingArtists = true;
         this.artistsStatus = "加载中...";
         MeteorExecutor.execute(() -> {
            try {
               List<CloudMusicApi.Artist> result = CloudMusicApi.subscribedArtists(this.getCookie(), 100);
               synchronized (this.artists) {
                  this.artists.clear();
                  this.artists.addAll(result);
               }

               this.artistsStatus = result.isEmpty() ? "没有关注歌手" : "关注歌手 " + result.size() + " 个";
               if (!result.isEmpty() && this.selectedArtistId <= 0L) {
                  this.loadArtistSongs(0);
               }
            } catch (Exception e) {
               MeteorClient.LOG.error("Failed to load subscribed artists.", e);
               this.artistsStatus = this.failureStatus(e, "加载失败");
            } finally {
               this.loadingArtists = false;
            }
         });
      }
   }

   public void loadArtistSongs(int index) {
      CloudMusicApi.Artist artist;
      synchronized (this.artists) {
         if (index < 0 || index >= this.artists.size()) {
            return;
         }

         artist = this.artists.get(index);
      }

      this.selectedArtistId = artist.id();
      this.selectedArtistName = artist.name();
      this.loadingArtistSongs = true;
      this.artistSongsStatus = "加载 " + artist.name() + "...";
      synchronized (this.artistSongs) {
         this.artistSongs.clear();
      }

      MeteorExecutor.execute(() -> {
         try {
            List<CloudMusicApi.Song> songs = CloudMusicApi.artistTopSongs(this.getCookie(), artist.id());
            synchronized (this.artistSongs) {
               this.artistSongs.clear();
               this.artistSongs.addAll(songs);
            }

            this.artistSongsStatus = songs.isEmpty() ? "没有热门歌曲" : "热门 " + songs.size() + " 首";
         } catch (Exception e) {
            MeteorClient.LOG.error("Failed to load artist songs.", e);
            this.artistSongsStatus = this.failureStatus(e, "加载失败");
         } finally {
            this.loadingArtistSongs = false;
         }
      });
   }

   public void playPause() {
      if (this.isLoggedIn()) {
         if (this.audioPlayer.isPlaying()) {
            this.audioPlayer.pause();
            this.status = "Paused";
         } else if (this.audioPlayer.isPaused()) {
            this.audioPlayer.resume();
            this.status = "Playing";
         } else {
            this.playCurrent();
         }
      }
   }

   public void play(int index) {
      synchronized (this.queue) {
         if (index < 0 || index >= this.queue.size()) {
            return;
         }

         this.currentIndex = index;
      }

      this.playCurrent();
   }

   public void previous() {
      synchronized (this.queue) {
         if (this.queue.isEmpty()) {
            return;
         }

         this.currentIndex = Math.floorMod(this.currentIndex - 1, this.queue.size());
      }

      this.playCurrent();
   }

   public void next() {
      synchronized (this.queue) {
         if (this.queue.isEmpty()) {
            return;
         }

         this.currentIndex = Math.floorMod(this.currentIndex + 1, this.queue.size());
      }

      this.playCurrent();
   }

   public void stop() {
      this.audioPlayer.stop();
      this.loadingStream = false;
      this.status = "Stopped";
   }

   private void playCurrent() {
      if (this.isLoggedIn()) {
         CloudMusicApi.Song song = this.getCurrentSong();
         if (song == null) {
            this.status = "Queue empty";
         } else {
            this.loadingStream = true;
            this.status = "Loading stream...";
            MeteorExecutor.execute(() -> {
               try {
                  String url = CloudMusicApi.playUrl(song.id(), this.getCookie());
                  this.audioPlayer.play(url, this::playNextFromAudioThread, ex -> {
                     this.loadingStream = false;
                     this.status = this.failureStatus(ex, "播放失败");
                  });
                  this.loadingStream = false;
                  this.status = "Playing";
               } catch (Exception e) {
                  MeteorClient.LOG.error("Failed to request cloud music stream.", e);
                  this.loadingStream = false;
                  this.status = this.failureStatus(e, "播放失败");
               }
            });
         }
      }
   }

   private String failureStatus(Throwable throwable, String fallback) {
      if (this.hasCause(throwable, "HttpConnectTimeoutException") || this.hasCause(throwable, "HttpTimeoutException")) {
         return "连接超时";
      } else if (this.hasCause(throwable, "ConnectException")) {
         return "连接失败";
      } else {
         return this.hasCause(throwable, "UnknownHostException") ? "网络不可用" : fallback;
      }
   }

   private boolean hasCause(Throwable throwable, String simpleName) {
      for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
         if (cause.getClass().getSimpleName().equals(simpleName)) {
            return true;
         }

         String message = cause.getMessage();
         if (message != null) {
            String lower = message.toLowerCase();
            if (simpleName.contains("Timeout") && lower.contains("timed out")) {
               return true;
            }

            if (simpleName.equals("ConnectException") && lower.contains("connect")) {
               return true;
            }
         }
      }

      return false;
   }

   public void playSearchResult(int index) {
      CloudMusicApi.Song song;
      synchronized (this.searchResults) {
         if (index < 0 || index >= this.searchResults.size()) {
            return;
         }

         song = this.searchResults.get(index);
      }

      this.playSong(song);
   }

   public void playLikedSong(int index) {
      this.playFrom(this.likedSongs, index);
   }

   public void playDailySong(int index) {
      this.playFrom(this.dailySongs, index);
   }

   public void playPlaylistSong(int index) {
      this.playFrom(this.playlistSongs, index);
   }

   public void playArtistSong(int index) {
      this.playFrom(this.artistSongs, index);
   }

   private void playFrom(List<CloudMusicApi.Song> songs, int index) {
      CloudMusicApi.Song song;
      synchronized (songs) {
         if (index < 0 || index >= songs.size()) {
            return;
         }

         song = songs.get(index);
      }

      this.playSong(song);
   }

   private void playSong(CloudMusicApi.Song song) {
      synchronized (this.queue) {
         int index = this.indexOfQueued(song.id());
         if (index == -1) {
            this.queue.add(song);
            this.currentIndex = this.queue.size() - 1;
         } else {
            this.currentIndex = index;
         }
      }

      this.playCurrent();
   }

   private void playNextFromAudioThread() {
      synchronized (this.queue) {
         if (this.queue.isEmpty()) {
            return;
         }

         this.currentIndex = Math.floorMod(this.currentIndex + 1, this.queue.size());
      }

      this.playCurrent();
   }

   private int indexOfQueued(long id) {
      for (int i = 0; i < this.queue.size(); i++) {
         if (this.queue.get(i).id() == id) {
            return i;
         }
      }

      return -1;
   }
}
