package meteordevelopment.meteorclient.gui.screens.settings;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.settings.base.CollectionMapSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BlockDataSetting;
import meteordevelopment.meteorclient.settings.IBlockData;
import meteordevelopment.meteorclient.utils.misc.IChangeable;
import meteordevelopment.meteorclient.utils.misc.ICopyable;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.render.DisplayItemUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class BlockDataSettingScreen<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> extends CollectionMapSettingScreen<Block, T> {
   private final BlockDataSetting<T> setting;
   private boolean invalidate;

   public BlockDataSettingScreen(GuiTheme theme, BlockDataSetting<T> setting) {
      super(theme, "Configure Blocks", setting, setting.get(), BuiltInRegistries.BLOCK);
      this.setting = setting;
   }

   protected boolean includeValue(Block value) {
      return value != Blocks.AIR;
   }

   protected WWidget getValueWidget(Block block) {
      return this.theme.itemWithLabel(DisplayItemUtils.toStack(block), Names.get(block));
   }

   protected WWidget getDataWidget(Block block, @Nullable T blockData) {
      WButton edit = this.theme.button(GuiRenderer.EDIT);
      edit.action = () -> {
         T data = blockData;
         if (data == null) {
            data = this.setting.defaultData.get().copy();
         }

         MeteorClient.mc.setScreen(data.createScreen(this.theme, block, this.setting));
         this.invalidate = true;
      };
      return edit;
   }

   @Override
   protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
      if (this.invalidate) {
         this.invalidateTable();
         this.invalidate = false;
      }
   }

   protected String[] getValueNames(Block block) {
      return new String[]{Names.get(block), BuiltInRegistries.BLOCK.getKey(block).toString()};
   }
}
