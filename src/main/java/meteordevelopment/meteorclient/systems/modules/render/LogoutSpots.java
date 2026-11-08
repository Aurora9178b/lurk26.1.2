package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3d;

public class LogoutSpots extends Module {
   private static final Color GREEN = new Color(25, 225, 25);
   private static final Color ORANGE = new Color(225, 105, 25);
   private static final Color RED = new Color(225, 25, 25);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("The scale.").defaultValue(1.0).min(0.0).build());
   private final Setting<Boolean> fullHeight = this.sgGeneral
      .add(new BoolSetting.Builder().name("full-height").description("Displays the height as the player's full height.").defaultValue(true).build());
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 0, 255, 55)).build());
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 0, 255)).build());
   private final Setting<SettingColor> nameColor = this.sgRender
      .add(new ColorSetting.Builder().name("name-color").description("The name color.").defaultValue(new SettingColor(255, 255, 255)).build());
   private final Setting<SettingColor> nameBackgroundColor = this.sgRender
      .add(
         new ColorSetting.Builder().name("name-background-color").description("The name background color.").defaultValue(new SettingColor(0, 0, 0, 75)).build()
      );
   private final List<LogoutSpots.Entry> players = new ArrayList<>();
   private final List<PlayerInfo> lastPlayerList = new ArrayList<>();
   private final List<Player> lastPlayers = new ArrayList<>();
   private int timer;
   private DimensionType lastDimension;
   private static final Vector3d pos = new Vector3d();

   public LogoutSpots() {
      super(Categories.Render, "logout-spots", "Displays a box where another player has logged out at.");
      this.lineColor.onChanged();
   }

   @Override
   public void onActivate() {
      this.lastPlayerList.addAll(this.mc.getConnection().getOnlinePlayers());
      this.updateLastPlayers();
      this.timer = 10;
      this.lastDimension = this.mc.level.dimensionType();
   }

   @Override
   public void onDeactivate() {
      this.players.clear();
      this.lastPlayerList.clear();
   }

   private void updateLastPlayers() {
      this.lastPlayers.clear();

      for (Entity entity : this.mc.level.entitiesForRendering()) {
         if (entity instanceof Player player) {
            this.lastPlayers.add(player);
         }
      }
   }

   @EventHandler
   private void onEntityAdded(EntityAddedEvent event) {
      if (event.entity instanceof Player) {
         int toRemove = -1;

         for (int i = 0; i < this.players.size(); i++) {
            if (this.players.get(i).uuid.equals(event.entity.getUUID())) {
               toRemove = i;
               break;
            }
         }

         if (toRemove != -1) {
            this.players.remove(toRemove);
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.getConnection().getOnlinePlayers().size() != this.lastPlayerList.size()) {
         for (PlayerInfo entry : this.lastPlayerList) {
            if (!this.mc.getConnection().getOnlinePlayers().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile()))) {
               for (Player player : this.lastPlayers) {
                  if (player.getUUID().equals(entry.getProfile().id())) {
                     this.add(new LogoutSpots.Entry(player));
                  }
               }
            }
         }

         this.lastPlayerList.clear();
         this.lastPlayerList.addAll(this.mc.getConnection().getOnlinePlayers());
         this.updateLastPlayers();
      }

      if (this.timer <= 0) {
         this.updateLastPlayers();
         this.timer = 10;
      } else {
         this.timer--;
      }

      DimensionType dimension = this.mc.level.dimensionType();
      if (dimension != this.lastDimension) {
         this.players.clear();
      }

      this.lastDimension = dimension;
   }

   private void add(LogoutSpots.Entry entry) {
      this.players.removeIf(player -> player.uuid.equals(entry.uuid));
      this.players.add(entry);
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      for (LogoutSpots.Entry player : this.players) {
         player.render3D(event);
      }
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      for (LogoutSpots.Entry player : this.players) {
         player.render2D();
      }
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.players.size());
   }

   private class Entry {
      public final double x;
      public final double y;
      public final double z;
      public final double xWidth;
      public final double zWidth;
      public final double halfWidth;
      public final double height;
      public final UUID uuid;
      public final String name;
      public final int health;
      public final int maxHealth;
      public final String healthText;

      public Entry(Player entity) {
         this.halfWidth = entity.getBbWidth() / 2.0F;
         this.x = entity.getX() - this.halfWidth;
         this.y = entity.getY();
         this.z = entity.getZ() - this.halfWidth;
         this.xWidth = entity.getBoundingBox().getXsize();
         this.zWidth = entity.getBoundingBox().getZsize();
         this.height = entity.getBoundingBox().getYsize();
         this.uuid = entity.getUUID();
         this.name = entity.getName().getString();
         this.health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
         this.maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());
         this.healthText = " " + this.health;
      }

      public void render3D(Render3DEvent event) {
         if (LogoutSpots.this.fullHeight.get()) {
            event.renderer
               .box(
                  this.x,
                  this.y,
                  this.z,
                  this.x + this.xWidth,
                  this.y + this.height,
                  this.z + this.zWidth,
                  LogoutSpots.this.sideColor.get(),
                  LogoutSpots.this.lineColor.get(),
                  LogoutSpots.this.shapeMode.get(),
                  0
               );
         } else {
            event.renderer
               .sideHorizontal(
                  this.x,
                  this.y,
                  this.z,
                  this.x + this.xWidth,
                  this.z,
                  LogoutSpots.this.sideColor.get(),
                  LogoutSpots.this.lineColor.get(),
                  LogoutSpots.this.shapeMode.get()
               );
         }
      }

      public void render2D() {
         if (PlayerUtils.isWithinCamera(this.x, this.y, this.z, LogoutSpots.this.mc.options.renderDistance().get() * 16)) {
            TextRenderer text = TextRenderer.get();
            double scale = LogoutSpots.this.scale.get();
            LogoutSpots.pos.set(this.x + this.halfWidth, this.y + this.height + 0.5, this.z + this.halfWidth);
            if (NametagUtils.to2D(LogoutSpots.pos, scale)) {
               NametagUtils.begin(LogoutSpots.pos);
               double healthPercentage = (double)this.health / this.maxHealth;
               Color healthColor;
               if (healthPercentage <= 0.333) {
                  healthColor = LogoutSpots.RED;
               } else if (healthPercentage <= 0.666) {
                  healthColor = LogoutSpots.ORANGE;
               } else {
                  healthColor = LogoutSpots.GREEN;
               }

               double i = text.getWidth(this.name) / 2.0 + text.getWidth(this.healthText) / 2.0;
               Renderer2D.COLOR.begin();
               Renderer2D.COLOR.quad(-i, 0.0, i * 2.0, text.getHeight(), LogoutSpots.this.nameBackgroundColor.get());
               Renderer2D.COLOR.render();
               text.beginBig();
               double hX = text.render(this.name, -i, 0.0, LogoutSpots.this.nameColor.get());
               text.render(this.healthText, hX, 0.0, healthColor);
               text.end();
               NametagUtils.end();
            }
         }
      }
   }
}
