package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.client.GoToSetting;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.phys.Vec3;

public class GoToCommand extends Command {
   private static final double ARRIVAL_DISTANCE = 0.75;
   private static final int ARRIVAL_CONFIRM_TICKS = 5;
   private Vec3 targetPos;
   private long lastTpTime;
   private int arrivalTicks;

   public GoToCommand() {
      super("goto", "Teleports toward a target X/Z position using GoToSetting.");
      MeteorClient.EVENT_BUS.subscribe(this);
   }

   @Override
   public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
      builder.then(literal("stop").executes(context -> {
         this.stop();
         this.info("Stopped.");
         return 1;
      }));
      builder.then(argument("x", DoubleArgumentType.doubleArg()).then(argument("z", DoubleArgumentType.doubleArg()).executes(context -> {
         double x = DoubleArgumentType.getDouble(context, "x");
         double z = DoubleArgumentType.getDouble(context, "z");
         GoToSetting setting = GoToSetting.get();
         if (setting == null) {
            this.error("GoToSetting is not loaded.");
            return 1;
         } else {
            this.start(x, z);
            this.info("Going to %.1f %.1f.", x, z);
            return 1;
         }
      })));
   }

   private void start(double x, double z) {
      this.targetPos = new Vec3(x, mc.player.getY(), z);
      this.lastTpTime = 0L;
      this.arrivalTicks = 0;
   }

   private void stop() {
      this.targetPos = null;
      this.lastTpTime = 0L;
      this.arrivalTicks = 0;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.targetPos != null) {
         if (mc.player != null && mc.level != null && mc.getConnection() != null) {
            GoToSetting setting = GoToSetting.get();
            int delay = setting == null ? 40 : setting.getDelay();
            Vec3 from = mc.player.position();
            Vec3 to = new Vec3(this.targetPos.x, from.y, this.targetPos.z);
            if (from.distanceTo(to) <= 0.75) {
               if (++this.arrivalTicks >= 5) {
                  this.stop();
               }
            } else {
               this.arrivalTicks = 0;
               if (System.currentTimeMillis() - this.lastTpTime >= delay) {
                  if (from.distanceTo(to) <= 0.001) {
                     this.stop();
                  } else {
                     this.teleport(from, to, mc.player.onGround());
                     mc.player.setPos(to);
                     this.lastTpTime = System.currentTimeMillis();
                  }
               }
            }
         } else {
            this.stop();
         }
      }
   }

   private void teleport(Vec3 from, Vec3 to, boolean onGround) {
      double distance = from.distanceTo(to);
      if (!(distance <= 0.001)) {
         int steps = (int)Math.ceil(distance / 10.0);
         steps = Math.max(1, steps);
         TpSetting setting = TpSetting.get();
         boolean useLtp = setting == null || setting.isLTPUtils();

         for (int i = 0; i < steps; i++) {
            double progress = (i + 1.0) / steps;
            Vec3 stepPos = from.lerp(to, progress);
            if (useLtp) {
               LTpUtils.sendMovePacket(stepPos, onGround);
            } else {
               QTPUtils.sendMovePacket(stepPos, onGround);
            }
         }
      }
   }
}
