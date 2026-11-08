package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.GuiGraphicsExtractorAccessor;
import meteordevelopment.meteorclient.utils.render.CustomBannerGuiElementRenderState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

public class BannerTooltipComponent implements MeteorTooltipData, ClientTooltipComponent {
   private final DyeColor color;
   private final BannerPatternLayers patterns;
   private final BannerFlagModel bannerFlag;

   public BannerTooltipComponent(ItemStack banner) {
      this.color = ((BannerItem)banner.getItem()).getColor();
      this.patterns = banner.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      ModelPart modelPart = MeteorClient.mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
      this.bannerFlag = new BannerFlagModel(modelPart);
   }

   public BannerTooltipComponent(DyeColor color, BannerPatternLayers patterns) {
      this.color = color;
      this.patterns = patterns;
      ModelPart modelPart = MeteorClient.mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
      this.bannerFlag = new BannerFlagModel(modelPart);
   }

   @Override
   public ClientTooltipComponent getComponent() {
      return this;
   }

   @Override
   public int getHeight(Font textRenderer) {
      return 80;
   }

   @Override
   public int getWidth(Font textRenderer) {
      return 40;
   }

   @Override
   public void extractImage(Font textRenderer, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
      int centerX = width / 2 - this.getWidth(null) / 2;
      GuiGraphicsExtractorAccessor contextAccessor = (GuiGraphicsExtractorAccessor)graphics;
      contextAccessor.getGuiRenderState()
         .addPicturesInPictureState(
            new CustomBannerGuiElementRenderState(
               this.bannerFlag,
               this.color,
               this.patterns,
               centerX + x,
               y,
               centerX + x + this.getWidth(null),
               y + this.getHeight(null),
               contextAccessor.getScissorStack().peek(),
               32.0F
            )
         );
   }
}
