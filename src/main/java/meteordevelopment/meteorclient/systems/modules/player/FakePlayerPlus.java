package meteordevelopment.meteorclient.systems.modules.player;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Criticals;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FakePlayerPlus extends Module {
   private final List<FakePlayerPlus.PlayerState> positions = new ArrayList<>();
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<String> name = this.sgGeneral
      .add(new StringSetting.Builder().name("name").description("Name of the fake player.").defaultValue("NoStrict").build());
   private final Setting<Boolean> copyInventory = this.sgGeneral
      .add(new BoolSetting.Builder().name("copy-inventory").description("Copy player inventory.").defaultValue(false).build());
   private final Setting<Boolean> record = this.sgGeneral
      .add(new BoolSetting.Builder().name("record").description("Record player position and rotation.").defaultValue(false).build());
   private final Setting<Boolean> play = this.sgGeneral
      .add(new BoolSetting.Builder().name("play").description("Replay recorded movement.").defaultValue(false).build());
   private final Setting<Boolean> autoTotem = this.sgGeneral
      .add(new BoolSetting.Builder().name("auto-totem").description("Keeps a totem in offhand.").defaultValue(true).build());
   private FakePlayerEntity fakePlayer;
   private int movementTick;
   private int deathTime;

   public FakePlayerPlus() {
      super(Categories.Player, "fake-player+", "Spawns a client-side fake player with record/play and totem logic.");
   }

   @Override
   public void onActivate() {
      this.positions.clear();
      this.movementTick = 0;
      this.deathTime = 0;
      if (this.mc.player != null && this.mc.level != null) {
         this.fakePlayer = new FakePlayerEntity(this.mc.player, this.name.get(), 20.0F, this.copyInventory.get());
         this.fakePlayer.spawn();
         this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 9999, 2), null);
         this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 9999, 4), null);
         this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 9999, 1), null);
      }
   }

   @Override
   public void onDeactivate() {
      this.despawnFakePlayer();
      this.positions.clear();
      this.movementTick = 0;
      this.deathTime = 0;
   }

   @EventHandler
   private void onTick(TickEvent.Pre event) {
      if (this.mc.player != null && this.mc.level != null && this.fakePlayer != null) {
         if (this.record.get()) {
            this.positions
               .add(
                  new FakePlayerPlus.PlayerState(
                     this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(), this.mc.player.getYRot(), this.mc.player.getXRot()
                  )
               );
         } else {
            if (this.play.get() && !this.positions.isEmpty()) {
               this.movementTick++;
               if (this.movementTick >= this.positions.size()) {
                  this.movementTick = 0;
               }

               FakePlayerPlus.PlayerState p = this.positions.get(this.movementTick);
               this.fakePlayer.setYRot(p.yaw);
               this.fakePlayer.setXRot(p.pitch);
               this.fakePlayer.yHeadRot = p.yaw;
               this.fakePlayer.yBodyRot = p.yaw;
               this.fakePlayer.setPos(p.x, p.y, p.z);
            } else {
               this.movementTick = 0;
            }

            if (this.autoTotem.get() && !this.fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
               this.fakePlayer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }

            if (!this.fakePlayer.isDeadOrDying() && !(this.fakePlayer.getHealth() <= 0.0F)) {
               this.deathTime = 0;
            } else {
               this.deathTime++;
               if (this.deathTime > 10) {
                  this.toggle();
               }
            }
         }
      }
   }

   @EventHandler
   private void onPacketReceive(PacketEvent.Receive event) {
      if (this.fakePlayer != null) {
         if (this.mc.level != null) {
            if (event.packet instanceof ClientboundExplodePacket explosion && this.fakePlayer.hurtTime == 0) {
               this.markHurt();
               float damage = DamageUtils.explosionDamage(
                  this.fakePlayer, this.fakePlayer.position(), this.fakePlayer.getBoundingBox(), explosion.center(), 6.0F, DamageUtils.HIT_FACTORY
               );
               this.fakePlayer.setHealth(this.fakePlayer.getHealth() + this.fakePlayer.getAbsorptionAmount() - damage);
               if (this.fakePlayer.isDeadOrDying()) {
                  this.tryPopTotem();
               }
            }

            if (event.packet instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35 && packet.getEntity(this.mc.level) == this.fakePlayer) {
               this.fakePlayer.hurtTime = 10;
            }
         }
      }
   }

   @EventHandler
   private void onAttack(AttackEntityEvent event) {
      if (this.fakePlayer != null && event.entity == this.fakePlayer && this.fakePlayer.hurtTime == 0 && this.mc.player != null && this.mc.level != null) {
         if (this.mc.player.fallDistance > 0.0 || Modules.get().isActive(Criticals.class)) {
            this.mc
               .level
               .playSound(
                  this.mc.player,
                  this.fakePlayer.getX(),
                  this.fakePlayer.getY(),
                  this.fakePlayer.getZ(),
                  SoundEvents.PLAYER_ATTACK_CRIT,
                  SoundSource.PLAYERS,
                  1.0F,
                  1.0F
               );
         }

         this.markHurt();
         float damage = this.mc.player.getAttackStrengthScale(0.5F) >= 0.85F
            ? DamageUtils.getAttackDamage(this.mc.player, this.fakePlayer, this.mc.player.getMainHandItem())
            : 1.0F;
         this.fakePlayer.setHealth(this.fakePlayer.getHealth() + this.fakePlayer.getAbsorptionAmount() - damage);
         if (this.fakePlayer.isDeadOrDying()) {
            this.tryPopTotem();
         }
      }
   }

   private void despawnFakePlayer() {
      if (this.fakePlayer != null) {
         if (this.mc.level != null) {
            this.fakePlayer.despawn();
         }

         this.fakePlayer = null;
      }
   }

   private void markHurt() {
      this.fakePlayer.hurtTime = 10;
      this.fakePlayer.hurtDuration = 10;
      this.fakePlayer.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
   }

   private boolean tryPopTotem() {
      if (!this.fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      this.fakePlayer.setHealth(10.0F);
      this.fakePlayer.handleEntityEvent((byte)35);
      return true;
   }

   @Override
   public String getInfoString() {
      return String.valueOf(this.fakePlayer == null ? 0 : 1);
   }

   private record PlayerState(double x, double y, double z, float yaw, float pitch) {
   }
}
