package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.util.UndashedUuid;
import java.util.Optional;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.MicrosoftLogin;
import net.minecraft.client.User;
import org.jetbrains.annotations.Nullable;

public class MicrosoftAccount extends Account<MicrosoftAccount> {
   @Nullable
   private String token;

   public MicrosoftAccount(String refreshToken) {
      super(AccountType.Microsoft, refreshToken);
   }

   @Override
   public boolean fetchInfo() {
      this.token = this.auth();
      return this.token != null;
   }

   @Override
   public boolean login() {
      if (this.token == null) {
         return false;
      }

      super.login();
      setSession(new User(this.cache.username, UndashedUuid.fromStringLenient(this.cache.uuid), this.token, Optional.empty(), Optional.empty()));
      return true;
   }

   @Nullable
   private String auth() {
      MicrosoftLogin.LoginData data = MicrosoftLogin.login(this.name);
      if (!data.isGood()) {
         return null;
      }

      this.name = data.newRefreshToken;
      this.cache.username = data.username;
      this.cache.uuid = data.uuid;
      return data.mcToken;
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof MicrosoftAccount account ? account.name.equals(this.name) : false;
   }
}
