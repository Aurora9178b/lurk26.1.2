package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.misc.autologin.AutoLoginAccount;
import meteordevelopment.meteorclient.systems.modules.misc.autologin.AutoLoginAccounts;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;

public class AutoLogin extends Module {
   public static AutoLogin INSTANCE;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Boolean> autoSave = this.sgGeneral
      .add(new BoolSetting.Builder().name("AutoSave").description("Automatically saves login information.").defaultValue(true).build());
   public final Setting<String> loginCommand = this.sgGeneral
      .add(new StringSetting.Builder().name("LoginCommand").description("Login command.").defaultValue("l").build());
   public final Setting<String> registerCommand = this.sgGeneral
      .add(new StringSetting.Builder().name("RegCommand").description("Register command.").defaultValue("reg").build());
   public final Setting<String> cpCommand = this.sgGeneral
      .add(new StringSetting.Builder().name("CpCommand").description("Change password command.").defaultValue("cp").build());
   private boolean check;
   private String password = "";
   private String lastIp = "";

   public AutoLogin() {
      super(Categories.Misc, "AutoLogin", "Automatically logs into saved servers.");
      MeteorClient.EVENT_BUS.subscribe(new AutoLogin.StaticListener());
      INSTANCE = this;
   }

   private List<AutoLoginAccount> accounts() {
      return AutoLoginAccounts.get().getAccounts();
   }

   @EventHandler
   private void onMessageSend(PacketEvent.Send event) {
      if (this.autoSave.get() && event.packet instanceof ServerboundChatCommandPacket packet) {
         String[] args = packet.command().trim().split("\\s+");
         if (args.length >= 2) {
            boolean isLogin = this.commandEquals(args[0], this.loginCommand.get());
            boolean isRegister = this.commandEquals(args[0], this.registerCommand.get());
            boolean isChangePassword = this.commandEquals(args[0], this.cpCommand.get());
            if (isLogin || isRegister || isChangePassword) {
               if (!isChangePassword || args.length >= 3) {
                  String savedPassword = isChangePassword ? args[2] : args[1];
                  String username = this.currentUsername();
                  String server = this.lastIp;

                  for (AutoLoginAccount account : this.accounts()) {
                     if (account.username.get().equalsIgnoreCase(username) && account.serverIp.get().equalsIgnoreCase(server)) {
                        account.password.set(savedPassword);
                        AutoLoginAccounts.get().save();
                        return;
                     }
                  }

                  AutoLoginAccount account = new AutoLoginAccount();
                  account.username.set(username);
                  account.serverIp.set(server);
                  account.password.set(savedPassword);
                  AutoLoginAccounts.get().add(account);
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.check && this.mc.player != null && this.mc.getConnection() != null) {
         this.mc.getConnection().sendCommand(this.loginCommand.get() + " " + this.password);
         this.check = false;
      }
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WTable table = theme.table();
      this.initTable(theme, table);
      return table;
   }

   private void initTable(GuiTheme theme, WTable table) {
      table.clear();
      table.add(theme.label("Username"));
      table.add(theme.label("Server"));
      table.add(theme.label("Password"));
      table.row();

      for (AutoLoginAccount account : this.accounts()) {
         table.add(theme.label(account.username.get()));
         table.add(theme.label(account.serverIp.get() + "  "));
         table.add(theme.label(account.password.get()));
         WButton edit = table.add(theme.button("Edit")).widget();
         edit.action = () -> this.mc.setScreen(new AutoLogin.EditAccountScreen(theme, account, () -> this.initTable(theme, table)));
         WMinus remove = table.add(theme.minus()).widget();
         remove.action = () -> {
            AutoLoginAccounts.get().remove(account);
            this.initTable(theme, table);
         };
         table.row();
      }

      table.row();
      table.add(theme.horizontalSeparator()).expandX();
      table.row();
      WButton createAccount = table.add(theme.button("CreateAccount")).expandX().widget();
      createAccount.action = () -> this.mc.setScreen(new AutoLogin.EditAccountScreen(theme, null, () -> this.initTable(theme, table)));
   }

   private boolean commandEquals(String command, String settingCommand) {
      return this.normalizeCommand(command).equalsIgnoreCase(this.normalizeCommand(settingCommand));
   }

   private String normalizeCommand(String command) {
      return command == null ? "" : command.strip().replaceFirst("^/+", "");
   }

   private String currentUsername() {
      return this.mc.getUser().getName();
   }

   private class EditAccountScreen extends EditSystemScreen<AutoLoginAccount> {
      public EditAccountScreen(GuiTheme theme, AutoLoginAccount value, Runnable reload) {
         super(theme, value, reload);
         if (value == null) {
            this.value.username.set(AutoLogin.this.currentUsername());
            this.value.serverIp.set(AutoLogin.this.lastIp);
         }
      }

      public AutoLoginAccount create() {
         return new AutoLoginAccount();
      }

      @Override
      public boolean save() {
         if (this.value.username.get().isBlank()) {
            return false;
         }

         if (!AutoLogin.this.accounts().contains(this.value)) {
            AutoLoginAccounts.get().add(this.value);
         } else {
            AutoLoginAccounts.get().save();
         }

         return true;
      }

      @Override
      public Settings getSettings() {
         return this.value.settings;
      }
   }

   private class StaticListener {
      @EventHandler
      private void onGameJoined(ServerConnectBeginEvent event) {
         AutoLogin.this.check = false;
         AutoLogin.this.lastIp = event.address == null ? "" : event.address.getHost();

         for (AutoLoginAccount account : AutoLogin.this.accounts()) {
            if (account.username.get().equalsIgnoreCase(AutoLogin.this.currentUsername()) && account.serverIp.get().equalsIgnoreCase(AutoLogin.this.lastIp)) {
               AutoLogin.this.check = true;
               AutoLogin.this.password = account.password.get();
               return;
            }
         }
      }
   }
}
