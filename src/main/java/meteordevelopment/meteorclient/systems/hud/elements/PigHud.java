package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.resources.Identifier;

public class PigHud extends HudElement {
   public static final HudElementInfo<PigHud> INFO = new HudElementInfo<>(Hud.GROUP, "pig", "Pig", "Displays an animated pig.", PigHud::new);
   private static final int FRAME_COUNT = 96;
   private static final int BASE_SIZE = 64;
   private static final Identifier[] FRAMES = new Identifier[96];
   private static final int[] FRAME_DURATIONS_MS = new int[]{
      150,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      30,
      240
   };
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("The scale of the pig.").defaultValue(1.0).min(0.1).sliderRange(0.1, 5.0).build());
   private final long startTimeMs = System.currentTimeMillis();

   public PigHud() {
      super(INFO);
   }

   @Override
   public void tick(HudRenderer renderer) {
      double size = 64.0 * this.scale.get();
      this.setSize(size, size);
   }

   @Override
   public void render(HudRenderer renderer) {
      renderer.texture(frameAt(System.currentTimeMillis() - this.startTimeMs), this.x, this.y, this.getWidth(), this.getHeight(), Color.WHITE);
   }

   private static Identifier frameAt(long elapsedMs) {
      int totalDuration = 0;

      for (int duration : FRAME_DURATIONS_MS) {
         totalDuration += duration;
      }

      int time = (int)(elapsedMs % totalDuration);

      for (int i = 0; i < 96; i++) {
         time -= FRAME_DURATIONS_MS[i];
         if (time < 0) {
            return FRAMES[i];
         }
      }

      return FRAMES[95];
   }

   static {
      for (int i = 0; i < 96; i++) {
         FRAMES[i] = MeteorClient.identifier(String.format("textures/hud/pig/frame_%05d.png", i));
      }
   }
}
