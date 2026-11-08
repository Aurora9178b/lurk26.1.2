package meteordevelopment.meteorclient.systems.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;

public class Notifier extends Module {
   private final SettingGroup sgTotemPops = this.settings.createGroup("Totem Pops");
   private final SettingGroup sgVisualRange = this.settings.createGroup("Visual Range");
   private final SettingGroup sgPearl = this.settings.createGroup("Pearl");
   private final SettingGroup sgJoinsLeaves = this.settings.createGroup("Joins/Leaves");
   private final Setting<Boolean> totemPops = this.sgTotemPops
      .add(new BoolSetting.Builder().name("totem-pops").description("Notifies you when a player pops a totem.").defaultValue(true).build());
   private final Setting<Boolean> totemsDistanceCheck = this.sgTotemPops
      .add(
         new BoolSetting.Builder()
            .name("distance-check")
            .description("Limits the distance in which the pops are recognized.")
            .defaultValue(false)
            .visible(this.totemPops::get)
            .build()
      );
   private final Setting<Integer> totemsDistance = this.sgTotemPops
      .add(
         new IntSetting.Builder()
            .name("player-radius")
            .description("The radius in which to log totem pops.")
            .defaultValue(30)
            .sliderRange(1, 50)
            .range(1, 100)
            .visible(() -> this.totemPops.get() && this.totemsDistanceCheck.get())
            .build()
      );
   private final Setting<Boolean> totemsIgnoreOwn = this.sgTotemPops
      .add(new BoolSetting.Builder().name("ignore-own").description("Ignores your own totem pops.").defaultValue(false).build());
   private final Setting<Boolean> totemsIgnoreFriends = this.sgTotemPops
      .add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends totem pops.").defaultValue(false).build());
   private final Setting<Boolean> totemsIgnoreOthers = this.sgTotemPops
      .add(new BoolSetting.Builder().name("ignore-others").description("Ignores other players totem pops.").defaultValue(false).build());
   private final Setting<Boolean> visualRange = this.sgVisualRange
      .add(new BoolSetting.Builder().name("visual-range").description("Notifies you when an entity enters your render distance.").defaultValue(false).build());
   private final Setting<Notifier.Event> event = this.sgVisualRange
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("event")).description("When to log the entities."))
               .defaultValue(Notifier.Event.Both))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgVisualRange
      .add(new EntityTypeListSetting.Builder().name("entities").description("Which entities to notify about.").defaultValue(EntityType.PLAYER).build());
   private final Setting<Boolean> visualRangeIgnoreFriends = this.sgVisualRange
      .add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends.").defaultValue(true).build());
   private final Setting<Boolean> visualRangeIgnoreFakes = this.sgVisualRange
      .add(new BoolSetting.Builder().name("ignore-fake-players").description("Ignores fake players.").defaultValue(true).build());
   private final Setting<Boolean> visualMakeSound = this.sgVisualRange
      .add(new BoolSetting.Builder().name("sound").description("Emits a sound effect on enter / leave").defaultValue(true).build());
   private final Setting<Boolean> pearl = this.sgPearl
      .add(new BoolSetting.Builder().name("pearl").description("Notifies you when a player is teleported using an ender pearl.").defaultValue(true).build());
   private final Setting<Boolean> pearlIgnoreOwn = this.sgPearl
      .add(new BoolSetting.Builder().name("ignore-own").description("Ignores your own pearls.").defaultValue(false).build());
   private final Setting<Boolean> pearlIgnoreFriends = this.sgPearl
      .add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends pearls.").defaultValue(false).build());
   private final Setting<Notifier.JoinLeaveModes> joinsLeavesMode = this.sgJoinsLeaves
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("player-joins-leaves"))
                  .description("How to handle player join/leave notifications."))
               .defaultValue(Notifier.JoinLeaveModes.None))
            .build()
      );
   private final Setting<Integer> notificationDelay = this.sgJoinsLeaves
      .add(
         new IntSetting.Builder()
            .name("notification-delay")
            .description("How long to wait in ticks before posting the next join/leave notification in your chat.")
            .range(0, 1000)
            .sliderRange(0, 100)
            .defaultValue(0)
            .build()
      );
   private final Setting<Boolean> simpleNotifications = this.sgJoinsLeaves
      .add(
         new BoolSetting.Builder()
            .name("simple-notifications")
            .description("Display join/leave notifications without a prefix, to reduce chat clutter.")
            .defaultValue(true)
            .build()
      );
   private int timer;
   private boolean loginPacket = true;
   private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
   private final Map<Integer, Vec3> pearlStartPosMap = new HashMap<>();
   private final ArrayListDeque<MutableComponent> messageQueue = new ArrayListDeque<>();
   private final Random random = new Random();

   public Notifier() {
      super(Categories.Misc, "notifier", "Notifies you of different events.");
   }

   @EventHandler
   private void onEntityAdded(EntityAddedEvent event) {
      if (!event.entity.getUUID().equals(this.mc.player.getUUID())
         && this.entities.get().contains(event.entity.getType())
         && this.visualRange.get()
         && this.event.get() != Notifier.Event.Despawn) {
         if (event.entity instanceof Player player) {
            if ((!this.visualRangeIgnoreFriends.get() || !Friends.get().isFriend(player))
               && (!this.visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
               ChatUtils.sendMsg(
                  event.entity.getId() + 100, ChatFormatting.GRAY, "(highlight)%s(default) has entered your visual range!", event.entity.getName().getString()
               );
               if (this.visualMakeSound.get()) {
                  this.mc.level.playSound(this.mc.player, this.mc.player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.AMBIENT, 3.0F, 1.0F);
               }
            }
         } else {
            MutableComponent text = Component.literal(event.entity.getType().getDescription().getString()).withStyle(ChatFormatting.WHITE);
            text.append(Component.literal(" has spawned at ").withStyle(ChatFormatting.GRAY));
            text.append(ChatUtils.formatCoords(event.entity.position()));
            text.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
            this.info(text);
         }
      }

      if (this.pearl.get() && event.entity instanceof ThrownEnderpearl pearlEntity) {
         this.pearlStartPosMap.put(pearlEntity.getId(), new Vec3(pearlEntity.getX(), pearlEntity.getY(), pearlEntity.getZ()));
      }
   }

   @EventHandler
   private void onEntityRemoved(EntityRemovedEvent event) {
      if (!event.entity.getUUID().equals(this.mc.player.getUUID())
         && this.entities.get().contains(event.entity.getType())
         && this.visualRange.get()
         && this.event.get() != Notifier.Event.Spawn) {
         if (event.entity instanceof Player player) {
            if ((!this.visualRangeIgnoreFriends.get() || !Friends.get().isFriend(player))
               && (!this.visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
               ChatUtils.sendMsg(
                  event.entity.getId() + 100, ChatFormatting.GRAY, "(highlight)%s(default) has left your visual range!", event.entity.getName().getString()
               );
               if (this.visualMakeSound.get()) {
                  this.mc.level.playSound(this.mc.player, this.mc.player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.AMBIENT, 3.0F, 1.0F);
               }
            }
         } else {
            MutableComponent text = Component.literal(event.entity.getType().getDescription().getString()).withStyle(ChatFormatting.WHITE);
            text.append(Component.literal(" has despawned at ").withStyle(ChatFormatting.GRAY));
            text.append(ChatUtils.formatCoords(event.entity.position()));
            text.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
            this.info(text);
         }
      }

      if (this.pearl.get()) {
         Entity e = event.entity;
         int i = e.getId();
         if (this.pearlStartPosMap.containsKey(i)) {
            ThrownEnderpearl pearl = (ThrownEnderpearl)e;
            if (pearl.getOwner() != null && pearl.getOwner() instanceof Player p) {
               double d = this.pearlStartPosMap.get(i).distanceTo(e.position());
               if ((!Friends.get().isFriend(p) || !this.pearlIgnoreFriends.get()) && (!p.equals(this.mc.player) || !this.pearlIgnoreOwn.get())) {
                  this.info(
                     "(highlight)%s's(default) pearl landed at %d, %d, %d (highlight)(%.1fm away, travelled %.1fm)(default).",
                     pearl.getOwner().getName().getString(),
                     pearl.blockPosition().getX(),
                     pearl.blockPosition().getY(),
                     pearl.blockPosition().getZ(),
                     pearl.distanceTo(this.mc.player),
                     d
                  );
               }
            }

            this.pearlStartPosMap.remove(i);
         }
      }
   }

   @Override
   public void onActivate() {
      this.chatIdMap.clear();
      this.pearlStartPosMap.clear();
   }

   @Override
   public void onDeactivate() {
      this.timer = 0;
      this.messageQueue.clear();
   }

   @EventHandler
   private void onGameJoin(GameJoinedEvent event) {
      this.timer = 0;
      Modules.get().get(AutoEz.class).clearTotemPops();
      this.chatIdMap.clear();
      this.messageQueue.clear();
      this.pearlStartPosMap.clear();
   }

   @EventHandler
   private void onGameLeave(GameLeftEvent event) {
      this.loginPacket = true;
      Modules.get().get(AutoEz.class).clearTotemPops();
   }

   @EventHandler
   private void onReceivePacket(PacketEvent.Receive event) {
      Packet var2 = event.packet;
      if (var2 instanceof ClientboundPlayerInfoUpdatePacket packet) {
         if (!this.joinsLeavesMode.get().equals(Notifier.JoinLeaveModes.Both) && !this.joinsLeavesMode.get().equals(Notifier.JoinLeaveModes.Joins)) {
            return;
         }

         if (this.loginPacket) {
            this.loginPacket = false;
            return;
         }

         if (packet.actions().contains(Action.ADD_PLAYER)) {
            this.createJoinNotifications(packet);
         }
      } else if (var2 instanceof ClientboundPlayerInfoRemovePacket packet) {
         if (!this.joinsLeavesMode.get().equals(Notifier.JoinLeaveModes.Both) && !this.joinsLeavesMode.get().equals(Notifier.JoinLeaveModes.Leaves)) {
            return;
         }

         this.createLeaveNotification(packet);
      } else if (var2 instanceof ClientboundEntityEventPacket packet) {
         if (!this.totemPops.get() || packet.getEventId() != 35 || !(packet.getEntity(this.mc.level) instanceof Player entity)) {
            return;
         }

         if (entity.equals(this.mc.player) && this.totemsIgnoreOwn.get()
            || Friends.get().isFriend(entity) && this.totemsIgnoreOthers.get()
            || !Friends.get().isFriend(entity) && this.totemsIgnoreFriends.get()) {
            return;
         }

         AutoEz autoEz = Modules.get().get(AutoEz.class);
         int pops = autoEz.recordTotemPop(entity);
         double distance = PlayerUtils.distanceTo(entity);
         if (this.totemsDistanceCheck.get() && distance > this.totemsDistance.get().intValue()) {
            return;
         }

         ChatUtils.sendMsg(
            this.getChatId(entity),
            ChatFormatting.GRAY,
            "(highlight)%s (default)popped (highlight)%d (default)%s.",
            entity.getName().getString(),
            pops,
            pops == 1 ? "totem" : "totems"
         );
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.joinsLeavesMode.get() != Notifier.JoinLeaveModes.None) {
         this.timer++;

         while (this.timer >= this.notificationDelay.get() && !this.messageQueue.isEmpty()) {
            this.timer = 0;
            if (this.simpleNotifications.get()) {
               this.mc.player.sendSystemMessage(this.messageQueue.removeFirst());
            } else {
               ChatUtils.sendMsg(this.messageQueue.removeFirst());
            }
         }
      }

      if (this.totemPops.get()) {
         AutoEz autoEz = Modules.get().get(AutoEz.class);

         for (Player player : this.mc.level.players()) {
            if (autoEz.hasTotemPops(player.getUUID()) && (player.deathTime > 0 || player.getHealth() <= 0.0F)) {
               int pops = autoEz.consumeTotemPops(player.getUUID());
               ChatUtils.sendMsg(
                  this.getChatId(player),
                  ChatFormatting.GRAY,
                  "(highlight)%s (default)died after popping (highlight)%d (default)%s.",
                  player.getName().getString(),
                  pops,
                  pops == 1 ? "totem" : "totems"
               );
               this.chatIdMap.removeInt(player.getUUID());
            }
         }
      }
   }

   private int getChatId(Entity entity) {
      return this.chatIdMap.computeIfAbsent(entity.getUUID(), var1x -> this.random.nextInt());
   }

   private void createJoinNotifications(ClientboundPlayerInfoUpdatePacket packet) {
      for (Entry entry : packet.newEntries()) {
         if (entry.profile() != null) {
            if (this.simpleNotifications.get()) {
               this.messageQueue
                  .addLast(Component.literal(ChatFormatting.GRAY + "[" + ChatFormatting.GREEN + "+" + ChatFormatting.GRAY + "] " + entry.profile().name()));
            } else {
               this.messageQueue.addLast(Component.literal(ChatFormatting.WHITE + entry.profile().name() + ChatFormatting.GRAY + " joined."));
            }
         }
      }
   }

   private void createLeaveNotification(ClientboundPlayerInfoRemovePacket packet) {
      if (this.mc.getConnection() != null) {
         for (UUID id : packet.profileIds()) {
            PlayerInfo toRemove = this.mc.getConnection().getPlayerInfo(id);
            if (toRemove != null) {
               if (this.simpleNotifications.get()) {
                  this.messageQueue
                     .addLast(
                        Component.literal(ChatFormatting.GRAY + "[" + ChatFormatting.RED + "-" + ChatFormatting.GRAY + "] " + toRemove.getProfile().name())
                     );
               } else {
                  this.messageQueue.addLast(Component.literal(ChatFormatting.WHITE + toRemove.getProfile().name() + ChatFormatting.GRAY + " left."));
               }
            }
         }
      }
   }

   public enum Event {
      Spawn,
      Despawn,
      Both;
   }

   public enum JoinLeaveModes {
      None,
      Joins,
      Leaves,
      Both;
   }
}
