package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;

public class PeekCommand extends Command {
   private static final ItemStack[] ITEMS = new ItemStack[27];
   private static final SimpleCommandExceptionType CANT_PEEK = new SimpleCommandExceptionType(
      Component.literal("You must be holding a storage block or looking at an item frame.")
   );

   public PeekCommand() {
      super("peek", "Lets you see what's inside storage block items.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.executes(var0 -> {
         if (Utils.openContainer(mc.player.getMainHandItem(), ITEMS, true)) {
            return 1;
         } else if (Utils.openContainer(mc.player.getOffhandItem(), ITEMS, true)) {
            return 1;
         } else if (mc.crosshairPickEntity instanceof ItemFrame itemFrame && Utils.openContainer(itemFrame.getItem(), ITEMS, true)) {
            return 1;
         } else {
            throw CANT_PEEK.create();
         }
      });
   }
}
