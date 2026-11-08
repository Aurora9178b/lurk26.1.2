package meteordevelopment.meteorclient.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.PreInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

public class GuiThemes {
   private static final File FOLDER = new File(MeteorClient.FOLDER, "gui");
   private static final File THEMES_FOLDER = new File(FOLDER, "themes");
   private static final File FILE = new File(FOLDER, "gui.nbt");
   private static final List<GuiTheme> themes = new ArrayList<>();
   private static GuiTheme theme;

   private GuiThemes() {
   }

   @PreInit
   public static void init() {
      add(new MeteorGuiTheme());
   }

   @PostInit
   public static void postInit() {
      if (FILE.exists()) {
         try {
            CompoundTag tag = NbtIo.read(FILE.toPath());
            if (tag != null) {
               select(tag.getStringOr("currentTheme", ""));
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      if (theme == null) {
         select("Meteor");
      }
   }

   public static void add(GuiTheme theme) {
      ListIterator<GuiTheme> it = themes.listIterator();

      while (it.hasNext()) {
         if (it.next().name.equals(theme.name)) {
            it.set(theme);
            MeteorClient.LOG.error("Theme with the name '{}' has already been added.", theme.name);
            return;
         }
      }

      themes.add(theme);
   }

   public static void select(String name) {
      GuiTheme theme = null;

      for (GuiTheme t : themes) {
         if (t.name.equals(name)) {
            theme = t;
            break;
         }
      }

      if (theme != null) {
         saveTheme();
         GuiThemes.theme = theme;

         try {
            File file = new File(THEMES_FOLDER, get().name + ".nbt");
            if (file.exists()) {
               CompoundTag tag = NbtIo.read(file.toPath());
               if (tag != null) {
                  get().fromTag(tag);
               }
            }
         } catch (IOException e) {
            e.printStackTrace();
         }

         saveGlobal();
      }
   }

   public static GuiTheme get() {
      return theme;
   }

   public static String[] getNames() {
      String[] names = new String[themes.size()];

      for (int i = 0; i < themes.size(); i++) {
         names[i] = themes.get(i).name;
      }

      return names;
   }

   private static void saveTheme() {
      if (get() != null) {
         try {
            CompoundTag tag = get().toTag();
            THEMES_FOLDER.mkdirs();
            NbtIo.write(tag, new File(THEMES_FOLDER, get().name + ".nbt").toPath());
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
   }

   private static void saveGlobal() {
      try {
         CompoundTag tag = new CompoundTag();
         tag.putString("currentTheme", get().name);
         FOLDER.mkdirs();
         NbtIo.write(tag, FILE.toPath());
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static void save() {
      saveTheme();
      saveGlobal();
   }
}
