package meteordevelopment.meteorclient.systems.modules.misc.autologin;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class AutoLoginAccounts extends System<AutoLoginAccounts> {
   private final List<AutoLoginAccount> accounts = new ArrayList<>();

   public AutoLoginAccounts() {
      super("autologin-accounts");
   }

   public static AutoLoginAccounts get() {
      return Systems.get(AutoLoginAccounts.class);
   }

   public List<AutoLoginAccount> getAccounts() {
      return this.accounts;
   }

   public void add(AutoLoginAccount account) {
      this.accounts.add(account);
      this.save();
   }

   public void remove(AutoLoginAccount account) {
      this.accounts.remove(account);
      this.save();
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      ListTag accountsTag = new ListTag();

      for (AutoLoginAccount account : this.accounts) {
         CompoundTag accountTag = new CompoundTag();
         accountTag.putString("username", account.username.get());
         accountTag.putString("ip", account.serverIp.get());
         accountTag.putString("password", account.password.get());
         accountsTag.add(accountTag);
      }

      tag.put("accounts", accountsTag);
      return tag;
   }

   public AutoLoginAccounts fromTag(CompoundTag tag) {
      this.accounts.clear();

      for (Tag item : tag.getListOrEmpty("accounts")) {
         if (item instanceof CompoundTag accountTag) {
            AutoLoginAccount account = new AutoLoginAccount();
            account.username.set(accountTag.getStringOr("username", ""));
            account.serverIp.set(accountTag.getStringOr("ip", ""));
            account.password.set(accountTag.getStringOr("password", ""));
            this.accounts.add(account);
         }
      }

      return this;
   }
}
