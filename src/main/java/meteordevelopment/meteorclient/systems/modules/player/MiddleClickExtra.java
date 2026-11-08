package meteordevelopment.meteorclient.systems.modules.player;

import meteordevelopment.meteorclient.events.entity.player.FinishUsingItemEvent;
import meteordevelopment.meteorclient.events.entity.player.StoppedUsingItemEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class MiddleClickExtra extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<MiddleClickExtra.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("Which item to use when you middle click."))
               .defaultValue(MiddleClickExtra.Mode.Pearl))
            .build()
      );
   private final Setting<Boolean> message = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("send-message")
            .description("Sends a message when you add a player as a friend.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == MiddleClickExtra.Mode.AddFriend)
            .build()
      );
   private final Setting<String> friendMessage = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("message-to-send")
            .description("Message to send when you add a player as a friend (use %player for the player's name)")
            .defaultValue("/msg %player I just friended you on Meteor.")
            .visible(() -> this.mode.get() == MiddleClickExtra.Mode.AddFriend)
            .build()
      );
   private final Setting<Boolean> quickSwap = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("quick-swap")
            .description("Allows you to use items in your inventory by simulating hotbar key presses. May get flagged by anticheats.")
            .defaultValue(false)
            .visible(() -> this.mode.get() != MiddleClickExtra.Mode.AddFriend)
            .build()
      );
   private final Setting<Boolean> swapBack = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("swap-back")
            .description("Swap back to your original slot when you finish using an item.")
            .defaultValue(false)
            .visible(() -> this.mode.get() != MiddleClickExtra.Mode.AddFriend && !this.quickSwap.get())
            .build()
      );
   private final Setting<Boolean> disableInCreative = this.sgGeneral
      .add(new BoolSetting.Builder().name("disable-in-creative").description("Middle click action is disabled in Creative mode.").defaultValue(true).build());
   private final Setting<Boolean> notify = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("notify")
            .description("Notifies you when you do not have the specified item in your hotbar.")
            .defaultValue(true)
            .visible(() -> this.mode.get() != MiddleClickExtra.Mode.AddFriend)
            .build()
      );
   private boolean isUsing;
   private boolean wasHeld;
   private int itemSlot;
   private int selectedSlot;

   public MiddleClickExtra() {
      super(Categories.Player, "middle-click-extra", "Perform various actions when you middle click.");
   }

   @Override
   public void onDeactivate() {
      this.stopIfUsing(false);
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      if (event.action == KeyAction.Press && event.button() == 2 && this.mc.screen == null) {
         if (!this.disabledByCreative()) {
            if (this.mode.get() != MiddleClickExtra.Mode.AddFriend) {
               FindItemResult result = InvUtils.find(this.mode.get().item);
               if (!result.found() || !result.isHotbar() && !this.quickSwap.get()) {
                  if (this.notify.get()) {
                     this.warning("Unable to find specified item.");
                  }
               } else {
                  this.selectedSlot = this.mc.player.getInventory().getSelectedSlot();
                  this.itemSlot = result.slot();
                  this.wasHeld = result.isMainHand();
                  if (!this.wasHeld) {
                     if (!this.quickSwap.get()) {
                        InvUtils.swap(result.slot(), this.swapBack.get());
                     } else {
                        InvUtils.quickSwap().fromId(this.selectedSlot).to(this.itemSlot);
                     }
                  }

                  if (this.mode.get().immediate) {
                     this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
                     this.swapBack(false);
                  } else {
                     this.mc.options.keyUse.setDown(true);
                     this.isUsing = true;
                  }
               }
            } else if (this.mc.crosshairPickEntity != null) {
               if (this.mc.crosshairPickEntity instanceof Player player) {
                  if (!Friends.get().isFriend(player)) {
                     Friends.get().add(new Friend(player));
                     this.info("Added %s to friends", player.getName().getString());
                     if (this.message.get()) {
                        String messageNotify = this.friendMessage.get().replace("%player", player.getName().getString());
                        ChatUtils.sendPlayerMsg(messageNotify);
                     }
                  } else {
                     Friends.get().remove(Friends.get().get(player));
                     this.info("Removed %s from friends", player.getName().getString());
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.isUsing) {
         boolean pressed = true;
         if (this.mc.player.getMainHandItem().getItem() instanceof BowItem) {
            pressed = BowItem.getPowerForTime(this.mc.player.getTicksUsingItem()) < 1.0F;
         }

         this.mc.options.keyUse.setDown(pressed);
      }
   }

   @EventHandler
   private void onPacketSendEvent(PacketEvent.Send event) {
      if (event.packet instanceof ServerboundSetCarriedItemPacket) {
         this.stopIfUsing(true);
      }
   }

   @EventHandler
   private void onStoppedUsingItem(StoppedUsingItemEvent event) {
      this.stopIfUsing(false);
   }

   @EventHandler
   private void onFinishUsingItem(FinishUsingItemEvent event) {
      this.stopIfUsing(false);
   }

   private void stopIfUsing(boolean wasCancelled) {
      if (this.isUsing) {
         this.swapBack(wasCancelled);
         this.mc.options.keyUse.setDown(false);
         this.isUsing = false;
      }
   }

   void swapBack(boolean wasCancelled) {
      if (!this.wasHeld) {
         if (this.quickSwap.get()) {
            InvUtils.quickSwap().fromId(this.selectedSlot).to(this.itemSlot);
         } else {
            if (!this.swapBack.get() || wasCancelled) {
               return;
            }

            InvUtils.swapBack();
         }
      }
   }

   private boolean disabledByCreative() {
      return this.mc.player == null ? false : this.disableInCreative.get() && this.mc.player.gameMode() == GameType.CREATIVE;
   }

   public enum Mode {
      Pearl(Items.ENDER_PEARL, true),
      XP(Items.EXPERIENCE_BOTTLE, true),
      Rocket(Items.FIREWORK_ROCKET, true),
      WindCharge(Items.WIND_CHARGE, true),
      Bow(Items.BOW, false),
      Gap(Items.GOLDEN_APPLE, false),
      EGap(Items.ENCHANTED_GOLDEN_APPLE, false),
      Chorus(Items.CHORUS_FRUIT, false),
      AddFriend(null, true);

      private final Item item;
      private final boolean immediate;

      Mode(Item item, boolean immediate) {
         this.item = item;
         this.immediate = immediate;
      }
   }
}
