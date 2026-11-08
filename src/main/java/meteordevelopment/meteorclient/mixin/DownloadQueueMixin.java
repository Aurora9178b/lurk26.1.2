package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import java.nio.file.Path;
import java.util.UUID;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.server.packs.DownloadQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DownloadQueue.class)
public abstract class DownloadQueueMixin {
   @Shadow
   @Final
   private Path cacheDir;

   @ModifyExpressionValue(
      method = "lambda$runDownload$0",
      at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;")
   )
   private Path hookResolve(Path original, @Local(argsOnly = true, name = "id") UUID id) {
      UUID accountId = MeteorClient.mc.getUser().getProfileId();
      if (accountId == null) {
         MeteorClient.LOG.warn("Failed to change resource pack download directory because the account id is null.");
         return original;
      } else {
         return this.cacheDir.resolve(accountId.toString()).resolve(id.toString());
      }
   }
}
