package meteordevelopment.meteorclient.systems.hud;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.BuiltinFontFace;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class HudRenderer {
   public static final HudRenderer INSTANCE = new HudRenderer();
   private static final double SCALE_TO_HEIGHT = 0.05555555555555555;
   private static final FontFace TAB_FONT = new BuiltinFontFace(new FontInfo("arial-unicode-bold", FontInfo.Type.Bold), "arial-unicode-bold");
   private final Hud hud = Hud.get();
   private final List<Runnable> postTasks = new ArrayList<>();
   private final Int2ObjectMap<HudRenderer.FontHolder> fontsInUse = new Int2ObjectOpenHashMap<>();
   private final LoadingCache<Integer, HudRenderer.FontHolder> fontCache = CacheBuilder.newBuilder()
      .maximumSize(4L)
      .expireAfterAccess(Duration.ofMinutes(10L))
      .removalListener(notification -> {
         if (notification.wasEvicted()) {
            ((HudRenderer.FontHolder)notification.getValue()).destroy();
         }
      })
      .build(CacheLoader.from(HudRenderer::loadFont));
   private final Int2ObjectMap<HudRenderer.FontHolder> tabFontsInUse = new Int2ObjectOpenHashMap<>();
   private final LoadingCache<Integer, HudRenderer.FontHolder> tabFontCache = CacheBuilder.newBuilder()
      .maximumSize(4L)
      .expireAfterAccess(Duration.ofMinutes(10L))
      .removalListener(notification -> {
         if (notification.wasEvicted()) {
            ((HudRenderer.FontHolder)notification.getValue()).destroy();
         }
      })
      .build(CacheLoader.from(HudRenderer::loadTabFont));
   public GuiGraphicsExtractor graphics;
   public double delta;

   private HudRenderer() {
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   public void begin(GuiGraphicsExtractor graphics) {
      Renderer2D.COLOR.begin();
      this.graphics = graphics;
      this.delta = Utils.frameTime;
      graphics.nextStratum();
      if (!this.hud.hasCustomFont()) {
         VanillaTextRenderer.INSTANCE.scaleIndividually = true;
         VanillaTextRenderer.INSTANCE.begin();
      }
   }

   public void end() {
      Renderer2D.COLOR.render();
      if (this.hud.hasCustomFont()) {
         Iterator<HudRenderer.FontHolder> it = this.fontsInUse.values().iterator();

         while (it.hasNext()) {
            HudRenderer.FontHolder fontHolder = it.next();
            if (fontHolder.visited) {
               MeshRenderer.begin()
                  .attachments(MeteorClient.mc.getMainRenderTarget())
                  .pipeline(MeteorRenderPipelines.UI_TEXT)
                  .mesh(fontHolder.getMesh())
                  .sampler("u_Texture", fontHolder.font.texture.getTextureView(), fontHolder.font.texture.getSampler())
                  .end();
            } else {
               it.remove();
               this.fontCache.put(fontHolder.font.getHeight(), fontHolder);
            }

            fontHolder.visited = false;
         }
      } else {
         VanillaTextRenderer.INSTANCE.end();
         VanillaTextRenderer.INSTANCE.scaleIndividually = false;
      }

      renderFontHolders(this.tabFontsInUse, this.tabFontCache);

      for (Runnable task : this.postTasks) {
         task.run();
      }

      this.postTasks.clear();
      this.graphics.nextStratum();
      this.graphics = null;
   }

   public void line(double x1, double y1, double x2, double y2, Color color) {
      Renderer2D.COLOR.line(x1, y1, x2, y2, color);
   }

   public void quad(double x, double y, double width, double height, Color color) {
      Renderer2D.COLOR.quad(x, y, width, height, color);
   }

   public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
      Renderer2D.COLOR.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
   }

   public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
      Renderer2D.COLOR.triangle(x1, y1, x2, y2, x3, y3, color);
   }

   public void texture(Identifier id, double x, double y, double width, double height, Color color) {
      Renderer2D.TEXTURE.begin();
      Renderer2D.TEXTURE.texQuad(x, y, width, height, color);
      Renderer2D.TEXTURE
         .render(MeteorClient.mc.getTextureManager().getTexture(id).getTextureView(), MeteorClient.mc.getTextureManager().getTexture(id).getSampler());
   }

   public double text(String text, double x, double y, Color color, boolean shadow, double scale) {
      if (scale == -1.0) {
         scale = this.hud.getTextScale();
      }

      if (!this.hud.hasCustomFont()) {
         VanillaTextRenderer.INSTANCE.scale = scale * 2.0;
         return VanillaTextRenderer.INSTANCE.render(text, x, y, color, shadow);
      }

      HudRenderer.FontHolder fontHolder = this.getFontHolder(scale, true);
      Font font = fontHolder.font;
      MeshBuilder mesh = fontHolder.getMesh();
      double width;
      if (shadow) {
         int preShadowA = CustomTextRenderer.SHADOW_COLOR.a;
         CustomTextRenderer.SHADOW_COLOR.a = (int)(color.a / 255.0 * preShadowA);
         width = font.render(mesh, text, x + 1.0, y + 1.0, CustomTextRenderer.SHADOW_COLOR, scale);
         font.render(mesh, text, x, y, color, scale);
         CustomTextRenderer.SHADOW_COLOR.a = preShadowA;
      } else {
         width = font.render(mesh, text, x, y, color, scale);
      }

      return width;
   }

   public double text(String text, double x, double y, Color color, boolean shadow) {
      return this.text(text, x, y, color, shadow, -1.0);
   }

   public double textWidth(String text, boolean shadow, double scale) {
      if (text.isEmpty()) {
         return 0.0;
      } else if (this.hud.hasCustomFont()) {
         double width = this.getFont(scale).getWidth(text, text.length());
         return (width + (shadow ? 1 : 0)) * (scale == -1.0 ? this.hud.getTextScale() : scale) + (shadow ? 1 : 0);
      } else {
         VanillaTextRenderer.INSTANCE.scale = (scale == -1.0 ? this.hud.getTextScale() : scale) * 2.0;
         return VanillaTextRenderer.INSTANCE.getWidth(text, shadow);
      }
   }

   public double textWidth(String text, boolean shadow) {
      return this.textWidth(text, shadow, -1.0);
   }

   public double textWidth(String text, double scale) {
      return this.textWidth(text, false, scale);
   }

   public double textWidth(String text) {
      return this.textWidth(text, false, -1.0);
   }

   public double textHeight(boolean shadow, double scale) {
      if (this.hud.hasCustomFont()) {
         double height = this.getFont(scale).getHeight() + 1;
         return (height + (shadow ? 1 : 0)) * (scale == -1.0 ? this.hud.getTextScale() : scale);
      } else {
         VanillaTextRenderer.INSTANCE.scale = (scale == -1.0 ? this.hud.getTextScale() : scale) * 2.0;
         return VanillaTextRenderer.INSTANCE.getHeight(shadow);
      }
   }

   public double textHeight(boolean shadow) {
      return this.textHeight(shadow, -1.0);
   }

   public double textHeight() {
      return this.textHeight(false, -1.0);
   }

   public double tabText(String text, double x, double y, Color color, boolean shadow, double scale) {
      HudRenderer.FontHolder fontHolder = this.getTabFontHolder(scale, true);
      Font font = fontHolder.font;
      MeshBuilder mesh = fontHolder.getMesh();
      double width;
      if (shadow) {
         int preShadowA = CustomTextRenderer.SHADOW_COLOR.a;
         CustomTextRenderer.SHADOW_COLOR.a = (int)(color.a / 255.0 * preShadowA);
         width = font.render(mesh, text, x + 1.0, y + 1.0, CustomTextRenderer.SHADOW_COLOR, scale);
         font.render(mesh, text, x, y, color, scale);
         CustomTextRenderer.SHADOW_COLOR.a = preShadowA;
      } else {
         width = font.render(mesh, text, x, y, color, scale);
      }

      return width;
   }

   public double tabTextWidth(String text, boolean shadow, double scale) {
      if (text.isEmpty()) {
         return 0.0;
      }

      double width = this.getTabFont(scale).getWidth(text, text.length());
      return (width + (shadow ? 1 : 0)) * scale + (shadow ? 1 : 0);
   }

   public double tabTextHeight(boolean shadow, double scale) {
      double height = this.getTabFont(scale).getHeight() + 1;
      return (height + (shadow ? 1 : 0)) * scale;
   }

   public void post(Runnable task) {
      this.postTasks.add(task);
   }

   public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay, String countOverlay) {
      RenderUtils.drawItem(this.graphics, itemStack, x, y, scale, overlay, countOverlay, true);
   }

   public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
      RenderUtils.drawItem(this.graphics, itemStack, x, y, scale, overlay);
   }

   public void entity(LivingEntity entity, int x, int y, int width, int height, float yaw, float pitch) {
      float previousBodyYaw = entity.yBodyRot;
      float previousYaw = entity.getYRot();
      float previousPitch = entity.getXRot();
      float lastLastHeadYaw = entity.yHeadRotO;
      float lastHeadYaw = entity.yHeadRot;
      float tanYaw = (float)Math.atan(yaw / 40.0F);
      float tanPitch = (float)Math.atan(pitch / 40.0F);
      entity.yBodyRot = 180.0F + tanYaw * 20.0F;
      entity.setYRot(180.0F + tanYaw * 40.0F);
      entity.setXRot(-tanPitch * 20.0F);
      entity.yHeadRot = entity.getYRot();
      entity.yHeadRotO = entity.getYRot();
      LivingEntityRenderState state = (LivingEntityRenderState)MeteorClient.mc.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1.0F);
      entity.yBodyRot = previousBodyYaw;
      entity.setYRot(previousYaw);
      entity.setXRot(previousPitch);
      entity.yHeadRot = lastLastHeadYaw;
      entity.yHeadRotO = lastHeadYaw;
      float s = 1.0F / MeteorClient.mc.getWindow().getGuiScale();
      int x1 = (int)(x * s);
      int y1 = (int)(y * s);
      int x2 = (int)((x + width) * s);
      int y2 = (int)((y + height) * s);
      float scale = Math.max(width, height) * s / 2.0F;
      Vector3f translation = new Vector3f(0.0F, 1.0F, 0.0F);
      Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
      this.graphics.entity(state, scale, translation, rotation, null, x1, y1, x2, y2);
   }

   private HudRenderer.FontHolder getFontHolder(double scale, boolean render) {
      if (scale == -1.0) {
         scale = this.hud.getTextScale();
      }

      int height = (int)Math.round(scale / 0.05555555555555555);
      HudRenderer.FontHolder fontHolder = this.fontsInUse.get(height);
      if (fontHolder != null) {
         if (render) {
            fontHolder.visited = true;
         }

         return fontHolder;
      } else if (render) {
         fontHolder = this.fontCache.getIfPresent(height);
         if (fontHolder == null) {
            fontHolder = loadFont(height);
         } else {
            this.fontCache.invalidate(height);
         }

         this.fontsInUse.put(height, fontHolder);
         fontHolder.visited = true;
         return fontHolder;
      } else {
         return this.fontCache.getUnchecked(height);
      }
   }

   private Font getFont(double scale) {
      return this.getFontHolder(scale, false).font;
   }

   private HudRenderer.FontHolder getTabFontHolder(double scale, boolean render) {
      int height = (int)Math.round(scale / 0.05555555555555555);
      HudRenderer.FontHolder fontHolder = this.tabFontsInUse.get(height);
      if (fontHolder != null) {
         if (render) {
            fontHolder.visited = true;
         }

         return fontHolder;
      } else if (render) {
         fontHolder = this.tabFontCache.getIfPresent(height);
         if (fontHolder == null) {
            fontHolder = loadTabFont(height);
         } else {
            this.tabFontCache.invalidate(height);
         }

         this.tabFontsInUse.put(height, fontHolder);
         fontHolder.visited = true;
         return fontHolder;
      } else {
         return this.tabFontCache.getUnchecked(height);
      }
   }

   private Font getTabFont(double scale) {
      return this.getTabFontHolder(scale, false).font;
   }

   private static void renderFontHolders(Int2ObjectMap<HudRenderer.FontHolder> inUse, LoadingCache<Integer, HudRenderer.FontHolder> cache) {
      Iterator<HudRenderer.FontHolder> it = inUse.values().iterator();

      while (it.hasNext()) {
         HudRenderer.FontHolder fontHolder = it.next();
         if (fontHolder.visited) {
            MeshRenderer.begin()
               .attachments(MeteorClient.mc.getMainRenderTarget())
               .pipeline(MeteorRenderPipelines.UI_TEXT)
               .mesh(fontHolder.getMesh())
               .sampler("u_Texture", fontHolder.font.texture.getTextureView(), fontHolder.font.texture.getSampler())
               .end();
         } else {
            it.remove();
            cache.put(fontHolder.font.getHeight(), fontHolder);
         }

         fontHolder.visited = false;
      }
   }

   @EventHandler
   private void onCustomFontChanged(CustomFontChangedEvent event) {
      for (HudRenderer.FontHolder fontHolder : this.fontsInUse.values()) {
         fontHolder.destroy();
      }

      for (HudRenderer.FontHolder fontHolder : this.fontCache.asMap().values()) {
         fontHolder.destroy();
      }

      for (HudRenderer.FontHolder fontHolder : this.tabFontsInUse.values()) {
         fontHolder.destroy();
      }

      for (HudRenderer.FontHolder fontHolder : this.tabFontCache.asMap().values()) {
         fontHolder.destroy();
      }

      this.fontsInUse.clear();
      this.fontCache.invalidateAll();
      this.tabFontsInUse.clear();
      this.tabFontCache.invalidateAll();
   }

   private static HudRenderer.FontHolder loadFont(int height) {
      try {
         ByteBuffer buffer = Fonts.RENDERER.fontFace.readToDirectByteBuffer();
         return new HudRenderer.FontHolder(new Font(buffer, height));
      } catch (IOException e) {
         throw new RuntimeException("Failed to load font: " + Fonts.RENDERER.fontFace, e);
      }
   }

   private static HudRenderer.FontHolder loadTabFont(int height) {
      try {
         ByteBuffer buffer = TAB_FONT.readToDirectByteBuffer();
         return new HudRenderer.FontHolder(new Font(buffer, height));
      } catch (IOException e) {
         throw new RuntimeException("Failed to load font: " + TAB_FONT, e);
      }
   }

   private static class FontHolder {
      public final Font font;
      public boolean visited;
      private MeshBuilder mesh;

      public FontHolder(Font font) {
         this.font = font;
      }

      public MeshBuilder getMesh() {
         if (this.mesh == null) {
            this.mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);
         }

         if (!this.mesh.isBuilding()) {
            this.mesh.begin();
         }

         return this.mesh;
      }

      public void destroy() {
         this.font.texture.close();
      }
   }
}
