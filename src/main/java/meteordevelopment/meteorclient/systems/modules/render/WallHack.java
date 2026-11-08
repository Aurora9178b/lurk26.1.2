package meteordevelopment.meteorclient.systems.modules.render;

import java.util.List;
import meteordevelopment.meteorclient.MixinPlugin;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.irisshaders.iris.api.v0.IrisApi;
import net.minecraft.world.level.block.Block;

public class WallHack extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public final Setting<Integer> opacity = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("opacity")
            .description("The opacity for rendered blocks.")
            .defaultValue(0)
            .range(0, 255)
            .sliderMax(255)
            .onChanged(var1 -> {
               if (this.isActive()) {
                  this.mc.levelRenderer.allChanged();
               }
            })
            .build()
      );
   public final Setting<List<Block>> blocks = this.sgGeneral
      .add(new BlockListSetting.Builder().name("blocks").description("What blocks should be targeted for Wall Hack.").defaultValue().onChanged(var1 -> {
         if (this.isActive()) {
            this.mc.levelRenderer.allChanged();
         }
      }).build());
   public final Setting<Boolean> occludeChunks = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("occlude-chunks")
            .description("Whether caves should occlude underground (may look wonky when on).")
            .defaultValue(false)
            .build()
      );

   public WallHack() {
      super(Categories.Render, "wall-hack", "Makes blocks translucent.");
   }

   @Override
   public void onActivate() {
      this.mc.levelRenderer.allChanged();
   }

   @Override
   public void onDeactivate() {
      this.mc.levelRenderer.allChanged();
   }

   @Override
   public WWidget getWidget(GuiTheme theme) {
      return MixinPlugin.isIrisPresent && IrisApi.getInstance().isShaderPackInUse()
         ? theme.label("Warning: Due to shaders in use, opacity is overridden to 0.")
         : null;
   }

   @EventHandler
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      if (!this.occludeChunks.get()) {
         event.cancel();
      }
   }
}
