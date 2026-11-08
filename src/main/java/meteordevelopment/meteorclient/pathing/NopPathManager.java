package meteordevelopment.meteorclient.pathing;

import java.util.function.Predicate;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

public class NopPathManager implements IPathManager {
   private final NopPathManager.NopSettings settings = new NopPathManager.NopSettings();

   @Override
   public String getName() {
      return "none";
   }

   @Override
   public boolean isPathing() {
      return false;
   }

   @Override
   public void pause() {
   }

   @Override
   public void resume() {
   }

   @Override
   public void stop() {
   }

   @Override
   public void moveTo(BlockPos pos, boolean ignoreY) {
   }

   @Override
   public void moveInDirection(float yaw) {
   }

   @Override
   public void mine(Block... blocks) {
   }

   @Override
   public void follow(Predicate<Entity> entity) {
   }

   @Override
   public float getTargetYaw() {
      return 0.0F;
   }

   @Override
   public float getTargetPitch() {
      return 0.0F;
   }

   @Override
   public IPathManager.ISettings getSettings() {
      return this.settings;
   }

   private static class NopSettings implements IPathManager.ISettings {
      private final Settings settings = new Settings();
      private final Setting<Boolean> setting = new BoolSetting.Builder().build();

      @Override
      public Settings get() {
         return this.settings;
      }

      @Override
      public Setting<Boolean> getWalkOnWater() {
         this.setting.reset();
         return this.setting;
      }

      @Override
      public Setting<Boolean> getWalkOnLava() {
         this.setting.reset();
         return this.setting;
      }

      @Override
      public Setting<Boolean> getStep() {
         this.setting.reset();
         return this.setting;
      }

      @Override
      public Setting<Boolean> getNoFall() {
         this.setting.reset();
         return this.setting;
      }

      @Override
      public void save() {
      }
   }
}
