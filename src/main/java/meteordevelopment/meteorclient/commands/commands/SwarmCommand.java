package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.objects.ObjectIntImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.ModuleArgumentType;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.Swarm;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmConnection;
import meteordevelopment.meteorclient.systems.modules.misc.swarm.SwarmWorker;
import meteordevelopment.meteorclient.systems.modules.world.InfinityMiner;
import meteordevelopment.meteorclient.utils.misc.text.MeteorClickEvent;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class SwarmCommand extends Command {
   private static final SimpleCommandExceptionType SWARM_NOT_ACTIVE = new SimpleCommandExceptionType(
      Component.literal("The swarm module must be active to use this command.")
   );
   @Nullable
   private ObjectIntPair<String> pendingConnection;

   public SwarmCommand() {
      super("swarm", "Sends commands to connected swarm workers.");
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("disconnect").executes(var0 -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            swarm.close();
            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }));
      builder.then(
         literal("join")
            .then(
               argument("ip", StringArgumentType.string())
                  .then(
                     argument("port", IntegerArgumentType.integer(0, 65535))
                        .executes(
                           context -> {
                              String ip = StringArgumentType.getString(context, "ip");
                              int port = IntegerArgumentType.getInteger(context, "port");
                              this.pendingConnection = new ObjectIntImmutablePair<>(ip, port);
                              this.info("Are you sure you want to connect to '%s:%s'?", ip, port);
                              this.info(
                                 Component.literal("Click here to confirm")
                                    .setStyle(
                                       Style.EMPTY
                                          .applyFormats(ChatFormatting.UNDERLINE, ChatFormatting.GREEN)
                                          .withClickEvent(new MeteorClickEvent(".swarm join confirm"))
                                    )
                              );
                              return 1;
                           }
                        )
                  )
            )
            .then(literal("confirm").executes(var1x -> {
               if (this.pendingConnection == null) {
                  this.error("No pending swarm connections.");
                  return 1;
               }

               Swarm swarm = Modules.get().get(Swarm.class);
               swarm.enable();
               swarm.close();
               swarm.mode.set(Swarm.Mode.Worker);
               swarm.worker = new SwarmWorker(this.pendingConnection.left(), this.pendingConnection.rightInt());
               this.pendingConnection = null;

               try {
                  this.info("Connected to (highlight)%s.", swarm.worker.getConnection());
               } catch (NullPointerException var4) {
                  this.error("Error connecting to swarm host.");
                  swarm.close();
                  swarm.toggle();
               }

               return 1;
            }))
      );
      builder.then(literal("connections").executes(var1x -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (!swarm.isActive()) {
            throw SWARM_NOT_ACTIVE.create();
         }

         if (swarm.isHost()) {
            if (swarm.host.getConnectionCount() > 0) {
               ChatUtils.info("--- Swarm Connections (highlight)(%s/%s)(default) ---", swarm.host.getConnectionCount(), swarm.host.getConnections().length);

               for (int i = 0; i < swarm.host.getConnections().length; i++) {
                  SwarmConnection connection = swarm.host.getConnections()[i];
                  if (connection != null) {
                     ChatUtils.info("(highlight)Worker %s(default): %s.", i, connection.getConnection());
                  }
               }
            } else {
               this.warning("No active connections");
            }
         } else if (swarm.isWorker()) {
            this.info("Connected to (highlight)%s", swarm.worker.getConnection());
         }

         return 1;
      }));
      builder.then(literal("follow").executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput() + " " + mc.player.getName().getString());
            } else if (swarm.isWorker()) {
               this.error("The follow host command must be used by the host.");
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }).then(argument("player", PlayerArgumentType.create()).executes(context -> {
         Player playerEntity = PlayerArgumentType.get(context);
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker() && playerEntity != null) {
               PathManagers.get().follow(entity -> entity.getName().getString().equalsIgnoreCase(playerEntity.getName().getString()));
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      })));
      builder.then(literal("goto").then(argument("x", IntegerArgumentType.integer()).then(argument("z", IntegerArgumentType.integer()).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               int x = IntegerArgumentType.getInteger(context, "x");
               int z = IntegerArgumentType.getInteger(context, "z");
               PathManagers.get().moveTo(new BlockPos(x, 0, z), true);
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }))));
      builder.then(literal("infinity-miner").executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               this.runInfinityMiner();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }).then(argument("target", BlockStateArgument.block(REGISTRY_ACCESS)).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Modules.get().get(InfinityMiner.class).targetBlocks.set(List.of(context.getArgument("target", BlockInput.class).getState().getBlock()));
               this.runInfinityMiner();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }).then(argument("repair", BlockStateArgument.block(REGISTRY_ACCESS)).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Modules.get().get(InfinityMiner.class).targetBlocks.set(List.of(context.getArgument("target", BlockInput.class).getState().getBlock()));
               Modules.get().get(InfinityMiner.class).repairBlocks.set(List.of(context.getArgument("repair", BlockInput.class).getState().getBlock()));
               this.runInfinityMiner();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }))).then(literal("logout").then(argument("logout", BoolArgumentType.bool()).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Modules.get().get(InfinityMiner.class).logOut.set(BoolArgumentType.getBool(context, "logout"));
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }))).then(literal("walkhome").then(argument("walkhome", BoolArgumentType.bool()).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Modules.get().get(InfinityMiner.class).walkHome.set(BoolArgumentType.getBool(context, "walkhome"));
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }))));
      builder.then(literal("mine").then(argument("block", BlockStateArgument.block(REGISTRY_ACCESS)).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               swarm.worker.target = context.getArgument("block", BlockInput.class).getState().getBlock();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      })));
      builder.then(literal("toggle").then(argument("module", ModuleArgumentType.create()).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Module module = ModuleArgumentType.get(context);
               module.toggle();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }).then(literal("on").executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Module m = ModuleArgumentType.get(context);
               m.enable();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      })).then(literal("off").executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               Module m = ModuleArgumentType.get(context);
               m.disable();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }))));
      builder.then(literal("scatter").executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               this.scatter(100);
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }).then(argument("radius", IntegerArgumentType.integer()).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               this.scatter(IntegerArgumentType.getInteger(context, "radius"));
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      })));
      builder.then(literal("stop").executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               PathManagers.get().stop();
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      }));
      builder.then(literal("exec").then(argument("command", StringArgumentType.greedyString()).executes(context -> {
         Swarm swarm = Modules.get().get(Swarm.class);
         if (swarm.isActive()) {
            if (swarm.isHost()) {
               swarm.host.sendMessage(context.getInput());
            } else if (swarm.isWorker()) {
               ChatUtils.sendPlayerMsg(StringArgumentType.getString(context, "command"));
            }

            return 1;
         } else {
            throw SWARM_NOT_ACTIVE.create();
         }
      })));
   }

   private void runInfinityMiner() {
      InfinityMiner infinityMiner = Modules.get().get(InfinityMiner.class);
      infinityMiner.disable();
      infinityMiner.enable();
   }

   private void scatter(int radius) {
      Random random = new Random();
      double a = random.nextDouble() * 2.0 * Math.PI;
      double r = radius * Math.sqrt(random.nextDouble());
      double x = mc.player.getX() + r * Math.cos(a);
      double z = mc.player.getZ() + r * Math.sin(a);
      PathManagers.get().stop();
      PathManagers.get().moveTo(new BlockPos((int)x, 0, (int)z), true);
   }
}
