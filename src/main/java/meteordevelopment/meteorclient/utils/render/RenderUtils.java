package meteordevelopment.meteorclient.utils.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public class RenderUtils {
   public static Vec3 center;
   public static final Matrix4f projection = new Matrix4f();
   private static final Pool<RenderUtils.RenderBlock> renderBlockPool = new Pool<>(RenderUtils.RenderBlock::new);
   private static final List<RenderUtils.RenderBlock> renderBlocks = new ObjectArrayList<>();

   private RenderUtils() {
   }

   @PostInit
   public static void init() {
      MeteorClient.EVENT_BUS.subscribe(RenderUtils.class);
   }

   public static boolean isShaderPackInUse() {
      return IrisApi.getInstance().isShaderPackInUse();
   }

   public static void drawItem(
      GuiGraphicsExtractor graphics, ItemStack itemStack, int x, int y, float scale, boolean overlay, String countOverride, boolean disableGuiScale
   ) {
      Matrix3x2fStack matrices = graphics.pose();
      matrices.pushMatrix();
      if (disableGuiScale) {
         matrices.scale(1.0F / MeteorClient.mc.getWindow().getGuiScale());
      }

      matrices.scale(scale, scale);
      int scaledX = (int)(x / scale);
      int scaledY = (int)(y / scale);
      graphics.item(itemStack, scaledX, scaledY);
      if (overlay) {
         graphics.itemDecorations(MeteorClient.mc.font, itemStack, scaledX, scaledY, countOverride);
      }

      matrices.popMatrix();
   }

   public static void drawItem(GuiGraphicsExtractor graphics, ItemStack itemStack, int x, int y, float scale, boolean overlay) {
      drawItem(graphics, itemStack, x, y, scale, overlay, null, true);
   }

   public static void updateScreenCenter(Matrix4fc projection, Matrix4fc view) {
      RenderUtils.projection.set(projection);
      Matrix4f invProjection = new Matrix4f(projection).invert();
      Matrix4f invView = new Matrix4f(view).invert();
      Vector4f center4 = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F).mul(invProjection).mul(invView);
      center4.div(center4.w);
      Vec3 camera = MeteorClient.mc.gameRenderer.getMainCamera().position();
      center = new Vec3(camera.x + center4.x, camera.y + center4.y, camera.z + center4.z);
   }

   public static void renderTickingBlock(
      BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink
   ) {
      renderBlocks.removeIf(next -> {
         if (next.pos.equals(blockPos)) {
            renderBlockPool.free(next);
            return true;
         } else {
            return false;
         }
      });
      renderBlocks.add(renderBlockPool.get().set(blockPos, sideColor, lineColor, shapeMode, excludeDir, duration, fade, shrink));
   }

   @EventHandler
   private static void onTick(TickEvent.Pre event) {
      if (!renderBlocks.isEmpty()) {
         renderBlocks.removeIf(next -> {
            next.tick();
            if (next.ticks <= 0) {
               renderBlockPool.free(next);
               return true;
            } else {
               return false;
            }
         });
      }
   }

   @EventHandler
   private static void onRender(Render3DEvent event) {
      renderBlocks.forEach(block -> block.render(event));
   }

   public static class RenderBlock {
      public MutableBlockPos pos = new MutableBlockPos();
      public Color sideColor;
      public Color lineColor;
      public ShapeMode shapeMode;
      public int excludeDir;
      public int ticks;
      public int duration;
      public boolean fade;
      public boolean shrink;

      public RenderUtils.RenderBlock set(
         BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean shrink
      ) {
         this.pos.set(blockPos);
         this.sideColor = sideColor;
         this.lineColor = lineColor;
         this.shapeMode = shapeMode;
         this.excludeDir = excludeDir;
         this.fade = fade;
         this.shrink = shrink;
         this.ticks = duration;
         this.duration = duration;
         return this;
      }

      public void tick() {
         this.ticks--;
      }

      public void render(Render3DEvent event) {
         int preSideA = this.sideColor.a;
         int preLineA = this.lineColor.a;
         double x1 = this.pos.getX();
         double y1 = this.pos.getY();
         double z1 = this.pos.getZ();
         double x2 = this.pos.getX() + 1;
         double y2 = this.pos.getY() + 1;
         double z2 = this.pos.getZ() + 1;
         double d = (double)(this.ticks - event.tickDelta) / this.duration;
         if (this.fade) {
            this.sideColor.a = (int)(this.sideColor.a * d);
            this.lineColor.a = (int)(this.lineColor.a * d);
         }

         if (this.shrink) {
            x1 += d;
            y1 += d;
            z1 += d;
            x2 -= d;
            y2 -= d;
            z2 -= d;
         }

         event.renderer.box(x1, y1, z1, x2, y2, z2, this.sideColor, this.lineColor, this.shapeMode, this.excludeDir);
         this.sideColor.a = preSideA;
         this.lineColor.a = preLineA;
      }
   }
}
