package meteordevelopment.meteorclient.pathing;

import java.util.function.Predicate;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

public interface IPathManager {
   String getName();

   boolean isPathing();

   void pause();

   void resume();

   void stop();

   default void moveTo(BlockPos pos) {
      this.moveTo(pos, false);
   }

   void moveTo(BlockPos var1, boolean var2);

   void moveInDirection(float var1);

   void mine(Block... var1);

   void follow(Predicate<Entity> var1);

   float getTargetYaw();

   float getTargetPitch();

   IPathManager.ISettings getSettings();

   interface ISettings {
      Settings get();

      Setting<Boolean> getWalkOnWater();

      Setting<Boolean> getWalkOnLava();

      Setting<Boolean> getStep();

      Setting<Boolean> getNoFall();

      void save();
   }
}
