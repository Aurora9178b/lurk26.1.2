package meteordevelopment.meteorclient.systems.hud.elements.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class KeyboardHud extends HudElement {
   public static final HudElementInfo<KeyboardHud> INFO = new HudElementInfo<>(Hud.GROUP, "keyboard", "Displays pressed keys.", KeyboardHud::new);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgColor = this.settings.createGroup("Color");
   private final SettingGroup sgBorder = this.settings.createGroup("Border");
   private final SettingGroup sgBackground = this.settings.createGroup("Background");
   private final Setting<KeyboardHud.Preset> preset = this.sgGeneral
      .add(
         new EnumSetting.Builder<KeyboardHud.Preset>()
            .name("preset")
            .description("Which keys to display.")
            .defaultValue(KeyboardHud.Preset.Movement)
            .onChanged(this::onPresetChanged)
            .build()
      );
   private final Setting<List<KeyboardHud.Key>> customKeys = this.sgGeneral.add(new KeyboardHud.CustomKeyListSetting());
   private final Setting<Double> scale = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("scale")
            .description("Scale of the keyboard.")
            .defaultValue(1.5)
            .min(0.5)
            .sliderRange(0.5, 5.0)
            .decimalPlaces(1)
            .onChanged(var1 -> this.calculateSize())
            .build()
      );
   private final Setting<Double> spacing = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("spacing")
            .description("Spacing between keys.")
            .defaultValue(1.0)
            .min(0.0)
            .sliderRange(0.0, 10.0)
            .decimalPlaces(1)
            .visible(() -> this.preset.get() != KeyboardHud.Preset.Custom)
            .onChanged(var1 -> this.onPresetChanged(this.preset.get()))
            .build()
      );
   private final Setting<KeyboardHud.KeyboardLayout> keyboardLayout = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder()
                           .name("keyboard-layout"))
                        .description("Physical keyboard layout (ANSI or ISO)."))
                     .defaultValue(KeyboardHud.KeyboardLayout.ANSI))
                  .visible(() -> this.preset.get() == KeyboardHud.Preset.Keyboard))
               .onChanged(var1 -> this.onPresetChanged(this.preset.get())))
            .build()
      );
   private final Setting<Boolean> showCps = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("Show CPS")
            .description("Shows clicks per second on keys.")
            .defaultValue(false)
            .visible(() -> this.preset.get() != KeyboardHud.Preset.Custom)
            .onChanged(var1 -> this.onPresetChanged(this.preset.get()))
            .build()
      );
   private final Setting<KeyboardHud.Alignment> alignment = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("alignment"))
                  .description("Horizontal alignment of the text."))
               .defaultValue(KeyboardHud.Alignment.Center))
            .build()
      );
   private final Setting<SettingColor> pressedColor = this.sgColor
      .add(new ColorSetting.Builder().name("pressed-color").description("Color of pressed keys.").defaultValue(new SettingColor(200, 200, 200, 100)).build());
   private final Setting<SettingColor> unpressedColor = this.sgColor
      .add(new ColorSetting.Builder().name("unpressed-color").description("Color of unpressed keys.").defaultValue(new SettingColor(0, 0, 0, 100)).build());
   private final Setting<Boolean> colorFade = this.sgColor
      .add(new BoolSetting.Builder().name("color-fade").description("Whether to fade the key color when pressing/unpressing.").defaultValue(false).build());
   private final Setting<Double> fadeTime = this.sgColor
      .add(
         new DoubleSetting.Builder()
            .name("fade-time")
            .description("How long to fade the color for, in seconds.")
            .visible(this.colorFade::get)
            .defaultValue(0.1)
            .min(0.01)
            .sliderRange(0.01, 0.5)
            .decimalPlaces(2)
            .build()
      );
   private final Setting<SettingColor> textColor = this.sgColor
      .add(new ColorSetting.Builder().name("text-color").description("Color of the key name.").defaultValue(new SettingColor(255, 255, 255)).build());
   private final Setting<Boolean> border = this.sgBorder
      .add(new BoolSetting.Builder().name("border").description("Draw a border around keys.").defaultValue(false).build());
   private final Setting<SettingColor> borderColor = this.sgBorder
      .add(
         new ColorSetting.Builder()
            .name("border-color")
            .description("Color of the key border.")
            .visible(this.border::get)
            .defaultValue(new SettingColor(255, 255, 255, 200))
            .build()
      );
   private final Setting<Double> borderWidth = this.sgBorder
      .add(
         new DoubleSetting.Builder()
            .name("border-width")
            .description("Width of the key border.")
            .visible(this.border::get)
            .defaultValue(1.0)
            .min(0.5)
            .sliderRange(0.5, 5.0)
            .build()
      );
   private final Setting<Double> opacity = this.sgColor
      .add(new DoubleSetting.Builder().name("opacity").description("Opacity of the whole element.").defaultValue(1.0).min(0.0).max(1.0).sliderMax(1.0).build());
   private final Setting<Boolean> background = this.sgBackground
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   private final Setting<SettingColor> backgroundColor = this.sgBackground
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );
   private final List<KeyboardHud.Key> keys = new ArrayList<>();
   private double minX;
   private double minY;

   private Color getColor(SettingColor color, SettingColor out) {
      out.set(color);
      out.a = (int)(out.a * this.opacity.get());
      return out;
   }

   private Color getKeyColor(KeyboardHud.Key key, SettingColor out) {
      if (this.colorFade.get()) {
         boolean pressed = key.isPressed;
         float target = pressed ? 1.0F : 0.0F;
         float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
         float frameDelta = (float)(tickDelta / 20.0F / this.fadeTime.get()) * (pressed ? 1 : -1);
         key.delta = Math.clamp(key.delta + frameDelta, 0.0F, 1.0F);
         if (key.delta == target) {
            out.set(target == 1.0F ? this.pressedColor.get() : this.unpressedColor.get());
         } else {
            Color c1 = this.pressedColor.get();
            Color c2 = this.unpressedColor.get();
            float[] hsb1 = new float[3];
            float[] hsb2 = new float[3];
            java.awt.Color.RGBtoHSB(c1.r, c1.g, c1.b, hsb2);
            java.awt.Color.RGBtoHSB(c2.r, c2.g, c2.b, hsb1);
            int rgb = java.awt.Color.HSBtoRGB(
               Mth.lerp(key.delta, hsb1[0], hsb2[0]), Mth.lerp(key.delta, hsb1[1], hsb2[1]), Mth.lerp(key.delta, hsb1[2], hsb2[2])
            );
            out.r = Color.toRGBAR(rgb);
            out.g = Color.toRGBAG(rgb);
            out.b = Color.toRGBAB(rgb);
            out.a = Mth.lerpInt(key.delta, this.pressedColor.get().a, this.unpressedColor.get().a);
         }
      } else {
         out.set(key.isPressed ? this.pressedColor.get() : this.unpressedColor.get());
      }

      out.a = (int)(out.a * this.opacity.get());
      return out;
   }

   @EventHandler(priority = 100)
   private void onKey(KeyInputEvent event) {
      for (KeyboardHud.Key key : this.keys) {
         if (key.matches(event.input.key(), event.input.scancode(), true)) {
            key.update(event.action);
         }
      }
   }

   @EventHandler(priority = 100)
   private void onMouseClick(MouseClickEvent event) {
      for (KeyboardHud.Key key : this.keys) {
         if (key.matches(event.input.button(), -1, false)) {
            key.update(event.action);
         }
      }
   }

   public KeyboardHud() {
      super(INFO);
      if (MeteorClient.mc.options != null) {
         this.onPresetChanged(this.preset.get());
      }

      MeteorClient.EVENT_BUS.subscribe(this);
   }

   private void onPresetChanged(KeyboardHud.Preset preset) {
      if (MeteorClient.mc.options != null) {
         this.keys.clear();
         double u = 35.0;
         double g = this.spacing.get() * 2.0;
         LayoutContext l = new LayoutContext(u, g, 15.0);
         switch (preset) {
            case Movement:
               this.keys.add(l.key(MeteorClient.mc.options.keyUp, l.ux(1.0), 0.0).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyLeft, 0.0, l.y(1.0)).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyDown, l.ux(1.0), l.y(1.0)).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyRight, l.ux(2.0), l.y(1.0)).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyShift, 0.0, l.y(2.0)).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyJump, l.ux(1.0), l.y(2.0), KeyDimensions.UNIT_2U).setShowCps(this.showCps.get()));
               break;
            case Clicks:
               this.keys.add(l.key(MeteorClient.mc.options.keyAttack, "LMB", 0.0, 0.0).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyUse, "RMB", l.ux(1.0), 0.0).setShowCps(this.showCps.get()));
               break;
            case Actions:
               this.keys.add(l.key(MeteorClient.mc.options.keyDrop, 0.0, 0.0).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keySwapOffhand, l.ux(1.0), 0.0).setShowCps(this.showCps.get()));
               this.keys.add(l.key(MeteorClient.mc.options.keyInventory, l.ux(2.0), 0.0).setShowCps(this.showCps.get()));
               break;
            case Hotbar:
               for (int i = 0; i < 9; i++) {
                  this.keys.add(l.key(MeteorClient.mc.options.keyHotbarSlots[i], l.ux(i), 0.0).setShowCps(this.showCps.get()));
               }
               break;
            case Keyboard:
               if (this.keyboardLayout.get() == KeyboardHud.KeyboardLayout.ANSI) {
                  this.buildAnsiLayout(l);
               } else {
                  this.buildIsoLayout(l);
               }

               for (KeyboardHud.Key key : this.keys) {
                  key.setShowCps(this.showCps.get());
               }
               break;
            case Custom:
               this.keys.addAll(this.customKeys.get());
         }

         this.calculateSize();
      }
   }

   private void buildAnsiLayout(LayoutContext l) {
      double row0 = l.uy(0.0);
      double row1 = l.uy(1.0);
      double row2 = l.uy(2.0);
      double row3 = l.uy(3.0);
      double row4 = l.uy(4.0);
      double row5 = l.uy(5.0);
      this.keys.add(l.key(Keybind.fromKey(256), 0.0, row0));

      for (int i = 0; i < 4; i++) {
         this.keys.add(l.key(Keybind.fromKey(290 + i), l.ux(2.0 + i), row0));
      }

      for (int i = 0; i < 4; i++) {
         this.keys.add(l.key(Keybind.fromKey(294 + i), l.ux(6.5 + i), row0));
      }

      for (int i = 0; i < 4; i++) {
         this.keys.add(l.key(Keybind.fromKey(298 + i), l.ux(11.0 + i), row0));
      }

      this.keys.add(l.key(Keybind.fromKey(283), l.ux(15.5), row0));
      this.keys.add(l.key(Keybind.fromKey(281), l.ux(16.5), row0));
      this.keys.add(l.key(Keybind.fromKey(284), l.ux(17.5), row0));
      int[] row1Keys = new int[]{96, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 45, 61};

      for (int i = 0; i < row1Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row1Keys[i]), l.ux(i), row1));
      }

      this.keys.add(l.key(Keybind.fromKey(259), l.ux(13.0), row1, KeyDimensions.BACKSPACE));
      this.keys.add(l.key(Keybind.fromKey(260), l.ux(15.5), row1));
      this.keys.add(l.key(Keybind.fromKey(268), l.ux(16.5), row1));
      this.keys.add(l.key(Keybind.fromKey(266), l.ux(17.5), row1));
      this.keys.add(l.key(Keybind.fromKey(258), 0.0, row2, KeyDimensions.TAB));
      int[] row2Keys = new int[]{81, 87, 69, 82, 84, 89, 85, 73, 79, 80, 91, 93};
      double tabEnd = l.px(KeyDimensions.TAB) + l.keyGap;

      for (int i = 0; i < row2Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row2Keys[i]), tabEnd + l.ux(i), row2));
      }

      this.keys.add(l.key(Keybind.fromKey(92), tabEnd + l.ux(12.0), row2, KeyDimensions.TAB));
      this.keys.add(l.key(Keybind.fromKey(261), l.ux(15.5), row2));
      this.keys.add(l.key(Keybind.fromKey(269), l.ux(16.5), row2));
      this.keys.add(l.key(Keybind.fromKey(267), l.ux(17.5), row2));
      this.keys.add(l.key(Keybind.fromKey(280), 0.0, row3, KeyDimensions.CAPS_LOCK));
      int[] row3Keys = new int[]{65, 83, 68, 70, 71, 72, 74, 75, 76, 59, 39};
      double capsEnd = l.px(KeyDimensions.CAPS_LOCK) + l.keyGap;

      for (int i = 0; i < row3Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row3Keys[i]), capsEnd + l.ux(i), row3));
      }

      this.keys.add(l.key(Keybind.fromKey(257), capsEnd + l.ux(11.0), row3, KeyDimensions.ENTER_ANSI));
      this.keys.add(l.key(Keybind.fromKey(340), 0.0, row4, KeyDimensions.LEFT_SHIFT_ANSI));
      int[] row4Keys = new int[]{90, 88, 67, 86, 66, 78, 77, 44, 46, 47};
      double lShiftEnd = l.px(KeyDimensions.LEFT_SHIFT_ANSI) + l.keyGap;

      for (int i = 0; i < row4Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row4Keys[i]), lShiftEnd + l.ux(i), row4));
      }

      this.keys.add(l.key(Keybind.fromKey(344), lShiftEnd + l.ux(10.0), row4, KeyDimensions.RIGHT_SHIFT));
      this.keys.add(l.key(Keybind.fromKey(265), l.ux(16.5), row4));
      double xPos = 0.0;
      this.keys.add(l.key(Keybind.fromKey(341), xPos, row5, KeyDimensions.CTRL));
      xPos += l.px(KeyDimensions.CTRL) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(343), xPos, row5, KeyDimensions.GUI));
      xPos += l.px(KeyDimensions.GUI) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(342), xPos, row5, KeyDimensions.ALT));
      xPos += l.px(KeyDimensions.ALT) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(32), xPos, row5, KeyDimensions.SPACEBAR));
      xPos += l.px(KeyDimensions.SPACEBAR) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(346), xPos, row5, KeyDimensions.ALT));
      xPos += l.px(KeyDimensions.ALT) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(347), xPos, row5, KeyDimensions.GUI));
      xPos += l.px(KeyDimensions.GUI) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(348), xPos, row5, KeyDimensions.MENU));
      xPos += l.px(KeyDimensions.MENU) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(345), xPos, row5, KeyDimensions.CTRL));
      this.keys.add(l.key(Keybind.fromKey(263), l.ux(15.5), row5));
      this.keys.add(l.key(Keybind.fromKey(264), l.ux(16.5), row5));
      this.keys.add(l.key(Keybind.fromKey(262), l.ux(17.5), row5));
   }

   private void buildIsoLayout(LayoutContext l) {
      double row0 = l.uy(0.0);
      double row1 = l.uy(1.0);
      double row2 = l.uy(2.0);
      double row3 = l.uy(3.0);
      double row4 = l.uy(4.0);
      double row5 = l.uy(5.0);
      this.keys.add(l.key(Keybind.fromKey(256), 0.0, row0));

      for (int i = 0; i < 4; i++) {
         this.keys.add(l.key(Keybind.fromKey(290 + i), l.ux(2.0 + i), row0));
      }

      for (int i = 0; i < 4; i++) {
         this.keys.add(l.key(Keybind.fromKey(294 + i), l.ux(6.5 + i), row0));
      }

      for (int i = 0; i < 4; i++) {
         this.keys.add(l.key(Keybind.fromKey(298 + i), l.ux(11.0 + i), row0));
      }

      this.keys.add(l.key(Keybind.fromKey(283), l.ux(15.5), row0));
      this.keys.add(l.key(Keybind.fromKey(281), l.ux(16.5), row0));
      this.keys.add(l.key(Keybind.fromKey(284), l.ux(17.5), row0));
      int[] row1Keys = new int[]{96, 49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 45, 61};

      for (int i = 0; i < row1Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row1Keys[i]), l.ux(i), row1));
      }

      this.keys.add(l.key(Keybind.fromKey(259), l.ux(13.0), row1, KeyDimensions.BACKSPACE));
      this.keys.add(l.key(Keybind.fromKey(260), l.ux(15.5), row1));
      this.keys.add(l.key(Keybind.fromKey(268), l.ux(16.5), row1));
      this.keys.add(l.key(Keybind.fromKey(266), l.ux(17.5), row1));
      this.keys.add(l.key(Keybind.fromKey(258), 0.0, row2, KeyDimensions.TAB));
      int[] row2Keys = new int[]{81, 87, 69, 82, 84, 89, 85, 73, 79, 80, 91, 93};
      double tabEnd = l.px(KeyDimensions.TAB) + l.keyGap;

      for (int i = 0; i < row2Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row2Keys[i]), tabEnd + l.ux(i), row2));
      }

      this.keys.add(l.key(Keybind.fromKey(261), l.ux(15.5), row2));
      this.keys.add(l.key(Keybind.fromKey(269), l.ux(16.5), row2));
      this.keys.add(l.key(Keybind.fromKey(267), l.ux(17.5), row2));
      this.keys.add(l.key(Keybind.fromKey(280), 0.0, row3, KeyDimensions.CAPS_LOCK));
      int[] row3Keys = new int[]{65, 83, 68, 70, 71, 72, 74, 75, 76, 59, 39};
      double capsEnd = l.px(KeyDimensions.CAPS_LOCK) + l.keyGap;

      for (int i = 0; i < row3Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row3Keys[i]), capsEnd + l.ux(i), row3));
      }

      this.keys.add(l.key(Keybind.fromKey(92), capsEnd + l.ux(11.0), row3));
      double topBarStartX = tabEnd + l.ux(12.0);
      double mainBlockRightEdge = tabEnd + l.ux(12.0) + l.px(KeyDimensions.TAB);
      double enterStemWidth = l.px(KeyDimensions.ENTER_ISO_WIDTH);
      double enterStemHeight = l.px(KeyDimensions.ENTER_ISO_HEIGHT);
      double enterStemX = mainBlockRightEdge - enterStemWidth;
      this.keys.add(new KeyboardHud.IsoEnterKey(Keybind.fromKey(257), enterStemX, row2, enterStemWidth, enterStemHeight, topBarStartX));
      this.keys.add(l.key(Keybind.fromKey(340), 0.0, row4, KeyDimensions.LEFT_SHIFT_ISO));
      double lShiftEnd = l.px(KeyDimensions.LEFT_SHIFT_ISO) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(162), lShiftEnd, row4));
      int[] row4Keys = new int[]{90, 88, 67, 86, 66, 78, 77, 44, 46, 47};

      for (int i = 0; i < row4Keys.length; i++) {
         this.keys.add(l.key(Keybind.fromKey(row4Keys[i]), lShiftEnd + l.ux(1.0 + i), row4));
      }

      double rShiftX = lShiftEnd + l.ux(11.0);
      this.keys.add(l.key(Keybind.fromKey(344), rShiftX, row4, KeyDimensions.RIGHT_SHIFT));
      this.keys.add(l.key(Keybind.fromKey(265), l.ux(16.5), row4));
      double xPos = 0.0;
      this.keys.add(l.key(Keybind.fromKey(341), xPos, row5, KeyDimensions.CTRL));
      xPos += l.px(KeyDimensions.CTRL) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(343), xPos, row5, KeyDimensions.GUI));
      xPos += l.px(KeyDimensions.GUI) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(342), xPos, row5, KeyDimensions.ALT));
      xPos += l.px(KeyDimensions.ALT) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(32), xPos, row5, KeyDimensions.SPACEBAR));
      xPos += l.px(KeyDimensions.SPACEBAR) + l.keyGap;
      this.keys.add(l.keyNamed(Keybind.fromKey(346), "AltGr", xPos, row5, KeyDimensions.ALT));
      xPos += l.px(KeyDimensions.ALT) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(347), xPos, row5, KeyDimensions.GUI));
      xPos += l.px(KeyDimensions.GUI) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(348), xPos, row5, KeyDimensions.MENU));
      xPos += l.px(KeyDimensions.MENU) + l.keyGap;
      this.keys.add(l.key(Keybind.fromKey(345), xPos, row5, KeyDimensions.CTRL));
      this.keys.add(l.key(Keybind.fromKey(263), l.ux(15.5), row5));
      this.keys.add(l.key(Keybind.fromKey(264), l.ux(16.5), row5));
      this.keys.add(l.key(Keybind.fromKey(262), l.ux(17.5), row5));
   }

   private void calculateSize() {
      if (this.keys.isEmpty()) {
         this.setSize(0.0, 0.0);
      } else {
         this.minX = this.minY = 0.0;
         double maxX = 0.0;
         double maxY = 0.0;

         for (KeyboardHud.Key key : this.keys) {
            this.minX = Math.min(this.minX, key.x);
            this.minY = Math.min(this.minY, key.y);
            maxX = Math.max(maxX, key.x + key.width);
            maxY = Math.max(maxY, key.y + key.height);
         }

         this.setSize((maxX - this.minX) * this.scale.get(), (maxY - this.minY) * this.scale.get());
      }
   }

   private static String getShortName(String name) {
      return switch (name.toUpperCase(Locale.ROOT)) {
         case "LEFT SHIFT", "LSHIFT" -> "LSh";
         case "RIGHT SHIFT", "RSHIFT" -> "RSh";
         case "LEFT CONTROL", "LCTRL" -> "LCtrl";
         case "RIGHT CONTROL", "RCTRL" -> "RCtrl";
         case "LEFT ALT", "LALT" -> "LAlt";
         case "RIGHT ALT", "RALT" -> "RAlt";
         case "LEFT SUPER" -> "LSup";
         case "RIGHT SUPER" -> "RSup";
         case "GRAVE ACCENT" -> "`";
         case "COMMA" -> ",";
         case "DOT", "PERIOD" -> ".";
         case "SLASH" -> "/";
         case "APOSTROPHE" -> "'";
         case "BACKSPACE" -> "BS";
         case "ENTER" -> "Ent";
         case "SCROLL", "SCROLL LOCK" -> "ScrL";
         case "PRINT", "PRTSC", "PRINT SCREEN" -> "PrtS";
         case "PAUSE" -> "Paus";
         case "PAGEUP", "PAGE UP", "PGUP" -> "PgUp";
         case "PAGEDOWN", "PAGE DOWN", "PGDN" -> "PgDn";
         case "INSERT", "INS" -> "Ins";
         case "DELETE", "DEL" -> "Del";
         case "HOME" -> "Home";
         case "END" -> "End";
         case "ARROW UP", "UP" -> "Up";
         case "ARROW DOWN", "DOWN" -> "Dn";
         case "ARROW LEFT", "LEFT" -> "Lt";
         case "ARROW RIGHT", "RIGHT" -> "Rt";
         case "WORLD 1" -> "#";
         case "WORLD 2" -> "\\";
         case "UNKNOWN" -> "?";
         default -> name;
      };
   }

   @Override
   public void render(HudRenderer renderer) {
      if (this.keys.isEmpty()) {
         if (MeteorClient.mc.options != null) {
            this.onPresetChanged(this.preset.get());
         }
      } else {
         SettingColor mutableColor = new SettingColor();
         this.pressedColor.get().update();
         this.unpressedColor.get().update();
         this.textColor.get().update();
         this.borderColor.get().update();
         this.backgroundColor.get().update();
         if (this.background.get()) {
            renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.getColor(this.backgroundColor.get(), mutableColor));
         }

         double s = this.scale.get();
         InputConstants.Key guiKey = ((KeyMappingAccessor)KeyBinds.OPEN_GUI).meteor$getKey();

         for (KeyboardHud.Key key : this.keys) {
            if (key.matches(guiKey.getValue(), guiKey.getValue(), guiKey.getType() != Type.MOUSE) && key.isPressed != key.isNativelyPressed()) {
               key.update(key.isPressed ? KeyAction.Release : KeyAction.Press);
            }

            if (key instanceof KeyboardHud.IsoEnterKey isoEnter) {
               isoEnter.render(this, renderer, s, mutableColor);
            } else {
               Color color = this.getKeyColor(key, mutableColor);
               double kX = this.x + (key.x - this.minX) * s;
               double kY = this.y + (key.y - this.minY) * s;
               double kW = key.width * s;
               double kH = key.height * s;
               renderer.quad(kX, kY, kW, kH, color);
               if (this.border.get()) {
                  Color bColor = this.getColor(this.borderColor.get(), mutableColor);
                  double bw = this.borderWidth.get();
                  renderer.quad(kX, kY, kW, bw, bColor);
                  renderer.quad(kX, kY + kH - bw, kW, bw, bColor);
                  renderer.quad(kX, kY, bw, kH, bColor);
                  renderer.quad(kX + kW - bw, kY, bw, kH, bColor);
               }

               String text = key.getName();
               Color txtColor = this.getColor(this.textColor.get(), mutableColor);
               double padding = 2.0 * s;
               double availableWidth = kW - padding;
               if (!key.showCps) {
                  double textScale = Math.min(1.0, availableWidth / renderer.textWidth(text, 1.0));
                  double textWidth = renderer.textWidth(text, textScale);
                  double yText = kY + (kH - renderer.textHeight(false, textScale)) / 2.0;
                  this.drawTextLine(renderer, text, textWidth, kX, yText, kW, textScale, txtColor);
               } else {
                  double topScale = Math.min(1.0, availableWidth / renderer.textWidth(text, 1.0));
                  double topWidth = renderer.textWidth(text, topScale);
                  double topHeight = renderer.textHeight(false, topScale);
                  String cpsText = key.getCps() + " CPS";
                  double botScale = Math.min(1.0, availableWidth / renderer.textWidth(cpsText, 1.0));
                  double botWidth = renderer.textWidth(cpsText, botScale);
                  double botHeight = renderer.textHeight(false, botScale);
                  double totalHeight = topHeight + botHeight;
                  double startY = kY + (kH - totalHeight) / 2.0;
                  this.drawTextLine(renderer, text, topWidth, kX, startY, kW, topScale, txtColor);
                  this.drawTextLine(renderer, cpsText, botWidth, kX, startY + topHeight, kW, botScale, txtColor);
               }
            }
         }
      }
   }

   private void drawTextLine(HudRenderer renderer, String text, double textWidth, double x, double y, double w, double textScale, Color color) {
      double s = this.scale.get();
      double padding = 2.0 * s;
      double xText = x + (w - textWidth) / 2.0;
      if (this.alignment.get() == KeyboardHud.Alignment.Left) {
         xText = x + padding;
      } else if (this.alignment.get() == KeyboardHud.Alignment.Right) {
         xText = x + w - padding - textWidth;
      }

      renderer.text(text, xText, y, color, false, textScale);
   }

   public static void fillTable(GuiTheme theme, WTable table, KeyboardHud.CustomKeyListSetting setting) {
      table.clear();
      Iterator<KeyboardHud.Key> it = setting.get().iterator();

      while (it.hasNext()) {
         KeyboardHud.Key key = it.next();
         table.add(theme.label("Key")).expandWidgetX().widget().color(theme.textSecondaryColor());
         table.add(theme.label(String.format("(%s)", key.keybind))).expandWidgetX();
         WButton edit = table.add(theme.button(GuiRenderer.EDIT)).expandCellX().widget();
         edit.action = () -> {
            WidgetScreen screen = (WidgetScreen)MeteorClient.mc.screen;
            MeteorClient.mc.setScreen(new KeyboardHud.CustomKeySettingScreen(theme, setting, key, screen));
         };
         WMinus delete = table.add(theme.minus()).right().widget();
         delete.action = () -> {
            it.remove();
            setting.onChanged();
            fillTable(theme, table, setting);
         };
         table.row();
      }

      if (!setting.get().isEmpty()) {
         table.add(theme.horizontalSeparator()).expandX();
         table.row();
      }

      WButton add = table.add(theme.button("Add")).expandX().widget();
      add.action = () -> {
         KeyboardHud.Key newKey = new KeyboardHud.Key();
         if (!setting.get().isEmpty()) {
            KeyboardHud.Key lastKey = setting.get().getLast();
            newKey.x = lastKey.x + lastKey.width + 10.0;
            newKey.y = lastKey.y;
         }

         setting.get().add(newKey);
         setting.onChanged();
         fillTable(theme, table, setting);
      };
      WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
      reset.action = () -> {
         setting.reset();
         fillTable(theme, table, setting);
      };
      reset.tooltip = "Reset";
   }

   public enum Alignment {
      Left,
      Center,
      Right;
   }

   public class CustomKeyListSetting extends Setting<List<KeyboardHud.Key>> {
      public CustomKeyListSetting() {
         super(
            "custom-keys",
            "Configure the custom keys display.",
            List.of(),
            var1 -> KeyboardHud.this.onPresetChanged(KeyboardHud.this.preset.get()),
            var0 -> {},
            () -> KeyboardHud.this.preset.get() == KeyboardHud.Preset.Custom
         );
      }

      @Override
      protected void resetImpl() {
         this.value = new ObjectArrayList<>();
         this.value.add(new KeyboardHud.Key());
      }

      protected List<KeyboardHud.Key> parseImpl(String str) {
         return List.of();
      }

      protected boolean isValueValid(List<KeyboardHud.Key> value) {
         return true;
      }

      @Override
      protected CompoundTag save(CompoundTag tag) {
         ListTag valueTag = new ListTag();

         for (KeyboardHud.Key key : this.get()) {
            valueTag.add(key.serialize());
         }

         tag.put("value", valueTag);
         return tag;
      }

      protected List<KeyboardHud.Key> load(CompoundTag tag) {
         this.get().clear();

         for (Tag tagI : tag.getListOrEmpty("value")) {
            tagI.asCompound().ifPresent(CompoundTag -> this.get().add(new KeyboardHud.Key(CompoundTag)));
         }

         return this.get();
      }
   }

   public static class CustomKeySettingScreen extends WindowScreen {
      private final KeyboardHud.CustomKeyListSetting setting;
      private final KeyboardHud.Key key;
      private final WidgetScreen screen;

      public CustomKeySettingScreen(GuiTheme theme, KeyboardHud.CustomKeyListSetting setting, KeyboardHud.Key key, WidgetScreen screen) {
         super(theme, "Select Key");
         this.setting = setting;
         this.key = key;
         this.screen = screen;
      }

      @Override
      public void initWidgets() {
         Settings settings = new Settings();
         SettingGroup sgGeneral = settings.getDefaultGroup();
         sgGeneral.add(new KeybindSetting.Builder().name("custom-key").description("The key to display.").defaultValue(Keybind.fromKey(32)).onChanged(k -> {
            this.key.keybind = k;
            this.screen.reload();
         }).onModuleActivated(setting -> setting.set(this.key.keybind)).build());
         sgGeneral.add(
            new StringSetting.Builder()
               .name("custom-label")
               .description("Replace the Key name with custom text.")
               .defaultValue("")
               .onChanged(s -> this.key.name = s)
               .onModuleActivated(setting -> setting.set(this.key.name))
               .build()
         );
         sgGeneral.add(
            new DoubleSetting.Builder()
               .name("key-width")
               .description("Width of the key.")
               .defaultValue(60.0)
               .min(20.0)
               .sliderRange(20.0, 200.0)
               .decimalPlaces(1)
               .onChanged(d -> {
                  this.key.width = d;
                  this.setting.onChanged();
               })
               .onModuleActivated(setting -> setting.set(this.key.width))
               .build()
         );
         sgGeneral.add(
            new DoubleSetting.Builder()
               .name("key-height")
               .description("Height of the key.")
               .defaultValue(40.0)
               .min(20.0)
               .sliderRange(20.0, 200.0)
               .decimalPlaces(1)
               .onChanged(d -> {
                  this.key.height = d;
                  this.setting.onChanged();
               })
               .onModuleActivated(setting -> setting.set(this.key.height))
               .build()
         );
         sgGeneral.add(
            new DoubleSetting.Builder()
               .name("key-x")
               .description("X position offset of the key.")
               .defaultValue(0.0)
               .sliderRange(-200.0, 200.0)
               .decimalPlaces(1)
               .onChanged(d -> {
                  this.key.x = d;
                  this.setting.onChanged();
               })
               .onModuleActivated(setting -> setting.set(this.key.x))
               .build()
         );
         sgGeneral.add(
            new DoubleSetting.Builder()
               .name("key-y")
               .description("Y position offset of the key.")
               .defaultValue(0.0)
               .sliderRange(-200.0, 200.0)
               .decimalPlaces(1)
               .onChanged(d -> {
                  this.key.y = d;
                  this.setting.onChanged();
               })
               .onModuleActivated(setting -> setting.set(this.key.y))
               .build()
         );
         sgGeneral.add(
            new BoolSetting.Builder()
               .name("show-cps")
               .description("Show CPS for this key.")
               .defaultValue(false)
               .onChanged(b -> this.key.showCps = b)
               .onModuleActivated(setting -> setting.set(this.key.showCps))
               .build()
         );
         settings.onActivated();
         this.add(this.theme.settings(settings)).expandX();
      }
   }

   public static class IsoEnterKey extends KeyboardHud.Key {
      private final double topBarStartX;

      public IsoEnterKey(Keybind keybind, double x, double y, double width, double height, double topBarStartX) {
         super(keybind, null, x, y, width, height);
         this.topBarStartX = topBarStartX;
      }

      public void render(KeyboardHud hud, HudRenderer renderer, double s, SettingColor mutableColor) {
         double kX = hud.x + (this.x - hud.minX) * s;
         double kY = hud.y + (this.y - hud.minY) * s;
         double kW = this.width * s;
         double kH = this.height * s;
         double u = 35.0 * s;
         Color color = hud.getKeyColor(this, mutableColor);
         double stemRight = kX + kW;
         double topBarX = hud.x + (this.topBarStartX - hud.minX) * s;
         double topBarLeftWidth = stemRight - topBarX - kW;
         if (topBarLeftWidth > 0.0) {
            renderer.quad(topBarX, kY, topBarLeftWidth, u, color);
         }

         renderer.quad(kX, kY, kW, u, color);
         renderer.quad(kX, kY + u, kW, kH - u, color);
         if (hud.border.get()) {
            Color bColor = hud.getColor(hud.borderColor.get(), mutableColor);
            double bw = hud.borderWidth.get();
            double fullTopBarWidth = topBarLeftWidth + kW;
            renderer.quad(topBarX, kY, fullTopBarWidth, bw, bColor);
            renderer.quad(topBarX, kY, bw, u, bColor);
            renderer.quad(topBarX + fullTopBarWidth - bw, kY, bw, kH, bColor);
            renderer.quad(kX, kY + kH - bw, kW, bw, bColor);
            renderer.quad(kX, kY + u, bw, kH - u, bColor);
            if (topBarLeftWidth > 0.0) {
               renderer.quad(topBarX, kY + u - bw, topBarLeftWidth, bw, bColor);
            }
         }

         String text = this.getName();
         Color txtColor = hud.getColor(hud.textColor.get(), mutableColor);
         double padding = 2.0 * s;
         double availableWidth = kW - padding * 2.0;
         double availableHeight = kH - padding * 2.0;
         double tH = renderer.textHeight();
         double tW = renderer.textWidth(text);
         double widthScale = tW > availableWidth ? availableWidth / tW : 1.0;
         double heightScale = tH > availableHeight * 0.6 ? availableHeight * 0.6 / tH : 1.0;
         double textScale = Math.min(widthScale, heightScale);
         double yText = kY + (kH - tH * textScale) / 2.0;
         hud.drawTextLine(renderer, text, tW, kX, yText, kW, textScale, txtColor);
      }
   }

   public static class Key {
      public String name = "";
      public KeyMapping binding;
      public Keybind keybind;
      public double x;
      public double y;
      public double width;
      public double height;
      public boolean showCps = false;
      private final KeyboardHud.RollingCps rollingCps = new KeyboardHud.RollingCps();
      private boolean isPressed;
      private float delta;

      public Key() {
         this.keybind = Keybind.fromKey(32);
         this.width = 60.0;
         this.height = 40.0;
      }

      public Key(CompoundTag compound) {
         this.keybind = Keybind.none().fromTag(compound.getCompoundOrEmpty("key"));
         this.name = compound.getStringOr("name", "");
         this.x = compound.getDoubleOr("x", 0.0);
         this.y = compound.getDoubleOr("y", 0.0);
         this.width = compound.getDoubleOr("width", 60.0);
         this.height = compound.getDoubleOr("height", 60.0);
         this.showCps = compound.getBooleanOr("showCps", false);
      }

      Key(KeyMapping binding, String name, double x, double y, double width, double height) {
         this.binding = binding;
         this.name = name;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      Key(Keybind keybind, String name, double x, double y, double width, double height) {
         this.keybind = keybind;
         this.name = name;
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
      }

      public KeyboardHud.Key setShowCps(boolean show) {
         this.showCps = show;
         return this;
      }

      public String getName() {
         if (this.name != null && !this.name.isEmpty()) {
            return this.name;
         } else if (this.keybind != null) {
            return KeyboardHud.getShortName(this.keybind.toString());
         } else {
            return this.binding != null ? KeyboardHud.getShortName(this.binding.getTranslatedKeyMessage().getString()) : "?";
         }
      }

      public boolean matches(int input, int scancode, boolean key) {
         if (this.keybind != null) {
            return this.keybind.isKey() == key && this.keybind.getValue() == input;
         }

         InputConstants.Key inputKey = ((KeyMappingAccessor)this.binding).meteor$getKey();
         boolean isKey = inputKey.getType() != Type.MOUSE;
         return isKey == key && inputKey.getType() == Type.SCANCODE ? scancode == inputKey.getValue() : input == inputKey.getValue();
      }

      public void update(KeyAction action) {
         if (action != KeyAction.Release) {
            this.isPressed = true;
            if (this.showCps && action == KeyAction.Press) {
               this.rollingCps.add();
            }
         } else {
            this.isPressed = false;
         }
      }

      public boolean isNativelyPressed() {
         long window = MeteorClient.mc.getWindow().handle();
         if (this.keybind != null) {
            if (!this.keybind.isSet()) {
               return false;
            } else {
               return this.keybind.isKey()
                  ? GLFW.glfwGetKey(window, this.keybind.getValue()) != 0
                  : GLFW.glfwGetMouseButton(window, this.keybind.getValue()) != 0;
            }
         } else {
            int key = ((KeyMappingAccessor)this.binding).meteor$getKey().getValue();
            return key >= 0 && key < 8 ? GLFW.glfwGetMouseButton(window, key) != 0 : GLFW.glfwGetKey(window, key) != 0;
         }
      }

      public int getCps() {
         return this.rollingCps.get();
      }

      public CompoundTag serialize() {
         CompoundTag compound = new CompoundTag();
         compound.put("key", this.keybind.toTag());
         compound.putString("name", this.name);
         compound.putDouble("x", this.x);
         compound.putDouble("y", this.y);
         compound.putDouble("width", this.width);
         compound.putDouble("height", this.height);
         compound.putBoolean("showCps", this.showCps);
         return compound;
      }
   }

   public enum KeyboardLayout {
      ANSI,
      ISO;
   }

   public enum Preset {
      Movement,
      Clicks,
      Actions,
      Hotbar,
      Keyboard,
      Custom;
   }

   private static class RollingCps {
      private final LongList clicks = new LongArrayList();

      public void add() {
         this.clicks.add(System.currentTimeMillis());
      }

      public int get() {
         long time = System.currentTimeMillis();
         this.clicks.removeIf(val -> val + 1000L < time);
         return this.clicks.size();
      }
   }
}
