package meteordevelopment.meteorclient.systems.modules.render;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class WaypointsModule extends Module {
   private static final Color GRAY = new Color(200, 200, 200);
   private static final Color TEXT = new Color(255, 255, 255);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgDeathPosition = this.settings.createGroup("Death Position");
   public final Setting<Integer> textRenderDistance = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("text-render-distance")
            .description("Maximum distance from the center of the screen at which text will be rendered.")
            .defaultValue(100)
            .min(0)
            .sliderMax(200)
            .build()
      );
   private final Setting<Integer> waypointFadeDistance = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("waypoint-fade-distance")
            .description("The distance to a waypoint at which it begins to start fading.")
            .defaultValue(20)
            .sliderRange(0, 100)
            .min(0)
            .build()
      );
   private final Setting<Integer> maxDeathPositions = this.sgDeathPosition
      .add(
         new IntSetting.Builder()
            .name("max-death-positions")
            .description("The amount of death positions to save, 0 to disable")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .onChanged(this::cleanDeathWPs)
            .build()
      );
   private final Setting<Boolean> dpChat = this.sgDeathPosition
      .add(new BoolSetting.Builder().name("chat").description("Send a chat message with your position once you die").defaultValue(false).build());
   private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

   public WaypointsModule() {
      super(Categories.Render, "waypoints", "Allows you to create waypoints.");
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      TextRenderer text = TextRenderer.get();
      Vector3d center = new Vector3d(this.mc.getWindow().getWidth() / 2.0, this.mc.getWindow().getHeight() / 2.0, 0.0);
      int textRenderDist = this.textRenderDistance.get();
      List<Waypoint> toRemove = new ArrayList<>();

      for (Waypoint waypoint : Waypoints.get()) {
         if (waypoint.visible.get() && Waypoints.checkDimension(waypoint)) {
            BlockPos blockPos = waypoint.getPos();
            Vector3d pos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            double dist = PlayerUtils.distanceToCamera(pos.x, pos.y, pos.z);
            boolean playerAlive = this.mc.player != null && !this.mc.player.isDeadOrDying();
            boolean waypointIsNear = waypoint.actionWhenNearCheck((int)Math.floor(dist));
            if (playerAlive && waypointIsNear) {
               switch ((Waypoint.NearAction)waypoint.actionWhenNear.get()) {
                  case Hide:
                     waypoint.visible.set(false);
                     break;
                  case Delete:
                     toRemove.add(waypoint);
                     continue;
               }
            }

            if (!(dist > waypoint.maxVisible.get().intValue()) && NametagUtils.to2D(pos, waypoint.scale.get() - 0.2)) {
               double distToCenter = pos.distance(center);
               double a = 1.0;
               if (dist < this.waypointFadeDistance.get().intValue()) {
                  a = (dist - this.waypointFadeDistance.get().intValue() / 2.0) / (this.waypointFadeDistance.get().intValue() / 2.0);
                  if (a < 0.01) {
                     continue;
                  }
               }

               NametagUtils.begin(pos);
               waypoint.renderIcon(-16.0, -16.0, a, 32.0);
               if (distToCenter <= textRenderDist) {
                  int preTextA = TEXT.a;
                  TEXT.a = (int)(TEXT.a * a);
                  text.begin();
                  text.render(waypoint.name.get(), -text.getWidth(waypoint.name.get()) / 2.0, -16.0 - text.getHeight(), TEXT, true);
                  String distText = String.format("%d blocks", (int)Math.round(dist));
                  text.render(distText, -text.getWidth(distText) / 2.0, 16.0, TEXT, true);
                  text.end();
                  TEXT.a = preTextA;
               }

               NametagUtils.end();
            }
         }
      }

      Waypoints.get().removeAll(toRemove);
   }

   @EventHandler
   private void onOpenScreen(OpenScreenEvent event) {
      if (event.screen instanceof DeathScreen) {
         if (!event.isCancelled()) {
            this.addDeath(this.mc.player.position());
         }
      }
   }

   public void addDeath(Vec3 deathPos) {
      String time = this.dateFormat.format(new Date());
      if (this.dpChat.get()) {
         MutableComponent text = Component.literal("Died at ");
         text.append(ChatUtils.formatCoords(deathPos));
         text.append(String.format(" on %s.", time));
         this.info(text);
      }

      if (this.maxDeathPositions.get() > 0) {
         Waypoint waypoint = new Waypoint.Builder()
            .name("Death " + time)
            .icon("skull")
            .pos(BlockPos.containing(deathPos).above(2))
            .dimension(PlayerUtils.getDimension())
            .build();
         waypoint.actionWhenNear.set(Waypoint.NearAction.Delete);
         waypoint.actionWhenNearDistance.set(4);
         Waypoints.get().add(waypoint);
      }

      this.cleanDeathWPs(this.maxDeathPositions.get());
   }

   private void cleanDeathWPs(int max) {
      int oldWpC = 0;
      List<Waypoint> toRemove = new ArrayList<>();

      for (Waypoint wp : Waypoints.get()) {
         if (wp.name.get().startsWith("Death ") && wp.icon.get().equals("skull")) {
            if (++oldWpC > max) {
               toRemove.add(wp);
            }
         }
      }

      Waypoints.get().removeAll(toRemove);
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      if (!Utils.canUpdate()) {
         return theme.label("You need to be in a world.");
      }

      WTable table = theme.table();
      this.initTable(theme, table);
      return table;
   }

   private void initTable(GuiTheme theme, WTable table) {
      table.clear();

      for (Waypoint waypoint : Waypoints.get()) {
         boolean validDim = Waypoints.checkDimension(waypoint);
         table.add(new WaypointsModule.WIcon(waypoint));
         WLabel name = table.add(theme.label(waypoint.name.get())).expandCellX().widget();
         if (!validDim) {
            name.color = GRAY;
         }

         WCheckbox visible = table.add(theme.checkbox(waypoint.visible.get())).widget();
         visible.action = () -> {
            waypoint.visible.set(visible.checked);
            Waypoints.get().save();
         };
         WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
         edit.action = () -> this.mc.setScreen(new WaypointsModule.EditWaypointScreen(theme, waypoint, () -> this.initTable(theme, table)));
         if (validDim) {
            WButton gotoB = table.add(theme.button("Goto")).widget();
            gotoB.action = () -> {
               if (PathManagers.get().isPathing()) {
                  PathManagers.get().stop();
               }

               PathManagers.get().moveTo(waypoint.getPos());
            };
         }

         WConfirmedMinus remove = table.add(theme.confirmedMinus()).widget();
         remove.action = () -> {
            Waypoints.get().remove(waypoint);
            this.initTable(theme, table);
         };
         table.row();
      }

      table.add(theme.horizontalSeparator()).expandX();
      table.row();
      WButton create = table.add(theme.button("Create")).expandX().widget();
      create.action = () -> this.mc.setScreen(new WaypointsModule.EditWaypointScreen(theme, null, () -> this.initTable(theme, table)));
   }

   private static class EditWaypointScreen extends EditSystemScreen<Waypoint> {
      public EditWaypointScreen(GuiTheme theme, Waypoint value, Runnable reload) {
         super(theme, value, reload);
      }

      public Waypoint create() {
         return new Waypoint.Builder().pos(Minecraft.getInstance().player.blockPosition().above(2)).dimension(PlayerUtils.getDimension()).build();
      }

      @Override
      public boolean save() {
         if (this.value.name.get().isBlank()) {
            return false;
         }

         Waypoints.get().add(this.value);
         return true;
      }

      @Override
      public Settings getSettings() {
         return this.value.settings;
      }
   }

   private static class WIcon extends WWidget {
      private final Waypoint waypoint;

      public WIcon(Waypoint waypoint) {
         this.waypoint = waypoint;
      }

      @Override
      protected void onCalculateSize() {
         double s = this.theme.scale(32.0);
         this.width = s;
         this.height = s;
      }

      @Override
      protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
         renderer.post(() -> this.waypoint.renderIcon(this.x, this.y, 1.0, this.width));
      }
   }
}
