package meteordevelopment.meteorclient.systems.modules.world;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.utils.BetterBlockPos;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.phys.BlockHitResult;

public class Excavator extends Module {
   private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgRendering = this.settings.createGroup("Rendering");
   private final Setting<Keybind> selectionBind = this.sgGeneral
      .add(new KeybindSetting.Builder().name("selection-bind").description("Bind to draw selection.").defaultValue(Keybind.fromButton(1)).build());
   private final Setting<Boolean> logSelection = this.sgGeneral
      .add(new BoolSetting.Builder().name("log-selection").description("Logs the selection coordinates to the chat.").defaultValue(true).build());
   private final Setting<Boolean> keepActive = this.sgGeneral
      .add(new BoolSetting.Builder().name("keep-active").description("Keep the module active after finishing the excavation.").defaultValue(false).build());
   private final Setting<ShapeMode> shapeMode = this.sgRendering
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode"))
                  .description("How the shapes are rendered."))
               .defaultValue(ShapeMode.Both))
            .build()
      );
   private final Setting<SettingColor> sideColor = this.sgRendering
      .add(new ColorSetting.Builder().name("side-color").description("The side color.").defaultValue(new SettingColor(255, 255, 255, 50)).build());
   private final Setting<SettingColor> lineColor = this.sgRendering
      .add(new ColorSetting.Builder().name("line-color").description("The line color.").defaultValue(new SettingColor(255, 255, 255, 255)).build());
   private Excavator.Status status = Excavator.Status.SEL_START;
   private BetterBlockPos start;
   private BetterBlockPos end;

   public Excavator() {
      super(Categories.World, "excavator", "Excavate a selection area.");
   }

   @Override
   public void onDeactivate() {
      this.baritone.getSelectionManager().removeSelection(this.baritone.getSelectionManager().getLastSelection());
      if (this.baritone.getBuilderProcess().isActive()) {
         try {
            this.baritone.getCommandManager().getClass().getMethod("execute", String.class).invoke(this.baritone.getCommandManager(), "stop");
         } catch (ReflectiveOperationException ignored) {
         }
      }

      this.status = Excavator.Status.SEL_START;
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (event.action == KeyAction.Press && this.selectionBind.get().isPressed() && this.mc.screen == null) {
         this.selectCorners();
      }
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      if (event.action == KeyAction.Press && this.selectionBind.get().isPressed() && this.mc.screen == null) {
         this.selectCorners();
      }
   }

   private void selectCorners() {
      if (this.mc.hitResult instanceof BlockHitResult result) {
         if (this.status == Excavator.Status.SEL_START) {
            this.start = new BetterBlockPos(result.getBlockPos().getX(), result.getBlockPos().getY(), result.getBlockPos().getZ());
            this.status = Excavator.Status.SEL_END;
            if (this.logSelection.get()) {
               this.info("Start corner set: (%d, %d, %d)".formatted(this.start.x, this.start.y, this.start.z));
            }
         } else if (this.status == Excavator.Status.SEL_END) {
            this.end = new BetterBlockPos(result.getBlockPos().getX(), result.getBlockPos().getY(), result.getBlockPos().getZ());
            this.status = Excavator.Status.WORKING;
            if (this.logSelection.get()) {
               this.info("End corner set: (%d, %d, %d)".formatted(this.end.x, this.end.y, this.end.z));
            }

            this.baritone.getSelectionManager().addSelection(this.start, this.end);
            try {
               this.baritone.getBuilderProcess().getClass().getMethod("clearArea", Object.class, Object.class).invoke(this.baritone.getBuilderProcess(), this.start, this.end);
            } catch (ReflectiveOperationException ignored) {
            }
         }
      }
   }

   @EventHandler
   private void onRender3D(Render3DEvent event) {
      if (this.status == Excavator.Status.SEL_START || this.status == Excavator.Status.SEL_END) {
         if (!(this.mc.hitResult instanceof BlockHitResult result)) {
            return;
         }

         event.renderer.box(result.getBlockPos(), this.sideColor.get(), this.lineColor.get(), this.shapeMode.get(), 0);
      } else if (this.status == Excavator.Status.WORKING && !this.baritone.getBuilderProcess().isActive()) {
         if (this.keepActive.get()) {
            this.baritone.getSelectionManager().removeSelection(this.baritone.getSelectionManager().getLastSelection());
            this.status = Excavator.Status.SEL_START;
         } else {
            this.toggle();
         }
      }
   }

   private enum Status {
      SEL_START,
      SEL_END,
      WORKING;
   }
}
