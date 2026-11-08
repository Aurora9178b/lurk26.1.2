package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.simulator.ProjectileEntitySimulator;
import meteordevelopment.meteorclient.utils.entity.simulator.SimulationStep;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3d;

public class Trajectories extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<List<Item>> items = this.sgGeneral
      .add(
         new ItemListSetting.Builder()
            .name("items")
            .description("Items to display trajectories for.")
            .defaultValue(this.getDefaultItems())
            .filter(this::itemFilter)
            .build()
      );
   private final Setting<Boolean> otherPlayers = this.sgGeneral
      .add(new BoolSetting.Builder().name("other-players").description("Calculates trajectories for other players.").defaultValue(true).build());
   private final Setting<Boolean> firedProjectiles = this.sgGeneral
      .add(
         new BoolSetting.Builder().name("fired-projectiles").description("Calculates trajectories for already fired projectiles.").defaultValue(false).build()
      );
   private final Setting<Boolean> ignoreWitherSkulls = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("ignore-wither-skulls")
            .description("Whether to ignore fired wither skulls.")
            .defaultValue(false)
            .visible(this.firedProjectiles::get)
            .build()
      );
   private final Setting<Boolean> accurate = this.sgGeneral
      .add(new BoolSetting.Builder().name("accurate").description("Whether or not to calculate more accurate.").defaultValue(false).build());
   public final Setting<Integer> simulationSteps = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("simulation-steps")
            .description("How many steps to simulate projectiles. Zero for no limit")
            .defaultValue(500)
            .sliderMax(5000)
            .build()
      );
   private final Setting<Integer> ignoreFirstTicks = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("ignore-rendering-first-ticks")
            .description("Ignores rendering the first given ticks, to make the rest of the path more visible.")
            .defaultValue(3)
            .min(0)
            .build()
      );
   private final Setting<ShapeMode> shapeMode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRender
      .add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 150, 0, 35)).build());
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 150, 0)).build());
   private final Setting<Boolean> renderPositionBox = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("render-position-boxes")
            .description("Renders the actual position the projectile will be at each tick along it's trajectory.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Double> positionBoxSize = this.sgRender
      .add(
         new DoubleSetting.Builder()
            .name("position-box-size")
            .description("The size of the box drawn at the simulated positions.")
            .defaultValue(0.02)
            .sliderRange(0.01, 0.1)
            .visible(this.renderPositionBox::get)
            .build()
      );
   private final Setting<SettingColor> positionSideColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("position-side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 150, 0, 35))
            .visible(this.renderPositionBox::get)
            .build()
      );
   private final Setting<SettingColor> positionLineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("position-line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 150, 0))
            .visible(this.renderPositionBox::get)
            .build()
      );
   private final ProjectileEntitySimulator simulator = new ProjectileEntitySimulator();
   private final Pool<Vector3d> vec3s = new Pool<>(Vector3d::new);
   private final List<Trajectories.Path> paths = new ArrayList<>();
   private static final double MULTISHOT_OFFSET = Math.toRadians(10.0);

   public Trajectories() {
      super(Categories.Render, "trajectories", "Predicts the trajectory of throwable items.");
   }

   private boolean itemFilter(Item item) {
      return item instanceof ProjectileWeaponItem
         || item instanceof FishingRodItem
         || item instanceof TridentItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof EnderpearlItem
         || item instanceof ExperienceBottleItem
         || item instanceof ThrowablePotionItem
         || item instanceof WindChargeItem;
   }

   private List<Item> getDefaultItems() {
      List<Item> items = new ArrayList<>();

      for (Item item : BuiltInRegistries.ITEM) {
         if (this.itemFilter(item)) {
            items.add(item);
         }
      }

      return items;
   }

   private Trajectories.Path getEmptyPath() {
      for (Trajectories.Path path : this.paths) {
         if (path.points.isEmpty()) {
            return path;
         }
      }

      Trajectories.Path path = new Trajectories.Path();
      this.paths.add(path);
      return path;
   }

   private void calculatePath(Player player, float tickDelta) {
      for (Trajectories.Path path : this.paths) {
         path.clear();
      }

      ItemStack itemStack = player.getMainHandItem();
      if (!this.items.get().contains(itemStack.getItem())) {
         itemStack = player.getOffhandItem();
         if (!this.items.get().contains(itemStack.getItem())) {
            return;
         }
      }

      if (this.simulator.set(player, itemStack, 0.0, this.accurate.get(), tickDelta)) {
         Trajectories.Path p = this.getEmptyPath().calculate();
         if (player == this.mc.player) {
            p.ignoreFirstTicks();
         }

         if (itemStack.getItem() instanceof CrossbowItem && Utils.hasEnchantment(itemStack, Enchantments.MULTISHOT)) {
            if (!this.simulator.set(player, itemStack, MULTISHOT_OFFSET, this.accurate.get(), tickDelta)) {
               return;
            }

            p = this.getEmptyPath().calculate();
            if (player == this.mc.player) {
               p.ignoreFirstTicks();
            }

            if (!this.simulator.set(player, itemStack, -MULTISHOT_OFFSET, this.accurate.get(), tickDelta)) {
               return;
            }

            p = this.getEmptyPath().calculate();
            if (player == this.mc.player) {
               p.ignoreFirstTicks();
            }
         }
      }
   }

   private void calculateFiredPath(Entity entity, double tickDelta) {
      for (Trajectories.Path path : this.paths) {
         path.clear();
      }

      if (this.simulator.set(entity)) {
         this.getEmptyPath().setStart(entity, tickDelta).calculate();
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      float tickDelta = this.mc.level.tickRateManager().isFrozen() ? 1.0F : event.tickDelta;

      for (Player player : this.mc.level.players()) {
         if (this.otherPlayers.get() || player == this.mc.player) {
            this.calculatePath(player, tickDelta);

            for (Trajectories.Path path : this.paths) {
               path.render(event);
            }
         }
      }

      if (this.firedProjectiles.get()) {
         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (entity instanceof Projectile
               && (!this.ignoreWitherSkulls.get() || !(entity instanceof WitherSkull))
               && (!(entity instanceof ThrownTrident trident) || !trident.noPhysics)) {
               this.calculateFiredPath(entity, tickDelta);

               for (Trajectories.Path path : this.paths) {
                  path.render(event);
               }
            }
         }
      }
   }

   private class Path {
      private final List<Vector3d> points = new ArrayList<>();
      private boolean hitQuad;
      private boolean hitQuadHorizontal;
      private double hitQuadX1;
      private double hitQuadY1;
      private double hitQuadZ1;
      private double hitQuadX2;
      private double hitQuadY2;
      private double hitQuadZ2;
      private final List<Entity> collidingEntities = new ArrayList<>();
      public Vector3d lastPoint;
      private int start;

      public void clear() {
         Trajectories.this.vec3s.freeAll(this.points);
         this.points.clear();
         this.hitQuad = false;
         this.collidingEntities.clear();
         this.lastPoint = null;
         this.start = 0;
      }

      public Trajectories.Path calculate() {
         this.addPoint();

         for (int i = 0; i < (Trajectories.this.simulationSteps.get() > 0 ? Trajectories.this.simulationSteps.get() : Integer.MAX_VALUE); i++) {
            SimulationStep result = Trajectories.this.simulator.tick();
            this.processHitResults(result);
            if (result.shouldStop) {
               break;
            }

            this.addPoint();
         }

         return this;
      }

      public Trajectories.Path setStart(Entity entity, double tickDelta) {
         this.lastPoint = new Vector3d(
            Mth.lerp(tickDelta, entity.xOld, entity.getX()), Mth.lerp(tickDelta, entity.yOld, entity.getY()), Mth.lerp(tickDelta, entity.zOld, entity.getZ())
         );
         return this;
      }

      private void addPoint() {
         this.points.add(Trajectories.this.vec3s.get().set(Trajectories.this.simulator.pos));
      }

      private void processHitResults(SimulationStep step) {
         for (int i = 0; i < step.hitResults.length; i++) {
            HitResult result = step.hitResults[i];
            if (result.getType() == Type.BLOCK) {
               BlockHitResult r = (BlockHitResult)result;
               this.hitQuad = true;
               this.hitQuadX1 = r.getLocation().x;
               this.hitQuadY1 = r.getLocation().y;
               this.hitQuadZ1 = r.getLocation().z;
               this.hitQuadX2 = r.getLocation().x;
               this.hitQuadY2 = r.getLocation().y;
               this.hitQuadZ2 = r.getLocation().z;
               if (r.getDirection() == Direction.UP || r.getDirection() == Direction.DOWN) {
                  this.hitQuadHorizontal = true;
                  this.hitQuadX1 -= 0.25;
                  this.hitQuadZ1 -= 0.25;
                  this.hitQuadX2 += 0.25;
                  this.hitQuadZ2 += 0.25;
               } else if (r.getDirection() != Direction.NORTH && r.getDirection() != Direction.SOUTH) {
                  this.hitQuadHorizontal = false;
                  this.hitQuadZ1 -= 0.25;
                  this.hitQuadY1 -= 0.25;
                  this.hitQuadZ2 += 0.25;
                  this.hitQuadY2 += 0.25;
               } else {
                  this.hitQuadHorizontal = false;
                  this.hitQuadX1 -= 0.25;
                  this.hitQuadY1 -= 0.25;
                  this.hitQuadX2 += 0.25;
                  this.hitQuadY2 += 0.25;
               }

               this.points.add(Utils.set(Trajectories.this.vec3s.get(), result.getLocation()));
            } else if (result.getType() == Type.ENTITY) {
               Entity entity = ((EntityHitResult)result).getEntity();
               this.collidingEntities.add(entity);
               if (step.shouldStop && i == step.hitResults.length - 1) {
                  this.points.add(Utils.set(Trajectories.this.vec3s.get(), result.getLocation()));
               }
            }
         }
      }

      public void ignoreFirstTicks() {
         this.start = this.points.size() <= Trajectories.this.ignoreFirstTicks.get() ? 0 : Trajectories.this.ignoreFirstTicks.get();
      }

      public void render(Render3DEvent event) {
         for (int i = this.start; i < this.points.size(); i++) {
            Vector3d point = this.points.get(i);
            if (this.lastPoint != null) {
               event.renderer.line(this.lastPoint.x, this.lastPoint.y, this.lastPoint.z, point.x, point.y, point.z, Trajectories.this.lineColor.get());
               if (Trajectories.this.renderPositionBox.get()) {
                  event.renderer
                     .box(
                        point.x - Trajectories.this.positionBoxSize.get(),
                        point.y - Trajectories.this.positionBoxSize.get(),
                        point.z - Trajectories.this.positionBoxSize.get(),
                        point.x + Trajectories.this.positionBoxSize.get(),
                        point.y + Trajectories.this.positionBoxSize.get(),
                        point.z + Trajectories.this.positionBoxSize.get(),
                        Trajectories.this.positionSideColor.get(),
                        Trajectories.this.positionLineColor.get(),
                        Trajectories.this.shapeMode.get(),
                        0
                     );
               }
            }

            this.lastPoint = point;
         }

         if (this.hitQuad) {
            if (this.hitQuadHorizontal) {
               event.renderer
                  .sideHorizontal(
                     this.hitQuadX1,
                     this.hitQuadY1,
                     this.hitQuadZ1,
                     this.hitQuadX1 + 0.5,
                     this.hitQuadZ1 + 0.5,
                     Trajectories.this.sideColor.get(),
                     Trajectories.this.lineColor.get(),
                     Trajectories.this.shapeMode.get()
                  );
            } else {
               event.renderer
                  .sideVertical(
                     this.hitQuadX1,
                     this.hitQuadY1,
                     this.hitQuadZ1,
                     this.hitQuadX2,
                     this.hitQuadY2,
                     this.hitQuadZ2,
                     Trajectories.this.sideColor.get(),
                     Trajectories.this.lineColor.get(),
                     Trajectories.this.shapeMode.get()
                  );
            }
         }

         for (Entity collidingEntity : this.collidingEntities) {
            double x = (collidingEntity.getX() - collidingEntity.xOld) * event.tickDelta;
            double y = (collidingEntity.getY() - collidingEntity.yOld) * event.tickDelta;
            double z = (collidingEntity.getZ() - collidingEntity.zOld) * event.tickDelta;
            AABB box = collidingEntity.getBoundingBox();
            event.renderer
               .box(
                  x + box.minX,
                  y + box.minY,
                  z + box.minZ,
                  x + box.maxX,
                  y + box.maxY,
                  z + box.maxZ,
                  Trajectories.this.sideColor.get(),
                  Trajectories.this.lineColor.get(),
                  Trajectories.this.shapeMode.get(),
                  0
               );
         }
      }
   }
}
