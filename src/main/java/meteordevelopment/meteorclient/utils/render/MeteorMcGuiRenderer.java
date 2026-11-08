package meteordevelopment.meteorclient.utils.render;

import java.util.List;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

public class MeteorMcGuiRenderer extends GuiRenderer {
   public MeteorMcGuiRenderer(
      GuiRenderState renderState,
      BufferSource bufferSource,
      SubmitNodeCollector submitNodeCollector,
      FeatureRenderDispatcher featureRenderDispatcher,
      List<PictureInPictureRenderer<?>> pictureInPictureRenderers
   ) {
      super(renderState, bufferSource, submitNodeCollector, featureRenderDispatcher, pictureInPictureRenderers);
   }
}
