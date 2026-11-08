package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.mixininterface.IGuiMessageVisible;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Line.class)
public abstract class GuiMessageVisibleMixin implements IGuiMessageVisible {
   @Shadow
   @Final
   private FormattedCharSequence content;
   @Unique
   private int id;
   @Unique
   private GameProfile sender;
   @Unique
   private boolean startOfEntry;

   @Override
   public String meteor$getText() {
      StringBuilder sb = new StringBuilder();
      this.content.accept((var1x, var2, codePoint) -> {
         sb.appendCodePoint(codePoint);
         return true;
      });
      return sb.toString();
   }

   @Override
   public int meteor$getId() {
      return this.id;
   }

   @Override
   public void meteor$setId(int id) {
      this.id = id;
   }

   @Override
   public GameProfile meteor$getSender() {
      return this.sender;
   }

   @Override
   public void meteor$setSender(GameProfile profile) {
      this.sender = profile;
   }

   @Override
   public boolean meteor$isStartOfEntry() {
      return this.startOfEntry;
   }

   @Override
   public void meteor$setStartOfEntry(boolean start) {
      this.startOfEntry = start;
   }
}
