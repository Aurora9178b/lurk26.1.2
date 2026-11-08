package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.misc.MusicPlayer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.music.CloudMusicApi;
import meteordevelopment.meteorclient.utils.music.MusicManager;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public class MusicPlayerScreen extends WidgetScreen {
   private static final double BASE_WIDTH = 880.0;
   private static final double BASE_HEIGHT = 520.0;
   private static final double PANEL_RADIUS = 18.0;
   private static final double CARD_RADIUS = 10.0;
   private static final double QR_SIZE = 220.0;
   private static final Color BLUR_TINT = new Color(8, 8, 8, 86);
   private static final Color SIDE_TINT = new Color(0, 0, 0, 132);
   private static final Color CARD_TINT = new Color(20, 20, 20, 118);
   private static final Color SELECT_TINT = new Color(48, 48, 48, 126);
   private static final Color LINE_TINT = new Color(255, 255, 255, 34);
   private static final Color TEXT = new Color(248, 248, 248, 245);
   private static final Color MUTED = new Color(210, 210, 210, 150);
   private static final Color ACCENT = new Color(109, 255, 178, 245);
   private static final Color DANGER = new Color(255, 112, 92, 235);
   private final MusicPlayer module;
   private MusicPlayerScreen.MusicPanel panel;
   private MusicPlayerScreen.Page page = MusicPlayerScreen.Page.Login;
   private volatile MusicPlayerScreen.LoginState loginState = MusicPlayerScreen.LoginState.Idle;
   private volatile String message = "点击登录";
   private volatile NativeImage pendingQrImage;
   private volatile boolean pollLogin;
   private volatile boolean closed;
   private Identifier qrTexture;
   private String qrKey;
   private int tickCounter;

   public MusicPlayerScreen(GuiTheme theme, MusicPlayer module) {
      super(theme, "Music Player");
      this.module = module;
      this.parent = null;
   }

   @Override
   public void initWidgets() {
      this.panel = this.add(new MusicPlayerScreen.MusicPanel()).center().widget();
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public void tick() {
      super.tick();
      if (this.module.isLoggedIn()) {
         this.module.ensureAccountLoaded();
      }

      NativeImage image = this.pendingQrImage;
      if (image != null) {
         this.pendingQrImage = null;
         this.releaseQrTexture();
         this.qrTexture = MeteorClient.identifier("music-player/qr-" + System.nanoTime());
         MeteorClient.mc.getTextureManager().register(this.qrTexture, new DynamicTexture(null, image));
         this.refreshWidgets();
      }

      if (this.pollLogin && ++this.tickCounter % 20 == 0) {
         this.pollLogin();
      }
   }

   private String getStatusText() {
      if (this.module.isLoggedIn()) {
         return "已登录";
      }

      return switch (this.loginState) {
         case Idle -> "点击左上角登录";
         case Loading -> "正在生成二维码...";
         case Waiting -> "请使用网易云音乐扫码";
         case Done -> "已登录";
         case Error -> "登录失败或二维码过期";
      };
   }

   private void refreshWidgets() {
      if (this.panel != null) {
         this.panel.invalidate();
      }
   }

   private void startLogin() {
      if (!this.module.isLoggedIn() && this.loginState != MusicPlayerScreen.LoginState.Loading && this.loginState != MusicPlayerScreen.LoginState.Waiting) {
         this.releaseQrTexture();
         this.releasePendingQrImage();
         this.loginState = MusicPlayerScreen.LoginState.Loading;
         this.message = "生成中...";
         this.pollLogin = false;
         this.tickCounter = 0;
         this.refreshWidgets();
         MeteorExecutor.execute(() -> {
            try {
               this.qrKey = CloudMusicApi.qrKey();
               NativeImage image = CloudMusicApi.qrImageLocal(this.qrKey);
               MeteorClient.mc.execute(() -> {
                  if (this.closed) {
                     image.close();
                  } else {
                     this.pendingQrImage = image;
                     this.loginState = MusicPlayerScreen.LoginState.Waiting;
                     this.message = "等待扫码";
                     this.pollLogin = true;
                     this.refreshWidgets();
                  }
               });
            } catch (Exception e) {
               MeteorClient.LOG.error("Failed to start cloud music login.", e);
               this.loginState = MusicPlayerScreen.LoginState.Error;
               this.message = this.errorMessage(e);
               MeteorClient.mc.execute(this::refreshWidgets);
            }
         });
      }
   }

   private void pollLogin() {
      if (this.qrKey != null && this.loginState == MusicPlayerScreen.LoginState.Waiting) {
         MeteorExecutor.execute(() -> {
            try {
               CloudMusicApi.LoginPollResult result = CloudMusicApi.checkQr(this.qrKey);
               if (result.code() == 803 && !result.cookie().isEmpty()) {
                  MeteorClient.mc.execute(() -> {
                     if (this.loginState == MusicPlayerScreen.LoginState.Waiting) {
                        this.module.setCookie(result.cookie());
                        this.markLoggedIn();
                        this.refreshWidgets();
                     }
                  });
               } else if (result.code() == 800) {
                  this.loginState = MusicPlayerScreen.LoginState.Error;
                  this.message = "已过期";
                  this.pollLogin = false;
                  MeteorClient.mc.execute(this::refreshWidgets);
               } else if (result.code() == 802) {
                  this.message = "确认登录";
                  MeteorClient.mc.execute(this::refreshWidgets);
               }
            } catch (Exception e) {
               MeteorClient.LOG.error("Failed to poll cloud music login.", e);
               this.loginState = MusicPlayerScreen.LoginState.Error;
               this.message = this.errorMessage(e);
               this.pollLogin = false;
               MeteorClient.mc.execute(this::refreshWidgets);
            }
         });
      }
   }

   private String errorMessage(Throwable throwable) {
      if (this.hasCause(throwable, "HttpConnectTimeoutException") || this.hasCause(throwable, "HttpTimeoutException")) {
         return "连接超时，重新登录";
      } else if (this.hasCause(throwable, "ConnectException")) {
         return "连接失败，重新登录";
      } else {
         return this.hasCause(throwable, "UnknownHostException") ? "网络不可用，重新登录" : "重新登录";
      }
   }

   private boolean hasCause(Throwable throwable, String simpleName) {
      for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
         if (cause.getClass().getSimpleName().equals(simpleName)) {
            return true;
         }
      }

      return false;
   }

   private void markLoggedIn() {
      this.loginState = MusicPlayerScreen.LoginState.Done;
      this.message = "已登录";
      this.pollLogin = false;
      this.releaseQrTexture();
   }

   private void logout() {
      this.module.setCookie("");
      this.loginState = MusicPlayerScreen.LoginState.Idle;
      this.message = "点击登录";
      this.pollLogin = false;
      this.qrKey = null;
      this.releasePendingQrImage();
      this.releaseQrTexture();
      this.refreshWidgets();
   }

   private void releaseQrTexture() {
      if (this.qrTexture != null) {
         MeteorClient.mc.getTextureManager().release(this.qrTexture);
         this.qrTexture = null;
      }
   }

   private void releasePendingQrImage() {
      NativeImage image = this.pendingQrImage;
      this.pendingQrImage = null;
      if (image != null) {
         image.close();
      }
   }

   private Identifier coverTexture(CloudMusicApi.Song song) {
      return song != null && !song.coverUrl().isBlank() ? this.imageTexture(song.coverUrl()) : null;
   }

   private Identifier accountAvatarTexture() {
      String avatarUrl = this.module.getAccountAvatarUrl();
      return avatarUrl.isBlank() ? null : this.imageTexture(avatarUrl);
   }

   private Identifier imageTexture(String url) {
      return MusicManager.imageTexture(url, () -> {
         if (!this.closed) {
            this.refreshWidgets();
         }
      });
   }

   @Override
   public void removed() {
      super.removed();
      this.closed = true;
      this.pollLogin = false;
      this.releasePendingQrImage();
      this.releaseQrTexture();
   }

   private enum ListKind {
      Liked,
      Daily;
   }

   private enum LoginState {
      Idle,
      Loading,
      Waiting,
      Done,
      Error;
   }

   private class MusicPanel extends WWidget {
      private double loginX;
      private double loginY;
      private double loginW;
      private double loginH;
      private double reloadX;
      private double reloadY;
      private double reloadW;
      private double reloadH;
      private double logoutX;
      private double logoutY;
      private double logoutW;
      private double logoutH;
      private final double[] navX = new double[7];
      private final double[] navY = new double[7];
      private final double[] navW = new double[7];
      private final double[] navH = new double[7];
      private double prevX;
      private double prevY;
      private double prevW;
      private double prevH;
      private double playX;
      private double playY;
      private double playW;
      private double playH;
      private double nextX;
      private double nextY;
      private double nextW;
      private double nextH;
      private double listX;
      private double listY;
      private double listW;
      private double rowH;
      private double rowStep;
      private double sideListX;
      private double sideListY;
      private double sideListW;
      private double sideListRowH;
      private double sideListRowStep;
      private double detailListX;
      private double detailListY;
      private double detailListW;
      private double detailListRowH;
      private double detailListRowStep;
      private double searchBoxX;
      private double searchBoxY;
      private double searchBoxW;
      private double searchBoxH;
      private double searchButtonX;
      private double searchButtonY;
      private double searchButtonW;
      private double searchButtonH;
      private int visibleQueueSongs;
      private int visibleSearchSongs;
      private int visibleLikedSongs;
      private int visibleDailySongs;
      private int visibleSideItems;
      private int visibleDetailSongs;
      private int queueScroll;
      private int searchScroll;
      private int likedScroll;
      private int dailyScroll;
      private int playlistScroll;
      private int playlistSongScroll;
      private int artistScroll;
      private int artistSongScroll;
      private float queueScrollProgress;
      private float searchScrollProgress;
      private float likedScrollProgress;
      private float dailyScrollProgress;
      private float playlistScrollProgress;
      private float playlistSongScrollProgress;
      private float artistScrollProgress;
      private float artistSongScrollProgress;
      private double lastMouseX;
      private double lastMouseY;
      private float openProgress;
      private float loginHoverProgress;
      private float reloadHoverProgress;
      private float logoutHoverProgress;
      private float searchFocusProgress;
      private float footerProgress;
      private final float[] navProgress = new float[7];
      private final float[] navHoverProgress = new float[7];
      private boolean searchFocused;
      private String searchQuery = "";

      @Override
      protected void onCalculateSize() {
         double margin = this.theme.scale(40.0);
         this.width = Math.min(this.theme.scale(880.0), Math.max(this.theme.scale(320.0), Utils.getWindowWidth() - margin));
         this.height = Math.min(this.theme.scale(520.0), Math.max(this.theme.scale(280.0), Utils.getWindowHeight() - margin));
      }

      @Override
      public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
         if (click.button() != 0) {
            return false;
         }

         double mouseX = click.x();
         double mouseY = click.y();
         if (this.inside(mouseX, mouseY, this.loginX, this.loginY, this.loginW, this.loginH)) {
            if (!MusicPlayerScreen.this.module.isLoggedIn()) {
               MusicPlayerScreen.this.startLogin();
            }

            MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Login;
            return true;
         } else if (this.inside(mouseX, mouseY, this.reloadX, this.reloadY, this.reloadW, this.reloadH)) {
            if (!MusicPlayerScreen.this.module.isLoggedIn()) {
               MusicPlayerScreen.this.startLogin();
            } else {
               this.refreshPage(MusicPlayerScreen.this.page);
            }

            return true;
         } else if (this.inside(mouseX, mouseY, this.logoutX, this.logoutY, this.logoutW, this.logoutH)) {
            if (MusicPlayerScreen.this.module.isLoggedIn()) {
               MusicPlayerScreen.this.logout();
            }

            MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Login;
            return true;
         } else {
            for (int i = 0; i < this.navX.length; i++) {
               if (this.inside(mouseX, mouseY, this.navX[i], this.navY[i], this.navW[i], this.navH[i])) {
                  MusicPlayerScreen.this.page = this.navPage(i);
                  this.searchFocused = false;
                  this.loadPage(MusicPlayerScreen.this.page);
                  return true;
               }
            }

            if (MusicPlayerScreen.this.module.isLoggedIn()) {
               if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Search) {
                  this.searchFocused = this.inside(mouseX, mouseY, this.searchBoxX, this.searchBoxY, this.searchBoxW, this.searchBoxH);
                  if (this.inside(mouseX, mouseY, this.searchButtonX, this.searchButtonY, this.searchButtonW, this.searchButtonH)) {
                     this.searchScroll = 0;
                     this.searchScrollProgress = 0.0F;
                     MusicPlayerScreen.this.module.search(this.searchQuery);
                     this.searchFocused = false;
                     return true;
                  }

                  for (int i = 0; i < this.visibleSearchSongs; i++) {
                     if (this.inside(mouseX, mouseY, this.listX, this.listY + i * this.rowStep, this.listW, this.rowH)) {
                        MusicPlayerScreen.this.module
                           .playSearchResult(
                              this.clickedAnimatedIndex(
                                 this.searchScrollProgress, mouseY, this.listY, this.rowStep, MusicPlayerScreen.this.module.getSearchResults().size()
                              )
                           );
                        MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Player;
                        this.searchFocused = false;
                        return true;
                     }
                  }

                  return this.searchFocused;
               }

               if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Liked) {
                  for (int i = 0; i < this.visibleLikedSongs; i++) {
                     if (this.inside(mouseX, mouseY, this.listX, this.listY + i * this.rowStep, this.listW, this.rowH)) {
                        MusicPlayerScreen.this.module
                           .playLikedSong(
                              this.clickedAnimatedIndex(
                                 this.likedScrollProgress, mouseY, this.listY, this.rowStep, MusicPlayerScreen.this.module.getLikedSongs().size()
                              )
                           );
                        MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Player;
                        return true;
                     }
                  }
               }

               if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Daily) {
                  for (int i = 0; i < this.visibleDailySongs; i++) {
                     if (this.inside(mouseX, mouseY, this.listX, this.listY + i * this.rowStep, this.listW, this.rowH)) {
                        MusicPlayerScreen.this.module
                           .playDailySong(
                              this.clickedAnimatedIndex(
                                 this.dailyScrollProgress, mouseY, this.listY, this.rowStep, MusicPlayerScreen.this.module.getDailySongs().size()
                              )
                           );
                        MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Player;
                        return true;
                     }
                  }
               }

               if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Playlists) {
                  for (int i = 0; i < this.visibleSideItems; i++) {
                     if (this.inside(mouseX, mouseY, this.sideListX, this.sideListY + i * this.sideListRowStep, this.sideListW, this.sideListRowH)) {
                        MusicPlayerScreen.this.module
                           .loadPlaylistSongs(
                              this.clickedAnimatedIndex(
                                 this.playlistScrollProgress, mouseY, this.sideListY, this.sideListRowStep, MusicPlayerScreen.this.module.getPlaylists().size()
                              )
                           );
                        this.playlistSongScroll = 0;
                        this.playlistSongScrollProgress = 0.0F;
                        return true;
                     }
                  }

                  for (int i = 0; i < this.visibleDetailSongs; i++) {
                     if (this.inside(mouseX, mouseY, this.detailListX, this.detailListY + i * this.detailListRowStep, this.detailListW, this.detailListRowH)) {
                        MusicPlayerScreen.this.module
                           .playPlaylistSong(
                              this.clickedAnimatedIndex(
                                 this.playlistSongScrollProgress,
                                 mouseY,
                                 this.detailListY,
                                 this.detailListRowStep,
                                 MusicPlayerScreen.this.module.getPlaylistSongs().size()
                              )
                           );
                        MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Player;
                        return true;
                     }
                  }
               }

               if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Artists) {
                  for (int i = 0; i < this.visibleSideItems; i++) {
                     if (this.inside(mouseX, mouseY, this.sideListX, this.sideListY + i * this.sideListRowStep, this.sideListW, this.sideListRowH)) {
                        MusicPlayerScreen.this.module
                           .loadArtistSongs(
                              this.clickedAnimatedIndex(
                                 this.artistScrollProgress, mouseY, this.sideListY, this.sideListRowStep, MusicPlayerScreen.this.module.getArtists().size()
                              )
                           );
                        this.artistSongScroll = 0;
                        this.artistSongScrollProgress = 0.0F;
                        return true;
                     }
                  }

                  for (int i = 0; i < this.visibleDetailSongs; i++) {
                     if (this.inside(mouseX, mouseY, this.detailListX, this.detailListY + i * this.detailListRowStep, this.detailListW, this.detailListRowH)) {
                        MusicPlayerScreen.this.module
                           .playArtistSong(
                              this.clickedAnimatedIndex(
                                 this.artistSongScrollProgress,
                                 mouseY,
                                 this.detailListY,
                                 this.detailListRowStep,
                                 MusicPlayerScreen.this.module.getArtistSongs().size()
                              )
                           );
                        MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Player;
                        return true;
                     }
                  }
               }

               if (this.inside(mouseX, mouseY, this.prevX, this.prevY, this.prevW, this.prevH)) {
                  MusicPlayerScreen.this.module.previous();
                  return true;
               }

               if (this.inside(mouseX, mouseY, this.playX, this.playY, this.playW, this.playH)) {
                  MusicPlayerScreen.this.module.playPause();
                  return true;
               }

               if (this.inside(mouseX, mouseY, this.nextX, this.nextY, this.nextW, this.nextH)) {
                  MusicPlayerScreen.this.module.next();
                  return true;
               }

               if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Queue) {
                  for (int i = 0; i < this.visibleQueueSongs; i++) {
                     if (this.inside(mouseX, mouseY, this.listX, this.listY + i * this.rowStep, this.listW, this.rowH)) {
                        MusicPlayerScreen.this.module
                           .play(
                              this.clickedAnimatedIndex(
                                 this.queueScrollProgress, mouseY, this.listY, this.rowStep, MusicPlayerScreen.this.module.getQueue().size()
                              )
                           );
                        MusicPlayerScreen.this.page = MusicPlayerScreen.Page.Player;
                        return true;
                     }
                  }
               }
            }

            return false;
         }
      }

      @Override
      public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
      }

      @Override
      public boolean onMouseScrolled(double amount) {
         if (MusicPlayerScreen.this.module.isLoggedIn() && amount != 0.0) {
            int delta = amount < 0.0 ? 1 : -1;

            return switch (MusicPlayerScreen.this.page) {
               case Search -> this.scrollMainList(
                  delta, MusicPlayerScreen.this.module.getSearchResults().size(), this.visibleSearchSongs, MusicPlayerScreen.ScrollTarget.Search
               );
               case Queue -> this.scrollMainList(
                  delta, MusicPlayerScreen.this.module.getQueue().size(), this.visibleQueueSongs, MusicPlayerScreen.ScrollTarget.Queue
               );
               case Liked -> this.scrollMainList(
                  delta, MusicPlayerScreen.this.module.getLikedSongs().size(), this.visibleLikedSongs, MusicPlayerScreen.ScrollTarget.Liked
               );
               case Daily -> this.scrollMainList(
                  delta, MusicPlayerScreen.this.module.getDailySongs().size(), this.visibleDailySongs, MusicPlayerScreen.ScrollTarget.Daily
               );
               case Playlists -> this.scrollSplitList(
                  delta,
                  MusicPlayerScreen.this.module.getPlaylists().size(),
                  this.visibleSideItems,
                  MusicPlayerScreen.this.module.getPlaylistSongs().size(),
                  this.visibleDetailSongs,
                  true
               );
               case Artists -> this.scrollSplitList(
                  delta,
                  MusicPlayerScreen.this.module.getArtists().size(),
                  this.visibleSideItems,
                  MusicPlayerScreen.this.module.getArtistSongs().size(),
                  this.visibleDetailSongs,
                  false
               );
               default -> false;
            };
         } else {
            return false;
         }
      }

      @Override
      public boolean onKeyPressed(KeyEvent input) {
         if (!this.searchFocused) {
            return false;
         }

         if (input.key() == 257 || input.key() == 335) {
            this.searchScroll = 0;
            this.searchScrollProgress = 0.0F;
            MusicPlayerScreen.this.module.search(this.searchQuery);
            this.searchFocused = false;
            return true;
         }

         if (input.key() == 256) {
            this.searchFocused = false;
            return true;
         }

         if (input.key() == 259) {
            if (!this.searchQuery.isEmpty()) {
               this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
            }

            return true;
         } else {
            return false;
         }
      }

      @Override
      public boolean onCharTyped(CharacterEvent input) {
         if (!this.searchFocused) {
            return false;
         } else {
            String value = input.codepointAsString();
            if (value != null && !value.isEmpty() && !Character.isISOControl(input.codepoint())) {
               this.searchQuery = this.searchQuery + value;
               return true;
            } else {
               return false;
            }
         }
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         double radius = this.theme.scale(18.0);
         double pad = this.theme.scale(18.0);
         this.updateAnimations(mouseX, mouseY);
         double animatedScale = 0.96 + 0.04 * this.openProgress;
         double originalX = this.x;
         double originalY = this.y;
         double originalWidth = this.width;
         double originalHeight = this.height;
         this.x = originalX + originalWidth * (1.0 - animatedScale) * 0.5;
         this.y = originalY + originalHeight * (1.0 - animatedScale) * 0.5;
         this.width = originalWidth * animatedScale;
         this.height = originalHeight * animatedScale;
         double scaledSideW = Math.min(this.theme.scale(174.0), this.width * 0.28);
         this.drawPanel(renderer, scaledSideW, radius);
         this.drawSidebar(renderer, scaledSideW, pad, mouseX, mouseY);
         this.drawContent(renderer, scaledSideW, pad);
         this.drawFooter(renderer, scaledSideW, pad);
         this.x = originalX;
         this.y = originalY;
         this.width = originalWidth;
         this.height = originalHeight;
      }

      private void updateAnimations(double mouseX, double mouseY) {
         this.openProgress = AnimationUtility.ease(this.openProgress, 1.0F, 7.0F);
         this.loginHoverProgress = AnimationUtility.ease(
            this.loginHoverProgress,
            this.inside(mouseX, mouseY, this.loginX, this.loginY, this.loginW, this.loginH) && !MusicPlayerScreen.this.module.isLoggedIn() ? 1.0F : 0.0F,
            8.0F
         );
         this.reloadHoverProgress = AnimationUtility.ease(
            this.reloadHoverProgress, this.inside(mouseX, mouseY, this.reloadX, this.reloadY, this.reloadW, this.reloadH) ? 1.0F : 0.0F, 8.0F
         );
         this.logoutHoverProgress = AnimationUtility.ease(
            this.logoutHoverProgress,
            this.inside(mouseX, mouseY, this.logoutX, this.logoutY, this.logoutW, this.logoutH) && MusicPlayerScreen.this.module.isLoggedIn() ? 1.0F : 0.0F,
            8.0F
         );
         this.searchFocusProgress = AnimationUtility.ease(this.searchFocusProgress, this.searchFocused ? 1.0F : 0.0F, 8.0F);
         this.footerProgress = AnimationUtility.ease(this.footerProgress, (float)MusicPlayerScreen.this.module.getProgress(), 5.0F);
         this.queueScrollProgress = AnimationUtility.ease(this.queueScrollProgress, this.queueScroll, 10.0F);
         this.searchScrollProgress = AnimationUtility.ease(this.searchScrollProgress, this.searchScroll, 10.0F);
         this.likedScrollProgress = AnimationUtility.ease(this.likedScrollProgress, this.likedScroll, 10.0F);
         this.dailyScrollProgress = AnimationUtility.ease(this.dailyScrollProgress, this.dailyScroll, 10.0F);
         this.playlistScrollProgress = AnimationUtility.ease(this.playlistScrollProgress, this.playlistScroll, 10.0F);
         this.playlistSongScrollProgress = AnimationUtility.ease(this.playlistSongScrollProgress, this.playlistSongScroll, 10.0F);
         this.artistScrollProgress = AnimationUtility.ease(this.artistScrollProgress, this.artistScroll, 10.0F);
         this.artistSongScrollProgress = AnimationUtility.ease(this.artistSongScrollProgress, this.artistSongScroll, 10.0F);

         for (int i = 0; i < this.navProgress.length; i++) {
            this.navProgress[i] = AnimationUtility.ease(this.navProgress[i], MusicPlayerScreen.this.page == this.navPage(i) ? 1.0F : 0.0F, 8.0F);
            this.navHoverProgress[i] = AnimationUtility.ease(
               this.navHoverProgress[i], this.inside(mouseX, mouseY, this.navX[i], this.navY[i], this.navW[i], this.navH[i]) ? 1.0F : 0.0F, 8.0F
            );
         }
      }

      private void drawPanel(GuiRenderer renderer, double sideW, double radius) {
         BlurUtil.render((int)Math.round(this.x), (int)Math.round(this.y), (int)Math.ceil(this.width), (int)Math.ceil(this.height), (float)radius);
         Render2DUtil.drawRoundedRect(renderer, this.x, this.y, this.width, this.height, radius, MusicPlayerScreen.BLUR_TINT);
         Render2DUtil.drawRoundedRectCorners(renderer, this.x, this.y, sideW, this.height, radius, 0.0, 0.0, radius, MusicPlayerScreen.SIDE_TINT);
         renderer.quad(
            this.x + sideW, this.y + this.theme.scale(18.0), this.theme.scale(1.0), this.height - this.theme.scale(36.0), MusicPlayerScreen.LINE_TINT
         );
      }

      private void drawSidebar(GuiRenderer renderer, double sideW, double pad, double mouseX, double mouseY) {
         double avatar = this.theme.scale(36.0);
         this.loginX = this.x + pad;
         this.loginY = this.y + pad;
         this.loginW = sideW - pad * 2.0;
         this.loginH = this.theme.scale(54.0);
         if (this.loginHoverProgress > 0.01F) {
            Render2DUtil.drawRoundedRect(
               renderer,
               this.loginX - this.theme.scale(6.0),
               this.loginY - this.theme.scale(6.0),
               this.loginW + this.theme.scale(12.0),
               this.loginH,
               this.theme.scale(9.0),
               this.withAlpha(MusicPlayerScreen.SELECT_TINT, (int)(MusicPlayerScreen.SELECT_TINT.a * this.loginHoverProgress))
            );
         }

         this.drawAccountAvatar(renderer, this.loginX, this.loginY, avatar, avatar, this.theme.scale(9.0), MusicPlayerScreen.SELECT_TINT);
         renderer.text(
            MusicPlayerScreen.this.module.isLoggedIn() ? MusicPlayerScreen.this.module.getAccountName() : "点击登录",
            this.loginX + avatar + this.theme.scale(10.0),
            this.loginY + this.theme.scale(2.0),
            MusicPlayerScreen.TEXT,
            false
         );
         renderer.text(
            MusicPlayerScreen.this.module.isLoggedIn() ? "Account" : MusicPlayerScreen.this.getStatusText(),
            this.loginX + avatar + this.theme.scale(10.0),
            this.loginY + this.theme.scale(20.0),
            MusicPlayerScreen.MUTED,
            false
         );
         this.reloadX = this.x + pad;
         this.reloadY = this.y + this.height - this.theme.scale(38.0);
         this.reloadW = this.theme.scale(54.0);
         this.reloadH = this.theme.scale(20.0);
         this.logoutX = this.reloadX + this.reloadW + this.theme.scale(12.0);
         this.logoutY = this.reloadY;
         this.logoutW = this.theme.scale(58.0);
         this.logoutH = this.reloadH;
         double navY = this.y + this.theme.scale(92.0);
         double navStep = Math.min(this.theme.scale(46.0), Math.max(this.theme.scale(32.0), (this.reloadY - navY - this.theme.scale(8.0)) / 7.0));
         this.drawNav(renderer, 0, "♪", "Player", "Now playing", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Player, navY, sideW, pad);
         this.drawNav(renderer, 1, "-", "Search", "Cloud search", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Search, navY + navStep, sideW, pad);
         this.drawNav(renderer, 2, "≡", "Queue", "Current list", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Queue, navY + navStep * 2.0, sideW, pad);
         this.drawNav(renderer, 3, "♥", "Liked", "Songs", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Liked, navY + navStep * 3.0, sideW, pad);
         this.drawNav(renderer, 4, "▣", "Daily", "Songs", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Daily, navY + navStep * 4.0, sideW, pad);
         this.drawNav(renderer, 5, "≡", "My", "Playlists", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Playlists, navY + navStep * 5.0, sideW, pad);
         this.drawNav(renderer, 6, "•", "Sub", "Artists", MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Artists, navY + navStep * 6.0, sideW, pad);
         renderer.text("Reload", this.reloadX, this.reloadY, this.mix(MusicPlayerScreen.MUTED, MusicPlayerScreen.TEXT, this.reloadHoverProgress), false);
         renderer.text(
            "Logout",
            this.logoutX,
            this.logoutY,
            MusicPlayerScreen.this.module.isLoggedIn()
               ? this.mix(MusicPlayerScreen.DANGER, MusicPlayerScreen.TEXT, this.logoutHoverProgress * 0.35F)
               : new Color(180, 180, 180, 90),
            false
         );
      }

      private void drawNav(
         GuiRenderer renderer, int index, String icon, String title, String subtitle, boolean selected, double itemY, double sideW, double pad
      ) {
         double itemX = this.x + pad;
         double itemW = sideW - pad * 2.0;
         double itemH = this.theme.scale(39.0);
         this.navX[index] = itemX - this.theme.scale(3.0);
         this.navY[index] = itemY - this.theme.scale(4.0);
         this.navW[index] = itemW + this.theme.scale(6.0);
         this.navH[index] = itemH + this.theme.scale(6.0);
         float selectProgress = Math.max(this.navProgress[index], this.navHoverProgress[index] * 0.45F);
         if (selectProgress > 0.01F) {
            double extra = this.theme.scale(3.0) * selectProgress;
            Render2DUtil.drawRoundedRect(
               renderer,
               itemX - extra,
               itemY - this.theme.scale(4.0),
               itemW + extra * 2.0,
               itemH + this.theme.scale(6.0),
               this.theme.scale(10.0),
               this.withAlpha(MusicPlayerScreen.SELECT_TINT, (int)(MusicPlayerScreen.SELECT_TINT.a * selectProgress))
            );
         }

         Color titleColor = this.mix(MusicPlayerScreen.MUTED, MusicPlayerScreen.TEXT, Math.max(this.navProgress[index], 0.55F));
         Color iconColor = this.mix(MusicPlayerScreen.MUTED, MusicPlayerScreen.TEXT, this.navProgress[index]);
         renderer.text(icon, itemX + this.theme.scale(4.0), itemY + this.theme.scale(8.0), iconColor, false);
         renderer.text(title, itemX + this.theme.scale(24.0), itemY, titleColor, false);
         renderer.text(
            subtitle,
            itemX + this.theme.scale(24.0),
            itemY + this.theme.scale(17.0),
            this.mix(MusicPlayerScreen.MUTED, MusicPlayerScreen.TEXT, this.navProgress[index] * 0.25F),
            false
         );
      }

      private void drawContent(GuiRenderer renderer, double sideW, double pad) {
         double contentX = this.x + sideW + pad;
         double contentY = this.y + pad;
         double contentW = this.width - sideW - pad * 2.0;
         double contentH = this.height - this.theme.scale(92.0);
         if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Login || !MusicPlayerScreen.this.module.isLoggedIn()) {
            renderer.text("Login", contentX, contentY, MusicPlayerScreen.TEXT, true);
            if (MusicPlayerScreen.this.module.isLoggedIn()) {
               this.drawLoggedInContent(renderer, contentX, contentY + this.theme.scale(48.0), contentW, contentH - this.theme.scale(60.0));
            } else {
               this.drawLoginContent(renderer, contentX, contentY + this.theme.scale(48.0), contentW, contentH - this.theme.scale(60.0));
            }
         } else if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Search) {
            this.drawSearchContent(renderer, contentX, contentY, contentW, contentH);
         } else if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Player) {
            this.drawPlayerContent(renderer, contentX, contentY, contentW, contentH);
         } else if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Liked) {
            this.drawSongListContent(
               renderer,
               contentX,
               contentY,
               contentW,
               contentH,
               "Liked",
               MusicPlayerScreen.this.module.getLikedStatus(),
               MusicPlayerScreen.this.module.getLikedSongs(),
               MusicPlayerScreen.this.module.isLoadingLiked(),
               MusicPlayerScreen.ListKind.Liked
            );
         } else if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Daily) {
            this.drawSongListContent(
               renderer,
               contentX,
               contentY,
               contentW,
               contentH,
               "Daily",
               MusicPlayerScreen.this.module.getDailyStatus(),
               MusicPlayerScreen.this.module.getDailySongs(),
               MusicPlayerScreen.this.module.isLoadingDaily(),
               MusicPlayerScreen.ListKind.Daily
            );
         } else if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Playlists) {
            this.drawPlaylistsContent(renderer, contentX, contentY, contentW, contentH);
         } else if (MusicPlayerScreen.this.page == MusicPlayerScreen.Page.Artists) {
            this.drawArtistsContent(renderer, contentX, contentY, contentW, contentH);
         } else {
            this.drawQueueContent(renderer, contentX, contentY, contentW, contentH);
         }
      }

      private void drawQueueContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         List<CloudMusicApi.Song> songs = MusicPlayerScreen.this.module.getQueue();
         renderer.text("Queue (" + songs.size() + ")", contentX, contentY + this.theme.scale(48.0), MusicPlayerScreen.TEXT, true);
         renderer.text(
            MusicPlayerScreen.this.module.getStatus(), contentX + this.theme.scale(110.0), contentY + this.theme.scale(50.0), MusicPlayerScreen.MUTED, false
         );
         this.listX = contentX;
         this.listY = contentY + this.theme.scale(82.0);
         this.listW = contentW;
         this.rowH = this.theme.scale(48.0);
         this.rowStep = this.theme.scale(55.0);
         this.visibleQueueSongs = 0;
         if (songs.isEmpty()) {
            renderer.text("队列为空。去 Search 搜一首歌，点击结果后才会加入队列。", contentX, this.listY + this.theme.scale(18.0), MusicPlayerScreen.MUTED, false);
         } else {
            int maxRows = Math.min(songs.size(), Math.max(1, (int)((contentH - this.theme.scale(98.0)) / this.rowStep)));
            this.queueScroll = this.clampScroll(this.queueScroll, songs.size(), maxRows);
            this.queueScrollProgress = this.clampAnimatedScroll(this.queueScrollProgress, songs.size(), maxRows);
            this.visibleQueueSongs = maxRows;
            this.drawSmoothSongList(
               renderer,
               songs,
               this.queueScrollProgress,
               maxRows,
               this.listX,
               this.listY,
               this.listW,
               this.rowH,
               this.rowStep,
               index -> index == MusicPlayerScreen.this.module.getCurrentIndex()
            );
         }
      }

      private void drawSmoothSongList(
         GuiRenderer renderer,
         List<CloudMusicApi.Song> songs,
         float animatedScroll,
         int maxRows,
         double x,
         double y,
         double width,
         double rowHeight,
         double step,
         MusicPlayerScreen.MusicPanel.SongSelection selection
      ) {
         if (!songs.isEmpty() && maxRows > 0) {
            int start = Math.max(0, (int)Math.floor(animatedScroll) - 1);
            int end = Math.min(songs.size(), start + maxRows + 3);
            double offset = (animatedScroll - start) * step;
            renderer.scissorStart(x, y, width, Math.max(rowHeight, maxRows * step - (step - rowHeight)));

            for (int index = start; index < end; index++) {
               double rowY = y + (index - start) * step - offset;
               if (!(rowY + rowHeight < y) && !(rowY > y + maxRows * step)) {
                  CloudMusicApi.Song song = songs.get(index);
                  this.drawSongRow(renderer, x, rowY, width, song, selection != null && selection.selected(index));
               }
            }

            renderer.scissorEnd();
         }
      }

      private void drawSmoothPlaylistList(
         GuiRenderer renderer,
         List<CloudMusicApi.Playlist> playlists,
         float animatedScroll,
         int maxRows,
         double x,
         double y,
         double width,
         double rowHeight,
         double step
      ) {
         if (!playlists.isEmpty() && maxRows > 0) {
            int start = Math.max(0, (int)Math.floor(animatedScroll) - 1);
            int end = Math.min(playlists.size(), start + maxRows + 3);
            double offset = (animatedScroll - start) * step;
            renderer.scissorStart(x, y, width, Math.max(rowHeight, maxRows * step - (step - rowHeight)));

            for (int index = start; index < end; index++) {
               double rowY = y + (index - start) * step - offset;
               if (!(rowY + rowHeight < y) && !(rowY > y + maxRows * step)) {
                  CloudMusicApi.Playlist playlist = playlists.get(index);
                  this.drawPlaylistRow(renderer, x, rowY, width, playlist, playlist.id() == MusicPlayerScreen.this.module.getSelectedPlaylistId());
               }
            }

            renderer.scissorEnd();
         }
      }

      private void drawSmoothArtistList(
         GuiRenderer renderer,
         List<CloudMusicApi.Artist> artists,
         float animatedScroll,
         int maxRows,
         double x,
         double y,
         double width,
         double rowHeight,
         double step
      ) {
         if (!artists.isEmpty() && maxRows > 0) {
            int start = Math.max(0, (int)Math.floor(animatedScroll) - 1);
            int end = Math.min(artists.size(), start + maxRows + 3);
            double offset = (animatedScroll - start) * step;
            renderer.scissorStart(x, y, width, Math.max(rowHeight, maxRows * step - (step - rowHeight)));

            for (int index = start; index < end; index++) {
               double rowY = y + (index - start) * step - offset;
               if (!(rowY + rowHeight < y) && !(rowY > y + maxRows * step)) {
                  CloudMusicApi.Artist artist = artists.get(index);
                  this.drawArtistRow(renderer, x, rowY, width, artist, artist.id() == MusicPlayerScreen.this.module.getSelectedArtistId());
               }
            }

            renderer.scissorEnd();
         }
      }

      private void drawSearchContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         renderer.text("Search", contentX, contentY, MusicPlayerScreen.TEXT, true);
         renderer.text(MusicPlayerScreen.this.module.getSearchStatus(), contentX, contentY + this.theme.scale(28.0), MusicPlayerScreen.MUTED, false);
         this.searchBoxX = contentX;
         this.searchBoxY = contentY + this.theme.scale(58.0);
         this.searchBoxW = contentW - this.theme.scale(96.0);
         this.searchBoxH = this.theme.scale(30.0);
         this.searchButtonX = this.searchBoxX + this.searchBoxW + this.theme.scale(12.0);
         this.searchButtonY = this.searchBoxY;
         this.searchButtonW = this.theme.scale(84.0);
         this.searchButtonH = this.searchBoxH;
         Render2DUtil.drawRoundedRect(
            renderer,
            this.searchBoxX,
            this.searchBoxY,
            this.searchBoxW,
            this.searchBoxH,
            this.theme.scale(8.0),
            this.mix(MusicPlayerScreen.CARD_TINT, MusicPlayerScreen.SELECT_TINT, this.searchFocusProgress)
         );
         renderer.text(
            this.searchQuery.isEmpty() ? "输入歌名 / 歌手" : this.searchQuery,
            this.searchBoxX + this.theme.scale(10.0),
            this.searchBoxY + this.theme.scale(8.0),
            this.searchQuery.isEmpty() ? MusicPlayerScreen.MUTED : MusicPlayerScreen.TEXT,
            false
         );
         if (this.searchFocusProgress > 0.01F) {
            renderer.quad(
               this.searchBoxX + this.theme.scale(10.0) + Math.min(this.searchBoxW - this.theme.scale(22.0), this.searchQuery.length() * this.theme.scale(6.0)),
               this.searchBoxY + this.theme.scale(7.0),
               this.theme.scale(1.0),
               this.theme.scale(16.0),
               this.withAlpha(MusicPlayerScreen.TEXT, (int)(MusicPlayerScreen.TEXT.a * this.searchFocusProgress))
            );
         }

         Render2DUtil.drawRoundedRect(
            renderer, this.searchButtonX, this.searchButtonY, this.searchButtonW, this.searchButtonH, this.theme.scale(8.0), MusicPlayerScreen.SELECT_TINT
         );
         renderer.text(
            MusicPlayerScreen.this.module.isLoadingSearch() ? "..." : "Search",
            this.searchButtonX + this.theme.scale(17.0),
            this.searchButtonY + this.theme.scale(8.0),
            MusicPlayerScreen.TEXT,
            false
         );
         List<CloudMusicApi.Song> songs = MusicPlayerScreen.this.module.getSearchResults();
         this.listX = contentX;
         this.listY = contentY + this.theme.scale(110.0);
         this.listW = contentW;
         this.rowH = this.theme.scale(48.0);
         this.rowStep = this.theme.scale(55.0);
         this.visibleSearchSongs = 0;
         if (songs.isEmpty()) {
            renderer.text("搜索结果会显示在这里，点击结果会播放并加入 Queue。", contentX, this.listY + this.theme.scale(18.0), MusicPlayerScreen.MUTED, false);
         } else {
            int maxRows = Math.min(songs.size(), Math.max(1, (int)((contentH - this.theme.scale(126.0)) / this.rowStep)));
            this.searchScroll = this.clampScroll(this.searchScroll, songs.size(), maxRows);
            this.searchScrollProgress = this.clampAnimatedScroll(this.searchScrollProgress, songs.size(), maxRows);
            this.visibleSearchSongs = maxRows;
            this.drawSmoothSongList(renderer, songs, this.searchScrollProgress, maxRows, this.listX, this.listY, this.listW, this.rowH, this.rowStep, null);
         }
      }

      private void drawSongListContent(
         GuiRenderer renderer,
         double contentX,
         double contentY,
         double contentW,
         double contentH,
         String title,
         String status,
         List<CloudMusicApi.Song> songs,
         boolean loading,
         MusicPlayerScreen.ListKind kind
      ) {
         renderer.text(title, contentX, contentY, MusicPlayerScreen.TEXT, true);
         renderer.text(status, contentX, contentY + this.theme.scale(28.0), MusicPlayerScreen.MUTED, false);
         this.listX = contentX;
         this.listY = contentY + this.theme.scale(58.0);
         this.listW = contentW;
         this.rowH = this.theme.scale(48.0);
         this.rowStep = this.theme.scale(55.0);
         if (kind == MusicPlayerScreen.ListKind.Liked) {
            this.visibleLikedSongs = 0;
         } else if (kind == MusicPlayerScreen.ListKind.Daily) {
            this.visibleDailySongs = 0;
         }

         if (songs.isEmpty()) {
            renderer.text(loading ? "正在加载..." : "没有歌曲，点击左侧页面会重新尝试加载。", contentX, this.listY + this.theme.scale(18.0), MusicPlayerScreen.MUTED, false);
         } else {
            int maxRows = Math.min(songs.size(), Math.max(1, (int)((contentH - this.theme.scale(78.0)) / this.rowStep)));
            if (kind == MusicPlayerScreen.ListKind.Liked) {
               this.likedScroll = this.clampScroll(this.likedScroll, songs.size(), maxRows);
            } else if (kind == MusicPlayerScreen.ListKind.Daily) {
               this.dailyScroll = this.clampScroll(this.dailyScroll, songs.size(), maxRows);
            }

            if (kind != MusicPlayerScreen.ListKind.Liked) {
               ;
            }

            float animatedScroll = kind == MusicPlayerScreen.ListKind.Liked ? this.likedScrollProgress : this.dailyScrollProgress;
            animatedScroll = this.clampAnimatedScroll(animatedScroll, songs.size(), maxRows);
            if (kind == MusicPlayerScreen.ListKind.Liked) {
               this.likedScrollProgress = animatedScroll;
               this.visibleLikedSongs = maxRows;
            } else if (kind == MusicPlayerScreen.ListKind.Daily) {
               this.dailyScrollProgress = animatedScroll;
               this.visibleDailySongs = maxRows;
            }

            this.drawSmoothSongList(renderer, songs, animatedScroll, maxRows, this.listX, this.listY, this.listW, this.rowH, this.rowStep, null);
         }
      }

      private void drawPlaylistsContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         renderer.text("My Playlists", contentX, contentY, MusicPlayerScreen.TEXT, true);
         renderer.text(MusicPlayerScreen.this.module.getPlaylistsStatus(), contentX, contentY + this.theme.scale(28.0), MusicPlayerScreen.MUTED, false);
         double gap = this.theme.scale(16.0);
         this.sideListX = contentX;
         this.sideListY = contentY + this.theme.scale(58.0);
         this.sideListW = Math.min(this.theme.scale(250.0), contentW * 0.42);
         this.sideListRowH = this.theme.scale(44.0);
         this.sideListRowStep = this.theme.scale(50.0);
         this.detailListX = this.sideListX + this.sideListW + gap;
         this.detailListY = this.sideListY;
         this.detailListW = contentW - this.sideListW - gap;
         this.detailListRowH = this.theme.scale(48.0);
         this.detailListRowStep = this.theme.scale(55.0);
         this.visibleSideItems = 0;
         this.visibleDetailSongs = 0;
         List<CloudMusicApi.Playlist> playlists = MusicPlayerScreen.this.module.getPlaylists();
         if (playlists.isEmpty()) {
            renderer.text(
               MusicPlayerScreen.this.module.isLoadingPlaylists() ? "正在加载歌单..." : "没有歌单。",
               this.sideListX,
               this.sideListY + this.theme.scale(18.0),
               MusicPlayerScreen.MUTED,
               false
            );
         } else {
            int maxRows = Math.min(playlists.size(), Math.max(1, (int)((contentH - this.theme.scale(78.0)) / this.sideListRowStep)));
            this.playlistScroll = this.clampScroll(this.playlistScroll, playlists.size(), maxRows);
            this.playlistScrollProgress = this.clampAnimatedScroll(this.playlistScrollProgress, playlists.size(), maxRows);
            this.visibleSideItems = maxRows;
            this.drawSmoothPlaylistList(
               renderer,
               playlists,
               this.playlistScrollProgress,
               maxRows,
               this.sideListX,
               this.sideListY,
               this.sideListW,
               this.sideListRowH,
               this.sideListRowStep
            );
         }

         renderer.text(
            MusicPlayerScreen.this.module.getSelectedPlaylistName().isEmpty() ? "选择一个歌单" : MusicPlayerScreen.this.module.getSelectedPlaylistName(),
            this.detailListX,
            contentY + this.theme.scale(28.0),
            MusicPlayerScreen.TEXT,
            false
         );
         renderer.text(
            MusicPlayerScreen.this.module.getPlaylistSongsStatus(),
            this.detailListX + this.theme.scale(120.0),
            contentY + this.theme.scale(28.0),
            MusicPlayerScreen.MUTED,
            false
         );
         List<CloudMusicApi.Song> songs = MusicPlayerScreen.this.module.getPlaylistSongs();
         if (songs.isEmpty()) {
            renderer.text(
               MusicPlayerScreen.this.module.isLoadingPlaylistSongs() ? "正在加载歌曲..." : "点击左侧歌单查看歌曲。",
               this.detailListX,
               this.detailListY + this.theme.scale(18.0),
               MusicPlayerScreen.MUTED,
               false
            );
         } else {
            int maxSongs = Math.min(songs.size(), Math.max(1, (int)((contentH - this.theme.scale(78.0)) / this.detailListRowStep)));
            this.playlistSongScroll = this.clampScroll(this.playlistSongScroll, songs.size(), maxSongs);
            this.playlistSongScrollProgress = this.clampAnimatedScroll(this.playlistSongScrollProgress, songs.size(), maxSongs);
            this.visibleDetailSongs = maxSongs;
            this.drawSmoothSongList(
               renderer,
               songs,
               this.playlistSongScrollProgress,
               maxSongs,
               this.detailListX,
               this.detailListY,
               this.detailListW,
               this.detailListRowH,
               this.detailListRowStep,
               null
            );
         }
      }

      private void drawArtistsContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         renderer.text("Artists", contentX, contentY, MusicPlayerScreen.TEXT, true);
         renderer.text(MusicPlayerScreen.this.module.getArtistsStatus(), contentX, contentY + this.theme.scale(28.0), MusicPlayerScreen.MUTED, false);
         double gap = this.theme.scale(16.0);
         this.sideListX = contentX;
         this.sideListY = contentY + this.theme.scale(58.0);
         this.sideListW = Math.min(this.theme.scale(250.0), contentW * 0.42);
         this.sideListRowH = this.theme.scale(44.0);
         this.sideListRowStep = this.theme.scale(50.0);
         this.detailListX = this.sideListX + this.sideListW + gap;
         this.detailListY = this.sideListY;
         this.detailListW = contentW - this.sideListW - gap;
         this.detailListRowH = this.theme.scale(48.0);
         this.detailListRowStep = this.theme.scale(55.0);
         this.visibleSideItems = 0;
         this.visibleDetailSongs = 0;
         List<CloudMusicApi.Artist> artists = MusicPlayerScreen.this.module.getArtists();
         if (artists.isEmpty()) {
            renderer.text(
               MusicPlayerScreen.this.module.isLoadingArtists() ? "正在加载关注歌手..." : "没有关注歌手。",
               this.sideListX,
               this.sideListY + this.theme.scale(18.0),
               MusicPlayerScreen.MUTED,
               false
            );
         } else {
            int maxRows = Math.min(artists.size(), Math.max(1, (int)((contentH - this.theme.scale(78.0)) / this.sideListRowStep)));
            this.artistScroll = this.clampScroll(this.artistScroll, artists.size(), maxRows);
            this.artistScrollProgress = this.clampAnimatedScroll(this.artistScrollProgress, artists.size(), maxRows);
            this.visibleSideItems = maxRows;
            this.drawSmoothArtistList(
               renderer, artists, this.artistScrollProgress, maxRows, this.sideListX, this.sideListY, this.sideListW, this.sideListRowH, this.sideListRowStep
            );
         }

         renderer.text(
            MusicPlayerScreen.this.module.getSelectedArtistName().isEmpty() ? "选择一个歌手" : MusicPlayerScreen.this.module.getSelectedArtistName(),
            this.detailListX,
            contentY + this.theme.scale(28.0),
            MusicPlayerScreen.TEXT,
            false
         );
         renderer.text(
            MusicPlayerScreen.this.module.getArtistSongsStatus(),
            this.detailListX + this.theme.scale(120.0),
            contentY + this.theme.scale(28.0),
            MusicPlayerScreen.MUTED,
            false
         );
         List<CloudMusicApi.Song> songs = MusicPlayerScreen.this.module.getArtistSongs();
         if (songs.isEmpty()) {
            renderer.text(
               MusicPlayerScreen.this.module.isLoadingArtistSongs() ? "正在加载热门歌曲..." : "点击左侧歌手查看热门歌曲。",
               this.detailListX,
               this.detailListY + this.theme.scale(18.0),
               MusicPlayerScreen.MUTED,
               false
            );
         } else {
            int maxSongs = Math.min(songs.size(), Math.max(1, (int)((contentH - this.theme.scale(78.0)) / this.detailListRowStep)));
            this.artistSongScroll = this.clampScroll(this.artistSongScroll, songs.size(), maxSongs);
            this.artistSongScrollProgress = this.clampAnimatedScroll(this.artistSongScrollProgress, songs.size(), maxSongs);
            this.visibleDetailSongs = maxSongs;
            this.drawSmoothSongList(
               renderer,
               songs,
               this.artistSongScrollProgress,
               maxSongs,
               this.detailListX,
               this.detailListY,
               this.detailListW,
               this.detailListRowH,
               this.detailListRowStep,
               null
            );
         }
      }

      private void drawPlayerContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         renderer.text("Now Playing", contentX, contentY, MusicPlayerScreen.TEXT, true);
         CloudMusicApi.Song song = MusicPlayerScreen.this.module.getCurrentSong();
         double cardY = contentY + this.theme.scale(58.0);
         Render2DUtil.drawRoundedRect(
            renderer,
            contentX,
            cardY,
            contentW,
            Math.min(this.theme.scale(170.0), contentH - this.theme.scale(80.0)),
            this.theme.scale(16.0),
            MusicPlayerScreen.CARD_TINT
         );
         if (song == null) {
            renderer.text("还没有播放歌曲", contentX + this.theme.scale(22.0), cardY + this.theme.scale(26.0), MusicPlayerScreen.TEXT, true);
            renderer.text("去 Search 搜索真实歌曲，点击结果后这里会显示播放状态。", contentX + this.theme.scale(22.0), cardY + this.theme.scale(58.0), MusicPlayerScreen.MUTED, false);
         } else {
            this.drawCover(
               renderer,
               song,
               contentX + this.theme.scale(22.0),
               cardY + this.theme.scale(24.0),
               this.theme.scale(86.0),
               this.theme.scale(86.0),
               this.theme.scale(14.0),
               MusicPlayerScreen.SELECT_TINT
            );
            renderer.text(song.name(), contentX + this.theme.scale(126.0), cardY + this.theme.scale(30.0), MusicPlayerScreen.ACCENT, true);
            renderer.text(song.artist(), contentX + this.theme.scale(126.0), cardY + this.theme.scale(58.0), MusicPlayerScreen.MUTED, false);
            renderer.text(
               MusicPlayerScreen.this.module.getStatus(), contentX + this.theme.scale(126.0), cardY + this.theme.scale(82.0), MusicPlayerScreen.TEXT, false
            );
         }
      }

      private void drawLoginContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         renderer.text(MusicPlayerScreen.this.getStatusText(), contentX, contentY, MusicPlayerScreen.TEXT, true);
         renderer.text("未登录时点击左上角账号区域生成网易云二维码。", contentX, contentY + this.theme.scale(28.0), MusicPlayerScreen.MUTED, false);
         double boxSize = Math.min(this.theme.scale(248.0), Math.max(this.theme.scale(150.0), Math.min(contentW * 0.58, contentH - this.theme.scale(52.0))));
         double boxX = contentX + contentW / 2.0 - boxSize / 2.0;
         double boxY = contentY + this.theme.scale(70.0);
         Render2DUtil.drawRoundedRect(renderer, boxX, boxY, boxSize, boxSize, this.theme.scale(16.0), MusicPlayerScreen.CARD_TINT);
         if (MusicPlayerScreen.this.qrTexture != null) {
            double qrSize = Math.max(this.theme.scale(120.0), boxSize - this.theme.scale(28.0));
            double qrX = boxX + (boxSize - qrSize) / 2.0;
            double qrY = boxY + (boxSize - qrSize) / 2.0;
            renderer.graphics()
               .blit(
                  RenderPipelines.GUI_TEXTURED,
                  MusicPlayerScreen.this.qrTexture,
                  (int)qrX,
                  (int)qrY,
                  0.0F,
                  0.0F,
                  (int)qrSize,
                  (int)qrSize,
                  (int)qrSize,
                  (int)qrSize
               );
         } else {
            renderer.text(
               MusicPlayerScreen.this.loginState == MusicPlayerScreen.LoginState.Loading ? "正在加载二维码..." : "点击登录后显示二维码",
               boxX + this.theme.scale(42.0),
               boxY + boxSize / 2.0 - this.theme.scale(5.0),
               MusicPlayerScreen.MUTED,
               false
            );
         }

         renderer.text(MusicPlayerScreen.this.message, boxX, boxY + boxSize + this.theme.scale(16.0), MusicPlayerScreen.MUTED, false);
      }

      private void drawLoggedInContent(GuiRenderer renderer, double contentX, double contentY, double contentW, double contentH) {
         renderer.text("已登录网易云音乐", contentX, contentY, MusicPlayerScreen.TEXT, true);
         double boxW = Math.min(contentW, this.theme.scale(420.0));
         double boxH = this.theme.scale(118.0);
         double boxY = contentY + this.theme.scale(48.0);
         Render2DUtil.drawRoundedRect(renderer, contentX, boxY, boxW, boxH, this.theme.scale(16.0), MusicPlayerScreen.CARD_TINT);
         this.drawAccountAvatar(
            renderer,
            contentX + this.theme.scale(18.0),
            boxY + this.theme.scale(20.0),
            this.theme.scale(54.0),
            this.theme.scale(54.0),
            this.theme.scale(12.0),
            MusicPlayerScreen.SELECT_TINT
         );
         renderer.text(
            MusicPlayerScreen.this.module.getAccountName(), contentX + this.theme.scale(90.0), boxY + this.theme.scale(22.0), MusicPlayerScreen.TEXT, true
         );
         renderer.text(
            "ID: " + MusicPlayerScreen.this.module.getAccountId(),
            contentX + this.theme.scale(90.0),
            boxY + this.theme.scale(50.0),
            MusicPlayerScreen.MUTED,
            false
         );
      }

      private void drawSongRow(GuiRenderer renderer, double rowX, double rowY, double rowW, CloudMusicApi.Song song, boolean selected) {
         double rowH = this.theme.scale(48.0);
         double album = this.theme.scale(36.0);
         if (selected) {
            Render2DUtil.drawRoundedRect(renderer, rowX, rowY, rowW, rowH, this.theme.scale(10.0), MusicPlayerScreen.SELECT_TINT);
         }

         this.drawCover(
            renderer,
            song,
            rowX + this.theme.scale(10.0),
            rowY + this.theme.scale(6.0),
            album,
            album,
            this.theme.scale(8.0),
            selected ? MusicPlayerScreen.ACCENT : MusicPlayerScreen.CARD_TINT
         );
         renderer.text(
            song.name(), rowX + this.theme.scale(58.0), rowY + this.theme.scale(8.0), selected ? MusicPlayerScreen.ACCENT : MusicPlayerScreen.TEXT, false
         );
         renderer.text(song.artist(), rowX + this.theme.scale(58.0), rowY + this.theme.scale(25.0), MusicPlayerScreen.MUTED, false);
      }

      private void drawCover(
         GuiRenderer renderer, CloudMusicApi.Song song, double coverX, double coverY, double coverW, double coverH, double radius, Color fallback
      ) {
         Identifier texture = MusicPlayerScreen.this.coverTexture(song);
         this.drawImage(renderer, texture, coverX, coverY, coverW, coverH, radius, fallback);
      }

      private void drawAccountAvatar(GuiRenderer renderer, double imageX, double imageY, double imageW, double imageH, double radius, Color fallback) {
         Identifier texture = MusicPlayerScreen.this.module.isLoggedIn() ? MusicPlayerScreen.this.accountAvatarTexture() : null;
         this.drawImage(renderer, texture, imageX, imageY, imageW, imageH, radius, fallback);
      }

      private void drawImage(
         GuiRenderer renderer, Identifier texture, double imageX, double imageY, double imageW, double imageH, double radius, Color fallback
      ) {
         if (texture == null) {
            Render2DUtil.drawRoundedRect(renderer, imageX, imageY, imageW, imageH, radius, fallback);
         } else {
            renderer.texture(texture, imageX, imageY, imageW, imageH, MusicPlayerScreen.TEXT);
         }
      }

      private void drawPlaylistRow(GuiRenderer renderer, double rowX, double rowY, double rowW, CloudMusicApi.Playlist playlist, boolean selected) {
         double rowH = this.theme.scale(44.0);
         Render2DUtil.drawRoundedRect(
            renderer, rowX, rowY, rowW, rowH, this.theme.scale(10.0), selected ? MusicPlayerScreen.SELECT_TINT : MusicPlayerScreen.CARD_TINT
         );
         renderer.text(
            playlist.name(), rowX + this.theme.scale(12.0), rowY + this.theme.scale(7.0), selected ? MusicPlayerScreen.ACCENT : MusicPlayerScreen.TEXT, false
         );
         renderer.text(playlist.count() + " songs", rowX + this.theme.scale(12.0), rowY + this.theme.scale(24.0), MusicPlayerScreen.MUTED, false);
      }

      private void drawArtistRow(GuiRenderer renderer, double rowX, double rowY, double rowW, CloudMusicApi.Artist artist, boolean selected) {
         double rowH = this.theme.scale(44.0);
         Render2DUtil.drawRoundedRect(
            renderer, rowX, rowY, rowW, rowH, this.theme.scale(10.0), selected ? MusicPlayerScreen.SELECT_TINT : MusicPlayerScreen.CARD_TINT
         );
         renderer.text(
            artist.name(), rowX + this.theme.scale(12.0), rowY + this.theme.scale(7.0), selected ? MusicPlayerScreen.ACCENT : MusicPlayerScreen.TEXT, false
         );
         renderer.text(artist.songCount() + " songs", rowX + this.theme.scale(12.0), rowY + this.theme.scale(24.0), MusicPlayerScreen.MUTED, false);
      }

      private void drawFooter(GuiRenderer renderer, double sideW, double pad) {
         double footerY = this.y + this.height - this.theme.scale(60.0);
         double contentX = this.x + sideW + pad;
         double contentW = this.width - sideW - pad * 2.0;
         Render2DUtil.drawRoundedRect(renderer, contentX, footerY, contentW, this.theme.scale(3.0), this.theme.scale(2.0), MusicPlayerScreen.LINE_TINT);
         if (MusicPlayerScreen.this.module.isLoggedIn() && this.footerProgress > 0.0F) {
            Render2DUtil.drawRoundedRect(
               renderer, contentX, footerY, contentW * this.footerProgress, this.theme.scale(3.0), this.theme.scale(2.0), new Color(236, 244, 220, 155)
            );
         }

         double album = this.theme.scale(36.0);
         double infoY = footerY + this.theme.scale(12.0);
         CloudMusicApi.Song song = MusicPlayerScreen.this.module.getCurrentSong();
         this.drawCover(renderer, song, contentX, infoY, album, album, this.theme.scale(8.0), MusicPlayerScreen.CARD_TINT);
         renderer.text(
            song != null ? song.name() : "Cloud Music", contentX + album + this.theme.scale(10.0), infoY + this.theme.scale(2.0), MusicPlayerScreen.TEXT, false
         );
         renderer.text(
            song != null ? song.artist() : MusicPlayerScreen.this.getStatusText(),
            contentX + album + this.theme.scale(10.0),
            infoY + this.theme.scale(20.0),
            MusicPlayerScreen.MUTED,
            false
         );
         double controlsX = contentX + contentW / 2.0 - this.theme.scale(62.0);
         this.prevX = controlsX - this.theme.scale(8.0);
         this.prevY = infoY + this.theme.scale(5.0);
         this.prevW = this.theme.scale(28.0);
         this.prevH = this.theme.scale(24.0);
         this.playX = controlsX + this.theme.scale(30.0);
         this.playY = this.prevY;
         this.playW = this.theme.scale(30.0);
         this.playH = this.prevH;
         this.nextX = controlsX + this.theme.scale(68.0);
         this.nextY = this.prevY;
         this.nextW = this.theme.scale(28.0);
         this.nextH = this.prevH;
         renderer.text("<<", controlsX, infoY + this.theme.scale(11.0), MusicPlayerScreen.MUTED, false);
         renderer.text(
            this.playText(),
            controlsX + this.theme.scale(38.0),
            infoY + this.theme.scale(11.0),
            MusicPlayerScreen.this.module.isLoggedIn() ? MusicPlayerScreen.TEXT : MusicPlayerScreen.MUTED,
            false
         );
         renderer.text(">>", controlsX + this.theme.scale(76.0), infoY + this.theme.scale(11.0), MusicPlayerScreen.MUTED, false);
         renderer.text("♥", controlsX + this.theme.scale(116.0), infoY + this.theme.scale(11.0), MusicPlayerScreen.DANGER, false);
         renderer.text(
            song != null ? this.formatDuration(MusicPlayerScreen.this.module.getPositionMs()) + " / " + this.formatDuration(song.durationMs()) : "--:--",
            contentX + contentW - this.theme.scale(76.0),
            infoY + this.theme.scale(11.0),
            MusicPlayerScreen.MUTED,
            false
         );
      }

      private MusicPlayerScreen.Page navPage(int index) {
         return switch (index) {
            case 0 -> MusicPlayerScreen.Page.Player;
            case 1 -> MusicPlayerScreen.Page.Search;
            case 2 -> MusicPlayerScreen.Page.Queue;
            case 3 -> MusicPlayerScreen.Page.Liked;
            case 4 -> MusicPlayerScreen.Page.Daily;
            case 5 -> MusicPlayerScreen.Page.Playlists;
            case 6 -> MusicPlayerScreen.Page.Artists;
            default -> MusicPlayerScreen.Page.Queue;
         };
      }

      private void loadPage(MusicPlayerScreen.Page page) {
         if (MusicPlayerScreen.this.module.isLoggedIn()) {
            MusicPlayerScreen.this.module.ensureAccountLoaded();
            switch (page) {
               case Liked:
                  MusicPlayerScreen.this.module.loadLikedSongs();
                  break;
               case Daily:
                  MusicPlayerScreen.this.module.loadDailySongs();
                  break;
               case Playlists:
                  MusicPlayerScreen.this.module.loadPlaylists();
                  break;
               case Artists:
                  MusicPlayerScreen.this.module.loadArtists();
            }
         }
      }

      private void refreshPage(MusicPlayerScreen.Page page) {
         if (MusicPlayerScreen.this.module.isLoggedIn()) {
            MusicPlayerScreen.this.module.ensureAccountLoaded();
            switch (page) {
               case Login:
                  MusicPlayerScreen.this.module.refreshAccount();
               case Player:
               case Queue:
               default:
                  break;
               case Search:
                  if (!this.searchQuery.isBlank()) {
                     this.searchScroll = 0;
                     this.searchScrollProgress = 0.0F;
                     MusicPlayerScreen.this.module.search(this.searchQuery);
                  }
                  break;
               case Liked:
                  this.likedScroll = 0;
                  this.likedScrollProgress = 0.0F;
                  MusicPlayerScreen.this.module.refreshLikedSongs();
                  break;
               case Daily:
                  this.dailyScroll = 0;
                  this.dailyScrollProgress = 0.0F;
                  MusicPlayerScreen.this.module.refreshDailySongs();
                  break;
               case Playlists:
                  this.playlistScroll = 0;
                  this.playlistSongScroll = 0;
                  this.playlistScrollProgress = 0.0F;
                  this.playlistSongScrollProgress = 0.0F;
                  MusicPlayerScreen.this.module.refreshPlaylists();
                  break;
               case Artists:
                  this.artistScroll = 0;
                  this.artistSongScroll = 0;
                  this.artistScrollProgress = 0.0F;
                  this.artistSongScrollProgress = 0.0F;
                  MusicPlayerScreen.this.module.refreshArtists();
            }
         }
      }

      private String titleForPage(MusicPlayerScreen.Page page) {
         return switch (page) {
            case Login -> "Login";
            case Player -> "Now Playing";
            case Search -> "Search";
            case Queue -> "Queue";
            case Liked -> "Liked";
            case Daily -> "Daily";
            case Playlists -> "My Playlists";
            case Artists -> "Artists";
         };
      }

      private String playText() {
         if (!MusicPlayerScreen.this.module.isLoggedIn()) {
            return ">";
         } else if (MusicPlayerScreen.this.module.isLoadingStream()) {
            return "...";
         } else {
            return MusicPlayerScreen.this.module.isPlaying() ? "||" : ">";
         }
      }

      private boolean scrollMainList(int delta, int total, int visible, MusicPlayerScreen.ScrollTarget target) {
         if (visible > 0 && total > visible) {
            int current = switch (target) {
               case Queue -> this.queueScroll;
               case Search -> this.searchScroll;
               case Liked -> this.likedScroll;
               case Daily -> this.dailyScroll;
            };
            int next = this.clampScroll(current + delta, total, visible);
            if (next == current) {
               return false;
            }

            switch (target) {
               case Queue:
                  this.queueScroll = next;
                  break;
               case Search:
                  this.searchScroll = next;
                  break;
               case Liked:
                  this.likedScroll = next;
                  break;
               case Daily:
                  this.dailyScroll = next;
            }

            this.invalidate();
            return true;
         } else {
            return false;
         }
      }

      private boolean scrollSplitList(int delta, int sideTotal, int sideVisible, int detailTotal, int detailVisible, boolean playlists) {
         if (this.inside(
            this.lastMouseX,
            this.lastMouseY,
            this.sideListX,
            this.sideListY,
            this.sideListW,
            Math.max(this.sideListRowH, this.sideListRowStep * Math.max(1, sideVisible))
         )) {
            int current = playlists ? this.playlistScroll : this.artistScroll;
            int next = this.clampScroll(current + delta, sideTotal, sideVisible);
            if (next == current) {
               return false;
            }

            if (playlists) {
               this.playlistScroll = next;
            } else {
               this.artistScroll = next;
            }

            this.invalidate();
            return true;
         } else if (this.inside(
            this.lastMouseX,
            this.lastMouseY,
            this.detailListX,
            this.detailListY,
            this.detailListW,
            Math.max(this.detailListRowH, this.detailListRowStep * Math.max(1, detailVisible))
         )) {
            int current = playlists ? this.playlistSongScroll : this.artistSongScroll;
            int next = this.clampScroll(current + delta, detailTotal, detailVisible);
            if (next == current) {
               return false;
            }

            if (playlists) {
               this.playlistSongScroll = next;
            } else {
               this.artistSongScroll = next;
            }

            this.invalidate();
            return true;
         } else {
            return false;
         }
      }

      private int clampScroll(int value, int total, int visible) {
         return Math.max(0, Math.min(value, Math.max(0, total - Math.max(1, visible))));
      }

      private float clampAnimatedScroll(float value, int total, int visible) {
         return Math.max(0.0F, Math.min(value, Math.max(0, total - Math.max(1, visible))));
      }

      private int clickedAnimatedIndex(float animatedScroll, double mouseY, double listY, double step, int total) {
         if (total <= 0) {
            return 0;
         }

         int start = Math.max(0, (int)Math.floor(animatedScroll) - 1);
         double offset = (animatedScroll - start) * step;
         int index = start + (int)Math.floor((mouseY - listY + offset) / step);
         return Math.max(0, Math.min(index, total - 1));
      }

      private String formatDuration(long durationMs) {
         if (durationMs <= 0L) {
            return "--:--";
         }

         long seconds = durationMs / 1000L;
         long minutes = seconds / 60L;
         seconds %= 60L;
         return minutes + ":" + (seconds < 10L ? "0" : "") + seconds;
      }

      private boolean inside(double mouseX, double mouseY, double x, double y, double width, double height) {
         return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
      }

      private Color mix(Color from, Color to, float progress) {
         float t = Math.max(0.0F, Math.min(1.0F, progress));
         return new Color(
            (int)(from.r + (to.r - from.r) * t), (int)(from.g + (to.g - from.g) * t), (int)(from.b + (to.b - from.b) * t), (int)(from.a + (to.a - from.a) * t)
         );
      }

      private Color withAlpha(Color color, int alpha) {
         return new Color(color.r, color.g, color.b, Math.max(0, Math.min(255, alpha)));
      }

      private interface SongSelection {
         boolean selected(int var1);
      }
   }

   private enum Page {
      Login,
      Player,
      Search,
      Queue,
      Liked,
      Daily,
      Playlists,
      Artists;
   }

   private enum ScrollTarget {
      Queue,
      Search,
      Liked,
      Daily;
   }
}
