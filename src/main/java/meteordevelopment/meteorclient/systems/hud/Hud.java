package meteordevelopment.meteorclient.systems.hud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorListSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.elements.ActiveModulesHud;
import meteordevelopment.meteorclient.systems.hud.elements.ArmorHud;
import meteordevelopment.meteorclient.systems.hud.elements.CombatHud;
import meteordevelopment.meteorclient.systems.hud.elements.CompassHud;
import meteordevelopment.meteorclient.systems.hud.elements.CooldownsHud;
import meteordevelopment.meteorclient.systems.hud.elements.DynamicIslandHud;
import meteordevelopment.meteorclient.systems.hud.elements.DynamicIslandV2;
import meteordevelopment.meteorclient.systems.hud.elements.HoleHud;
import meteordevelopment.meteorclient.systems.hud.elements.Info;
import meteordevelopment.meteorclient.systems.hud.elements.InventoryHud;
import meteordevelopment.meteorclient.systems.hud.elements.ItemHud;
import meteordevelopment.meteorclient.systems.hud.elements.LagNotifierHud;
import meteordevelopment.meteorclient.systems.hud.elements.MapHud;
import meteordevelopment.meteorclient.systems.hud.elements.MeteorTextHud;
import meteordevelopment.meteorclient.systems.hud.elements.ModuleInfosHud;
import meteordevelopment.meteorclient.systems.hud.elements.ModuleListHud;
import meteordevelopment.meteorclient.systems.hud.elements.PacketStatsHud;
import meteordevelopment.meteorclient.systems.hud.elements.PigHud;
import meteordevelopment.meteorclient.systems.hud.elements.PlayerModelHud;
import meteordevelopment.meteorclient.systems.hud.elements.PlayerRadarHud;
import meteordevelopment.meteorclient.systems.hud.elements.PotionHUD;
import meteordevelopment.meteorclient.systems.hud.elements.PotionTimersHud;
import meteordevelopment.meteorclient.systems.hud.elements.RadarHud;
import meteordevelopment.meteorclient.systems.hud.elements.ScoreboardHud;
import meteordevelopment.meteorclient.systems.hud.elements.TargetHUD;
import meteordevelopment.meteorclient.systems.hud.elements.keyboard.KeyboardHud;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public class Hud extends System<Hud> implements Iterable<HudElement> {
   public static final HudGroup GROUP = new HudGroup("Meteor");
   public boolean active;
   public Settings settings = new Settings();
   public final Map<String, HudElementInfo<?>> infos = new TreeMap<>();
   private final List<HudElement> elements = new ArrayList<>();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgEditor = this.settings.createGroup("Editor");
   private final SettingGroup sgKeybind = this.settings.createGroup("Bind");
   private final Setting<Boolean> customFont = this.sgGeneral
      .add(new BoolSetting.Builder().name("custom-font").description("Text will use custom font.").defaultValue(true).onChanged(var1 -> {
         for (HudElement element : this.elements) {
            element.onFontChanged();
         }
      }).build());
   private final Setting<Boolean> hideInMenus = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("hide-in-menus")
            .description("Hides the meteor hud when in inventory screens or game menus.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Double> textScale = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("text-scale")
            .description("Scale of text if not overridden by the element.")
            .defaultValue(1.0)
            .min(0.5)
            .sliderRange(0.5, 3.0)
            .build()
      );
   public final Setting<List<SettingColor>> textColors = this.sgGeneral
      .add(
         new ColorListSetting.Builder()
            .name("text-colors")
            .description("Colors used for the Text element.")
            .defaultValue(List.of(new SettingColor(), new SettingColor(175, 175, 175), new SettingColor(25, 225, 25), new SettingColor(225, 25, 25)))
            .build()
      );
   public final Setting<Integer> border = this.sgEditor
      .add(new IntSetting.Builder().name("border").description("Space around the edges of the screen.").defaultValue(4).sliderMax(20).build());
   public final Setting<Integer> snappingRange = this.sgEditor
      .add(new IntSetting.Builder().name("snapping-range").description("Snapping range in editor.").defaultValue(10).sliderMax(20).build());
   private final Setting<Keybind> keybind = this.sgKeybind
      .add(new KeybindSetting.Builder().name("bind").defaultValue(Keybind.none()).action(() -> this.active = !this.active).build());
   private boolean resetToDefaultElements;

   public Hud() {
      super("hud");
   }

   public static Hud get() {
      return Systems.get(Hud.class);
   }

   @Override
   public void init() {
      this.settings.registerColorSettings(null);
      this.register(MeteorTextHud.INFO);
      this.register(ItemHud.INFO);
      this.register(InventoryHud.INFO);
      this.register(CompassHud.INFO);
      this.register(ArmorHud.INFO);
      this.register(HoleHud.INFO);
      this.register(PlayerModelHud.INFO);
      this.register(ActiveModulesHud.INFO);
      this.register(ModuleListHud.INFO);
      this.register(LagNotifierHud.INFO);
      this.register(PlayerRadarHud.INFO);
      this.register(RadarHud.INFO);
      this.register(PacketStatsHud.INFO);
      this.register(ModuleInfosHud.INFO);
      this.register(PotionTimersHud.INFO);
      this.register(PotionHUD.INFO);
      this.register(CombatHud.INFO);
      this.register(TargetHUD.INFO);
      this.register(CooldownsHud.INFO);
      this.register(MapHud.INFO);
      this.register(KeyboardHud.INFO);
      this.register(ScoreboardHud.INFO);
      this.register(Info.INFO);
      this.register(DynamicIslandHud.INFO);
      this.register(DynamicIslandV2.INFO);
      this.register(PigHud.INFO);
      if (this.isFirstInit) {
         this.resetToDefaultElements();
      }
   }

   public void register(HudElementInfo<?> info) {
      this.infos.put(info.name, info);
   }

   private void add(HudElement element, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
      element.box.setPos(x, y);
      if (xAnchor != null && yAnchor != null) {
         element.box.xAnchor = xAnchor;
         element.box.yAnchor = yAnchor;
      } else {
         element.box.updateAnchors();
      }

      element.settings.registerColorSettings(null);
      this.elements.add(element);
   }

   public void add(HudElementInfo<?> info, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
      this.add(info.create(), x, y, xAnchor, yAnchor);
   }

   public void add(HudElementInfo<?> info, int x, int y) {
      this.add(info, x, y, null, null);
   }

   public void add(@NotNull HudElementInfo.Preset preset, int x, int y, XAnchor xAnchor, YAnchor yAnchor) {
      HudElement element = preset.info.create();
      preset.callback.accept(element);
      this.add(element, x, y, xAnchor, yAnchor);
   }

   public void add(@NotNull HudElementInfo.Preset preset, int x, int y) {
      this.add(preset, x, y, null, null);
   }

   void remove(HudElement element) {
      element.settings.unregisterColorSettings();
      this.elements.remove(element);
   }

   public void clear() {
      this.elements.clear();
   }

   public void resetToDefaultElements() {
      this.resetToDefaultElements = true;
   }

   private void resetToDefaultElementsImpl() {
      this.elements.clear();
      int h = (int)Math.ceil(HudRenderer.INSTANCE.textHeight(true));
      this.add(MeteorTextHud.WATERMARK, 4, 4, XAnchor.Left, YAnchor.Top);
      this.add(MeteorTextHud.FPS, 4, 4 + h, XAnchor.Left, YAnchor.Top);
      this.add(MeteorTextHud.TPS, 4, 4 + h * 2, XAnchor.Left, YAnchor.Top);
      this.add(MeteorTextHud.PING, 4, 4 + h * 3, XAnchor.Left, YAnchor.Top);
      this.add(MeteorTextHud.SPEED, 4, 4 + h * 4, XAnchor.Left, YAnchor.Top);
      this.add(ActiveModulesHud.INFO, -4, 4, XAnchor.Right, YAnchor.Top);
      this.add(MeteorTextHud.POSITION, -4, -4, XAnchor.Right, YAnchor.Bottom);
      this.add(MeteorTextHud.OPPOSITE_POSITION, -4, -4 - h, XAnchor.Right, YAnchor.Bottom);
      this.add(MeteorTextHud.ROTATION, -4, -4 - h * 2, XAnchor.Right, YAnchor.Bottom);
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (!Utils.isLoading()) {
         if (this.resetToDefaultElements) {
            this.resetToDefaultElementsImpl();
            this.resetToDefaultElements = false;
         }

         if (this.active || HudEditorScreen.isOpen()) {
            for (HudElement element : this.elements) {
               if (element.isActive() || element.isInEditor()) {
                  element.tick(HudRenderer.INSTANCE);
               }
            }
         }
      }
   }

   @EventHandler
   private void onRender(Render2DEvent event) {
      if (!Utils.isLoading()) {
         if ((this.active || HudEditorScreen.isOpen()) && !this.shouldHideHud()) {
            if (!MeteorClient.mc.options.hideGui && !MeteorClient.mc.debugEntries.isOverlayVisible() || HudEditorScreen.isOpen()) {
               HudRenderer.INSTANCE.begin(event.graphics);

               for (HudElement element : this.elements) {
                  element.updatePos();
                  if (element.isActive() || element.isInEditor()) {
                     element.render(HudRenderer.INSTANCE);
                  }
               }

               HudRenderer.INSTANCE.end();
            }
         }
      }
   }

   private boolean shouldHideHud() {
      return this.hideInMenus.get() && MeteorClient.mc.screen != null && !(MeteorClient.mc.screen instanceof WidgetScreen);
   }

   @EventHandler
   private void onCustomFontChanged(CustomFontChangedEvent event) {
      if (this.customFont.get()) {
         for (HudElement element : this.elements) {
            element.onFontChanged();
         }
      }
   }

   public boolean hasCustomFont() {
      return this.customFont.get();
   }

   public double getTextScale() {
      return this.textScale.get();
   }

   @NotNull
   @Override
   public Iterator<HudElement> iterator() {
      return this.elements.iterator();
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putInt("__version__", 1);
      tag.putBoolean("active", this.active);
      tag.put("settings", this.settings.toTag());
      tag.put("elements", NbtUtils.listToTag(this.elements));
      return tag;
   }

   public Hud fromTag(CompoundTag tag) {
      if (!tag.contains("__version__")) {
         this.resetToDefaultElements();
         return this;
      }

      tag.getBoolean("active").ifPresent(active1 -> this.active = active1);
      this.settings.fromTag(tag.getCompoundOrEmpty("settings"));
      this.elements.clear();

      for (Tag e : tag.getListOrEmpty("elements")) {
         CompoundTag c = (CompoundTag)e;
         if (!c.getString("name").isEmpty()) {
            HudElementInfo<?> info = this.infos.get(c.getString("name").get());
            if (info != null) {
               HudElement element = info.create();
               element.fromTag(c);
               element.settings.registerColorSettings(null);
               this.elements.add(element);
            }
         }
      }

      return this;
   }
}
