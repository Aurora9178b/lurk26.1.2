package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;

public class LightOverlay extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgColors = this.settings.createGroup("Colors");
   private final Setting<Integer> horizontalRange = this.sgGeneral
      .add(new IntSetting.Builder().name("horizontal-range").description("Horizontal range in blocks.").defaultValue(8).min(0).build());
   private final Setting<Integer> verticalRange = this.sgGeneral
      .add(new IntSetting.Builder().name("vertical-range").description("Vertical range in blocks.").defaultValue(4).min(0).build());
   private final Setting<Boolean> seeThroughBlocks = this.sgGeneral
      .add(new BoolSetting.Builder().name("see-through-blocks").description("Allows you to see the lines through blocks.").defaultValue(false).build());
   private final Setting<Integer> lightLevel = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("light-level")
            .description("Which light levels to render. Old spawning light: 7.")
            .defaultValue(0)
            .min(0)
            .sliderMax(15)
            .build()
      );
   private final Setting<SettingColor> color = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("color")
            .description("Color of places where mobs can currently spawn.")
            .defaultValue(new SettingColor(225, 25, 25))
            .build()
      );
   private final Setting<SettingColor> potentialColor = this.sgColors
      .add(
         new ColorSetting.Builder()
            .name("potential-color")
            .description("Color of places where mobs can potentially spawn (eg at night).")
            .defaultValue(new SettingColor(225, 225, 25))
            .build()
      );
   private final Pool<LightOverlay.Cross> crossPool = new Pool<>(() -> new LightOverlay.Cross());
   private final List<LightOverlay.Cross> crosses = new ArrayList<>();

   public LightOverlay() {
      super(Categories.Render, "light-overlay", "Shows blocks where mobs can spawn.");
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      this.crossPool.freeAll(this.crosses);
      this.crosses.clear();
      BlockIterator.register(this.horizontalRange.get(), this.verticalRange.get(), (blockPos, blockState) -> {
         switch (BlockUtils.isValidMobSpawn(blockPos, blockState, this.lightLevel.get())) {
            case Potential:
               this.crosses.add(this.crossPool.get().set(blockPos, true));
               break;
            case Always:
               this.crosses.add(this.crossPool.get().set(blockPos, false));
         }
      });
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      if (!this.crosses.isEmpty()) {
         Renderer3D renderer = this.seeThroughBlocks.get() ? event.renderer : event.depthRenderer;

         for (LightOverlay.Cross cross : this.crosses) {
            cross.render(renderer);
         }
      }
   }

   private class Cross {
      private double x;
      private double y;
      private double z;
      private boolean potential;

      public LightOverlay.Cross set(BlockPos blockPos, boolean potential) {
         this.x = blockPos.getX();
         this.y = blockPos.getY() + 0.0075;
         this.z = blockPos.getZ();
         this.potential = potential;
         return this;
      }

      public void render(Renderer3D renderer) {
         Color c = this.potential ? LightOverlay.this.potentialColor.get() : LightOverlay.this.color.get();
         renderer.line(this.x, this.y, this.z, this.x + 1.0, this.y, this.z + 1.0, c);
         renderer.line(this.x + 1.0, this.y, this.z, this.x, this.y, this.z + 1.0, c);
      }
   }

   public enum Spawn {
      Never,
      Potential,
      Always;
   }
}
