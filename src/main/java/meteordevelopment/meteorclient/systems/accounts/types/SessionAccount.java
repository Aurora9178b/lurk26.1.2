package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.util.UndashedUuid;
import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;

public class SessionAccount extends Account<SessionAccount> implements TokenAccount {
   private String accessToken;

   public SessionAccount(String label) {
      super(AccountType.Session, label);
      this.accessToken = label;
   }

   public SessionAccount fromTag(CompoundTag tag) {
      super.fromTag(tag);
      this.accessToken = tag.getStringOr("token", "");
      return this;
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = super.toTag();
      tag.putString("token", this.accessToken);
      return tag;
   }

   @Override
   public boolean fetchInfo() {
      if (this.accessToken != null && !this.accessToken.isBlank()) {
         SessionAccount.ProfileResponse profile;
         try {
            profile = Http.get("https://api.minecraftservices.com/minecraft/profile").bearer(this.accessToken).sendJson(SessionAccount.ProfileResponse.class);
         } catch (IllegalArgumentException e) {
            MeteorClient.LOG.error("Invalid session account token", e);
            return false;
         }

         if (profile != null && profile.id != null && profile.name != null) {
            this.cache.username = profile.name;
            this.cache.uuid = profile.id;
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean login() {
      if (this.accessToken != null && !this.accessToken.isBlank()) {
         super.login();
         setSession(new User(this.cache.username, UndashedUuid.fromStringLenient(this.cache.uuid), this.accessToken, Optional.empty(), Optional.empty()));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String getToken() {
      return this.accessToken;
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof SessionAccount account2 ? account2.name.equals(this.name) : false;
   }

   private static class ProfileResponse {
      public String id;
      public String name;
   }
}
