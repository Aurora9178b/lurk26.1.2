package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import java.util.Random;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;

public class AntiAFK extends Module {
   private final SettingGroup sgActions = this.settings.createGroup("Actions");
   private final SettingGroup sgMessages = this.settings.createGroup("Messages");
   private final Setting<Boolean> jump = this.sgActions.add(new BoolSetting.Builder().name("jump").description("Jump randomly.").defaultValue(true).build());
   private final Setting<Boolean> swing = this.sgActions
      .add(new BoolSetting.Builder().name("swing").description("Swings your hand.").defaultValue(false).build());
   private final Setting<Boolean> sneak = this.sgActions
      .add(new BoolSetting.Builder().name("sneak").description("Sneaks and unsneaks quickly.").defaultValue(false).build());
   private final Setting<Integer> sneakTime = this.sgActions
      .add(
         new IntSetting.Builder()
            .name("sneak-time")
            .description("How many ticks to stay sneaked.")
            .defaultValue(5)
            .min(1)
            .sliderMin(1)
            .visible(this.sneak::get)
            .build()
      );
   private final Setting<Boolean> strafe = this.sgActions
      .add(new BoolSetting.Builder().name("strafe").description("Strafe right and left.").defaultValue(false).onChanged(var1 -> {
         this.strafeTimer = 0;
         this.direction = false;
         if (this.isActive()) {
            this.mc.options.keyLeft.setDown(false);
            this.mc.options.keyRight.setDown(false);
         }
      }).build());
   private final Setting<Boolean> spin = this.sgActions
      .add(new BoolSetting.Builder().name("spin").description("Spins the player in place.").defaultValue(true).build());
   private final Setting<AntiAFK.SpinMode> spinMode = this.sgActions
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("spin-mode"))
                     .description("The method of rotating."))
                  .defaultValue(AntiAFK.SpinMode.Server))
               .visible(this.spin::get))
            .build()
      );
   private final Setting<Integer> spinSpeed = this.sgActions
      .add(new IntSetting.Builder().name("speed").description("The speed to spin you.").defaultValue(7).visible(this.spin::get).build());
   private final Setting<Integer> pitch = this.sgActions
      .add(
         new IntSetting.Builder()
            .name("pitch")
            .description("The pitch to send to the server.")
            .defaultValue(0)
            .range(-90, 90)
            .sliderRange(-90, 90)
            .visible(() -> this.spin.get() && this.spinMode.get() == AntiAFK.SpinMode.Server)
            .build()
      );
   private final Setting<Boolean> sendMessages = this.sgMessages
      .add(new BoolSetting.Builder().name("send-messages").description("Sends messages to prevent getting kicked for AFK.").defaultValue(false).build());
   private final Setting<Boolean> randomMessage = this.sgMessages
      .add(
         new BoolSetting.Builder()
            .name("random")
            .description("Selects a random message from your message list.")
            .defaultValue(false)
            .visible(this.sendMessages::get)
            .build()
      );
   private final Setting<Integer> delay = this.sgMessages
      .add(
         new IntSetting.Builder()
            .name("delay")
            .description("The delay between specified messages in seconds.")
            .defaultValue(15)
            .min(0)
            .sliderMax(30)
            .visible(this.sendMessages::get)
            .build()
      );
   private final Setting<List<String>> messages = this.sgMessages
      .add(
         new StringListSetting.Builder()
            .name("messages")
            .description("The messages to choose from.")
            .defaultValue("Meteor on top!", "Meteor on crack!")
            .visible(this.sendMessages::get)
            .build()
      );
   private final Random random = new Random();
   private int messageTimer = 0;
   private int messageI = 0;
   private int sneakTimer = 0;
   private int strafeTimer = 0;
   private boolean direction = false;
   private float lastYaw;

   public AntiAFK() {
      super(Categories.Player, "anti-afk", "Performs different actions to prevent getting kicked while AFK.");
   }

   @Override
   public void onActivate() {
      if (this.sendMessages.get() && this.messages.get().isEmpty()) {
         this.warning("Message list is empty, disabling messages...");
         this.sendMessages.set(false);
      }

      this.lastYaw = this.mc.player.getYRot();
      this.messageTimer = this.delay.get() * 20;
   }

   @Override
   public void onDeactivate() {
      if (this.strafe.get()) {
         this.mc.options.keyLeft.setDown(false);
         this.mc.options.keyRight.setDown(false);
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (Utils.canUpdate()) {
         if (this.jump.get()) {
            if (this.mc.options.keyJump.isDown()) {
               this.mc.options.keyJump.setDown(false);
            } else if (this.random.nextInt(99) == 0) {
               this.mc.options.keyJump.setDown(true);
            }
         }

         if (this.swing.get() && this.random.nextInt(99) == 0) {
            this.mc.player.swing(this.mc.player.getUsedItemHand());
         }

         if (this.sneak.get()) {
            if (this.sneakTimer++ >= this.sneakTime.get()) {
               this.mc.options.keyShift.setDown(false);
               if (this.random.nextInt(99) == 0) {
                  this.sneakTimer = 0;
               }
            } else {
               this.mc.options.keyShift.setDown(true);
            }
         }

         if (this.strafe.get() && this.strafeTimer-- <= 0) {
            this.mc.options.keyLeft.setDown(!this.direction);
            this.mc.options.keyRight.setDown(this.direction);
            this.direction = !this.direction;
            this.strafeTimer = 20;
         }

         if (this.spin.get()) {
            this.lastYaw = this.lastYaw + this.spinSpeed.get().intValue();
            switch ((AntiAFK.SpinMode)this.spinMode.get()) {
               case Server:
                  Rotations.rotate(this.lastYaw, this.pitch.get().intValue(), -15);
                  break;
               case Client:
                  this.mc.player.setYRot(this.lastYaw);
            }
         }

         if (this.sendMessages.get() && !this.messages.get().isEmpty() && this.messageTimer-- <= 0) {
            if (this.randomMessage.get()) {
               this.messageI = this.random.nextInt(this.messages.get().size());
            } else if (++this.messageI >= this.messages.get().size()) {
               this.messageI = 0;
            }

            ChatUtils.sendPlayerMsg(this.messages.get().get(this.messageI));
            this.messageTimer = this.delay.get() * 20;
         }
      }
   }

   public enum SpinMode {
      Server,
      Client;
   }
}
