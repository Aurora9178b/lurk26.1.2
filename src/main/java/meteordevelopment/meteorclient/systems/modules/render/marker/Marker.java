package meteordevelopment.meteorclient.systems.modules.render.marker;

import java.util.ArrayList;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class Marker extends Module {
   private final MarkerFactory factory = new MarkerFactory();
   private final ArrayList<BaseMarker> markers = new ArrayList<>();

   public Marker() {
      super(Categories.Render, "marker", "Renders shapes. Useful for large scale projects");
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      for (BaseMarker marker : this.markers) {
         if (marker.isVisible()) {
            marker.tick();
         }
      }
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      for (BaseMarker marker : this.markers) {
         if (marker.isVisible()) {
            marker.render(event);
         }
      }
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = super.toTag();
      ListTag list = new ListTag();

      for (BaseMarker marker : this.markers) {
         CompoundTag mTag = new CompoundTag();
         mTag.putString("type", marker.getTypeName());
         mTag.put("marker", marker.toTag());
         list.add(mTag);
      }

      tag.put("markers", list);
      return tag;
   }

   @Override
   public Module fromTag(CompoundTag tag) {
      super.fromTag(tag);
      this.markers.clear();

      for (Tag tagII : tag.getListOrEmpty("markers")) {
         CompoundTag tagI = (CompoundTag)tagII;
         String type = tagI.getStringOr("type", "");
         BaseMarker marker = this.factory.createMarker(type);
         if (marker != null) {
            CompoundTag markerTag = (CompoundTag)tagI.get("marker");
            if (markerTag != null) {
               marker.fromTag(markerTag);
            }

            this.markers.add(marker);
         }
      }

      return this;
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      WVerticalList list = theme.verticalList();
      this.fillList(theme, list);
      return list;
   }

   protected void fillList(GuiTheme theme, WVerticalList list) {
      for (BaseMarker marker : this.markers) {
         WHorizontalList hList = list.add(theme.horizontalList()).expandX().widget();
         WLabel label = hList.add(theme.label(marker.name.get())).widget();
         label.tooltip = marker.description.get();
         hList.add(theme.label(" - " + marker.getDimension().toString())).expandX().widget().color = theme.textSecondaryColor();
         WCheckbox checkbox = hList.add(theme.checkbox(marker.isActive())).widget();
         checkbox.action = () -> {
            if (marker.isActive() != checkbox.checked) {
               marker.toggle();
            }
         };
         WButton edit = hList.add(theme.button(GuiRenderer.EDIT)).widget();
         edit.action = () -> this.mc.setScreen(marker.getScreen(theme));
         WMinus remove = hList.add(theme.minus()).widget();
         remove.action = () -> {
            this.markers.remove(marker);
            marker.settings.unregisterColorSettings();
            list.clear();
            this.fillList(theme, list);
         };
      }

      WHorizontalList bottom = list.add(theme.horizontalList()).expandX().widget();
      WDropdown<String> newMarker = bottom.add(theme.dropdown(this.factory.getNames(), this.factory.getNames()[0])).widget();
      WButton add = bottom.add(theme.button("Add")).expandX().widget();
      add.action = () -> {
         String name = newMarker.get();
         this.markers.add(this.factory.createMarker(name));
         list.clear();
         this.fillList(theme, list);
      };
   }
}
