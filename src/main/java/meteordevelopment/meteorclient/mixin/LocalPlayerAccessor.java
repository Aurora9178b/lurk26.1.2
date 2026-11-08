package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
   @Accessor("positionReminder")
   int meteor$getPositionReminder();

   @Accessor("positionReminder")
   void meteor$setPositionReminder(int var1);

   @Accessor("wasSprinting")
   void meteor$setWasSprinting(boolean var1);

   @Accessor("lastSentInput")
   void meteor$setLastSentInput(Input var1);
}
