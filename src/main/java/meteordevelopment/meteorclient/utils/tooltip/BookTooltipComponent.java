package meteordevelopment.meteorclient.utils.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;

public class BookTooltipComponent implements ClientTooltipComponent, MeteorTooltipData {
   private static final Identifier TEXTURE_BOOK_BACKGROUND = Identifier.parse("textures/gui/book.png");
   private final Component page;

   public BookTooltipComponent(Component page) {
      this.page = page;
   }

   @Override
   public ClientTooltipComponent getComponent() {
      return this;
   }

   @Override
   public int getHeight(Font textRenderer) {
      return 134;
   }

   @Override
   public int getWidth(Font textRenderer) {
      return 112;
   }

   @Override
   public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
      graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_BOOK_BACKGROUND, x - 10, y, 0.0F, 0.0F, 128, 128, 179, 179);
      Matrix3x2fStack matrices = graphics.pose();
      matrices.pushMatrix();
      matrices.translate(x + 16, y + 12);
      matrices.scale(0.7F, 0.7F);
      int offset = 0;

      for (FormattedCharSequence line : font.split(this.page, 112)) {
         graphics.text(font, line, 0, offset, -16777216, false);
         offset += 8;
      }

      matrices.popMatrix();
   }
}
