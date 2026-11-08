package meteordevelopment.meteorclient.systems.hud.elements;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import org.meteordev.starscript.Script;
import org.meteordev.starscript.Section;
import org.meteordev.starscript.compiler.Compiler;
import org.meteordev.starscript.compiler.Parser;
import org.meteordev.starscript.compiler.Parser.Result;
import org.meteordev.starscript.utils.Error;
import org.meteordev.starscript.utils.StarscriptError;

public class TextHud extends HudElement {
   private static final Color WHITE = new Color();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgShown = this.settings.createGroup("Shown");
   private final SettingGroup sgScale = this.settings.createGroup("Scale");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private double originalWidth;
   private double originalHeight;
   private boolean needsCompile;
   private boolean recalculateSize;
   private int timer;
   public final Setting<String> text = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("text")
            .description("Text to display with Starscript.")
            .defaultValue(MeteorClient.NAME)
            .onChanged(var1x -> this.recompile())
            .wide()
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   public final Setting<Integer> updateDelay = this.sgGeneral
      .add(new IntSetting.Builder().name("update-delay").description("Update delay in ticks").defaultValue(4).onChanged(integer -> {
         if (this.timer > integer) {
            this.timer = integer;
         }
      }).min(0).build());
   public final Setting<Boolean> shadow = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("shadow")
            .description("Renders shadow behind text.")
            .defaultValue(true)
            .onChanged(var1x -> this.recalculateSize = true)
            .build()
      );
   public final Setting<Integer> border = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("border")
            .description("How much space to add around the text.")
            .defaultValue(0)
            .onChanged(integer -> super.setSize(this.originalWidth + integer * 2, this.originalHeight + integer * 2))
            .build()
      );
   public final Setting<TextHud.Shown> shown = this.sgShown
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shown"))
                     .description("When this text element is shown."))
                  .defaultValue(TextHud.Shown.Always))
               .onChanged(var1x -> this.recompile()))
            .build()
      );
   public final Setting<String> condition = this.sgShown
      .add(
         new StringSetting.Builder()
            .name("condition")
            .description("Condition to check when shown is not Always.")
            .visible(() -> this.shown.get() != TextHud.Shown.Always)
            .onChanged(var1x -> this.recompile())
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   public final Setting<Boolean> customScale = this.sgScale
      .add(
         new BoolSetting.Builder()
            .name("custom-scale")
            .description("Applies a custom scale to this hud element.")
            .defaultValue(false)
            .onChanged(var1x -> this.recalculateSize = true)
            .build()
      );
   public final Setting<Double> scale = this.sgScale
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Custom scale.")
            .visible(this.customScale::get)
            .defaultValue(1.0)
            .onChanged(var1x -> this.recalculateSize = true)
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   public final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   public final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );
   private Script script;
   private Script conditionScript;
   private Section section;
   private boolean firstTick = true;
   private boolean empty = false;
   private boolean visible;

   public TextHud(HudElementInfo<TextHud> info) {
      super(info);
      this.needsCompile = true;
   }

   private void recompile() {
      this.firstTick = true;
      this.needsCompile = true;
   }

   @Override
   public void setSize(double width, double height) {
      this.originalWidth = width;
      this.originalHeight = height;
      super.setSize(width + this.border.get() * 2, height + this.border.get() * 2);
   }

   private void calculateSize(HudRenderer renderer) {
      double width = 0.0;
      if (this.section != null) {
         String str = this.section.toString();
         if (!str.isBlank()) {
            width = renderer.textWidth(str, this.shadow.get(), this.getScale());
         }
      }

      if (width != 0.0) {
         this.setSize(width, renderer.textHeight(this.shadow.get(), this.getScale()));
         this.empty = false;
      } else {
         this.setSize(100.0, renderer.textHeight(this.shadow.get(), this.getScale()));
         this.empty = true;
      }
   }

   @Override
   public void tick(HudRenderer renderer) {
      if (this.recalculateSize) {
         this.calculateSize(renderer);
         this.recalculateSize = false;
      }

      if (this.timer <= 0) {
         this.runTick(renderer);
         this.timer = this.updateDelay.get();
      } else {
         this.timer--;
      }
   }

   private void runTick(HudRenderer renderer) {
      if (this.needsCompile) {
         Result result = Parser.parse(this.text.get());
         if (result.hasErrors()) {
            this.script = null;
            this.section = new Section(0, ((Error)result.errors.getFirst()).toString());
            this.calculateSize(renderer);
         } else {
            this.script = Compiler.compile(result);
         }

         if (this.shown.get() != TextHud.Shown.Always) {
            this.conditionScript = Compiler.compile(Parser.parse(this.condition.get()));
         }

         this.needsCompile = false;
      }

      try {
         if (this.script != null) {
            this.section = MeteorStarscript.ss.run(this.script);
            this.calculateSize(renderer);
         }
      } catch (StarscriptError error) {
         this.section = new Section(0, error.getMessage());
         this.calculateSize(renderer);
      }

      if (this.shown.get() != TextHud.Shown.Always && this.conditionScript != null) {
         String text = MeteorStarscript.run(this.conditionScript);
         if (text == null) {
            this.visible = false;
         } else {
            this.visible = this.shown.get() == TextHud.Shown.WhenTrue ? text.equalsIgnoreCase("true") : text.equalsIgnoreCase("false");
         }
      }

      this.firstTick = false;
   }

   @Override
   public void render(HudRenderer renderer) {
      if (this.firstTick) {
         this.runTick(renderer);
      }

      boolean visible = this.shown.get() == TextHud.Shown.Always || this.visible;
      if ((this.empty || !visible) && this.isInEditor()) {
         renderer.line(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), Color.GRAY);
         renderer.line(this.x, this.y + this.getHeight(), this.x + this.getWidth(), this.y, Color.GRAY);
      }

      if (this.section != null && visible) {
         double x = this.x + this.border.get();

         for (Section s = this.section; s != null; s = s.next) {
            x = renderer.text(s.text, x, this.y + this.border.get(), getSectionColor(s.index), this.shadow.get(), this.getScale());
         }

         if (this.background.get()) {
            renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
         }
      }
   }

   @Override
   public void onFontChanged() {
      this.recalculateSize = true;
   }

   private double getScale() {
      return this.customScale.get() ? this.scale.get() : Hud.get().getTextScale();
   }

   public static Color getSectionColor(int i) {
      List<SettingColor> colors = Hud.get().textColors.get();
      return i >= 0 && i < colors.size() ? colors.get(i) : WHITE;
   }

   public enum Shown {
      Always,
      WhenTrue,
      WhenFalse;

      @Override
      public String toString() {
         return switch (this) {
            case Always -> "Always";
            case WhenTrue -> "When True";
            case WhenFalse -> "When False";
         };
      }
   }
}
