package meteordevelopment.meteorclient.utils.entity.fakeplayer;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.mixin.AbstractClientPlayerAccessor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class FakePlayerEntity extends RemotePlayer {
   public boolean doNotPush;
   public boolean hideWhenInsideCamera;
   public boolean noHit;

   public FakePlayerEntity(Player player, String name, float health, boolean copyInv) {
      super(MeteorClient.mc.level, new GameProfile(UUID.randomUUID(), name));
      this.copyPosition(player);
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.yHeadRot = player.yHeadRot;
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = player.yBodyRot;
      this.yBodyRotO = this.yBodyRot;
      this.getAttributes().assignAllValues(player.getAttributes());
      this.setPose(player.getPose());
      if (health <= 20.0F) {
         this.setHealth(health);
      } else {
         this.setHealth(health);
         this.setAbsorptionAmount(health - 20.0F);
      }

      if (copyInv) {
         this.getInventory().replaceWith(player.getInventory());
      }
   }

   public void spawn() {
      this.unsetRemoved();
      MeteorClient.mc.level.addEntity(this);
   }

   public void despawn() {
      MeteorClient.mc.level.removeEntity(this.getId(), RemovalReason.DISCARDED);
      this.setRemoved(RemovalReason.DISCARDED);
   }

   @Nullable
   @Override
   public PlayerInfo getPlayerInfo() {
      PlayerInfo entry = super.getPlayerInfo();
      if (entry == null) {
         ((AbstractClientPlayerAccessor)this).meteor$setPlayerInfo(MeteorClient.mc.getConnection().getPlayerInfo(MeteorClient.mc.player.getUUID()));
      }

      return entry;
   }
}
