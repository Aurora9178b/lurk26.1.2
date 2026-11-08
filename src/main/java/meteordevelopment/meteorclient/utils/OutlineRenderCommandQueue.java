package meteordevelopment.meteorclient.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.List;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector.ParticleGroupRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.ShadowPiece;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class OutlineRenderCommandQueue extends SubmitNodeStorage {
   private int color;
   private int[] tints;

   public void setColor(Color color) {
      this.color = color.getPacked();
   }

   @Override
   public SubmitNodeCollection order(int i) {
      return (SubmitNodeCollection)this.submitsPerOrder.computeIfAbsent(i, var1x -> new OutlineRenderCommandQueue.OutlineBatchingRenderCommandQueue(this));
   }

   private class OutlineBatchingRenderCommandQueue extends SubmitNodeCollection {
      public OutlineBatchingRenderCommandQueue(SubmitNodeStorage orderedQueueImpl) {
         super(orderedQueueImpl);
      }

      @Override
      public void submitShadow(PoseStack poseStack, float shadowRadius, List<ShadowPiece> shadowPieces) {
      }

      @Override
      public void submitNameTag(
         PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState
      ) {
      }

      @Override
      public void submitText(
         PoseStack poseStack,
         float x,
         float y,
         FormattedCharSequence text,
         boolean dropShadow,
         DisplayMode layerType,
         int light,
         int color,
         int backgroundColor,
         int outlineColor
      ) {
      }

      @Override
      public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
      }

      @Override
      public void submitLeash(PoseStack poseStack, LeashState leashState) {
      }

      @Override
      public <S> void submitModel(
         Model<? super S> model,
         S state,
         PoseStack matrices,
         RenderType renderLayer,
         int light,
         int overlay,
         int tintedColor,
         @Nullable TextureAtlasSprite sprite,
         int outlineColor,
         @Nullable CrumblingOverlay crumblingOverlay
      ) {
         super.submitModel(model, state, matrices, renderLayer, light, overlay, OutlineRenderCommandQueue.this.color, sprite, 0, crumblingOverlay);
      }

      @Override
      public void submitModelPart(
         ModelPart part,
         PoseStack matrices,
         RenderType renderLayer,
         int light,
         int overlay,
         @Nullable TextureAtlasSprite sprite,
         boolean sheeted,
         boolean hasGlint,
         int tintedColor,
         @Nullable CrumblingOverlay crumblingOverlay,
         int i
      ) {
         super.submitModelPart(
            part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, OutlineRenderCommandQueue.this.color, crumblingOverlay, i
         );
      }

      @Override
      public void submitMovingBlock(PoseStack matrices, MovingBlockRenderState state) {
      }

      @Override
      public void submitBlockModel(
         PoseStack poseStack,
         RenderType renderType,
         List<BlockStateModelPart> modelParts,
         int[] tintLayers,
         int lightCoords,
         int overlayCoords,
         int outlineColor
      ) {
         Arrays.fill(tintLayers, OutlineRenderCommandQueue.this.color);
         super.submitBlockModel(poseStack, renderType, modelParts, tintLayers, lightCoords, overlayCoords, outlineColor);
      }

      @Override
      public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
      }

      @Override
      public void submitItem(
         PoseStack poseStack,
         ItemDisplayContext displayContext,
         int lightCoords,
         int overlayCoords,
         int outlineColor,
         int[] tintLayers,
         List<BakedQuad> quads,
         FoilType foilType
      ) {
         if (OutlineRenderCommandQueue.this.tints == null || OutlineRenderCommandQueue.this.tints[0] != OutlineRenderCommandQueue.this.color) {
            OutlineRenderCommandQueue.this.tints = new int[]{
               OutlineRenderCommandQueue.this.color,
               OutlineRenderCommandQueue.this.color,
               OutlineRenderCommandQueue.this.color,
               OutlineRenderCommandQueue.this.color
            };
         }

         super.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
      }

      @Override
      public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {
      }

      @Override
      public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {
      }
   }
}
