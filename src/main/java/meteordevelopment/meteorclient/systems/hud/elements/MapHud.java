package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

public class MapHud extends HudElement {
   public static final HudElementInfo<MapHud> INFO = new HudElementInfo<>(Hud.GROUP, "map", "Displays the contents of a map on your Hud.", MapHud::new);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgVisual = this.settings.createGroup("Visual");
   private final Setting<MapHud.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("How to determine which map to render."))
               .defaultValue(MapHud.Mode.Simple))
            .build()
      );
   private final Setting<Integer> slotIndex = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("slot-index")
            .description("Which slot to grab the map from.")
            .visible(() -> this.mode.get() == MapHud.Mode.SlotIndex)
            .defaultValue(0)
            .sliderRange(0, 40)
            .build()
      );
   private final Setting<Integer> mapId = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("map-id")
            .description("Which map id to render from. Must be in your inventory!")
            .visible(() -> this.mode.get() == MapHud.Mode.MapId)
            .defaultValue(0)
            .noSlider()
            .build()
      );
   private final Setting<Double> scale = this.sgVisual
      .add(new DoubleSetting.Builder().name("scale").description("How big to render the map.").defaultValue(1.0).min(0.5).sliderRange(0.5, 3.0).build());
   private final Setting<Boolean> background = this.sgVisual
      .add(new BoolSetting.Builder().name("background").description("Displays background.").defaultValue(false).build());
   private final Setting<SettingColor> backgroundColor = this.sgVisual
      .add(
         new ColorSetting.Builder()
            .name("background-color")
            .description("Color used for the background.")
            .visible(this.background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
      );
   private final MapRenderState renderState = new MapRenderState();
   @Nullable
   private MapId mapComponent;
   @Nullable
   private MapItemSavedData mapState;

   public MapHud() {
      super(INFO);
   }

   @Override
   public void tick(HudRenderer renderer) {
      double scale = this.scale.get();
      this.setSize(128.0 * scale, 128.0 * scale);
      if (Utils.canUpdate()) {
         ItemStack mapStack = ItemStack.EMPTY;
         switch ((MapHud.Mode)this.mode.get()) {
            case SlotIndex:
               mapStack = MeteorClient.mc.player.getInventory().getItem(this.slotIndex.get());
               break;
            case MapId:
               FindItemResult mapResultx = InvUtils.find(stack -> {
                  MapId mapIdComponent = stack.get(DataComponents.MAP_ID);
                  return mapIdComponent != null && mapIdComponent.id() == this.mapId.get();
               });
               if (mapResultx.found()) {
                  mapStack = MeteorClient.mc.player.getInventory().getItem(mapResultx.slot());
               }
               break;
            case Simple:
               FindItemResult mapResult = InvUtils.find(stack -> {
                  MapId mapIdComponent = stack.get(DataComponents.MAP_ID);
                  return mapIdComponent != null;
               });
               if (mapResult.found()) {
                  mapStack = MeteorClient.mc.player.getInventory().getItem(mapResult.slot());
               }
         }

         if (!mapStack.isEmpty() && mapStack.has(DataComponents.MAP_ID)) {
            this.mapComponent = mapStack.get(DataComponents.MAP_ID);
            this.mapState = MapItem.getSavedData(this.mapComponent, MeteorClient.mc.level);
         } else {
            this.mapComponent = null;
            this.mapState = null;
         }
      }
   }

   @Override
   public void render(HudRenderer renderer) {
      if (this.mapComponent != null && this.mapState != null) {
         if (this.background.get()) {
            renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
         }

         renderer.post(() -> {
            MeteorClient.mc.getMapRenderer().extractRenderState(this.mapComponent, this.mapState, this.renderState);
            Matrix3x2fStack matrices = renderer.graphics.pose();
            matrices.pushMatrix();
            matrices.scale(1.0F / MeteorClient.mc.getWindow().getGuiScale());
            matrices.translate(this.x, this.y);
            matrices.scale(this.scale.get().floatValue());
            renderer.graphics.map(this.renderState);
            matrices.popMatrix();
         });
      } else {
         if (HudEditorScreen.isOpen()) {
            renderer.quad(this.x, this.y, this.getWidth(), this.getHeight(), this.backgroundColor.get());
            renderer.line(this.x, this.y, this.x + this.getWidth(), this.y + this.getHeight(), Color.GRAY);
            renderer.line(this.x + this.getWidth(), this.y, this.x, this.y + this.getHeight(), Color.GRAY);
         }
      }
   }

   private enum Mode {
      SlotIndex,
      MapId,
      Simple;
   }
}
