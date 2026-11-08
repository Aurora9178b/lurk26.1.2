package meteordevelopment.meteorclient.gui.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import net.minecraft.client.gui.screens.inventory.BookViewScreen.BookAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;

public class EditBookTitleAndAuthorScreen extends WindowScreen {
   private final ItemStack itemStack;
   private final InteractionHand hand;

   public EditBookTitleAndAuthorScreen(GuiTheme theme, ItemStack itemStack, InteractionHand hand) {
      super(theme, "Edit title & author");
      this.itemStack = itemStack;
      this.hand = hand;
   }

   @Override
   public void initWidgets() {
      WTable t = this.add(this.theme.table()).expandX().widget();
      t.add(this.theme.label("Title"));
      WTextBox title = t.add(this.theme.textBox(this.itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT).title().get(MeteorClient.mc.isTextFilteringEnabled())))
         .minWidth(220.0)
         .expandX()
         .widget();
      t.row();
      t.add(this.theme.label("Author"));
      WTextBox author = t.add(this.theme.textBox(this.itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT).author())).minWidth(220.0).expandX().widget();
      t.row();
      t.add(this.theme.button("Done")).expandX().widget().action = () -> {
         WrittenBookContent component = this.itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
         WrittenBookContent newComponent = new WrittenBookContent(
            Filterable.passThrough(title.get()), author.get(), component.generation(), component.pages(), component.resolved()
         );
         this.itemStack.set(DataComponents.WRITTEN_BOOK_CONTENT, newComponent);
         BookAccess contents = new BookAccess(this.itemStack.get(DataComponents.WRITTEN_BOOK_CONTENT).getPages(MeteorClient.mc.isTextFilteringEnabled()));
         List<String> pages = new ArrayList<>(contents.getPageCount());

         for (int i = 0; i < contents.getPageCount(); i++) {
            pages.add(contents.getPage(i).getString());
         }

         MeteorClient.mc
            .getConnection()
            .send(
               new ServerboundEditBookPacket(
                  this.hand == InteractionHand.MAIN_HAND ? MeteorClient.mc.player.getInventory().getSelectedSlot() : 40, pages, Optional.of(title.get())
               )
            );
         this.onClose();
      };
   }
}
