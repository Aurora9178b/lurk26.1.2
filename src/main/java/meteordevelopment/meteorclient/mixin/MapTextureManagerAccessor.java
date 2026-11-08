package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.client.resources.MapTextureManager.MapInstance;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MapTextureManager.class)
public interface MapTextureManagerAccessor {
   @Invoker("getOrCreateMapInstance")
   MapInstance meteor$invokeGetOrCreateMapInstance(MapId var1, MapItemSavedData var2);
}
