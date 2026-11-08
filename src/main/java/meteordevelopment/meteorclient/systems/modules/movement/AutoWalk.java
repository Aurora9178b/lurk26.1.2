package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3;
import meteordevelopment.meteorclient.pathing.NopPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class AutoWalk extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoWalk.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                     .description("Walking mode."))
                  .defaultValue(AutoWalk.Mode.Smart))
               .onChanged(mode1 -> {
                  if (this.isActive() && Utils.canUpdate()) {
                     if (mode1 == AutoWalk.Mode.Simple) {
                        PathManagers.get().stop();
                     } else {
                        this.createGoal();
                     }

                     this.unpress();
                  }
               }))
            .build()
      );
   private final Setting<AutoWalk.Direction> direction = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder()
                           .name("simple-direction"))
                        .description("The direction to walk in simple mode."))
                     .defaultValue(AutoWalk.Direction.Forwards))
                  .onChanged(var1 -> {
                     if (this.isActive()) {
                        this.unpress();
                     }
                  }))
               .visible(() -> this.mode.get() == AutoWalk.Mode.Simple))
            .build()
      );
   private final Setting<Boolean> disableOnInput = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-on-input").description("Disable module on manual movement input").defaultValue(false).build());
   private final Setting<Boolean> disableOnY = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("disable-on-y-change")
            .description("Disable module if player moves vertically")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AutoWalk.Mode.Simple)
            .build()
      );
   private final Setting<Boolean> waitForChunks = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("no-unloaded-chunks")
            .description("Do not allow movement into unloaded chunks")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AutoWalk.Mode.Simple)
            .build()
      );

   public AutoWalk() {
      super(Categories.Movement, "auto-walk", "Automatically walks forward.");
   }

   @Override
   public void onActivate() {
      if (this.mode.get() == AutoWalk.Mode.Smart) {
         this.createGoal();
      }
   }

   @Override
   public void onDeactivate() {
      if (this.mode.get() == AutoWalk.Mode.Simple) {
         this.unpress();
      } else {
         PathManagers.get().stop();
      }
   }

   @EventHandler(priority = 100)
   private void onTick(TickEvent.Pre event) {
      if (this.mode.get() == AutoWalk.Mode.Simple) {
         if (this.disableOnY.get() && this.mc.player.yo != this.mc.player.getY()) {
            this.toggle();
            return;
         }

         switch ((AutoWalk.Direction)this.direction.get()) {
            case Forwards:
               this.mc.options.keyUp.setDown(true);
               break;
            case Backwards:
               this.mc.options.keyDown.setDown(true);
               break;
            case Left:
               this.mc.options.keyLeft.setDown(true);
               break;
            case Right:
               this.mc.options.keyRight.setDown(true);
         }
      } else if (PathManagers.get() instanceof NopPathManager) {
         this.info("Smart mode requires Baritone");
         this.toggle();
      }
   }

   private void onMovement() {
      if (this.disableOnInput.get()) {
         if (this.mc.screen != null) {
            GUIMove guiMove = Modules.get().get(GUIMove.class);
            if (!guiMove.isActive()) {
               return;
            }

            if (guiMove.skip()) {
               return;
            }
         }

         this.toggle();
      }
   }

   @EventHandler
   private void onKey(KeyInputEvent event) {
      if (this.isMovementKey(event.input) && event.action == KeyAction.Press) {
         this.onMovement();
      }
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (this.isMovementButton(event.click) && event.action == KeyAction.Press) {
         this.onMovement();
      }
   }

   @EventHandler
   private void onPlayerMove(PlayerMoveEvent event) {
      if (this.mode.get() == AutoWalk.Mode.Simple && this.waitForChunks.get()) {
         int chunkX = (int)((this.mc.player.getX() + event.movement.x * 2.0) / 16.0);
         int chunkZ = (int)((this.mc.player.getZ() + event.movement.z * 2.0) / 16.0);
         if (!this.mc.level.getChunkSource().hasChunk(chunkX, chunkZ)) {
            ((IVec3)event.movement).meteor$set(0.0, event.movement.y, 0.0);
         }
      }
   }

   private void unpress() {
      this.mc.options.keyUp.setDown(false);
      this.mc.options.keyDown.setDown(false);
      this.mc.options.keyLeft.setDown(false);
      this.mc.options.keyRight.setDown(false);
   }

   private boolean isMovementKey(KeyEvent input) {
      return this.mc.options.keyUp.matches(input)
         || this.mc.options.keyDown.matches(input)
         || this.mc.options.keyLeft.matches(input)
         || this.mc.options.keyRight.matches(input)
         || this.mc.options.keyShift.matches(input)
         || this.mc.options.keyJump.matches(input);
   }

   private boolean isMovementButton(MouseButtonEvent click) {
      return this.mc.options.keyUp.matchesMouse(click)
         || this.mc.options.keyDown.matchesMouse(click)
         || this.mc.options.keyLeft.matchesMouse(click)
         || this.mc.options.keyRight.matchesMouse(click)
         || this.mc.options.keyShift.matchesMouse(click)
         || this.mc.options.keyJump.matchesMouse(click);
   }

   private void createGoal() {
      PathManagers.get().moveInDirection(this.mc.player.getYRot());
   }

   public enum Direction {
      Forwards,
      Backwards,
      Left,
      Right;
   }

   public enum Mode {
      Simple,
      Smart;
   }
}
