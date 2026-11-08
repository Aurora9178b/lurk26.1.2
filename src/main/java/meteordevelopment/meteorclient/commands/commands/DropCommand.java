package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class DropCommand extends Command {
   private static final SimpleCommandExceptionType NOT_SPECTATOR = new SimpleCommandExceptionType(Component.literal("Can't drop items while in spectator."));
   private static final SimpleCommandExceptionType NO_SUCH_ITEM = new SimpleCommandExceptionType(Component.literal("Could not find an item with that name!"));

   public DropCommand() {
      super("drop", "Automatically drops specified items.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("hand").executes(var1x -> this.drop(player -> player.drop(true))));
      builder.then(literal("offhand").executes(var1x -> this.drop(var0 -> InvUtils.drop().slotOffhand())));
      builder.then(literal("hotbar").executes(var1x -> this.drop(var0 -> {
         for (int i = 0; i < 9; i++) {
            InvUtils.drop().slotHotbar(i);
         }
      })));
      builder.then(literal("inventory").executes(var1x -> this.drop(var0 -> {
         for (int i = 0; i < 27; i++) {
            InvUtils.drop().slotMain(i);
         }
      })));
      builder.then(literal("all").executes(var1x -> this.drop(player -> {
         for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            InvUtils.drop().slot(i);
         }

         if (!mc.player.getOffhandItem().isEmpty()) {
            InvUtils.drop().slotOffhand();
         }
      })));
      builder.then(literal("armor").executes(var1x -> this.drop(var0 -> {
         for (EquipmentSlot equipmentSlot : EquipmentSlotGroup.ARMOR) {
            if (equipmentSlot.getType() == Type.HUMANOID_ARMOR) {
               InvUtils.drop().slotArmor(equipmentSlot.getIndex());
            }
         }
      })));
      builder.then(
         argument("item", ItemArgument.item(REGISTRY_ACCESS))
            .executes(context -> this.drop(player -> this.dropItem(player, context, Integer.MAX_VALUE)))
            .then(argument("amount", IntegerArgumentType.integer(1)).executes(context -> this.drop(player -> {
               int amount = IntegerArgumentType.getInteger(context, "amount");
               this.dropItem(player, context, amount);
            })))
      );
   }

   private void dropItem(LocalPlayer player, CommandContext<ClientSuggestionProvider> context, int amount) throws CommandSyntaxException {
      ItemStack stack = ItemArgument.getItem(context, "item").createItemStack(1);
      if (stack != null && stack.getItem() != Items.AIR) {
         for (int i = 0; i < player.getInventory().getContainerSize() && amount > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty() && stack.getItem() == invStack.getItem()) {
               int dropCount = Math.min(amount, invStack.getCount());
               if (dropCount == invStack.getCount()) {
                  InvUtils.drop().slot(i);
               } else {
                  for (int j = 0; j < dropCount; j++) {
                     InvUtils.dropOne().slot(i);
                  }
               }

               amount -= dropCount;
            }
         }
      } else {
         throw NO_SUCH_ITEM.create();
      }
   }

   private int drop(DropCommand.PlayerConsumer consumer) throws CommandSyntaxException {
      if (mc.player.isSpectator()) {
         throw NOT_SPECTATOR.create();
      }

      consumer.accept(mc.player);
      return 1;
   }

   @FunctionalInterface
   private interface PlayerConsumer {
      void accept(LocalPlayer var1) throws CommandSyntaxException;
   }
}
