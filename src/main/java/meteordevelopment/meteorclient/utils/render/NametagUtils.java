package meteordevelopment.meteorclient.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Zoom;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class NametagUtils {
   private static final Vector4f vec4 = new Vector4f();
   private static final Vector4f mmMat4 = new Vector4f();
   private static final Vector4f pmMat4 = new Vector4f();
   private static final Vector3d camera = new Vector3d();
   private static final Vector3d cameraNegated = new Vector3d();
   private static final Matrix4f model = new Matrix4f();
   private static final Matrix4f projection = new Matrix4f();
   private static double windowScale;
   public static double scale;

   private NametagUtils() {
   }

   public static void onRender(Matrix4fc modelView) {
      model.set(modelView);
      projection.set(RenderUtils.projection);
      Utils.set(camera, MeteorClient.mc.gameRenderer.getMainCamera().position());
      cameraNegated.set(camera);
      cameraNegated.negate();
      windowScale = MeteorClient.mc.getWindow().calculateScale(1, false);
   }

   public static boolean to2D(Vector3d pos, double scale) {
      return to2D(pos, scale, true);
   }

   public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling) {
      return to2D(pos, scale, distanceScaling, false);
   }

   public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling, boolean allowBehind) {
      Zoom zoom = Modules.get().get(Zoom.class);
      NametagUtils.scale = scale * zoom.getScaling();
      if (distanceScaling) {
         NametagUtils.scale = NametagUtils.scale * getScale(pos);
      }

      vec4.set(cameraNegated.x + pos.x, cameraNegated.y + pos.y, cameraNegated.z + pos.z, 1.0);
      vec4.mul(model, mmMat4);
      mmMat4.mul(projection, pmMat4);
      boolean behind = pmMat4.w <= 0.0F;
      if (behind && !allowBehind) {
         return false;
      }

      toScreen(pmMat4);
      double x = pmMat4.x * MeteorClient.mc.getWindow().getWidth();
      double y = pmMat4.y * MeteorClient.mc.getWindow().getHeight();
      if (behind) {
         x = MeteorClient.mc.getWindow().getWidth() - x;
         y = MeteorClient.mc.getWindow().getHeight() - y;
      }

      if (!Double.isInfinite(x) && !Double.isInfinite(y)) {
         pos.set(x / windowScale, MeteorClient.mc.getWindow().getHeight() - y / windowScale, allowBehind ? pmMat4.w : pmMat4.z);
         return true;
      } else {
         return false;
      }
   }

   public static void begin(Vector3d pos) {
      Matrix4fStack matrices = RenderSystem.getModelViewStack();
      begin(matrices, pos);
   }

   public static void begin(Vector3d pos, GuiGraphicsExtractor graphics) {
      begin(pos);
      Matrix3x2fStack matrices = graphics.pose();
      matrices.pushMatrix();
      matrices.scale(1.0F / MeteorClient.mc.getWindow().getGuiScale());
      matrices.translate((float)pos.x, (float)pos.y);
      matrices.scale((float)scale, (float)scale);
   }

   private static void begin(Matrix4fStack matrices, Vector3d pos) {
      matrices.pushMatrix();
      matrices.translate((float)pos.x, (float)pos.y, 0.0F);
      matrices.scale((float)scale, (float)scale, 1.0F);
   }

   public static void end() {
      RenderSystem.getModelViewStack().popMatrix();
   }

   public static void end(GuiGraphicsExtractor graphics) {
      end();
      graphics.pose().popMatrix();
   }

   private static double getScale(Vector3d pos) {
      double dist = camera.distance(pos);
      return Mth.clamp(1.0 - dist * 0.01, 0.5, 2.147483647E9);
   }

   private static void toScreen(Vector4f vec) {
      float newW = 1.0F / vec.w * 0.5F;
      vec.x = vec.x * newW + 0.5F;
      vec.y = vec.y * newW + 0.5F;
      vec.z = vec.z * newW + 0.5F;
      vec.w = newW;
   }
}
