package meteordevelopment.meteorclient.systems.macros;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.utils.StarscriptTextBoxRenderer;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.utils.misc.ISerializable;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.meteordev.starscript.Script;

public class Macro implements ISerializable<Macro> {
   public final Settings settings = new Settings();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   public Setting<String> name = this.sgGeneral.add(new StringSetting.Builder().name("name").description("The name of the macro.").build());
   public Setting<List<String>> messages = this.sgGeneral
      .add(
         new StringListSetting.Builder()
            .name("messages")
            .description("The messages for the macro to send.")
            .onChanged(var1 -> this.dirty = true)
            .renderer(StarscriptTextBoxRenderer.class)
            .build()
      );
   public Setting<Keybind> keybind = this.sgGeneral.add(new KeybindSetting.Builder().name("keybind").description("The bind to run the macro.").build());
   private final List<Script> scripts = new ArrayList<>(1);
   private boolean dirty;

   public Macro() {
   }

   public Macro(Tag tag) {
      this.fromTag((CompoundTag)tag);
   }

   public boolean onAction(boolean isKey, int value, int modifiers) {
      return this.keybind.get().matches(isKey, value, modifiers) && MeteorClient.mc.screen == null ? this.onAction() : false;
   }

   public boolean onAction() {
      if (this.dirty) {
         this.scripts.clear();

         for (String message : this.messages.get()) {
            Script script = MeteorStarscript.compile(message);
            if (script != null) {
               this.scripts.add(script);
            }
         }

         this.dirty = false;
      }

      for (Script script : this.scripts) {
         String message = MeteorStarscript.run(script);
         if (message != null) {
            ChatUtils.sendPlayerMsg(message, false);
         }
      }

      return true;
   }

   @Override
   public CompoundTag toTag() {
      CompoundTag tag = new CompoundTag();
      tag.put("settings", this.settings.toTag());
      return tag;
   }

   public Macro fromTag(CompoundTag tag) {
      if (tag.contains("settings")) {
         this.settings.fromTag(tag.getCompoundOrEmpty("settings"));
      }

      return this;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Macro macro = (Macro)o;
         return Objects.equals(macro.name.get(), this.name.get());
      } else {
         return false;
      }
   }
}
