package meteordevelopment.meteorclient.settings;

import java.util.List;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.renderer.Fonts;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import net.minecraft.nbt.CompoundTag;

public class FontFaceSetting extends Setting<FontFace> {
   public FontFaceSetting(
      String name, String description, FontFace defaultValue, Consumer<FontFace> onChanged, Consumer<Setting<FontFace>> onModuleActivated, IVisible visible
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
   }

   protected FontFace parseImpl(String str) {
      String[] split = str.replace(" ", "").split("-");
      if (split.length != 2) {
         return null;
      }

      for (FontFamily family : Fonts.FONT_FAMILIES) {
         if (family.getName().replace(" ", "").equals(split[0])) {
            try {
               return family.get(FontInfo.Type.valueOf(split[1]));
            } catch (IllegalArgumentException var6) {
               return null;
            }
         }
      }

      return null;
   }

   @Override
   public List<String> getSuggestions() {
      return List.of("JetBrainsMono-Regular", "Arial-Bold");
   }

   protected boolean isValueValid(FontFace value) {
      if (value == null) {
         return false;
      }

      for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
         if (fontFamily.hasType(value.info.type())) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected CompoundTag save(CompoundTag tag) {
      tag.putString("family", this.get().info.family());
      tag.putString("type", this.get().info.type().toString());
      return tag;
   }

   protected FontFace load(CompoundTag tag) {
      String family = tag.getStringOr("family", "");

      FontInfo.Type type;
      try {
         type = FontInfo.Type.valueOf(tag.getStringOr("type", ""));
      } catch (IllegalArgumentException var7) {
         this.set(Fonts.DEFAULT_FONT);
         return this.get();
      }

      boolean changed = false;

      for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
         if (fontFamily.getName().equals(family)) {
            this.set(fontFamily.get(type));
            changed = true;
         }
      }

      if (!changed) {
         this.set(Fonts.DEFAULT_FONT);
      }

      return this.get();
   }

   public static class Builder extends Setting.SettingBuilder<FontFaceSetting.Builder, FontFace, FontFaceSetting> {
      public Builder() {
         super(Fonts.DEFAULT_FONT);
      }

      public FontFaceSetting build() {
         return new FontFaceSetting(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }
}
