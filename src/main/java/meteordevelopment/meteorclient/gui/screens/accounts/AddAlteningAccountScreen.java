package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.accounts.types.TheAlteningAccount;

public class AddAlteningAccountScreen extends AddAccountScreen {
   public AddAlteningAccountScreen(GuiTheme theme, AccountsScreen parent) {
      super(theme, "Add The Altening Account", parent);
   }

   @Override
   public void initWidgets() {
      WTable t = this.add(this.theme.table()).widget();
      t.add(this.theme.label("Token: "));
      WTextBox token = t.add(this.theme.textBox("")).minWidth(400.0).expandX().widget();
      token.setFocused(true);
      t.row();
      this.add = t.add(this.theme.button("Add")).expandX().widget();
      this.add.action = () -> {
         if (!token.get().isEmpty()) {
            AccountsScreen.addAccount(this, this.parent, new TheAlteningAccount(token.get()));
         }
      };
      this.enterAction = this.add.action;
   }
}
