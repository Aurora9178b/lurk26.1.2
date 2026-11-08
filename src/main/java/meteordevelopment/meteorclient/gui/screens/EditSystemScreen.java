package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.Settings;

public abstract class EditSystemScreen<T> extends WindowScreen {
   private WContainer settingsContainer;
   protected final T value;
   protected final boolean isNew;
   private final Runnable reload;

   public EditSystemScreen(GuiTheme theme, T value, Runnable reload) {
      super(theme, value == null ? "New" : "Edit");
      this.isNew = value == null;
      this.value = this.isNew ? this.create() : value;
      this.reload = reload;
   }

   @Override
   public void initWidgets() {
      this.settingsContainer = this.add(this.theme.verticalList()).expandX().minWidth(400.0).widget();
      this.settingsContainer.add(this.theme.settings(this.getSettings())).expandX();
      this.add(this.theme.horizontalSeparator()).expandX();
      WButton done = this.add(this.theme.button(this.isNew ? "Create" : "Save")).expandX().widget();
      done.action = () -> {
         if (this.save()) {
            this.onClose();
         }
      };
      this.enterAction = done.action;
   }

   @Override
   public void tick() {
      this.getSettings().tick(this.settingsContainer, this.theme);
   }

   @Override
   protected void onClosed() {
      if (this.reload != null) {
         this.reload.run();
      }
   }

   public abstract T create();

   public abstract boolean save();

   public abstract Settings getSettings();
}
