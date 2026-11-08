package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.BlurUtil;
import meteordevelopment.meteorclient.utils.render.Render2DUtil;
import meteordevelopment.meteorclient.utils.render.Animation.AnimationUtility;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class ModuleListHud extends HudElement {
   public static final HudElementInfo<ModuleListHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "module-list", "Module List", "Displays active modules with a two-part rounded background.", ModuleListHud::new
   );
   private static final double DIAMETER = 16.0;
   private static final double RADIUS = 8.0;
   private static final double SPACING = 1.0;
   private static final double TEXT_SCALE = 0.6;
   private static final double LEFT_WIDTH = 16.0;
   private static final double DOT_RADIUS = 0.4;
   private static final double TEXT_PADDING = 5.0;
   private static final Color LEFT_BACKGROUND = new Color(255, 255, 255, 38);
   private static final Color RIGHT_BACKGROUND = new Color(255, 255, 255, 13);
   private static final Color TEXT = new Color(255, 255, 255, 255);
   private static final Color INFO_TEXT = new Color(155, 155, 155, 255);
   private static final Color DOT = new Color(255, 255, 255, 255);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Module>> hiddenModules = this.sgGeneral
      .add(new ModuleListSetting.Builder().name("hidden-module").description("Modules hidden from the list.").build());
   private final Setting<Boolean> moduleInfo = this.sgGeneral
      .add(new BoolSetting.Builder().name("module-info").description("Shows module info next to the name.").defaultValue(true).build());
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("Scale of the module list.").defaultValue(1.0).min(0.5).sliderRange(0.5, 3.0).build());
   private final List<ModuleListHud.ModuleEntry> modules = new ArrayList<>();
   private final Map<String, Float> yAnimations = new HashMap<>();
   private float hAnimation = 50.0F;

   public ModuleListHud() {
      super(INFO);
   }

   @Override
   public void tick(HudRenderer renderer) {
      this.updateModules(renderer);
      double s = this.scale.get();
      if (this.modules.isEmpty()) {
         this.setSize(renderer.textWidth("Module List", false, s), renderer.textHeight(false, s));
      } else {
         double maxWidth = 0.0;

         for (ModuleListHud.ModuleEntry module : this.modules) {
            maxWidth = Math.max(maxWidth, module.textWidth);
         }

         double width = (40.0 + maxWidth + 5.0) * s;
         double height = (this.modules.size() * 16.0 + Math.max(0, this.modules.size() - 1) * 1.0) * s;
         this.setSize(width, height);
      }
   }

   @Override
   public void render(HudRenderer renderer) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.level != null) {
         if (this.modules.isEmpty()) {
            if (this.isInEditor()) {
               renderer.text("Module List", this.x, this.y, TEXT, false, this.scale.get());
            }
         } else {
            double s = this.scale.get();
            double originX = this.x;
            double originY = this.y;
            this.hAnimation = AnimationUtility.ease(this.hAnimation, 0.0F, 5.0F);

            for (int i = 0; i < this.modules.size(); i++) {
               ModuleListHud.ModuleEntry entry = this.modules.get(i);
               double targetY = i * 17.0;
               float animatedY = AnimationUtility.ease(this.yAnimations.getOrDefault(entry.key, (float)(targetY - 20.0)), (float)targetY, 8.0F);
               this.yAnimations.put(entry.key, animatedY);
               double centerX = originX + (this.hAnimation + 8.0) * s;
               double centerY = originY + (animatedY + 8.0) * s;
               double rectX = centerX + 11.0 * s;
               double rectY = centerY - 8.0 * s;
               double leftWidth = 16.0 * s;
               double rightWidth = (5.0 + entry.textWidth + 5.0) * s;
               double height = 16.0 * s;
               double radius = 8.0 * s;
               this.renderBlur(rectX, rectY, leftWidth, height, radius, 0.0, 0.0, radius);
               this.renderBlur(rectX + leftWidth, rectY, rightWidth, height, 0.0, radius, radius, 0.0);
               Render2DUtil.drawRoundedRectCorners(renderer, rectX, rectY, leftWidth, height, radius, 0.0, 0.0, radius, LEFT_BACKGROUND);
               Render2DUtil.drawRoundedRectCorners(renderer, rectX + leftWidth, rectY, rightWidth, height, 0.0, radius, radius, 0.0, RIGHT_BACKGROUND);
               Render2DUtil.drawRoundedRect(renderer, rectX + leftWidth * 0.5 - 0.4 * s, rectY + height * 0.5 - 0.4 * s, 0.8 * s, 0.8 * s, 0.4 * s, DOT);
               double textX = rectX + leftWidth + 5.0 * s;
               double textY = rectY + (height - renderer.textHeight(false, 0.6 * s)) * 0.5;
               renderer.text(entry.name, textX, textY, TEXT, false, 0.6 * s);
               if (!entry.info.isEmpty()) {
                  renderer.text(entry.info, textX + entry.nameWidth * s, textY, INFO_TEXT, false, 0.6 * s);
               }
            }

            this.yAnimations.keySet().removeIf(key -> this.modules.stream().noneMatch(entryx -> entryx.key.equals(key)));
         }
      }
   }

   private void renderBlur(
      double x, double y, double width, double height, double radiusTopLeft, double radiusTopRight, double radiusBottomRight, double radiusBottomLeft
   ) {
      BlurUtil.render(
         (int)Math.round(x),
         (int)Math.round(y),
         (int)Math.round(width),
         (int)Math.round(height),
         (float)radiusTopLeft,
         (float)radiusTopRight,
         (float)radiusBottomRight,
         (float)radiusBottomLeft
      );
   }

   private void updateModules(HudRenderer renderer) {
      this.modules.clear();
      double textScale = 0.6 * this.scale.get();

      for (Module module : Modules.get().getActive()) {
         if (!this.hiddenModules.get().contains(module)) {
            String name = module.title;
            String info = this.getModuleInfo(module);
            double nameWidth = renderer.textWidth(name, false, textScale) / this.scale.get();
            double infoWidth = info.isEmpty() ? 0.0 : renderer.textWidth(info, false, textScale) / this.scale.get();
            this.modules.add(new ModuleListHud.ModuleEntry(module.name, name, info, nameWidth, nameWidth + infoWidth));
         }
      }

      this.modules.sort((a, b) -> Double.compare(b.textWidth, a.textWidth));
   }

   private String getModuleInfo(Module module) {
      if (!this.moduleInfo.get()) {
         return "";
      }

      String info = module.getInfoString();
      return info != null && !info.isEmpty() ? " [" + info + "]" : "";
   }

   private record ModuleEntry(String key, String name, String info, double nameWidth, double textWidth) {
   }
}
