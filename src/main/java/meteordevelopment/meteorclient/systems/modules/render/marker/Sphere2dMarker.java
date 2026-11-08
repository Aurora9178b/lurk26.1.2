package meteordevelopment.meteorclient.systems.modules.render.marker;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.BlockPos;

public class Sphere2dMarker extends BaseMarker {
   public static final String type = "Sphere-2D";
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final SettingGroup sgKeybinding = this.settings.createGroup("Keybinding");
   private final Setting<BlockPos> center = this.sgGeneral
      .add(new BlockPosSetting.Builder().name("center").description("Center of the sphere").onChanged(var1 -> this.dirty = true).build());
   private final Setting<Integer> radius = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("radius")
            .description("Radius of the sphere")
            .defaultValue(20)
            .min(1)
            .noSlider()
            .onChanged(var1 -> this.dirty = true)
            .build()
      );
   private final Setting<Integer> layer = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("layer")
            .description("Which layer to render")
            .defaultValue(0)
            .min(0)
            .noSlider()
            .onChanged(var1 -> this.dirty = true)
            .build()
      );
   private final Setting<Boolean> limitRenderRange = this.sgRender
      .add(
         new BoolSetting.Builder()
            .name("limit-render-range")
            .description("Whether to limit rendering range (useful in very large circles)")
            .defaultValue(false)
            .build()
      );
   private final Setting<Integer> renderRange = this.sgRender
      .add(
         new IntSetting.Builder()
            .name("render-range")
            .description("Rendering range")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 20)
            .visible(this.limitRenderRange::get)
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
      .add(
         new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 100, 255, 50))
            .build()
      );
   private final Setting<SettingColor> lineColor = this.sgRender
      .add(
         new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(0, 100, 255, 255))
            .build()
      );
   private final Setting<Keybind> nextLayerKey = this.sgKeybinding
      .add(new KeybindSetting.Builder().name("next-layer-keybind").description("Keybind to increment layer").action(() -> {
         if (this.isVisible() && this.layer.get() < this.radius.get() * 2) {
            this.layer.set(this.layer.get() + 1);
         }
      }).build());
   private final Setting<Keybind> prevLayerKey = this.sgKeybinding
      .add(new KeybindSetting.Builder().name("prev-layer-keybind").description("Keybind to increment layer").action(() -> {
         if (this.isVisible()) {
            this.layer.set(this.layer.get() - 1);
         }
      }).build());
   private final List<Sphere2dMarker.Block> blocks = new ArrayList<>();
   private boolean dirty = true;
   private boolean calculating;

   public Sphere2dMarker() {
      super("Sphere-2D");
   }

   @Override
   protected void render(Render3DEvent event) {
      if (this.dirty && !this.calculating) {
         this.calcCircle();
      }

      synchronized (this.blocks) {
         for (Sphere2dMarker.Block block : this.blocks) {
            if (!this.limitRenderRange.get() || PlayerUtils.isWithin(block.x, block.y, block.z, this.renderRange.get().intValue())) {
               event.renderer
                  .box(
                     block.x,
                     block.y,
                     block.z,
                     block.x + 1,
                     block.y + 1,
                     block.z + 1,
                     this.sideColor.get(),
                     this.lineColor.get(),
                     this.shapeMode.get(),
                     block.excludeDir
                  );
            }
         }
      }
   }

   @Override
   public String getTypeName() {
      return "Sphere-2D";
   }

   private void calcCircle() {
      this.calculating = true;
      this.blocks.clear();
      Runnable action = () -> {
         int cX = this.center.get().getX();
         int cY = this.center.get().getY();
         int cZ = this.center.get().getZ();
         int rSq = this.radius.get() * this.radius.get();
         int dY = -this.radius.get() + this.layer.get();
         int dX = 0;

         while (true) {
            int dZ = (int)Math.round(Math.sqrt(rSq - (dX * dX + dY * dY)));
            synchronized (this.blocks) {
               this.add(cX + dX, cY + dY, cZ + dZ);
               this.add(cX + dZ, cY + dY, cZ + dX);
               this.add(cX - dX, cY + dY, cZ - dZ);
               this.add(cX - dZ, cY + dY, cZ - dX);
               this.add(cX + dX, cY + dY, cZ - dZ);
               this.add(cX + dZ, cY + dY, cZ - dX);
               this.add(cX - dX, cY + dY, cZ + dZ);
               this.add(cX - dZ, cY + dY, cZ + dX);
            }

            if (dX >= dZ) {
               synchronized (this.blocks) {
                  for (Sphere2dMarker.Block block : this.blocks) {
                     for (Sphere2dMarker.Block b : this.blocks) {
                        if (b != block) {
                           if (b.x == block.x + 1 && b.z == block.z) {
                              block.excludeDir |= 64;
                           }

                           if (b.x == block.x - 1 && b.z == block.z) {
                              block.excludeDir |= 32;
                           }

                           if (b.x == block.x && b.z == block.z + 1) {
                              block.excludeDir |= 16;
                           }

                           if (b.x == block.x && b.z == block.z - 1) {
                              block.excludeDir |= 8;
                           }
                        }
                     }
                  }
               }

               this.dirty = false;
               this.calculating = false;
               return;
            }

            dX++;
         }
      };
      if (this.radius.get() <= 50) {
         action.run();
      } else {
         MeteorExecutor.execute(action);
      }
   }

   private void add(int x, int y, int z) {
      for (Sphere2dMarker.Block b : this.blocks) {
         if (b.x == x && b.y == y && b.z == z) {
            return;
         }
      }

      this.blocks.add(new Sphere2dMarker.Block(x, y, z));
   }

   private static class Block {
      public final int x;
      public final int y;
      public final int z;
      public int excludeDir;

      public Block(int x, int y, int z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
