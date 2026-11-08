package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.BlockPosArgumentType;
import meteordevelopment.meteorclient.commands.arguments.WaypointArgumentType;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.BlockPos;

public class WaypointCommand extends Command {
   public WaypointCommand() {
      super("waypoint", "Manages waypoints.", "wp");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(
         literal("list")
            .executes(
               var1x -> {
                  if (Waypoints.get().isEmpty()) {
                     this.error("No created waypoints.");
                  } else {
                     this.info(ChatFormatting.WHITE + "Created Waypoints:");

                     for (Waypoint waypoint : Waypoints.get()) {
                        this.info(
                           "Name: (highlight)'%s'(default), Dimension: (highlight)%s(default), Pos: (highlight)%s(default)",
                           waypoint.name.get(),
                           waypoint.dimension.get(),
                           this.waypointPos(waypoint)
                        );
                     }
                  }

                  return 1;
               }
            )
      );
      builder.then(literal("get").then(argument("waypoint", WaypointArgumentType.create()).executes(context -> {
         Waypoint waypoint = WaypointArgumentType.get(context);
         this.info("Name: " + ChatFormatting.WHITE + waypoint.name.get());
         this.info("Actual Dimension: " + ChatFormatting.WHITE + waypoint.dimension.get());
         this.info("Position: " + ChatFormatting.WHITE + this.waypointFullPos(waypoint));
         this.info("Visible: " + (waypoint.visible.get() ? ChatFormatting.GREEN + "True" : ChatFormatting.RED + "False"));
         return 1;
      })));
      builder.then(
         literal("add")
            .then(
               argument("pos", BlockPosArgumentType.blockPos())
                  .then(argument("waypoint", StringArgumentType.greedyString()).executes(context -> this.addWaypoint(context, true)))
            )
            .then(argument("waypoint", StringArgumentType.greedyString()).executes(context -> this.addWaypoint(context, false)))
      );
      builder.then(literal("delete").then(argument("waypoint", WaypointArgumentType.create()).executes(context -> {
         Waypoint waypoint = WaypointArgumentType.get(context);
         this.info("The waypoint (highlight)'%s'(default) has been deleted.", waypoint.name.get());
         Waypoints.get().remove(waypoint);
         return 1;
      })));
      builder.then(literal("toggle").then(argument("waypoint", WaypointArgumentType.create()).executes(context -> {
         Waypoint waypoint = WaypointArgumentType.get(context);
         waypoint.visible.set(!waypoint.visible.get());
         Waypoints.get().save();
         return 1;
      })));
   }

   private String waypointPos(Waypoint waypoint) {
      return "X: " + waypoint.pos.get().getX() + " Z: " + waypoint.pos.get().getZ();
   }

   private String waypointFullPos(Waypoint waypoint) {
      return "X: " + waypoint.pos.get().getX() + ", Y: " + waypoint.pos.get().getY() + ", Z: " + waypoint.pos.get().getZ();
   }

   private int addWaypoint(CommandContext<ClientSuggestionProvider> context, boolean withCoords) {
      if (mc.player == null) {
         return -1;
      }

      BlockPos pos = withCoords ? BlockPosArgumentType.getBlockPos(context, "pos") : mc.player.blockPosition().above(2);
      Waypoint waypoint = new Waypoint.Builder().name(StringArgumentType.getString(context, "waypoint")).pos(pos).dimension(PlayerUtils.getDimension()).build();
      Waypoints.get().add(waypoint);
      this.info("Created waypoint with name: (highlight)%s(default)", waypoint.name.get());
      return 1;
   }
}
