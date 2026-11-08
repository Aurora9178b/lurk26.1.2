package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SafeWalk extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<Integer> fallDistance = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("minimum-fall-distance")
            .description("The minimum number of blocks you are expected to fall before the module activates.")
            .defaultValue(1)
            .min(1)
            .build()
      );
   private final Setting<Boolean> sneak = this.sgGeneral
      .add(new BoolSetting.Builder().name("sneak").description("Sneak when approaching edge of block.").defaultValue(false).build());
   private final Setting<Boolean> safeSneak = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("safe-sneak")
            .description("Prevent you from falling if sneak doesn't trigger correctly.")
            .defaultValue(true)
            .visible(this.sneak::get)
            .build()
      );
   private final Setting<Boolean> sneakSprint = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("sneak-on-sprint")
            .description("Sneak even when sprinting at the block edge.")
            .defaultValue(true)
            .visible(this.sneak::get)
            .build()
      );
   private final Setting<Double> edgeDistance = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("edge-distance")
            .description("Distance offset before reaching an edge.")
            .defaultValue(0.3)
            .sliderRange(0.0, 0.3)
            .decimalPlaces(2)
            .visible(this.sneak::get)
            .build()
      );
   private final Setting<Boolean> renderEdgeDistance = this.sgRender
      .add(new BoolSetting.Builder().name("render").description("Render edge distance helper.").defaultValue(false).visible(this.sneak::get).build());
   private final Setting<Boolean> renderPlayerBox = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("render-player-box")
            .description("Render player box helper.")
            .defaultValue(false)
            .visible(() -> this.sneak.get() && this.renderEdgeDistance.get())
            .build()
      );

   public SafeWalk() {
      super(Categories.Movement, "safe-walk", "Prevents you from walking off blocks.");
   }

   @EventHandler
   private void onClipAtLedge(ClipAtLedgeEvent event) {
      if (this.fallDistance.get() > 1) {
         int surface = this.mc
            .level
            .getChunkAt(this.mc.player.blockPosition())
            .getOrCreateHeightmapUnprimed(Types.MOTION_BLOCKING)
            .getFirstAvailable(this.mc.player.getBlockX() & 15, this.mc.player.getBlockZ() & 15);
         if (this.mc.player.getBlockY() >= surface) {
            if (this.mc.player.getBlockY() - surface < this.fallDistance.get()) {
               return;
            }
         } else {
            BlockHitResult raycastResult = this.mc
               .level
               .clip(
                  new ClipContext(
                     this.mc.player.position(),
                     new Vec3(this.mc.player.getX(), this.mc.level.getMinY(), this.mc.player.getZ()),
                     Block.COLLIDER,
                     Fluid.WATER,
                     this.mc.player
                  )
               );
            if (raycastResult.getType() != Type.MISS && (int)(this.mc.player.getY() - raycastResult.getBlockPos().above().getY()) < this.fallDistance.get()) {
               return;
            }
         }
      }

      if (this.sneak.get()) {
         boolean closeToEdge = false;
         boolean isSprinting = !this.sneakSprint.get() && this.mc.options.keySprint.isDown();
         AABB playerBox = this.mc.player.getBoundingBox();
         AABB adjustedBox = this.getAdjustedPlayerBox(playerBox);
         if (this.mc.level.noCollision(this.mc.player, adjustedBox) && this.mc.player.onGround()) {
            closeToEdge = true;
         }

         if (!isSprinting) {
            if (closeToEdge) {
               this.mc.player.input.keyPresses = new Input(
                  this.mc.player.input.keyPresses.forward(),
                  this.mc.player.input.keyPresses.backward(),
                  this.mc.player.input.keyPresses.left(),
                  this.mc.player.input.keyPresses.right(),
                  this.mc.player.input.keyPresses.jump(),
                  true,
                  this.mc.player.input.keyPresses.sprint()
               );
            } else if (this.safeSneak.get()) {
               event.setClip(true);
            }
         }
      } else if (!this.mc.player.isShiftKeyDown()) {
         event.setClip(true);
      }
   }

   private AABB getAdjustedPlayerBox(AABB playerBox) {
      return playerBox.expandTowards(0.0, -this.mc.player.maxUpStep(), 0.0).inflate(-this.edgeDistance.get(), 0.0, -this.edgeDistance.get());
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (this.sneak.get() && this.renderEdgeDistance.get()) {
         AABB playerBox = this.mc.player.getBoundingBox();
         AABB adjustedBox = this.getAdjustedPlayerBox(playerBox);
         event.renderer.box(adjustedBox, Color.BLUE, Color.RED, ShapeMode.Lines, 0);
         if (this.renderPlayerBox.get()) {
            event.renderer.box(playerBox, Color.BLUE, Color.GREEN, ShapeMode.Lines, 0);
         }
      }
   }
}
