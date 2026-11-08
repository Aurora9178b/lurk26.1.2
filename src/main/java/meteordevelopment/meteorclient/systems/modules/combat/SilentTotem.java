package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;

public class SilentTotem extends Module {
   private static final int TOTEM_HOTBAR_SLOT = 8;
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The ticks between slot movements.").defaultValue(0).min(0).sliderMin(0).sliderMax(10).build());
   private final Setting<Boolean> pauseOnUse = this.sgGeneral
      .add(new BoolSetting.Builder().name("pause-on-use").description("Pauses while using items.").defaultValue(true).build());
   private final Setting<Boolean> popDupe = this.sgGeneral
      .add(new BoolSetting.Builder().name("pop-dupe").description("Runs the dupe command after popping enough totems.").defaultValue(true).build());
   private final Setting<Integer> pop = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("pop-count")
            .description("The amount of pops before running the dupe command.")
            .defaultValue(2)
            .min(1)
            .sliderMin(1)
            .sliderMax(10)
            .visible(this.popDupe::get)
            .build()
      );
   private final Setting<String> dupeCommand = this.sgGeneral
      .add(
         new StringSetting.Builder()
            .name("dupe-command")
            .description("The command used to dupe items.")
            .defaultValue("dupe")
            .visible(this.popDupe::get)
            .build()
      );
   private int totems;
   private int ticks;
   private int popCount;

   public SilentTotem() {
      super(Categories.Combat, "silent-totem", "Moves totems into a hotbar slot and swaps to it.");
   }

   @Override
   public void onActivate() {
      this.totems = 0;
      this.ticks = 0;
      this.popCount = 0;
   }

   @EventHandler(priority = 200)
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (!this.pauseOnUse.get() || !this.mc.player.isUsingItem()) {
            FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
            this.totems = result.count();
            if (result.found()) {
               if (this.ticks < this.delay.get()) {
                  this.ticks++;
               } else {
                  this.ticks = 0;
                  this.refillTotemSlot(result);
                  this.fillEmptyHotbarSlots();
                  if (this.mc.player.getInventory().getItem(8).is(Items.TOTEM_OF_UNDYING)) {
                     InvUtils.swap(8, false);
                  }
               }
            }
         }
      }
   }

   @EventHandler(priority = 100)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (this.mc.player != null && this.mc.level != null) {
         if (event.packet instanceof ClientboundEntityEventPacket packet) {
            if (packet.getEventId() == 35) {
               Entity entity = packet.getEntity(this.mc.level);
               if (entity != null && entity.equals(this.mc.player)) {
                  this.ticks = 0;
                  FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
                  this.totems = result.count();
                  if (result.found()) {
                     InvUtils.move().from(result.slot()).to(this.mc.player.getInventory().getSelectedSlot());
                  }

                  if (this.popDupe.get()) {
                     this.popCount++;
                     if (this.popCount >= this.pop.get()) {
                        ChatUtils.sendPlayerMsg("/" + this.dupeCommand.get());
                        this.popCount = 0;
                     }
                  }
               }
            }
         }
      }
   }

   private void refillTotemSlot(FindItemResult result) {
      if (!this.mc.player.getInventory().getItem(8).is(Items.TOTEM_OF_UNDYING)) {
         InvUtils.move().from(result.slot()).toHotbar(8);
      }
   }

   private void fillEmptyHotbarSlots() {
      for (int i = 8; i >= 0; i--) {
         if (this.mc.player.getInventory().getItem(i).isEmpty()) {
            FindItemResult result = InvUtils.find(itemStack -> itemStack.is(Items.TOTEM_OF_UNDYING), 9, 35);
            if (result.found()) {
               InvUtils.move().from(result.slot()).toHotbar(i);
            }
         }
      }
   }

   @Override
   public String getInfoString() {
      return Integer.toString(this.totems);
   }
}
