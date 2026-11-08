package meteordevelopment.meteorclient.systems.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import meteordevelopment.meteorclient.events.render.ApplyTransformationEvent;
import meteordevelopment.meteorclient.events.render.RenderItemEntityEvent;
import meteordevelopment.meteorclient.mixin.ItemStackRenderStateAccessor;
import meteordevelopment.meteorclient.mixin.LayerRenderStateAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class ItemPhysics extends Module {
   private static final float PIXEL_SIZE = 0.0625F;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Boolean> randomRotation = this.sgGeneral
      .add(new BoolSetting.Builder().name("random-rotation").description("Adds a random rotation to every item.").defaultValue(true).build());
   private final RandomSource random = RandomSource.createThreadLocalInstance();
   private boolean skipTransformation;

   public ItemPhysics() {
      super(Categories.Render, "item-physics", "Applies physics to items on the ground.");
   }

   @EventHandler
   private void onRenderItemEntity(RenderItemEntityEvent event) {
      event.cancel();
      if (!event.renderState.item.isEmpty() && event.itemEntity != null) {
         PoseStack matrices = event.matrixStack;
         this.random.setSeed(event.itemEntity.getId() * 89748956L);

         for (int i = 0; i < ((ItemStackRenderStateAccessor)event.renderState.item).meteor$getActiveLayerCount(); i++) {
            LayerRenderState layer = ((ItemStackRenderStateAccessor)event.renderState.item).meteor$getLayers()[i];
            ItemPhysics.ModelInfo info = this.getInfo(layer.prepareQuadList());
            matrices.pushPose();
            this.applyTransformation(matrices, ((LayerRenderStateAccessor)layer).meteor$getTransform());
            matrices.translate(0.0F, info.offsetY, 0.0F);
            this.offsetInWater(matrices, event.itemEntity);
            if (info.flat) {
               matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
               matrices.translate(0.0F, 0.0F, info.offsetZ);
            }

            if (this.randomRotation.get()) {
               Axis axis = Axis.YP;
               float x = 0.5F;
               float y = 0.0F;
               float z = 0.5F;
               if (info.flat) {
                  axis = Axis.ZP;
                  y = 0.5F;
                  z = 0.0F;
               }

               float degrees = (this.random.nextFloat() * 2.0F - 1.0F) * 90.0F;
               matrices.translate(x, y, z);
               matrices.mulPose(axis.rotationDegrees(degrees));
               matrices.translate(-x, -y, -z);
            }

            this.renderLayer(event, info);
            matrices.popPose();
         }
      }
   }

   @EventHandler
   private void onApplyTransformation(ApplyTransformationEvent event) {
      if (this.skipTransformation) {
         event.cancel();
      }
   }

   private void renderLayer(RenderItemEntityEvent event, ItemPhysics.ModelInfo info) {
      PoseStack matrices = event.matrixStack;
      this.skipTransformation = true;

      for (int j = 0; j < event.renderState.count; j++) {
         matrices.pushPose();
         if (j > 0) {
            float x = (this.random.nextFloat() * 2.0F - 1.0F) * 0.25F;
            float z = (this.random.nextFloat() * 2.0F - 1.0F) * 0.25F;
            this.translate(matrices, info, x, 0.0F, z);
         }

         event.renderState.item.submit(matrices, event.renderCommandQueue, event.light, OverlayTexture.NO_OVERLAY, event.renderState.outlineColor);
         matrices.popPose();
         float y = Math.max(this.random.nextFloat() * 0.0625F, 0.03125F);
         this.translate(matrices, info, 0.0F, y, 0.0F);
      }

      this.skipTransformation = false;
   }

   private void translate(PoseStack matrices, ItemPhysics.ModelInfo info, float x, float y, float z) {
      if (info.flat) {
         float temp = y;
         y = z;
         z = -temp;
      }

      matrices.translate(x, y, z);
   }

   private void applyTransformation(PoseStack matrices, ItemTransform transform) {
      transform = new ItemTransform(transform.rotation(), new Vector3f(transform.translation().x(), 0.0F, transform.translation().z()), transform.scale());
      transform.apply(false, matrices.last());
   }

   private void offsetInWater(PoseStack matrices, ItemEntity entity) {
      if (entity.isInWater()) {
         matrices.translate(0.0F, 0.333F, 0.0F);
      }
   }

   private ItemPhysics.ModelInfo getInfo(List<BakedQuad> quads) {
      float minX = Float.MAX_VALUE;
      float maxX = Float.MIN_VALUE;
      float minY = Float.MAX_VALUE;
      float maxY = Float.MIN_VALUE;
      float minZ = Float.MAX_VALUE;
      float maxZ = Float.MIN_VALUE;

      for (BakedQuad quad : quads) {
         for (int i = 0; i < 4; i++) {
            Vector3fc vec = quad.position(i);
            minY = Math.min(minY, vec.y());
            maxY = Math.max(maxY, vec.y());
            minZ = Math.min(minZ, vec.z());
            maxZ = Math.max(maxZ, vec.z());
            minX = Math.min(minX, vec.x());
            maxX = Math.max(maxX, vec.x());
         }
      }

      if (minX == Float.MAX_VALUE) {
         minX = 0.0F;
      }

      if (minY == Float.MAX_VALUE) {
         minY = 0.0F;
      }

      if (minZ == Float.MAX_VALUE) {
         minZ = 0.0F;
      }

      if (maxX == Float.MIN_VALUE) {
         maxX = 1.0F;
      }

      if (maxY == Float.MIN_VALUE) {
         maxY = 1.0F;
      }

      if (maxZ == Float.MIN_VALUE) {
         maxZ = 1.0F;
      }

      float x = maxX - minX;
      float y = maxY - minY;
      float z = maxZ - minZ;
      boolean flat = x > 0.0625F && y > 0.0625F && z <= 0.0625F;
      return new ItemPhysics.ModelInfo(flat, 0.5F - minY, -maxZ);
   }

   record ModelInfo(boolean flat, float offsetY, float offsetZ) {
   }
}
