package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.DataResult;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ComponentMapArgumentType;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.component.DataComponentPatch.Builder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.item.ItemStack;

public class NbtCommand extends Command {
   private static final DynamicCommandExceptionType MALFORMED_ITEM_EXCEPTION = new DynamicCommandExceptionType(
      error -> Component.translatableEscape("arguments.item.malformed", error)
   );
   private final MutableComponent copyButton = Component.literal("NBT")
      .setStyle(
         Style.EMPTY
            .applyFormat(ChatFormatting.UNDERLINE)
            .withClickEvent(new MeteorClickEvent(this.toString("copy")))
            .withHoverEvent(new ShowText(Component.literal("Copy the NBT data to your clipboard.")))
      );

   public NbtCommand() {
      super("nbt", "Modifies NBT data for an item, example: .nbt add {display:{Name:'{\"text\":\"$cRed Name\"}'}}");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("add").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
         ItemStack stack = mc.player.getInventory().getSelectedItem();
         if (this.validBasic(stack)) {
            DataComponentMap itemComponents = stack.getComponents();
            DataComponentMap newComponents = ComponentMapArgumentType.getComponentMap(ctx, "component");
            DataComponentMap testComponents = DataComponentMap.composite(itemComponents, newComponents);
            ItemStack testStack = stack.copy();
            testStack.applyComponents(testComponents);
            DataResult<ItemStack> dataResult = ItemStack.validateStrict(testStack);
            dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);
            stack.applyComponents(testComponents);
            this.setStack(stack);
         }

         return 1;
      })));
      builder.then(literal("set").then(argument("component", ComponentMapArgumentType.componentMap(REGISTRY_ACCESS)).executes(ctx -> {
         ItemStack stack = mc.player.getInventory().getSelectedItem();
         if (this.validBasic(stack)) {
            DataComponentMap components = ComponentMapArgumentType.getComponentMap(ctx, "component");
            PatchedDataComponentMap stackComponents = (PatchedDataComponentMap)stack.getComponents();
            ItemStack testStack = stack.copy();
            testStack.applyComponents(components);
            DataResult<ItemStack> dataResult = ItemStack.validateStrict(testStack);
            dataResult.getOrThrow(MALFORMED_ITEM_EXCEPTION::create);
            Builder changesBuilder = DataComponentPatch.builder();
            Set<DataComponentType<?>> types = new HashSet<>(stackComponents.keySet());

            for (TypedDataComponent<?> entry : components) {
               changesBuilder.set(entry);
               types.remove(entry.type());
            }

            for (DataComponentType<?> type : types) {
               changesBuilder.remove(type);
            }

            stackComponents.applyPatch(changesBuilder.build());
            this.setStack(stack);
         }

         return 1;
      })));
      builder.then(
         literal("remove")
            .then(
               argument("component", ResourceKeyArgument.key(Registries.DATA_COMPONENT_TYPE))
                  .executes(ctx -> {
                     ItemStack stack = mc.player.getInventory().getSelectedItem();
                     if (this.validBasic(stack)) {
                        ResourceKey<DataComponentType<?>> componentTypeKey = ctx.getArgument("component", ResourceKey.class);
                        DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(componentTypeKey);
                        PatchedDataComponentMap components = (PatchedDataComponentMap)stack.getComponents();
                        components.applyPatch(DataComponentPatch.builder().remove(componentType).build());
                        this.setStack(stack);
                     }

                     return 1;
                  })
                  .suggests(
                     (var0, suggestionsBuilder) -> {
                        ItemStack stack = mc.player.getInventory().getSelectedItem();
                        if (stack != ItemStack.EMPTY) {
                           DataComponentMap components = stack.getComponents();
                           String remaining = suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT);
                           SharedSuggestionProvider.filterResources(
                              components.keySet().stream().map(BuiltInRegistries.DATA_COMPONENT_TYPE::wrapAsHolder).toList(),
                              remaining,
                              entry -> entry.unwrapKey().isPresent() ? entry.unwrapKey().get().identifier() : null,
                              entry -> {
                                 DataComponentType<?> dataComponentType = entry.value();
                                 if (dataComponentType.codec() != null && entry.unwrapKey().isPresent()) {
                                    suggestionsBuilder.suggest(entry.unwrapKey().get().identifier().toString());
                                 }
                              }
                           );
                        }

                        return suggestionsBuilder.buildFuture();
                     }
                  )
            )
      );
      builder.then(literal("get").executes(var1x -> {
         DataAccessor dataCommandObject = new EntityDataAccessor(mc.player);
         NbtPath handPath = NbtPath.of("SelectedItem");
         MutableComponent text = Component.empty().append(this.copyButton);

         try {
            List<Tag> nbtElement = handPath.get(dataCommandObject.getData());
            if (!nbtElement.isEmpty()) {
               text.append(" ").append(NbtUtils.toPrettyComponent(nbtElement.getFirst()));
            }
         } catch (CommandSyntaxException var6) {
            text.append("{}");
         }

         this.info(text);
         return 1;
      }));
      builder.then(literal("copy").executes(var1x -> {
         DataAccessor dataCommandObject = new EntityDataAccessor(mc.player);
         NbtPath handPath = NbtPath.of("SelectedItem");
         MutableComponent text = Component.empty().append(this.copyButton);
         String nbt = "{}";

         try {
            List<Tag> nbtElement = handPath.get(dataCommandObject.getData());
            if (!nbtElement.isEmpty()) {
               text.append(" ").append(NbtUtils.toPrettyComponent(nbtElement.getFirst()));
               nbt = nbtElement.getFirst().toString();
            }
         } catch (CommandSyntaxException var7) {
            text.append("{}");
         }

         mc.keyboardHandler.setClipboard(nbt);
         text.append(" data copied!");
         this.info(text);
         return 1;
      }));
      builder.then(literal("count").then(argument("count", IntegerArgumentType.integer(-127, 127)).executes(context -> {
         ItemStack stack = mc.player.getInventory().getSelectedItem();
         if (this.validBasic(stack)) {
            int count = IntegerArgumentType.getInteger(context, "count");
            stack.setCount(count);
            this.setStack(stack);
            this.info("Set mainhand stack count to %s.", count);
         }

         return 1;
      })));
   }

   private void setStack(ItemStack stack) {
      mc.player.connection.send(new ServerboundSetCreativeModeSlotPacket(36 + mc.player.getInventory().getSelectedSlot(), stack));
   }

   private boolean validBasic(ItemStack stack) {
      if (!mc.player.getAbilities().instabuild) {
         this.error("Creative mode only.");
         return false;
      } else if (stack == ItemStack.EMPTY) {
         this.error("You must hold an item in your main hand.");
         return false;
      } else {
         return true;
      }
   }
}
