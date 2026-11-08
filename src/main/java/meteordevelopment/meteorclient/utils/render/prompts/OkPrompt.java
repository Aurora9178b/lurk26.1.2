package meteordevelopment.meteorclient.utils.render.prompts;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import net.minecraft.client.gui.screens.Screen;

public class OkPrompt extends Prompt<OkPrompt> {
   private Runnable onOk = () -> {};

   private OkPrompt(GuiTheme theme, Screen parent) {
      super(theme, parent);
   }

   public static OkPrompt create() {
      return new OkPrompt(GuiThemes.get(), MeteorClient.mc.screen);
   }

   public static OkPrompt create(GuiTheme theme, Screen parent) {
      return new OkPrompt(theme, parent);
   }

   public OkPrompt onOk(Runnable action) {
      this.onOk = action;
      return this;
   }

   @Override
   protected void initialiseWidgets(Prompt<OkPrompt>.PromptScreen screen) {
      WButton okButton = screen.list.add(this.theme.button("Ok")).expandX().widget();
      okButton.action = () -> {
         this.dontShowAgain(screen);
         this.onOk.run();
         screen.onClose();
      };
   }
}
