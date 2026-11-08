package meteordevelopment.meteorclient.systems.modules.render;

import java.util.ArrayDeque;
import java.util.Queue;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.dimension.DimensionType;

public class Breadcrumbs extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<SettingColor> color = this.sgGeneral
      .add(new ColorSetting.Builder().name("color").description("The color of the Breadcrumbs trail.").defaultValue(new SettingColor(225, 25, 25)).build());
   private final Setting<Integer> maxSections = this.sgGeneral
      .add(new IntSetting.Builder().name("max-sections").description("The maximum number of sections.").defaultValue(1000).min(1).sliderRange(1, 5000).build());
   private final Setting<Double> sectionLength = this.sgGeneral
      .add(new DoubleSetting.Builder().name("section-length").description("The section length in blocks.").defaultValue(0.5).min(0.0).sliderMax(1.0).build());
   private final Pool<Breadcrumbs.Section> sectionPool = new Pool<>(() -> new Breadcrumbs.Section());
   private final Queue<Breadcrumbs.Section> sections = new ArrayDeque<>();
   private Breadcrumbs.Section section;
   private DimensionType lastDimension;

   public Breadcrumbs() {
      super(Categories.Render, "breadcrumbs", "Displays a trail behind where you have walked.");
   }

   @Override
   public void onActivate() {
      this.section = this.sectionPool.get();
      this.section.set1();
      this.lastDimension = this.mc.level.dimensionType();
   }

   @Override
   public void onDeactivate() {
      this.sectionPool.freeAll(this.sections);
      this.sections.clear();
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.getConnection().hasClientLoaded()) {
         if (this.lastDimension != this.mc.level.dimensionType()) {
            this.sectionPool.freeAll(this.sections);
            this.sections.clear();
         }

         if (this.isFarEnough(this.section.x1, this.section.y1, this.section.z1)) {
            this.section.set2();
            if (this.sections.size() >= this.maxSections.get()) {
               Breadcrumbs.Section section = this.sections.poll();
               if (section != null) {
                  this.sectionPool.free(section);
               }
            }

            this.sections.add(this.section);
            this.section = this.sectionPool.get();
            this.section.set1();
         }

         this.lastDimension = this.mc.level.dimensionType();
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      int iLast = -1;

      for (Breadcrumbs.Section section : this.sections) {
         event.renderer.lines.ensureLineCapacity();
         if (iLast == -1) {
            iLast = event.renderer.lines.vec3(section.x1, section.y1, section.z1).color(this.color.get()).next();
         }

         int i = event.renderer.lines.vec3(section.x2, section.y2, section.z2).color(this.color.get()).next();
         event.renderer.lines.line(iLast, i);
         iLast = i;
      }
   }

   private boolean isFarEnough(double x, double y, double z) {
      return Math.abs(this.mc.player.getX() - x) >= this.sectionLength.get()
         || Math.abs(this.mc.player.getY() - y) >= this.sectionLength.get()
         || Math.abs(this.mc.player.getZ() - z) >= this.sectionLength.get();
   }

   private class Section {
      public float x1;
      public float y1;
      public float z1;
      public float x2;
      public float y2;
      public float z2;

      public void set1() {
         this.x1 = (float)Breadcrumbs.this.mc.player.getX();
         this.y1 = (float)Breadcrumbs.this.mc.player.getY();
         this.z1 = (float)Breadcrumbs.this.mc.player.getZ();
      }

      public void set2() {
         this.x2 = (float)Breadcrumbs.this.mc.player.getX();
         this.y2 = (float)Breadcrumbs.this.mc.player.getY();
         this.z2 = (float)Breadcrumbs.this.mc.player.getZ();
      }
   }
}
