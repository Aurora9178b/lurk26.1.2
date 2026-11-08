package meteordevelopment.meteorclient.systems.modules.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.Predicate;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.OpenFile;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class BookBot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<BookBot.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("What kind of text to write."))
               .defaultValue(BookBot.Mode.Random))
            .build()
      );
   private final Setting<BookBot.RandomType> randomType = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("random-type"))
                     .description("What kind of random to use."))
                  .defaultValue(BookBot.RandomType.Utf8))
               .visible(() -> this.mode.get() == BookBot.Mode.Random))
            .build()
      );
   private final Setting<Integer> pages = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("pages")
            .description("The number of pages to write per book.")
            .defaultValue(50)
            .range(1, 100)
            .sliderRange(1, 100)
            .visible(() -> this.mode.get() != BookBot.Mode.File && this.randomType.get() != BookBot.RandomType.PaperMC)
            .build()
      );
   private final Setting<Integer> characters = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("characters")
            .description("How many characters to write per page.")
            .defaultValue(128)
            .range(1, 1024)
            .sliderRange(1, 1024)
            .visible(() -> this.mode.get() == BookBot.Mode.Random && this.randomType.get() != BookBot.RandomType.PaperMC)
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The amount of delay between writing books.").defaultValue(20).min(1).sliderRange(1, 200).build());
   private final Setting<Boolean> sign = this.sgGeneral
      .add(new BoolSetting.Builder().name("sign").description("Whether to sign the book.").defaultValue(true).build());
   private final Setting<String> name = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("name")
            .description("The name you want to give your books.")
            .defaultValue("Meteor on Crack!")
            .visible(this.sign::get)
            .build()
      );
   private final Setting<Boolean> count = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("append-count")
            .description("Whether to append the number of the book to the title.")
            .defaultValue(true)
            .visible(this.sign::get)
            .build()
      );
   private final Setting<Boolean> wordWrap = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("word-wrap")
            .description("Prevents words from being cut in the middle of lines.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == BookBot.Mode.File)
            .build()
      );
   private File file = new File(MeteorClient.FOLDER, "bookbot.txt");
   private final PointerBuffer filters;
   private int delayTimer;
   private int bookCount;
   private Random random;

   public BookBot() {
      super(Categories.Misc, "book-bot", "Automatically writes in books.");
      if (!this.file.exists()) {
         this.file = null;
      }

      this.filters = BufferUtils.createPointerBuffer(1);
      ByteBuffer txtFilter = MemoryUtil.memASCII("*.txt");
      this.filters.put(txtFilter);
      this.filters.rewind();
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WHorizontalList list = theme.horizontalList();
      WButton selectFile = list.add(theme.button("Select File")).widget();
      WLabel fileName = list.add(theme.label(this.file != null && this.file.exists() ? this.file.getName() : "No file selected.")).widget();
      selectFile.action = () -> {
         String path = TinyFileDialogs.tinyfd_openFileDialog(
            "Select File", new File(MeteorClient.FOLDER, "bookbot.txt").getAbsolutePath(), this.filters, null, false
         );
         if (path != null) {
            this.file = new File(path);
            fileName.set(this.file.getName());
         }
      };
      return list;
   }

   @Override
   public void onActivate() {
      if ((this.file == null || !this.file.exists()) && this.mode.get() == BookBot.Mode.File) {
         this.info("No file selected, please select a file in the GUI.");
         this.toggle();
      } else {
         this.random = new Random();
         this.delayTimer = this.delay.get();
         this.bookCount = 0;
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      Predicate<ItemStack> bookPredicate = i -> {
         WritableBookContent component = i.get(DataComponents.WRITABLE_BOOK_CONTENT);
         return i.getItem() == Items.WRITABLE_BOOK && (component == null || component.pages().isEmpty());
      };
      FindItemResult writableBook = InvUtils.find(bookPredicate);
      if (!writableBook.found()) {
         this.toggle();
      } else if (!InvUtils.testInMainHand(bookPredicate)) {
         InvUtils.move().from(writableBook.slot()).toHotbar(this.mc.player.getInventory().getSelectedSlot());
      } else if (this.delayTimer > 0) {
         this.delayTimer--;
      } else {
         this.delayTimer = this.delay.get();
         if (this.mode.get() == BookBot.Mode.Random) {
            switch ((BookBot.RandomType)this.randomType.get()) {
               case Ascii:
                  this.writeBook(this.random.ints(33, 128).filter(i -> !Character.isWhitespace(i) && i != 13 && i != 10).iterator());
                  break;
               case Utf8:
                  this.writeBook(this.random.ints(33, 55296).filter(i -> !Character.isWhitespace(i) && i != 13 && i != 10).iterator());
                  break;
               case PaperMC:
                  this.writePaperMcBook();
            }
         } else if (this.mode.get() == BookBot.Mode.File) {
            if ((this.file == null || !this.file.exists()) && this.mode.get() == BookBot.Mode.File) {
               this.info("No file selected, please select a file in the GUI.");
               this.toggle();
               return;
            }

            if (this.file.length() == 0L) {
               MutableComponent message = Component.literal("");
               message.append(Component.literal("The bookbot file is empty! ").withStyle(ChatFormatting.RED));
               message.append(
                  Component.literal("Click here to edit it.")
                     .setStyle(Style.EMPTY.applyFormats(ChatFormatting.UNDERLINE, ChatFormatting.RED).withClickEvent(new OpenFile(this.file.getAbsolutePath())))
               );
               this.info(message);
               this.toggle();
               return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
               StringBuilder file = new StringBuilder();

               String line;
               while ((line = reader.readLine()) != null) {
                  file.append(line).append('\n');
               }

               reader.close();
               this.writeBook(file.toString().chars().iterator());
            } catch (IOException var9) {
               this.error("Failed to read the file.");
            }
         }
      }
   }

   private void writeBook(OfInt chars) {
      ArrayList<String> pages = new ArrayList<>();
      ArrayList<Filterable<Component>> filteredPages = new ArrayList<>();
      int maxPages = this.mode.get() == BookBot.Mode.File ? 100 : this.pages.get();
      if (this.wordWrap.get() && this.mode.get() == BookBot.Mode.File) {
         StringBuilder text = new StringBuilder();

         while (chars.hasNext()) {
            text.appendCodePoint(chars.nextInt());
         }

         List<FormattedText> wrappedLines = this.mc.font.splitIgnoringLanguage(Component.literal(text.toString()), 114);
         this.processLinesToPages(wrappedLines, pages, filteredPages, maxPages);
      } else {
         int pageIndex = 0;
         StringBuilder page = new StringBuilder();

         while (pageIndex != maxPages) {
            for (int i = 0; i < this.characters.get() && chars.hasNext(); i++) {
               page.appendCodePoint(chars.nextInt());
            }

            if (!page.isEmpty()) {
               String builtPage = page.toString();
               filteredPages.add(Filterable.passThrough(Component.nullToEmpty(builtPage)));
               pages.add(builtPage);
               page.setLength(0);
            }

            pageIndex++;
         }
      }

      this.createBook(pages, filteredPages);
   }

   private void writePaperMcBook() {
      ArrayList<String> pages = new ArrayList<>();
      ArrayList<Filterable<Component>> filteredPages = new ArrayList<>();
      StringBuilder page = new StringBuilder();
      OfInt oneByte = this.random.ints(33, 128).iterator();
      OfInt twoBytes = this.random.ints(128, 2048).iterator();
      OfInt threeBytes = this.random.ints(2048, 55296).iterator();

      for (int pageIndex = 0; pageIndex < 100; pageIndex++) {
         if (pageIndex < 50) {
            page.appendCodePoint(threeBytes.nextInt());

            for (int i = 1; i < 1024; i++) {
               page.appendCodePoint(oneByte.nextInt());
            }
         } else if (pageIndex != 50) {
            for (int i = 0; i < 1024; i++) {
               page.appendCodePoint(threeBytes.nextInt());
            }
         } else {
            for (int i = 0; i < 110; i++) {
               page.appendCodePoint(threeBytes.nextInt());
            }

            page.appendCodePoint(twoBytes.nextInt());

            for (int i = 0; i < 913; i++) {
               page.appendCodePoint(oneByte.nextInt());
            }
         }

         String builtPage = page.toString();
         filteredPages.add(Filterable.passThrough(Component.nullToEmpty(builtPage)));
         pages.add(builtPage);
         page.setLength(0);
      }

      this.createBook(pages, filteredPages);
   }

   private void processLinesToPages(List<FormattedText> lines, ArrayList<String> pages, ArrayList<Filterable<Component>> filteredPages, int maxPages) {
      int pageIndex = 0;
      int lineIndex = 0;
      StringBuilder currentPage = new StringBuilder();

      for (FormattedText line : lines) {
         String lineText = line.getString();
         if (!currentPage.isEmpty()) {
            currentPage.append('\n');
         }

         currentPage.append(lineText);
         if (++lineIndex == 14) {
            filteredPages.add(Filterable.passThrough(Component.nullToEmpty(currentPage.toString())));
            pages.add(currentPage.toString());
            currentPage.setLength(0);
            pageIndex++;
            lineIndex = 0;
            if (pageIndex == maxPages) {
               break;
            }
         }
      }

      if (!currentPage.isEmpty() && pageIndex < maxPages) {
         filteredPages.add(Filterable.passThrough(Component.nullToEmpty(currentPage.toString())));
         pages.add(currentPage.toString());
      }
   }

   private void createBook(ArrayList<String> pages, ArrayList<Filterable<Component>> filteredPages) {
      String title = this.name.get();
      if (this.count.get() && this.bookCount != 0) {
         title = title + " #" + this.bookCount;
      }

      this.mc
         .player
         .getMainHandItem()
         .set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            new WrittenBookContent(Filterable.passThrough(title), this.mc.player.getGameProfile().name(), 0, filteredPages, true)
         );
      this.mc
         .player
         .connection
         .send(new ServerboundEditBookPacket(this.mc.player.getInventory().getSelectedSlot(), pages, this.sign.get() ? Optional.of(title) : Optional.empty()));
      this.bookCount++;
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = super.toTag();
      if (this.file != null && this.file.exists()) {
         tag.putString("file", this.file.getAbsolutePath());
      }

      return tag;
   }

   @Override
   public Module fromTag(CompoundTag tag) {
      if (tag.contains("file")) {
         this.file = new File(tag.getStringOr("file", ""));
      }

      return super.fromTag(tag);
   }

   public enum Mode {
      File,
      Random;
   }

   public enum RandomType {
      Ascii,
      Utf8,
      PaperMC;
   }
}
