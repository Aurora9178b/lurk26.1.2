package meteordevelopment.meteorclient.systems.modules.misc.swarm;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Util;

public class Swarm extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Swarm.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("What type of client to run."))
               .defaultValue(Swarm.Mode.Host))
            .build()
      );
   private final Setting<String> ipAddress = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("ip")
            .description("The IP address of the host server.")
            .defaultValue("localhost")
            .visible(() -> this.mode.get() == Swarm.Mode.Worker)
            .build()
      );
   private final Setting<Integer> serverPort = this.sgGeneral
      .add(new IntSetting.Builder().name("port").description("The port used for connections.").defaultValue(6969).range(1, 65535).noSlider().build());
   public SwarmHost host;
   public SwarmWorker worker;

   public Swarm() {
      super(Categories.Misc, "swarm", "Allows you to control multiple instances of Meteor from one central host.");
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      WHorizontalList b = list.add(theme.horizontalList()).expandX().widget();
      WButton start = b.add(theme.button("Start")).expandX().widget();
      start.action = () -> {
         if (this.isActive()) {
            this.close();
            if (this.mode.get() == Swarm.Mode.Host) {
               this.host = new SwarmHost(this.serverPort.get());
            } else {
               this.worker = new SwarmWorker(this.ipAddress.get(), this.serverPort.get());
            }
         }
      };
      WButton stop = b.add(theme.button("Stop")).expandX().widget();
      stop.action = this::close;
      WButton guide = list.add(theme.button("Guide")).expandX().widget();
      guide.action = () -> Util.getPlatform().openUri("https://github.com/MeteorDevelopment/meteor-client/wiki/Swarm-Guide");
      return list;
   }

   @Override
   public String getInfoString() {
      return this.mode.get().name();
   }

   @Override
   public void onActivate() {
      this.close();
   }

   @Override
   public void onDeactivate() {
      this.close();
   }

   public void close() {
      try {
         if (this.host != null) {
            this.host.disconnect();
            this.host = null;
         }

         if (this.worker != null) {
            this.worker.disconnect();
            this.worker = null;
         }
      } catch (Exception var2) {
      }
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.toggle();
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.toggle();
   }

   @Override
   public void toggle() {
      this.close();
      super.toggle();
   }

   public boolean isHost() {
      return this.mode.get() == Swarm.Mode.Host && this.host != null && !this.host.isInterrupted();
   }

   public boolean isWorker() {
      return this.mode.get() == Swarm.Mode.Worker && this.worker != null && !this.worker.isInterrupted();
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.isWorker()) {
         this.worker.tick();
      }
   }

   public enum Mode {
      Host,
      Worker;
   }
}
