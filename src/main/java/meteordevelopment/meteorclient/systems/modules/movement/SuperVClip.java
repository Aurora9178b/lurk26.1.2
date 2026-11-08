package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.client.TpSetting;
import meteordevelopment.meteorclient.utils.movement.LTpUtils;
import meteordevelopment.meteorclient.utils.movement.QTPUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.world.phys.Vec3;

public class SuperVClip extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> vclipHeight = this.sgGeneral
      .add(
         new DoubleSetting.Builder()
            .name("vclip-height")
            .description("Height to vclip. Positive clips up, negative clips down.")
            .defaultValue(10.0)
            .sliderMin(-128.0)
            .sliderMax(256.0)
            .build()
      );
   private final Setting<Integer> protectY = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("protect-y")
            .description("The minimum Y this module can clip to.")
            .defaultValue(-63)
            .sliderMin(-70)
            .sliderMax(256)
            .build()
      );
   private final Setting<Boolean> downClipWhenSneak = this.sgGeneral
      .add(new BoolSetting.Builder().name("down-clip-when-sneaking").description("Clips down while sneaking.").defaultValue(false).build());
   private final Setting<Boolean> voidProtect = this.sgGeneral
      .add(new BoolSetting.Builder().name("void-protect").description("Prevents clipping into the void.").defaultValue(false).build());
   private final Setting<Boolean> feedBack = this.sgGeneral
      .add(new BoolSetting.Builder().name("send-feedback").description("Sends feedback after clipping.").defaultValue(true).build());

   public SuperVClip() {
      super(Categories.Movement, "SuperVClip", "Teleports you vertically using the selected TP setting.");
   }

   @Override
   public void onActivate() {
      if (this.mc.player != null && this.mc.level != null) {
         Vec3 startPos = this.mc.player.position();
         double clipY = Math.abs(this.vclipHeight.get());
         double targetY = this.downClipWhenSneak.get() && this.mc.player.isShiftKeyDown() ? startPos.y - clipY : startPos.y + clipY;
         if (this.voidProtect.get()) {
            targetY = Math.max(targetY, this.protectY.get().intValue());
         }

         Vec3 targetPos = new Vec3(startPos.x, targetY, startPos.z);
         this.toggle();
         this.doTp(startPos, targetPos);
         this.mc.player.setPos(targetPos.x, targetPos.y, targetPos.z);
         if (this.feedBack.get()) {
            ChatUtils.info("VClip %.2f m", targetPos.y - startPos.y);
         }
      } else {
         this.toggle();
      }
   }

   private void doTp(Vec3 from, Vec3 to) {
      TpSetting setting = TpSetting.get();
      double moveDistance = setting != null ? setting.getMoveDistance() : 128.0;
      if (setting != null && setting.isQTPUtils()) {
         QTPUtils.doTpMove(from, to, moveDistance, false, count -> {});
      } else {
         LTpUtils.doTp(from, to, moveDistance);
      }
   }
}
