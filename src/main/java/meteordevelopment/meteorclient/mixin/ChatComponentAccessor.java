package meteordevelopment.meteorclient.mixin;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage.Line;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
   @Accessor("trimmedMessages")
   List<Line> meteor$getTrimmedMessages();

   @Accessor("allMessages")
   List<GuiMessage> meteor$getAllMessages();
}
