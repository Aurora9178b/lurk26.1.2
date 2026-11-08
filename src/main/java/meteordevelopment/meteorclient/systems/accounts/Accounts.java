package meteordevelopment.meteorclient.systems.accounts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;
import meteordevelopment.meteorclient.systems.accounts.types.MicrosoftAccount;
import meteordevelopment.meteorclient.systems.accounts.types.SessionAccount;
import meteordevelopment.meteorclient.systems.accounts.types.TheAlteningAccount;
import meteordevelopment.meteorclient.utils.misc.NbtException;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class Accounts extends System<Accounts> implements Iterable<Account<?>> {
   private List<Account<?>> accounts = new ArrayList<>();

   public Accounts() {
      super("accounts");
   }

   public static Accounts get() {
      return Systems.get(Accounts.class);
   }

   public void add(Account<?> account) {
      this.accounts.add(account);
      this.save();
   }

   public boolean exists(Account<?> account) {
      return this.accounts.contains(account);
   }

   public void remove(Account<?> account) {
      if (this.accounts.remove(account)) {
         this.save();
      }
   }

   public int size() {
      return this.accounts.size();
   }

   @NotNull
   @Override
   public Iterator<Account<?>> iterator() {
      return this.accounts.iterator();
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.put("accounts", NbtUtils.listToTag(this.accounts));
      return tag;
   }

   public Accounts fromTag(CompoundTag tag) {
      MeteorExecutor.execute(() -> this.accounts = NbtUtils.listFromTag(tag.getListOrEmpty("accounts"), tag1 -> {
         CompoundTag t = (CompoundTag)tag1;
         if (!t.contains("type")) {
            return null;
         }

         AccountType type = AccountType.valueOf(t.getStringOr("type", ""));

         try {
            return switch (type) {
               case Cracked -> (CrackedAccount)new CrackedAccount(null).fromTag(t);
               case Microsoft -> (MicrosoftAccount)new MicrosoftAccount(null).fromTag(t);
               case TheAltening -> new TheAlteningAccount(null).fromTag(t);
               case Session -> new SessionAccount(null).fromTag(t);
            };
         } catch (NbtException var4) {
            return null;
         }
      }));
      return this;
   }
}
