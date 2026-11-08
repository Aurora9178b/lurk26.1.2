package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.ResourceLoadStateTracker.ReloadState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLoadStateTracker.class)
public interface ResourceLoadStateTrackerAccessor {
   @Accessor("reloadState")
   ReloadState meteor$getReloadState();
}
