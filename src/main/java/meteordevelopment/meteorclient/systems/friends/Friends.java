package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UndashedUuid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class Friends extends System<Friends> implements Iterable<Friend> {
   private final List<Friend> friends = new ArrayList<>();

   public Friends() {
      super("friends");
   }

   public static Friends get() {
      return Systems.get(Friends.class);
   }

   public boolean add(Friend friend) {
      if (friend.name.isEmpty() || friend.name.contains(" ")) {
         return false;
      }

      if (this.get(friend.name) != null) {
         return false;
      }

      this.friends.add(friend);
      this.save();
      return true;
   }

   public boolean remove(Friend friend) {
      if (this.friends.remove(friend)) {
         this.save();
         return true;
      } else {
         return false;
      }
   }

   public Friend get(String name) {
      for (Friend friend : this.friends) {
         if (friend.name.equalsIgnoreCase(name)) {
            return friend;
         }
      }

      return null;
   }

   public Friend get(Player player) {
      return this.get(player.getName().getString());
   }

   public Friend get(PlayerInfo player) {
      return this.get(player.getProfile().name());
   }

   public boolean isFriend(Player player) {
      return player != null && this.get(player) != null;
   }

   public boolean isFriend(PlayerInfo player) {
      return this.get(player) != null;
   }

   public boolean shouldAttack(Player player) {
      return !this.isFriend(player);
   }

   public int count() {
      return this.friends.size();
   }

   public boolean isEmpty() {
      return this.friends.isEmpty();
   }

   @NotNull
   @Override
   public Iterator<Friend> iterator() {
      return this.friends.iterator();
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.put("friends", NbtUtils.listToTag(this.friends));
      return tag;
   }

   public Friends fromTag(CompoundTag tag) {
      this.friends.clear();

      for (Tag itemTag : tag.getListOrEmpty("friends")) {
         CompoundTag friendTag = (CompoundTag)itemTag;
         if (friendTag.contains("name")) {
            String name = friendTag.getStringOr("name", "");
            if (this.get(name) == null) {
               String uuid = friendTag.getStringOr("id", "");
               Friend friend = !uuid.isBlank() ? new Friend(name, UndashedUuid.fromStringLenient(uuid)) : new Friend(name);
               this.friends.add(friend);
            }
         }
      }

      Collections.sort(this.friends);
      MeteorExecutor.execute(() -> this.friends.forEach(Friend::updateInfo));
      return this;
   }
}
