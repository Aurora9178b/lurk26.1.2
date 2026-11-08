package meteordevelopment.meteorclient.mixin;

import java.nio.file.Path;
import net.minecraft.client.resources.SkinManager.TextureCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureCache.class)
public interface FileCacheAccessor {
   @Accessor("root")
   Path meteor$getRoot();
}
