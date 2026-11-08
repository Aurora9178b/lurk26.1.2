package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

public abstract class AddAccountScreen extends WindowScreen {
   public final AccountsScreen parent;
   public WButton add;
   private int timer;

   protected AddAccountScreen(GuiTheme theme, String title, AccountsScreen parent) {
      super(theme, title);
      this.parent = parent;
   }

   @Override
   public void tick() {
      if (this.locked) {
         if (this.timer > 2) {
            this.add.set(this.getNext(this.add));
            this.timer = 0;
         } else {
            this.timer++;
         }
      } else if (!this.add.getText().equals("Add")) {
         this.add.set("Add");
      }
   }

   private String getNext(WButton add) {
      return switch (add.getText()) {
         case "Add", "oo0" -> "ooo";
         case "ooo" -> "0oo";
         case "0oo" -> "o0o";
         case "o0o" -> "oo0";
         default -> "Add";
      };
   }
}
