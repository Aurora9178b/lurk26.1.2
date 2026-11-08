package meteordevelopment.meteorclient.systems.modules.render;

import java.util.List;
import java.util.Set;
import meteordevelopment.meteorclient.events.render.RenderBlockEntityEvent;
import meteordevelopment.meteorclient.events.world.ChunkOcclusionEvent;
import meteordevelopment.meteorclient.events.world.ParticleEvent;
import meteordevelopment.meteorclient.mixin.BlockEntityRenderStateAccessor;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ParticleTypeListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;

public class NoRender extends Module {
   private final SettingGroup sgOverlay = this.settings.createGroup("Overlay");
   private final SettingGroup sgHUD = this.settings.createGroup("HUD");
   private final SettingGroup sgWorld = this.settings.createGroup("World");
   private final SettingGroup sgEntity = this.settings.createGroup("Entity");
   private final Setting<Boolean> noPortalOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("portal-overlay").description("Disables rendering of the nether portal overlay.").defaultValue(false).build());
   private final Setting<Boolean> noSpyglassOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("spyglass-overlay").description("Disables rendering of the spyglass overlay.").defaultValue(false).build());
   private final Setting<Boolean> noNausea = this.sgOverlay
      .add(new BoolSetting.Builder().name("nausea").description("Disables rendering of the nausea overlay.").defaultValue(false).build());
   private final Setting<Boolean> noPumpkinOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("pumpkin-overlay").description("Disables rendering of the pumpkin head overlay").defaultValue(false).build());
   private final Setting<Boolean> noPowderedSnowOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("powdered-snow-overlay").description("Disables rendering of the powdered snow overlay.").defaultValue(false).build());
   private final Setting<Boolean> noFireOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("fire-overlay").description("Disables rendering of the fire overlay.").defaultValue(false).build());
   private final Setting<Boolean> noLiquidOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("liquid-overlay").description("Disables rendering of the liquid overlay.").defaultValue(false).build());
   private final Setting<Boolean> noInWallOverlay = this.sgOverlay
      .add(new BoolSetting.Builder().name("in-wall-overlay").description("Disables rendering of the overlay when inside blocks.").defaultValue(false).build());
   private final Setting<Boolean> noVignette = this.sgOverlay
      .add(new BoolSetting.Builder().name("vignette").description("Disables rendering of the vignette overlay.").defaultValue(false).build());
   private final Setting<Boolean> noGuiBackground = this.sgOverlay
      .add(new BoolSetting.Builder().name("gui-background").description("Disables rendering of the GUI background overlay.").defaultValue(false).build());
   private final Setting<Boolean> noTotemAnimation = this.sgOverlay
      .add(
         new BoolSetting.Builder()
            .name("totem-animation")
            .description("Disables rendering of the totem animation when you pop a totem.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noEatParticles = this.sgOverlay
      .add(new BoolSetting.Builder().name("eating-particles").description("Disables rendering of eating particles.").defaultValue(false).build());
   private final Setting<Boolean> noEnchantGlint = this.sgOverlay
      .add(new BoolSetting.Builder().name("enchantment-glint").description("Disables rending of the enchantment glint.").defaultValue(false).build());
   private final Setting<Boolean> noBossBar = this.sgHUD
      .add(new BoolSetting.Builder().name("boss-bar").description("Disable rendering of boss bars.").defaultValue(false).build());
   private final Setting<Boolean> noScoreboard = this.sgHUD
      .add(new BoolSetting.Builder().name("scoreboard").description("Disable rendering of the scoreboard.").defaultValue(false).build());
   private final Setting<Boolean> noCrosshair = this.sgHUD
      .add(new BoolSetting.Builder().name("crosshair").description("Disables rendering of the crosshair.").defaultValue(false).build());
   private final Setting<Boolean> noTitle = this.sgHUD
      .add(new BoolSetting.Builder().name("title").description("Disables rendering of the title.").defaultValue(false).build());
   private final Setting<Boolean> noHeldItemName = this.sgHUD
      .add(new BoolSetting.Builder().name("held-item-name").description("Disables rendering of the held item name.").defaultValue(false).build());
   private final Setting<Boolean> noObfuscation = this.sgHUD
      .add(new BoolSetting.Builder().name("obfuscation").description("Disables obfuscation styling of characters.").defaultValue(false).build());
   private final Setting<Boolean> noPotionIcons = this.sgHUD
      .add(new BoolSetting.Builder().name("potion-icons").description("Disables rendering of status effect icons.").defaultValue(false).build());
   private final Setting<Boolean> noMessageSignatureIndicator = this.sgHUD
      .add(
         new BoolSetting.Builder()
            .name("message-signature-indicator")
            .description("Disables chat message signature indicator on the left of the message.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noWeather = this.sgWorld
      .add(new BoolSetting.Builder().name("weather").description("Disables rendering of weather.").defaultValue(false).build());
   private final Setting<Boolean> noWorldBorder = this.sgWorld
      .add(new BoolSetting.Builder().name("world-border").description("Disables rendering of the world border.").defaultValue(false).build());
   private final Setting<Boolean> noBlindness = this.sgWorld
      .add(new BoolSetting.Builder().name("blindness").description("Disables rendering of blindness.").defaultValue(false).build());
   private final Setting<Boolean> noDarkness = this.sgWorld
      .add(new BoolSetting.Builder().name("darkness").description("Disables rendering of darkness.").defaultValue(false).build());
   private final Setting<Boolean> noFog = this.sgWorld
      .add(new BoolSetting.Builder().name("fog").description("Disables rendering of fog.").defaultValue(false).build());
   private final Setting<Boolean> noEnchTableBook = this.sgWorld
      .add(
         new BoolSetting.Builder()
            .name("enchantment-table-book")
            .description("Disables rendering of books above enchanting tables.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noSignText = this.sgWorld
      .add(new BoolSetting.Builder().name("sign-text").description("Disables rendering of text on signs.").defaultValue(false).build());
   private final Setting<Boolean> noBlockBreakOverlay = this.sgWorld
      .add(new BoolSetting.Builder().name("block-break-overlay").description("Disables rendering of block-break overlay.").defaultValue(false).build());
   private final Setting<Boolean> noBeaconBeams = this.sgWorld
      .add(new BoolSetting.Builder().name("beacon-beams").description("Disables rendering of beacon beams.").defaultValue(false).build());
   private final Setting<Boolean> noFallingBlocks = this.sgWorld
      .add(new BoolSetting.Builder().name("falling-blocks").description("Disables rendering of falling blocks.").defaultValue(false).build());
   private final Setting<Boolean> noCaveCulling = this.sgWorld
      .add(
         new BoolSetting.Builder()
            .name("cave-culling")
            .description("Disables Minecraft's cave culling algorithm.")
            .defaultValue(false)
            .onChanged(var1 -> this.mc.levelRenderer.allChanged())
            .build()
      );
   private final Setting<Boolean> noMapMarkers = this.sgWorld
      .add(new BoolSetting.Builder().name("map-markers").description("Disables markers on maps.").defaultValue(false).build());
   private final Setting<Boolean> noMapContents = this.sgWorld
      .add(new BoolSetting.Builder().name("map-contents").description("Disable rendering of maps.").defaultValue(false).build());
   private final Setting<NoRender.BannerRenderMode> bannerRender = this.sgWorld
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("banners"))
                  .description("Changes rendering of banners."))
               .defaultValue(NoRender.BannerRenderMode.Everything))
            .build()
      );
   private final Setting<Boolean> noFireworkExplosions = this.sgWorld
      .add(new BoolSetting.Builder().name("firework-explosions").description("Disables rendering of firework explosions.").defaultValue(false).build());
   private final Setting<List<ParticleType<?>>> particles = this.sgWorld
      .add(new ParticleTypeListSetting.Builder().name("particles").description("Particles to not render.").build());
   private final Setting<Boolean> noBarrierInvis = this.sgWorld
      .add(
         new BoolSetting.Builder()
            .name("barrier-invisibility")
            .description("Disables barriers being invisible when not holding one.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noTextureRotations = this.sgWorld
      .add(
         new BoolSetting.Builder()
            .name("texture-rotations")
            .description("Changes texture rotations and model offsets to use a constant value instead of the block position.")
            .defaultValue(false)
            .onChanged(var1 -> this.mc.levelRenderer.allChanged())
            .build()
      );
   private final Setting<List<Block>> blockEntities = this.sgWorld
      .add(
         new BlockListSetting.Builder()
            .name("block-entities")
            .description("Block entities (chest, shulker block, etc.) to not render.")
            .filter(block -> block instanceof EntityBlock && !(block instanceof AbstractBannerBlock))
            .build()
      );
   private final Setting<Set<EntityType<?>>> entities = this.sgEntity
      .add(new EntityTypeListSetting.Builder().name("entities").description("Disables rendering of selected entities.").build());
   private final Setting<Boolean> dropSpawnPacket = this.sgEntity
      .add(
         new BoolSetting.Builder()
            .name("drop-spawn-packets")
            .description("WARNING! Drops all spawn packets of entities selected in the above list.")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noArmor = this.sgEntity
      .add(new BoolSetting.Builder().name("armor").description("Disables rendering of armor on entities.").defaultValue(false).build());
   private final Setting<Boolean> noInvisibility = this.sgEntity
      .add(new BoolSetting.Builder().name("invisibility").description("Shows invisible entities.").defaultValue(false).build());
   private final Setting<Boolean> noGlowing = this.sgEntity
      .add(new BoolSetting.Builder().name("glowing").description("Disables rendering of the glowing effect").defaultValue(false).build());
   private final Setting<Boolean> noMobInSpawner = this.sgEntity
      .add(
         new BoolSetting.Builder()
            .name("spawner-entities")
            .description("Disables rendering of spinning mobs inside of mob spawners")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> noDeadEntities = this.sgEntity
      .add(new BoolSetting.Builder().name("dead-entities").description("Disables rendering of dead entities").defaultValue(false).build());
   private final Setting<Boolean> noNametags = this.sgEntity
      .add(new BoolSetting.Builder().name("nametags").description("Disables rendering of entity nametags").defaultValue(false).build());

   public NoRender() {
      super(Categories.Render, "no-render", "Disables certain animations or overlays from rendering.");
   }

   @Override
   public void onActivate() {
      if (this.noCaveCulling.get() || this.noTextureRotations.get()) {
         this.mc.levelRenderer.allChanged();
      }
   }

   @Override
   public void onDeactivate() {
      if (this.noCaveCulling.get() || this.noTextureRotations.get()) {
         this.mc.levelRenderer.allChanged();
      }
   }

   public boolean noPortalOverlay() {
      return this.isActive() && this.noPortalOverlay.get();
   }

   public boolean noSpyglassOverlay() {
      return this.isActive() && this.noSpyglassOverlay.get();
   }

   public boolean noNausea() {
      return this.isActive() && this.noNausea.get();
   }

   public boolean noPumpkinOverlay() {
      return this.isActive() && this.noPumpkinOverlay.get();
   }

   public boolean noFireOverlay() {
      return this.isActive() && this.noFireOverlay.get();
   }

   public boolean noLiquidOverlay() {
      return this.isActive() && this.noLiquidOverlay.get();
   }

   public boolean noPowderedSnowOverlay() {
      return this.isActive() && this.noPowderedSnowOverlay.get();
   }

   public boolean noInWallOverlay() {
      return this.isActive() && this.noInWallOverlay.get();
   }

   public boolean noVignette() {
      return this.isActive() && this.noVignette.get();
   }

   public boolean noGuiBackground() {
      return this.isActive() && this.noGuiBackground.get();
   }

   public boolean noTotemAnimation() {
      return this.isActive() && this.noTotemAnimation.get();
   }

   public boolean noEatParticles() {
      return this.isActive() && this.noEatParticles.get();
   }

   public boolean noEnchantGlint() {
      return this.isActive() && this.noEnchantGlint.get();
   }

   public boolean noBossBar() {
      return this.isActive() && this.noBossBar.get();
   }

   public boolean noScoreboard() {
      return this.isActive() && this.noScoreboard.get();
   }

   public boolean noCrosshair() {
      return this.isActive() && this.noCrosshair.get();
   }

   public boolean noTitle() {
      return this.isActive() && this.noTitle.get();
   }

   public boolean noHeldItemName() {
      return this.isActive() && this.noHeldItemName.get();
   }

   public boolean noObfuscation() {
      return this.isActive() && this.noObfuscation.get();
   }

   public boolean noPotionIcons() {
      return this.isActive() && this.noPotionIcons.get();
   }

   public boolean noMessageSignatureIndicator() {
      return this.isActive() && this.noMessageSignatureIndicator.get();
   }

   public boolean noWeather() {
      return this.isActive() && this.noWeather.get();
   }

   public boolean noWorldBorder() {
      return this.isActive() && this.noWorldBorder.get();
   }

   public boolean noBlindness() {
      return this.isActive() && this.noBlindness.get();
   }

   public boolean noDarkness() {
      return this.isActive() && this.noDarkness.get();
   }

   public boolean noFog() {
      return this.isActive() && this.noFog.get();
   }

   public boolean noEnchTableBook() {
      return this.isActive() && this.noEnchTableBook.get();
   }

   public boolean noSignText() {
      return this.isActive() && this.noSignText.get();
   }

   public boolean noParticle(ParticleType<?> type) {
      return this.isActive() && this.particles.get().contains(type);
   }

   public boolean noBlockBreakOverlay() {
      return this.isActive() && this.noBlockBreakOverlay.get();
   }

   public boolean noBeaconBeams() {
      return this.isActive() && this.noBeaconBeams.get();
   }

   public boolean noFallingBlocks() {
      return this.isActive() && this.noFallingBlocks.get();
   }

   @EventHandler
   private void onChunkOcclusion(ChunkOcclusionEvent event) {
      if (this.noCaveCulling.get()) {
         event.cancel();
      }
   }

   public boolean noMapMarkers() {
      return this.isActive() && this.noMapMarkers.get();
   }

   public boolean noMapContents() {
      return this.isActive() && this.noMapContents.get();
   }

   public NoRender.BannerRenderMode getBannerRenderMode() {
      return !this.isActive() ? NoRender.BannerRenderMode.Everything : this.bannerRender.get();
   }

   public boolean noFireworkExplosions() {
      return this.isActive() && this.noFireworkExplosions.get();
   }

   @EventHandler
   private void onAddParticle(ParticleEvent event) {
      if (this.noWeather.get() && event.particle.getType() == ParticleTypes.RAIN) {
         event.cancel();
      } else if (this.noFireworkExplosions.get() && event.particle.getType() == ParticleTypes.FIREWORK) {
         event.cancel();
      } else if (this.particles.get().contains(event.particle.getType())) {
         event.cancel();
      }
   }

   public boolean noBarrierInvis() {
      return this.isActive() && this.noBarrierInvis.get();
   }

   public boolean noTextureRotations() {
      return this.isActive() && this.noTextureRotations.get();
   }

   @EventHandler
   private void onRenderBlockEntity(RenderBlockEntityEvent event) {
      if (this.blockEntities.get().contains(((BlockEntityRenderStateAccessor)event.blockEntityState).meteor$getBlockState().getBlock())) {
         event.cancel();
      }
   }

   public boolean noEntity(Entity entity) {
      return this.isActive() && this.entities.get().contains(entity.getType());
   }

   public boolean noEntity(EntityType<?> entity) {
      return this.isActive() && this.entities.get().contains(entity);
   }

   public boolean getDropSpawnPacket() {
      return this.isActive() && this.dropSpawnPacket.get();
   }

   public boolean noArmor() {
      return this.isActive() && this.noArmor.get();
   }

   public boolean noInvisibility() {
      return this.isActive() && this.noInvisibility.get();
   }

   public boolean noGlowing() {
      return this.isActive() && this.noGlowing.get();
   }

   public boolean noMobInSpawner() {
      return this.isActive() && this.noMobInSpawner.get();
   }

   public boolean noDeadEntities() {
      return this.isActive() && this.noDeadEntities.get();
   }

   public boolean noNametags() {
      return this.isActive() && this.noNametags.get();
   }

   public enum BannerRenderMode {
      Everything,
      Pillar,
      None;
   }
}
