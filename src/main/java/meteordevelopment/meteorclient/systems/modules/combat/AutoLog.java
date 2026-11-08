package meteordevelopment.meteorclient.systems.modules.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public class AutoLog extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgEntities = this.settings.createGroup("Entities");
   private final Setting<Integer> health = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("health")
            .description("Automatically disconnects when health is lower or equal to this value. Set to 0 to disable.")
            .defaultValue(6)
            .range(0, 19)
            .sliderMax(19)
            .build()
      );
   private final Setting<Boolean> smart = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("predict-incoming-damage")
            .description("Disconnects when it detects you're about to take enough damage to set you under the 'health' setting.")
            .defaultValue(true)
            .build()
      );
   private final Setting<Integer> totemPops = this.sgGeneral
      .add(
         new IntSetting.Builder()
            .name("totem-pops")
            .description("Disconnects when you have popped this many totems. Set to 0 to disable.")
            .defaultValue(0)
            .min(0)
            .build()
      );
   private final Setting<Boolean> onlyTrusted = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("only-trusted")
            .description("Disconnects when a player not on your friends list appears in render distance.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> instantDeath = this.sgGeneral
      .add(new BoolSetting.Builder().name("32K").description("Disconnects when a player near you can instantly kill you.").defaultValue(false).build());
   private final Setting<Boolean> smartToggle = this.sgGeneral
      .add(
         new BoolSetting.Builder()
            .name("smart-toggle")
            .description("Disables Auto Log after a low-health logout. WILL re-enable once you heal.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> toggleOff = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-off").description("Disables Auto Log after usage.").defaultValue(true).build());
   private final Setting<Boolean> toggleAutoReconnect = this.sgGeneral
      .add(new BoolSetting.Builder().name("toggle-auto-reconnect").description("Whether to disable Auto Reconnect after a logout.").defaultValue(true).build());
   private final Setting<Set<EntityType<?>>> entities = this.sgEntities
      .add(
         new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Disconnects when a specified entity is present within a specified range.")
            .defaultValue(EntityType.END_CRYSTAL)
            .build()
      );
   private final Setting<Boolean> useTotalCount = this.sgEntities
      .add(
         new BoolSetting.Builder()
            .name("use-total-count")
            .description("Toggle between counting the total number of all selected entities or each entity individually.")
            .defaultValue(true)
            .visible(() -> !this.entities.get().isEmpty())
            .build()
      );
   private final Setting<Integer> combinedEntityThreshold = this.sgEntities
      .add(
         new IntSetting.Builder()
            .name("combined-entity-threshold")
            .description("The minimum total number of selected entities that must be near you before disconnection occurs.")
            .defaultValue(10)
            .min(1)
            .sliderMax(32)
            .visible(() -> this.useTotalCount.get() && !this.entities.get().isEmpty())
            .build()
      );
   private final Setting<Integer> individualEntityThreshold = this.sgEntities
      .add(
         new IntSetting.Builder()
            .name("individual-entity-threshold")
            .description("The minimum number of entities individually that must be near you before disconnection occurs.")
            .defaultValue(2)
            .min(1)
            .sliderMax(16)
            .visible(() -> !this.useTotalCount.get() && !this.entities.get().isEmpty())
            .build()
      );
   private final Setting<Integer> range = this.sgEntities
      .add(
         new IntSetting.Builder()
            .name("range")
            .description("How close an entity has to be to you before you disconnect.")
            .defaultValue(5)
            .min(1)
            .sliderMax(16)
            .visible(() -> !this.entities.get().isEmpty())
            .build()
      );
   private final Object2IntMap<EntityType<?>> entityCounts = new Object2IntOpenHashMap<>();
   private int pops;
   private final AutoLog.StaticListener staticListener = new AutoLog.StaticListener();

   public AutoLog() {
      super(Categories.Combat, "auto-log", "Automatically disconnects you when certain requirements are met.");
   }

   @Override
   public void onActivate() {
      this.pops = 0;
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      if (event.packet instanceof ClientboundEntityEventPacket p) {
         if (p.getEventId() == 35) {
            Entity entity = p.getEntity(this.mc.level);
            if (entity != null && entity.equals(this.mc.player)) {
               this.pops++;
               if (this.totemPops.get() > 0 && this.pops >= this.totemPops.get()) {
                  this.disconnect("Popped " + this.pops + " totems.");
                  if (this.toggleOff.get()) {
                     this.toggle();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      float playerHealth = this.mc.player.getHealth();
      if (playerHealth <= 0.0F) {
         this.toggle();
      } else if (playerHealth <= this.health.get().intValue()) {
         this.disconnect("Health was lower than " + this.health.get() + ".");
         if (this.smartToggle.get()) {
            if (this.isActive()) {
               this.toggle();
            }

            this.enableHealthListener();
         } else if (this.toggleOff.get()) {
            this.toggle();
         }
      } else if (this.smart.get()
         && playerHealth + this.mc.player.getAbsorptionAmount() - PlayerUtils.possibleHealthReductions() < this.health.get().intValue()) {
         this.disconnect("Health was going to be lower than " + this.health.get() + ".");
         if (this.toggleOff.get()) {
            this.toggle();
         }
      } else if (this.onlyTrusted.get() || this.instantDeath.get() || !this.entities.get().isEmpty()) {
         for (Entity entity : this.mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && player.getUUID() != this.mc.player.getUUID()) {
               if (this.onlyTrusted.get() && player != this.mc.player && !Friends.get().isFriend(player)) {
                  this.disconnect(
                     Component.literal(
                        "Non-trusted player '"
                           + ChatFormatting.RED
                           + player.getName().getString()
                           + ChatFormatting.WHITE
                           + "' appeared in your render distance."
                     )
                  );
                  if (this.toggleOff.get()) {
                     this.toggle();
                  }

                  return;
               }

               if (this.instantDeath.get()
                  && PlayerUtils.isWithin(entity, 8.0)
                  && DamageUtils.getAttackDamage(player, this.mc.player) > playerHealth + this.mc.player.getAbsorptionAmount()) {
                  this.disconnect("Anti-32k measures.");
                  if (this.toggleOff.get()) {
                     this.toggle();
                  }

                  return;
               }
            }
         }

         if (!this.entities.get().isEmpty()) {
            int totalEntities = 0;
            this.entityCounts.clear();

            for (Entity entity : this.mc.level.entitiesForRendering()) {
               if (PlayerUtils.isWithin(entity, this.range.get().intValue()) && this.entities.get().contains(entity.getType())) {
                  totalEntities++;
                  if (!this.useTotalCount.get()) {
                     this.entityCounts.put(entity.getType(), this.entityCounts.getOrDefault(entity.getType(), 0) + 1);
                  }
               }
            }

            if (this.useTotalCount.get() && totalEntities >= this.combinedEntityThreshold.get()) {
               this.disconnect("Total number of selected entities within range exceeded the limit.");
               if (this.toggleOff.get()) {
                  this.toggle();
               }
            } else if (!this.useTotalCount.get()) {
               for (Entry<EntityType<?>> entry : this.entityCounts.object2IntEntrySet()) {
                  if (entry.getIntValue() >= this.individualEntityThreshold.get()) {
                     this.disconnect("Number of " + entry.getKey().getDescription().getString() + " within range exceeded the limit.");
                     if (this.toggleOff.get()) {
                        this.toggle();
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   private void disconnect(String reason) {
      this.disconnect(Component.literal(reason));
   }

   private void disconnect(Component reason) {
      MutableComponent text = Component.literal("[AutoLog] ");
      text.append(reason);
      AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
      if (autoReconnect.isActive() && this.toggleAutoReconnect.get()) {
         text.append(Component.literal("\n\nINFO - AutoReconnect was disabled").withColor(-8355712));
         autoReconnect.toggle();
      }

      this.mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(text));
   }

   private void enableHealthListener() {
      MeteorClient.EVENT_BUS.subscribe(this.staticListener);
   }

   private void disableHealthListener() {
      MeteorClient.EVENT_BUS.unsubscribe(this.staticListener);
   }

   private class StaticListener {
      @EventHandler
      private void healthListener(TickEvent.Post event) {
         if (AutoLog.this.isActive()) {
            AutoLog.this.disableHealthListener();
         } else if (Utils.canUpdate() && !AutoLog.this.mc.player.isDeadOrDying() && AutoLog.this.mc.player.getHealth() > AutoLog.this.health.get().intValue()) {
            AutoLog.this.info("Player health greater than minimum, re-enabling module.");
            AutoLog.this.toggle();
            AutoLog.this.disableHealthListener();
         }
      }
   }
}
