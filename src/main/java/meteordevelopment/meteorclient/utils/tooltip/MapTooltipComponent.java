package meteordevelopment.meteorclient.utils.tooltip;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BetterTooltips;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapTooltipComponent implements ClientTooltipComponent, MeteorTooltipData {
   private static final Identifier TEXTURE_MAP_BACKGROUND = Identifier.parse("textures/map/map_background.png");
   private final int mapId;
   private final MapRenderState mapRenderState = new MapRenderState();

   public MapTooltipComponent(int mapId) {
      this.mapId = mapId;
   }

   @Override
   public int getHeight(Font textRenderer) {
      double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
      return (int)(144.0 * scale) + 2;
   }

   @Override
   public int getWidth(Font textRenderer) {
      double scale = Modules.get().get(BetterTooltips.class).mapsScale.get();
      return (int)(144.0 * scale);
   }

   @Override
   public ClientTooltipComponent getComponent() {
      return this;
   }

   @Override
   public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
      float scale = Modules.get().get(BetterTooltips.class).mapsScale.get().floatValue();
      int size = (int)(144.0F * scale);
      graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_MAP_BACKGROUND, x, y, 0.0F, 0.0F, size, size, size, size);
      MapItemSavedData mapState = MapItem.getSavedData(new MapId(this.mapId), MeteorClient.mc.level);
      if (mapState != null) {
         graphics.pose().pushMatrix();
         graphics.pose().translate(x, y);
         graphics.pose().scale(scale, scale);
         graphics.pose().translate(8.0F, 8.0F);
         MeteorClient.mc.getMapRenderer().extractRenderState(new MapId(this.mapId), mapState, this.mapRenderState);
         graphics.map(this.mapRenderState);
         graphics.pose().popMatrix();
      }
   }
}
