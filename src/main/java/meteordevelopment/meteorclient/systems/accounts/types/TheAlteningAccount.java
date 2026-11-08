package meteordevelopment.meteorclient.systems.accounts.types;

import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.florianreuth.waybackauthlib.InvalidCredentialsException;
import de.florianreuth.waybackauthlib.WaybackAuthLib;
import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.accounts.AccountType;
import meteordevelopment.meteorclient.systems.accounts.TokenAccount;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import net.minecraft.client.User;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class TheAlteningAccount extends Account<TheAlteningAccount> implements TokenAccount {
   private static final Environment ENVIRONMENT = new Environment(
      "http://sessionserver.thealtening.com", "http://authserver.thealtening.com", "https://api.mojang.com", "The Altening"
   );
   private static final YggdrasilAuthenticationService SERVICE = new YggdrasilAuthenticationService(MeteorClient.mc.getProxy(), ENVIRONMENT);
   private String token;
   @Nullable
   private WaybackAuthLib auth;

   public TheAlteningAccount(String token) {
      super(AccountType.TheAltening, token);
      this.token = token;
   }

   @Override
   public boolean fetchInfo() {
      this.auth = this.getAuth();

      try {
         this.auth.logIn();
         this.cache.username = this.auth.getCurrentProfile().name();
         this.cache.uuid = this.auth.getCurrentProfile().id().toString();
         this.cache.loadHead();
         return true;
      } catch (InvalidCredentialsException var2) {
         MeteorClient.LOG.error("Invalid TheAltening credentials.");
         return false;
      } catch (Exception var3) {
         MeteorClient.LOG.error("Failed to fetch info for TheAltening account!");
         return false;
      }
   }

   @Override
   public boolean login() {
      if (this.auth == null) {
         return false;
      }

      applyLoginEnvironment(SERVICE);

      try {
         setSession(
            new User(this.auth.getCurrentProfile().name(), this.auth.getCurrentProfile().id(), this.auth.getAccessToken(), Optional.empty(), Optional.empty())
         );
         return true;
      } catch (Exception var2) {
         MeteorClient.LOG.error("Failed to login with TheAltening.");
         return false;
      }
   }

   private WaybackAuthLib getAuth() {
      WaybackAuthLib auth = new WaybackAuthLib(ENVIRONMENT.servicesHost());
      auth.setUsername(this.name);
      auth.setPassword("Meteor on Crack!");
      return auth;
   }

   @Override
   public String getToken() {
      return this.token;
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putString("type", this.type.name());
      tag.putString("name", this.name);
      tag.putString("token", this.token);
      tag.put("cache", this.cache.toTag());
      return tag;
   }

   public TheAlteningAccount fromTag(CompoundTag tag) {
      if (!tag.getString("name").isEmpty() && !tag.getCompound("cache").isEmpty() && !tag.getString("token").isEmpty()) {
         this.name = tag.getString("name").get();
         this.token = tag.getString("token").get();
         this.cache.fromTag(tag.getCompound("cache").get());
         return this;
      } else {
         throw new NbtException();
      }
   }
}
