package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.List;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.BedAura;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class ModuleInfosHud extends HudElement {
   public static final HudElementInfo<ModuleInfosHud> INFO = new HudElementInfo<>(
      Hud.GROUP, "module-infos", "Displays if selected modules are enabled or disabled.", ModuleInfosHud::new
   );
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<Module>> modules = this.sgGeneral
      .add(
         new ModuleListSetting.Builder()
            .name("modules")
            .description("Which modules to display")
            .defaultValue(KillAura.class, CrystalAura.class, BedAura.class, Surround.class)
            .build()
      );
   private final Setting<Boolean> additionalInfo = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("additional-info")
            .description("Shows additional info from the module next to the name in the module info list.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Boolean> textShadow = this.sgGeneral
      .add(new BoolSetting.Builder().name("text-shadow").description("Renders shadow behind text.").defaultValue(true).build());
   private final Setting<SettingColor> moduleColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("module-color").description("Module color.").defaultValue(new SettingColor()).build());
   private final Setting<SettingColor> onColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("on-color").description("Color when module is on.").defaultValue(new SettingColor(25, 225, 25)).build());
   private final Setting<SettingColor> offColor = this.sgGeneral
      .add(new ColorSetting.Builder().name("off-color").description("Color when module is off.").defaultValue(new SettingColor(225, 25, 25)).build());
   private final Setting<Alignment> alignment = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("alignment")).description("Horizontal alignment."))
               .defaultValue(Alignment.Auto))
            .build()
      );

   public ModuleInfosHud() {
      super(INFO);
   }

   @Override
   public void render(HudRenderer renderer) {
      if (Modules.get() != null && !this.modules.get().isEmpty()) {
         double y = this.y;
         double width = 0.0;
         double height = 0.0;
         int i = 0;

         for (Module module : this.modules.get()) {
            double moduleWidth = renderer.textWidth(module.title) + renderer.textWidth(" ");
            String text = null;
            if (module.isActive()) {
               if (this.additionalInfo.get()) {
                  String info = module.getInfoString();
                  if (info != null) {
                     text = info;
                  }
               }

               if (text == null) {
                  text = "ON";
               }
            } else {
               text = "OFF";
            }

            moduleWidth += renderer.textWidth(text);
            double x = this.x + this.alignX(moduleWidth, this.alignment.get());
            x = renderer.text(module.title, x, y, this.moduleColor.get(), this.textShadow.get());
            renderer.text(text, x + renderer.textWidth(" "), y, module.isActive() ? this.onColor.get() : this.offColor.get(), this.textShadow.get());
            y += renderer.textHeight() + 2.0;
            width = Math.max(width, moduleWidth);
            height += renderer.textHeight();
            if (i > 0) {
               height += 2.0;
            }

            i++;
         }

         this.setSize(width, height);
      } else {
         renderer.text("Module Info", this.x, this.y, this.moduleColor.get(), this.textShadow.get());
         this.setSize(renderer.textWidth("Module Info"), renderer.textHeight());
      }
   }
}
