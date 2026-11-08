package meteordevelopment.meteorclient.systems.modules.misc;

import java.util.List;
import meteordevelopment.meteorclient.events.world.PlaySoundEvent;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.SoundEventListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public class SoundBlocker extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<List<SoundEvent>> sounds = this.sgGeneral
      .add(new SoundEventListSetting.Builder().name("sounds").description("Sounds to block.").build());

   public SoundBlocker() {
      super(Categories.Misc, "sound-blocker", "Cancels out selected sounds.");
   }

   @EventHandler
   private void onPlaySound(PlaySoundEvent event) {
      for (SoundEvent sound : this.sounds.get()) {
         if (sound.location().equals(event.sound.getIdentifier())) {
            event.cancel();
            break;
         }
      }
   }

   public boolean shouldBlock(SoundInstance soundInstance) {
      return this.isActive() && this.sounds.get().contains(Setting.parseId(BuiltInRegistries.SOUND_EVENT, soundInstance.getIdentifier().getPath()));
   }
}
