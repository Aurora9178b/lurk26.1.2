package meteordevelopment.meteorclient.utils.player;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class SoundStorage {
   public static final Identifier ENABLE = Identifier.parse("meteor-client:enable");
   public static final Identifier DISABLE = Identifier.parse("meteor-client:disable");
   public static final SoundEvent SOUND_ENABLE = SoundEvent.createVariableRangeEvent(ENABLE);
   public static final SoundEvent SOUND_DISABLE = SoundEvent.createVariableRangeEvent(DISABLE);

   public static void register() {
      Registry.register(BuiltInRegistries.SOUND_EVENT, ENABLE, SOUND_ENABLE);
      Registry.register(BuiltInRegistries.SOUND_EVENT, DISABLE, SOUND_DISABLE);
   }
}
