package meteordevelopment.meteorclient.mixin;

import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.mixininterface.IGuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public abstract class GuiMessageMixin implements IGuiMessage {
   @Shadow
   @Final
   private Component content;
   @Unique
   private int id;
   @Unique
   private GameProfile sender;

   @Override
   public String meteor$getText() {
      return this.content.getString();
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
}
