package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.ResourceLoadStateTracker.ReloadState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ReloadState.class)
public interface ReloadStateAccessor {
   @Accessor("finished")
   boolean meteor$isFinished();
}
