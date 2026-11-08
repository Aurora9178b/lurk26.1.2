package meteordevelopment.meteorclient.mixin;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
   @Mutable
   @Accessor("x")
   void meteor$setX(double var1);

   @Mutable
   @Accessor("y")
   void meteor$setY(double var1);

   @Mutable
   @Accessor("z")
   void meteor$setZ(double var1);

   @Accessor("yRot")
   float meteor$getYRot();

   @Mutable
   @Accessor("yRot")
   void meteor$setYRot(float var1);

   @Accessor("xRot")
   float meteor$getXRot();

   @Mutable
   @Accessor("xRot")
   void meteor$setXRot(float var1);

   @Mutable
   @Accessor("onGround")
   void meteor$setOnGround(boolean var1);
}
