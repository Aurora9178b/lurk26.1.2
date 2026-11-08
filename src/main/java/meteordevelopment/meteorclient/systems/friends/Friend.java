package meteordevelopment.meteorclient.systems.friends;

import com.mojang.util.UndashedUuid;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.network.FailedHttpResponse;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.PlayerHeadTexture;
import meteordevelopment.meteorclient.utils.render.PlayerHeadUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class Friend implements ISerializable<Friend>, Comparable<Friend> {
   public volatile String name;
   @Nullable
   private volatile UUID id;
   @Nullable
   private volatile PlayerHeadTexture headTexture;
   private volatile boolean updating;
   private volatile long nextUpdateRequestAt;

   public Friend(String name, @Nullable UUID id) {
      this.name = name;
      this.id = id;
      this.headTexture = null;
   }

   public Friend(Player player) {
      this(player.getName().getString(), player.getUUID());
   }

   public Friend(String name) {
      this(name, null);
   }

   public String getName() {
      return this.name;
   }

   public PlayerHeadTexture getHead() {
      return this.headTexture != null ? this.headTexture : PlayerHeadUtils.STEVE_HEAD;
   }

   public boolean requestUpdateInfo() {
      long now = System.currentTimeMillis();
      synchronized (this) {
         if (this.updating || this.headTexture != null || now < this.nextUpdateRequestAt) {
            return false;
         }

         this.updating = true;
         this.nextUpdateRequestAt = now + 30000L;
      }

      MeteorExecutor.execute(this::updateInfo);
      return true;
   }

   public void updateInfo() {
      this.updating = true;
      HttpResponse<Friend.APIResponse> res = null;
      if (this.id != null) {
         res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + UndashedUuid.toString(this.id))
            .exceptionHandler(var1x -> MeteorClient.LOG.error("Error while trying to connect session server for friend '{}'", this.name))
            .sendJsonResponse(Friend.APIResponse.class);
      }

      if (res == null || res.statusCode() != 200) {
         res = Http.get("https://api.mojang.com/users/profiles/minecraft/" + this.name)
            .exceptionHandler(var1x -> MeteorClient.LOG.error("Error while trying to update info for friend '{}'", this.name))
            .sendJsonResponse(Friend.APIResponse.class);
      }

      if (res != null && res.statusCode() == 200) {
         this.name = res.body().name;
         this.id = UndashedUuid.fromStringLenient(res.body().id);
         byte[] head = PlayerHeadUtils.fetchHead(this.id);
         MeteorClient.mc.execute(() -> {
            if (head != null) {
               this.headTexture = new PlayerHeadTexture(head, true);
            }
         });
      } else if (!(res instanceof FailedHttpResponse)) {
         this.id = null;
      }

      this.updating = false;
   }

   public boolean headTextureNeedsUpdate() {
      return !this.updating && this.headTexture == null;
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putString("name", this.name);
      if (this.id != null) {
         tag.putString("id", UndashedUuid.toString(this.id));
      }

      return tag;
   }

   public Friend fromTag(CompoundTag tag) {
      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Friend friend = (Friend)o;
         return Objects.equals(this.name, friend.name);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.name);
   }

   public int compareTo(@NotNull Friend friend) {
      return this.name.compareToIgnoreCase(friend.name);
   }

   private static class APIResponse {
      String name;
      String id;
   }
}
