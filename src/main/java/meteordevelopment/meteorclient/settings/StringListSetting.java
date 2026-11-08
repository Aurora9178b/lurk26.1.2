package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

public class StringListSetting extends Setting<List<String>> {
   public final Class<? extends WTextBox.Renderer> renderer;
   public final CharFilter filter;

   public StringListSetting(
      String name,
      String description,
      List<String> defaultValue,
      Consumer<List<String>> onChanged,
      Consumer<Setting<List<String>>> onModuleActivated,
      IVisible visible,
      Class<? extends WTextBox.Renderer> renderer,
      CharFilter filter
   ) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
      this.renderer = renderer;
      this.filter = filter;
   }

   protected List<String> parseImpl(String str) {
      return Arrays.asList(str.split(","));
   }

   protected boolean isValueValid(List<String> value) {
      return true;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag valueTag = new ListTag();

      for (int i = 0; i < this.value.size(); i++) {
         valueTag.add(i, StringTag.valueOf(this.get().get(i)));
      }

      tag.put("value", valueTag);
      return tag;
   }

   public List<String> load(CompoundTag tag) {
      this.get().clear();

      for (Tag tagI : tag.getListOrEmpty("value")) {
         this.get().add(tagI.asString().orElse(""));
      }

      return this.get();
   }

   @Override
   public void resetImpl() {
      this.value = new ArrayList<>(this.defaultValue);
   }

   public static void fillTable(GuiTheme theme, WTable table, StringListSetting setting) {
      table.clear();
      ArrayList<String> strings = new ArrayList<>(setting.get());
      CharFilter filter = setting.filter == null ? (var0x, var1x) -> true : setting.filter;

      for (int i = 0; i < setting.get().size(); i++) {
         int msgI = i;
         String message = setting.get().get(i);
         WTextBox textBox = table.add(theme.textBox(message, filter, setting.renderer)).expandX().widget();
         textBox.action = () -> strings.set(msgI, textBox.get());
         textBox.actionOnUnfocused = () -> setting.set(strings);
         WMinus delete = table.add(theme.minus()).widget();
         delete.action = () -> {
            strings.remove(msgI);
            setting.set(strings);
            fillTable(theme, table, setting);
         };
         table.row();
      }

      if (!setting.get().isEmpty()) {
         table.add(theme.horizontalSeparator()).expandX();
         table.row();
      }

      WButton add = table.add(theme.button("Add")).expandX().widget();
      add.action = () -> {
         strings.add("");
         setting.set(strings);
         fillTable(theme, table, setting);
      };
      WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
      reset.action = () -> {
         setting.reset();
         fillTable(theme, table, setting);
      };
      reset.tooltip = "Reset";
   }

   public static class Builder extends Setting.SettingBuilder<StringListSetting.Builder, List<String>, StringListSetting> {
      private Class<? extends WTextBox.Renderer> renderer;
      private CharFilter filter;

      public Builder() {
         super(new ArrayList<>(0));
      }

      public StringListSetting.Builder defaultValue(String... defaults) {
         return this.defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
      }

      public StringListSetting.Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
         this.renderer = renderer;
         return this;
      }

      public StringListSetting.Builder filter(CharFilter filter) {
         this.filter = filter;
         return this;
      }

      public StringListSetting build() {
         return new StringListSetting(
            this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible, this.renderer, this.filter
         );
      }
   }
}
