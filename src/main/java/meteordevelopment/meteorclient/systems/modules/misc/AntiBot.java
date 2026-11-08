package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AntiBot extends Module {
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final SettingGroup sgCustom = this.settings.createGroup("Custom");
   private final Setting<AntiBot.Mode> mode = this.sgGeneral
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("mode")).description("AntiBot mode."))
               .defaultValue(AntiBot.Mode.Custom))
            .build()
      );
   private final Setting<Boolean> literalNpc = this.sgGeneral
      .add(new BoolSetting.Builder().name("literal-npc").description("Marks players missing from the online player list as bots.").defaultValue(false).build());
   private final Setting<Boolean> notInTabList = this.sgGeneral
      .add(new BoolSetting.Builder().name("not-in-tab-list").description("Marks players missing from the listed tab list as bots.").defaultValue(false).build());
   private final Setting<Boolean> invalidGround = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("invalid-ground")
            .description("Detects impossible ground movement.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Integer> invalidGroundVl = this.sgCustom
      .add(
         new IntSetting.Builder()
            .name("invalid-ground-vl")
            .description("Invalid ground flags needed to mark a bot.")
            .defaultValue(10)
            .range(1, 50)
            .sliderRange(1, 50)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom && this.invalidGround.get())
            .build()
      );
   private final Setting<Boolean> noGameMode = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("no-game-mode")
            .description("Detects players without game mode info.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> illegalPitch = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("illegal-pitch")
            .description("Detects impossible pitch.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> fakeEntityId = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("fake-entity-id")
            .description("Detects fake entity ids.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> nameCheck = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("name")
            .description("Detects invalid vanilla player names.")
            .defaultValue(true)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> age = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("age")
            .description("Detects very new player entities.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Integer> minimumAge = this.sgCustom
      .add(
         new IntSetting.Builder()
            .name("minimum-age")
            .description("Minimum entity age in ticks.")
            .defaultValue(20)
            .range(0, 120)
            .sliderRange(0, 120)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom && this.age.get())
            .build()
      );
   private final Setting<Boolean> needHit = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("need-hit")
            .description("Detects players you have not hit yet.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> swung = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("swung")
            .description("Detects players that have not swung.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> critted = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("critted")
            .description("Detects players that have not critted.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> attributes = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("attributes")
            .description("Detects players without attribute update packets.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> illegalHealth = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("illegal-health")
            .description("Detects impossible health.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Setting<Boolean> illegalScale = this.sgCustom
      .add(
         new BoolSetting.Builder()
            .name("illegal-scale")
            .description("Detects non-vanilla entity scale.")
            .defaultValue(false)
            .visible(() -> this.mode.get() == AntiBot.Mode.Custom)
            .build()
      );
   private final Set<UUID> horizonBots = new HashSet<>();
   private final Set<UUID> matrixSuspects = new HashSet<>();
   private final Map<UUID, List<ItemStack>> matrixArmor = new HashMap<>();
   private final Set<UUID> matrixBots = new HashSet<>();
   private final Map<UUID, AntiBot.SuspectInfo> intaveSuspects = new HashMap<>();
   private final Set<UUID> intaveBots = new HashSet<>();
   private final Int2IntOpenHashMap invalidGroundFlags = new Int2IntOpenHashMap();
   private final IntOpenHashSet hitSet = new IntOpenHashSet();
   private final IntOpenHashSet swungSet = new IntOpenHashSet();
   private final IntOpenHashSet crittedSet = new IntOpenHashSet();
   private final IntOpenHashSet attributesSet = new IntOpenHashSet();
   private int ticks;

   public AntiBot() {
      super(Categories.Misc, "anti-bot", "LiquidBounce-style AntiBot checks for aura targets.", "antibot");
   }

   @Override
   public void onDeactivate() {
      this.reset();
   }

   @EventHandler
   private void onGameJoined(GameJoinedEvent event) {
      this.reset();
   }

   @EventHandler
   private void onGameLeft(GameLeftEvent event) {
      this.reset();
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (event.entity != null) {
         this.hitSet.add(event.entity.getId());
      }
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.level != null) {
         this.ticks++;
         if (this.mode.get() == AntiBot.Mode.Matrix && !this.matrixSuspects.isEmpty()) {
            for (Player player : this.mc.level.players()) {
               UUID uuid = player.getUUID();
               if (this.matrixSuspects.contains(uuid)) {
                  List<ItemStack> previousArmor = this.matrixArmor.get(uuid);
                  if ((this.isFullyArmored(player) || this.updatesArmor(player, previousArmor)) && player.getGameProfile().properties().isEmpty()) {
                     this.matrixBots.add(uuid);
                  }

                  this.matrixSuspects.remove(uuid);
                  this.matrixArmor.remove(uuid);
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacket(PacketEvent.Receive event) {
      if (this.mc.level != null) {
         switch (event.packet) {
            case ClientboundPlayerInfoUpdatePacket packet:
               this.handlePlayerInfoUpdate(packet);
               break;
            case ClientboundPlayerInfoRemovePacket packet:
               this.handlePlayerInfoRemove(packet);
               break;
            case ClientboundMoveEntityPacket packet:
               this.handleMoveEntity(packet);
               break;
            case ClientboundAnimatePacket packet:
               this.handleAnimate(packet);
               break;
            case ClientboundUpdateAttributesPacket packet:
               this.attributesSet.add(packet.getEntityId());
               break;
            case ClientboundRemoveEntitiesPacket packet:
               for (int id : packet.getEntityIds()) {
                  this.removeEntityId(id);
               }
               break;
            default:
         }
      }
   }

   public boolean isBot(Entity entity) {
      if (this.isActive() && entity != this.mc.player && entity instanceof Player player) {
         if (this.mc.getConnection() == null) {
            return false;
         }

         if (this.literalNpc.get() && this.mc.getConnection().getPlayerInfo(player.getUUID()) == null) {
            return true;
         }

         if (this.notInTabList.get() && this.isMissingFromTabList(player)) {
            return true;
         }

         return switch ((AntiBot.Mode)this.mode.get()) {
            case Custom -> this.isCustomBot(player);
            case Matrix -> this.matrixBots.contains(player.getUUID());
            case IntaveHeavy -> this.intaveBots.contains(player.getUUID());
            case Horizon -> this.horizonBots.contains(player.getUUID());
         };
      } else {
         return false;
      }
   }

   private void handlePlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet) {
      if (packet.actions().contains(Action.ADD_PLAYER)) {
         this.handleAddPlayers(packet.newEntries());
      }

      if (packet.actions().contains(Action.UPDATE_LATENCY)) {
         this.handleLatencyUpdate(packet.entries());
      }
   }

   private void handleAddPlayers(List<Entry> entries) {
      for (Entry entry : entries) {
         GameProfile profile = entry.profile();
         if (profile != null) {
            if (entry.gameMode() == null) {
               this.horizonBots.add(entry.profileId());
            }

            if (entry.latency() >= 2 && !this.isGameProfileUnique(profile) && profile.properties().isEmpty()) {
               if (this.isDuplicate(profile)) {
                  this.matrixBots.add(entry.profileId());
               } else {
                  this.matrixSuspects.add(entry.profileId());
                  Player player = this.mc.level.getEntity(entry.profileId()) instanceof Player p ? p : null;
                  if (player != null && !this.isFullyArmored(player)) {
                     this.matrixArmor.put(entry.profileId(), this.armorItems(player));
                  }
               }
            }

            if (entry.latency() >= 2 && !this.isGameProfileUnique(profile)) {
               this.intaveSuspects.put(entry.profileId(), new AntiBot.SuspectInfo(entry.latency(), this.ticks));
            }
         }
      }
   }

   private void handleLatencyUpdate(List<Entry> entries) {
      for (Entry entry : entries) {
         AntiBot.SuspectInfo info = this.intaveSuspects.remove(entry.profileId());
         if (info != null && entry.latency() == 0 && this.ticks - info.tick <= 1) {
            this.intaveBots.add(entry.profileId());
         }
      }
   }

   private void handlePlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet) {
      for (UUID uuid : packet.profileIds()) {
         this.horizonBots.remove(uuid);
         this.matrixSuspects.remove(uuid);
         this.matrixArmor.remove(uuid);
         this.matrixBots.remove(uuid);
         this.intaveSuspects.remove(uuid);
         this.intaveBots.remove(uuid);
      }
   }

   private void handleMoveEntity(ClientboundMoveEntityPacket packet) {
      if (this.invalidGround.get() && packet.hasPosition()) {
         Entity entity = packet.getEntity(this.mc.level);
         if (entity != null) {
            int id = entity.getId();
            int value = this.invalidGroundFlags.getOrDefault(id, 0);
            if (entity.onGround() && entity.yo != entity.getY()) {
               this.invalidGroundFlags.put(id, value + 1);
            } else if (!entity.onGround() && value > 0) {
               int newValue = value / 2;
               if (newValue <= 0) {
                  this.invalidGroundFlags.remove(id);
               } else {
                  this.invalidGroundFlags.put(id, newValue);
               }
            }
         }
      }
   }

   private void handleAnimate(ClientboundAnimatePacket packet) {
      switch (packet.getAction()) {
         case 0:
         case 3:
            this.swungSet.add(packet.getId());
         case 1:
         case 2:
         default:
            break;
         case 4:
         case 5:
            this.crittedSet.add(packet.getId());
      }
   }

   private boolean isCustomBot(Player player) {
      int id = player.getId();
      return this.invalidGround.get() && this.invalidGroundFlags.getOrDefault(id, 0) >= this.invalidGroundVl.get()
         || this.age.get() && player.tickCount < this.minimumAge.get()
         || this.nameCheck.get() && this.isInvalidName(player)
         || this.noGameMode.get() && this.hasNoGameMode(player)
         || this.illegalPitch.get() && Math.abs(player.getXRot()) > 90.0F
         || this.fakeEntityId.get() && (id < 0 || id > 1000000000)
         || this.needHit.get() && !this.hitSet.contains(id)
         || this.swung.get() && !this.swungSet.contains(id)
         || this.critted.get() && !this.crittedSet.contains(id)
         || this.attributes.get() && !this.attributesSet.contains(id)
         || this.illegalHealth.get() && this.mc.player != null && player.getHealth() > this.mc.player.getMaxHealth()
         || this.illegalScale.get() && player.getAttributeValue(Attributes.SCALE) != 1.0;
   }

   private boolean isInvalidName(Player player) {
      String name = player.getScoreboardName();
      if (name.length() >= 3 && name.length() <= 16) {
         for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && c != '_') {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean hasNoGameMode(Player player) {
      if (this.mc.getConnection() == null) {
         return false;
      }

      PlayerInfo info = this.mc.getConnection().getPlayerInfo(player.getUUID());
      return info == null || info.getGameMode() == null;
   }

   private boolean isMissingFromTabList(Player player) {
      if (this.mc.getConnection() == null) {
         return false;
      }

      for (PlayerInfo info : this.mc.getConnection().getListedOnlinePlayers()) {
         if (Objects.equals(info.getProfile().id(), player.getUUID())) {
            return false;
         }
      }

      return true;
   }

   private boolean isDuplicate(GameProfile profile) {
      if (this.mc.getConnection() == null) {
         return false;
      }

      int count = 0;

      for (PlayerInfo info : this.mc.getConnection().getOnlinePlayers()) {
         GameProfile other = info.getProfile();
         if (Objects.equals(other.name(), profile.name()) && !Objects.equals(other.id(), profile.id())) {
            count++;
         }
      }

      return count == 1;
   }

   private boolean isGameProfileUnique(GameProfile profile) {
      if (this.mc.getConnection() == null) {
         return true;
      }

      int count = 0;

      for (PlayerInfo info : this.mc.getConnection().getOnlinePlayers()) {
         GameProfile other = info.getProfile();
         if (Objects.equals(other.name(), profile.name()) && Objects.equals(other.id(), profile.id())) {
            count++;
         }
      }

      return count == 1;
   }

   private boolean isFullyArmored(Player player) {
      for (ItemStack stack : this.armorItems(player)) {
         if (!this.isPlayerArmor(stack) || !stack.isEnchanted()) {
            return false;
         }
      }

      return true;
   }

   private boolean updatesArmor(Player player, List<ItemStack> previousArmor) {
      return previousArmor == null ? false : !ItemStack.listMatches(previousArmor, this.armorItems(player));
   }

   private List<ItemStack> armorItems(Player player) {
      return List.of(
         player.getItemBySlot(EquipmentSlot.HEAD).copy(),
         player.getItemBySlot(EquipmentSlot.CHEST).copy(),
         player.getItemBySlot(EquipmentSlot.LEGS).copy(),
         player.getItemBySlot(EquipmentSlot.FEET).copy()
      );
   }

   private boolean isPlayerArmor(ItemStack stack) {
      return stack != null && !stack.isEmpty()
         ? stack.is(Items.LEATHER_HELMET)
            || stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS)
            || stack.is(Items.LEATHER_BOOTS)
            || stack.is(Items.CHAINMAIL_HELMET)
            || stack.is(Items.CHAINMAIL_CHESTPLATE)
            || stack.is(Items.CHAINMAIL_LEGGINGS)
            || stack.is(Items.CHAINMAIL_BOOTS)
            || stack.is(Items.IRON_HELMET)
            || stack.is(Items.IRON_CHESTPLATE)
            || stack.is(Items.IRON_LEGGINGS)
            || stack.is(Items.IRON_BOOTS)
            || stack.is(Items.GOLDEN_HELMET)
            || stack.is(Items.GOLDEN_CHESTPLATE)
            || stack.is(Items.GOLDEN_LEGGINGS)
            || stack.is(Items.GOLDEN_BOOTS)
            || stack.is(Items.DIAMOND_HELMET)
            || stack.is(Items.DIAMOND_CHESTPLATE)
            || stack.is(Items.DIAMOND_LEGGINGS)
            || stack.is(Items.DIAMOND_BOOTS)
            || stack.is(Items.NETHERITE_HELMET)
            || stack.is(Items.NETHERITE_CHESTPLATE)
            || stack.is(Items.NETHERITE_LEGGINGS)
            || stack.is(Items.NETHERITE_BOOTS)
            || stack.is(Items.ELYTRA)
            || stack.is(Items.TURTLE_HELMET)
            || stack.is(Items.CARVED_PUMPKIN)
            || stack.is(ItemTags.SKULLS)
         : false;
   }

   private void removeEntityId(int id) {
      this.invalidGroundFlags.remove(id);
      this.hitSet.remove(id);
      this.swungSet.remove(id);
      this.crittedSet.remove(id);
      this.attributesSet.remove(id);
   }

   private void reset() {
      this.horizonBots.clear();
      this.matrixSuspects.clear();
      this.matrixArmor.clear();
      this.matrixBots.clear();
      this.intaveSuspects.clear();
      this.intaveBots.clear();
      this.invalidGroundFlags.clear();
      this.hitSet.clear();
      this.swungSet.clear();
      this.crittedSet.clear();
      this.attributesSet.clear();
      this.ticks = 0;
   }

   @Override
   public String getInfoString() {
      return this.mode.get().toString();
   }

   private enum Mode {
      Custom,
      Matrix,
      IntaveHeavy,
      Horizon;
   }

   private record SuspectInfo(int latency, int tick) {
   }
}
