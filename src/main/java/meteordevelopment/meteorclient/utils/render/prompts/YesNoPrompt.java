package meteordevelopment.meteorclient.utils.render.prompts;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screens.Screen;

public class YesNoPrompt extends Prompt<YesNoPrompt> {
   private Runnable onYes = () -> {};
   private Runnable onNo = () -> {};

   private YesNoPrompt(GuiTheme theme, Screen parent) {
      super(theme, parent);
   }

   public static YesNoPrompt create() {
      return new YesNoPrompt(GuiThemes.get(), MeteorClient.mc.screen);
   }

   public static YesNoPrompt create(GuiTheme theme, Screen parent) {
      return new YesNoPrompt(theme, parent);
   }

   public YesNoPrompt onYes(Runnable action) {
      this.onYes = action;
      return this;
   }

   public YesNoPrompt onNo(Runnable action) {
      this.onNo = action;
      return this;
   }

   @Override
   protected void initialiseWidgets(Prompt<YesNoPrompt>.PromptScreen screen) {
      WButton yesButton = screen.list.add(this.theme.button("Yes")).expandX().widget();
      yesButton.action = () -> {
         this.dontShowAgain(screen);
         this.onYes.run();
         screen.onClose();
      };
      WButton noButton = screen.list.add(this.theme.button("No")).expandX().widget();
      noButton.action = () -> {
         this.dontShowAgain(screen);
         this.onNo.run();
         screen.onClose();
      };
   }
}
