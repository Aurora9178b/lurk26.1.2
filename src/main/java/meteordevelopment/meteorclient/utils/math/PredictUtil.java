package meteordevelopment.meteorclient.utils.math;

import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.client.PredictSetting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PredictUtil {
   public static Vec3 predictPos(Vec3 old, Vec3 newPos) {
      PredictSetting setting = Modules.get().get(PredictSetting.class);
      return setting == null ? newPos : newPos.add(newPos.subtract(old).multiply(setting.getResultMul(), setting.getResultMul(), setting.getResultMul()));
   }

   public static int getPing() {
      if (MeteorClient.mc.getConnection() != null && MeteorClient.mc.player != null) {
         PlayerInfo playerListEntry = MeteorClient.mc.getConnection().getPlayerInfo(MeteorClient.mc.player.getUUID());
         return playerListEntry != null ? playerListEntry.getLatency() : 0;
      } else {
         return 0;
      }
   }

   public static Vec3 predictEntity(Entity entity) {
      Vec3 entityPos = entity.position();
      Vec3 old = new Vec3(entity.xOld, entity.yOld, entity.zOld);
      Vec3 pos = predictPos(old, entityPos);
      Vec3 result = entityPos;
      Vec3 velocity = pos.subtract(entityPos);
      PredictSetting setting = Modules.get().get(PredictSetting.class);
      int ticks = 1;
      if (setting != null && setting.getPingSync()) {
         int ping = getPing();
         ticks = Math.max(1, (int)Math.ceil((ping + 1) / 50.0));
      }

      for (int i = 0; i < ticks; i++) {
         AABB box = entity.getBoundingBox().move(result.subtract(entityPos));
         Vec3 adjusted = Entity.collideBoundingBox(entity, velocity, box, entity.level(), List.of());
         result = result.add(adjusted);
      }

      return result;
   }
}
