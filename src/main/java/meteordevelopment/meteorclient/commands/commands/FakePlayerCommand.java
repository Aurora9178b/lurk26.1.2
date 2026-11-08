package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.FakePlayerArgumentType;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.FakePlayer;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerManager;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class FakePlayerCommand extends Command {
   public FakePlayerCommand() {
      super("fake-player", "Manages fake players that you can use for testing.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("add").executes(var0 -> {
         FakePlayer fakePlayer = Modules.get().get(FakePlayer.class);
         FakePlayerManager.add(fakePlayer.name.get(), fakePlayer.health.get().intValue(), fakePlayer.copyInv.get());
         return 1;
      }).then(argument("name", StringArgumentType.word()).executes(context -> {
         FakePlayer fakePlayer = Modules.get().get(FakePlayer.class);
         FakePlayerManager.add(StringArgumentType.getString(context, "name"), fakePlayer.health.get().intValue(), fakePlayer.copyInv.get());
         return 1;
      })));
      builder.then(literal("remove").then(argument("fp", FakePlayerArgumentType.create()).executes(context -> {
         FakePlayerEntity fp = FakePlayerArgumentType.get(context);
         if (fp != null && FakePlayerManager.contains(fp)) {
            FakePlayerManager.remove(fp);
            this.info("Removed Fake Player %s.".formatted(fp.getName().getString()));
            return 1;
         } else {
            this.error("Couldn't find a Fake Player with that name.");
            return 1;
         }
      })));
      builder.then(literal("clear").executes(var0 -> {
         FakePlayerManager.clear();
         return 1;
      }));
      builder.then(literal("list").executes(var1x -> {
         this.info("--- Fake Players ((highlight)%s(default)) ---", FakePlayerManager.count());
         FakePlayerManager.forEach(fp -> ChatUtils.info("(highlight)%s".formatted(fp.getName().getString())));
         return 1;
      }));
   }
}
