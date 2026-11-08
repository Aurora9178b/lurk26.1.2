package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class AccountInfoScreen extends WindowScreen {
   private final Account<?> account;

   public AccountInfoScreen(GuiTheme theme, Account<?> account) {
      super(theme, account.getUsername() + " details");
      this.account = account;
   }

   @Override
   public void initWidgets() {
      TokenAccount e = (TokenAccount)this.account;
      WHorizontalList l = this.add(this.theme.horizontalList()).expandX().widget();
      String tokenLabel = this.account.getType() + " token:";
      if (this.account.getType() == AccountType.Session) {
         tokenLabel = "";
      }

      WButton copy = this.theme.button("Copy");
      copy.action = () -> MeteorClient.mc.keyboardHandler.setClipboard(e.getToken());
      l.add(this.theme.label(tokenLabel));
      l.add(this.theme.label(this.account.getType() == AccountType.Session ? "Click to copy Token" : e.getToken()).color(Color.GRAY)).pad(5.0);
      l.add(copy);
   }
}
