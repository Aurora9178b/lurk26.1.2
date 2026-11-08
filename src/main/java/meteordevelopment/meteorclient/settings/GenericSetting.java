package meteordevelopment.meteorclient.settings;

import java.util.function.Consumer;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import net.minecraft.nbt.CompoundTag;

public class GenericSetting<T extends IGeneric<T>> extends Setting<T> {
   public GenericSetting(String name, String description, T defaultValue, Consumer<T> onChanged, Consumer<Setting<T>> onModuleActivated, IVisible visible) {
      super(name, description, defaultValue, onChanged, onModuleActivated, visible);
   }

   public WidgetScreen createScreen(GuiTheme theme) {
      return this.get().createScreen(theme, this);
   }

   @Override
   public void resetImpl() {
      if (this.value == null) {
         this.value = this.defaultValue.copy();
      }

      this.value.set(this.defaultValue);
   }

   protected T parseImpl(String str) {
      return this.defaultValue.copy();
   }

   protected boolean isValueValid(T value) {
      return true;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.put("value", this.get().toTag());
      return tag;
   }

   public T load(CompoundTag tag) {
      this.get().fromTag(tag.getCompoundOrEmpty("value"));
      return this.get();
   }

   public static class Builder<T extends IGeneric<T>> extends Setting.SettingBuilder<GenericSetting.Builder<T>, T, GenericSetting<T>> {
      public Builder() {
         super(null);
      }

      public GenericSetting<T> build() {
         return new GenericSetting<>(this.name, this.description, this.defaultValue, this.onChanged, this.onModuleActivated, this.visible);
      }
   }
}
