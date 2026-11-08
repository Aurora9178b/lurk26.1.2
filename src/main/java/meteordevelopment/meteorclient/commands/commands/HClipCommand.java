package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class HClipCommand extends Command {
   public HClipCommand() {
      super("hclip", "Lets you clip through blocks horizontally.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(argument("blocks", DoubleArgumentType.doubleArg()).executes(context -> {
         double blocks = context.getArgument("blocks", Double.class);
         Vec3 forward = Vec3.directionFromRotation(0.0F, mc.player.getYRot()).normalize();
         if (mc.player.isPassenger()) {
            Entity vehicle = mc.player.getVehicle();
            vehicle.setPos(vehicle.getX() + forward.x * blocks, vehicle.getY(), vehicle.getZ() + forward.z * blocks);
         }

         mc.player.setPos(mc.player.getX() + forward.x * blocks, mc.player.getY(), mc.player.getZ() + forward.z * blocks);
         return 1;
      }));
   }
}
