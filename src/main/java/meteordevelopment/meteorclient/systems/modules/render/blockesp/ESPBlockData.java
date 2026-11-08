package meteordevelopment.meteorclient.systems.modules.render.blockesp;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.GenericSetting;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.settings.IGeneric;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

public class ESPBlockData implements IGeneric<ESPBlockData>, IChangeable, IBlockData<ESPBlockData> {
   public ShapeMode shapeMode;
   public SettingColor lineColor;
   public SettingColor sideColor;
   public boolean tracer;
   public SettingColor tracerColor;
   private boolean changed;

   public ESPBlockData(ShapeMode shapeMode, SettingColor lineColor, SettingColor sideColor, boolean tracer, SettingColor tracerColor) {
      this.shapeMode = shapeMode;
      this.lineColor = lineColor;
      this.sideColor = sideColor;
      this.tracer = tracer;
      this.tracerColor = tracerColor;
   }

   @Override
   public WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<ESPBlockData> setting) {
      return new ESPBlockDataScreen(theme, this, block, setting);
   }

   @Override
   public WidgetScreen createScreen(GuiTheme theme, GenericSetting<ESPBlockData> setting) {
      return new ESPBlockDataScreen(theme, this, setting);
   }

   @Override
   public boolean isChanged() {
      return this.changed;
   }

   public void changed() {
      this.changed = true;
   }

   public void tickRainbow() {
      this.lineColor.update();
      this.sideColor.update();
      this.tracerColor.update();
   }

   public ESPBlockData set(ESPBlockData value) {
      this.shapeMode = value.shapeMode;
      this.lineColor.set(value.lineColor);
      this.sideColor.set(value.sideColor);
      this.tracer = value.tracer;
      this.tracerColor.set(value.tracerColor);
      this.changed = value.changed;
      return this;
   }

   public ESPBlockData copy() {
      return new ESPBlockData(
         this.shapeMode, new SettingColor(this.lineColor), new SettingColor(this.sideColor), this.tracer, new SettingColor(this.tracerColor)
      );
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.putString("shapeMode", this.shapeMode.name());
      tag.put("lineColor", this.lineColor.toTag());
      tag.put("sideColor", this.sideColor.toTag());
      tag.putBoolean("tracer", this.tracer);
      tag.put("tracerColor", this.tracerColor.toTag());
      tag.putBoolean("changed", this.changed);
      return tag;
   }

   public ESPBlockData fromTag(CompoundTag tag) {
      this.shapeMode = ShapeMode.valueOf(tag.getStringOr("shapeMode", ""));
      this.lineColor.fromTag(tag.getCompoundOrEmpty("lineColor"));
      this.sideColor.fromTag(tag.getCompoundOrEmpty("sideColor"));
      this.tracer = tag.getBooleanOr("tracer", false);
      this.tracerColor.fromTag(tag.getCompoundOrEmpty("tracerColor"));
      this.changed = tag.getBooleanOr("changed", false);
      return this;
   }
}
