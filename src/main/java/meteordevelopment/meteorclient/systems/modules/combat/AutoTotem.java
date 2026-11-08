package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<AutoTotem.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode"))
                  .description("Determines when to hold a totem, strict will always hold."))
               .defaultValue(AutoTotem.Mode.Smart))
            .build()
      );
   private final Setting<Integer> delay = this.sgGeneral
      .add(new IntSetting.Builder().name("delay").description("The ticks between slot movements.").defaultValue(0).min(0).build());
   private final Setting<Integer> health = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("health")
            .description("The health to hold a totem at.")
            .defaultValue(10)
            .range(0, 36)
            .sliderMax(36)
            .visible(() -> this.mode.get() == AutoTotem.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> elytra = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("elytra")
            .description("Will always hold a totem when flying with elytra.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AutoTotem.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> fall = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("fall")
            .description("Will hold a totem when fall damage could kill you.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AutoTotem.Mode.Smart)
            .build()
      );
   private final Setting<Boolean> explosion = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("explosion")
            .description("Will hold a totem when explosion damage could kill you.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AutoTotem.Mode.Smart)
            .build()
      );
   public boolean locked;
   private int totems;
   private int ticks;

   public AutoTotem() {
      super(Categories.Combat, "auto-totem", "Automatically equips a totem in your offhand.");
   }

   @EventHandler(priority = 200)
   private void onTick(TickEvent.Pre event) {
      FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
      this.totems = result.count();
      if (this.totems <= 0) {
         this.locked = false;
      } else if (this.ticks >= this.delay.get()) {
         boolean low = this.mc.player.getHealth()
               + this.mc.player.getAbsorptionAmount()
               - PlayerUtils.possibleHealthReductions(this.explosion.get(), this.fall.get())
            <= this.health.get().intValue();
         boolean ely = this.elytra.get() && this.mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && this.mc.player.isFallFlying();
         this.locked = this.mode.get() == AutoTotem.Mode.Strict || this.mode.get() == AutoTotem.Mode.Smart && (low || ely);
         if (this.locked && this.mc.player.getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(result.slot()).toOffhand();
         }

         this.ticks = 0;
         return;
      }

      this.ticks++;
   }

   @EventHandler(priority = 100)
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundEntityEventPacket p) {
         if (p.getEventId() == 35) {
            Entity entity = p.getEntity(this.mc.level);
            if (entity != null && entity.equals(this.mc.player)) {
               this.ticks = 0;
            }
         }
      }
   }

   public boolean isLocked() {
      return this.isActive() && this.locked;
   }

   @Override
   public String getInfoString() {
      return String.valueOf(this.totems);
   }

   public enum Mode {
      Smart,
      Strict;
   }
}
