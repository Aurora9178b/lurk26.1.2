package meteordevelopment.meteorclient.mixin;

import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.SkinManager.TextureCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SkinManager.class)
public interface SkinManagerAccessor {
   @Accessor("skinTextures")
   TextureCache meteor$getSkinTextures();
}
