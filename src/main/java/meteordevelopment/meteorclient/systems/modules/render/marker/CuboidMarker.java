package meteordevelopment.meteorclient.systems.modules.render.marker;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockPosSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.core.BlockPos;

public class CuboidMarker extends BaseMarker {
   public static final String type = "Cuboid";
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRender = this.settings.createGroup("Render");
   private final Setting<BlockPos> pos1 = this.sgGeneral.add(new BlockPosSetting.Builder().name("pos-1").description("1st corner of the cuboid").build());
   private final Setting<BlockPos> pos2 = this.sgGeneral.add(new BlockPosSetting.Builder().name("pos-2").description("2nd corner of the cuboid").build());
   private final Setting<CuboidMarker.Mode> mode = this.sgRender
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("What mode to use for this marker."))
               .defaultValue(CuboidMarker.Mode.Full))
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

   public CuboidMarker() {
      super("Cuboid");
   }

   @Override
   public String getTypeName() {
      return "Cuboid";
   }

   @Override
   protected void render(Render3DEvent event) {
      int minX = Math.min(this.pos1.get().getX(), this.pos2.get().getX());
      int minY = Math.min(this.pos1.get().getY(), this.pos2.get().getY());
      int minZ = Math.min(this.pos1.get().getZ(), this.pos2.get().getZ());
      int maxX = Math.max(this.pos1.get().getX(), this.pos2.get().getX());
      int maxY = Math.max(this.pos1.get().getY(), this.pos2.get().getY());
      int maxZ = Math.max(this.pos1.get().getZ(), this.pos2.get().getZ());
      event.renderer.box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1, this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
   }

   public enum Mode {
      Full;
   }
}
