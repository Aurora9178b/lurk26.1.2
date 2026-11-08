package meteordevelopment.meteorclient.mixin;

import java.util.Optional;
import meteordevelopment.meteorclient.mixininterface.IClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientboundExplodePacket.class)
public abstract class ClientboundExplodePacketMixin implements IClientboundExplodePacket {
   @Shadow
   @Final
   @Mutable
   private Optional<Vec3> playerKnockback;

   @Override
   public void meteor$setVelocityX(float velocity) {
      if (this.playerKnockback.isPresent()) {
         Vec3 kb = this.playerKnockback.get();
         this.playerKnockback = Optional.of(new Vec3(velocity, kb.y, kb.z));
      } else {
         this.playerKnockback = Optional.of(new Vec3(velocity, 0.0, 0.0));
      }
   }

   @Override
   public void meteor$setVelocityY(float velocity) {
      if (this.playerKnockback.isPresent()) {
         Vec3 kb = this.playerKnockback.get();
         this.playerKnockback = Optional.of(new Vec3(kb.x, velocity, kb.z));
      } else {
         this.playerKnockback = Optional.of(new Vec3(0.0, velocity, 0.0));
      }
   }

   @Override
   public void meteor$setVelocityZ(float velocity) {
      if (this.playerKnockback.isPresent()) {
         Vec3 kb = this.playerKnockback.get();
         this.playerKnockback = Optional.of(new Vec3(kb.x, kb.y, velocity));
      } else {
         this.playerKnockback = Optional.of(new Vec3(0.0, 0.0, velocity));
      }
   }
}
