package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Items;

public class Offhand extends Module {
   private final SettingGroup sgCombat = this.settings.createGroup("Combat");
   private final SettingGroup sgTotem = this.settings.createGroup("Totem");
   private final Setting<Integer> delayTicks = this.sgCombat
      .add(
         new IntSetting.Builder()
            .name("item-switch-delay")
            .description("The delay in ticks between slot movements.")
            .defaultValue(0)
            .min(0)
            .sliderMax(20)
            .build()
      );
   private final Setting<Offhand.Item> preferreditem = this.sgCombat
      .add(
         ((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("item"))
                  .description("Which item to hold in your offhand."))
               .defaultValue(Offhand.Item.Crystal))
            .build()
      );
   private final Setting<Boolean> hotbar = this.sgCombat
      .add(new BoolSetting.Builder().name("hotbar").description("Whether to use items from your hotbar.").defaultValue(false).build());
   private final Setting<Boolean> rightgapple = this.sgCombat
      .add(
         new BoolSetting.Builder()
            .name("right-gapple")
            .description("Will switch to a gapple when holding right click.(DO NOT USE WITH POTION ON)")
            .defaultValue(false)
            .build()
      );
   private final Setting<Boolean> SwordGap = this.sgCombat
      .add(
         new BoolSetting.Builder()
            .name("sword-gapple")
            .description("Will switch to a gapple when holding a sword and right click.")
            .defaultValue(false)
            .visible(this.rightgapple::get)
            .build()
      );
   private final Setting<Boolean> alwaysSwordGap = this.sgCombat
      .add(
         new BoolSetting.Builder()
            .name("always-gap-on-sword")
            .description("Holds an Enchanted Golden Apple when you are holding a sword.")
            .defaultValue(false)
            .visible(() -> !this.rightgapple.get())
            .build()
      );
   private final Setting<Boolean> alwaysPot = this.sgCombat
      .add(
         new BoolSetting.Builder()
            .name("always-pot-on-sword")
            .description("Will switch to a potion when holding a sword")
            .defaultValue(false)
            .visible(() -> !this.rightgapple.get() && !this.alwaysSwordGap.get())
            .build()
      );
   private final Setting<Boolean> potionClick = this.sgCombat
      .add(
         new BoolSetting.Builder()
            .name("sword-pot")
            .description("Will switch to a potion when holding a sword and right click.")
            .defaultValue(false)
            .visible(() -> !this.rightgapple.get() && !this.alwaysPot.get() && !this.alwaysSwordGap.get())
            .build()
      );
   private final Setting<Double> minHealth = this.sgTotem
      .add(
         new DoubleSetting.Builder()
            .name("min-health")
            .description("Will hold a totem when below this amount of health.")
            .defaultValue(10.0)
            .range(0.0, 36.0)
            .sliderRange(0.0, 36.0)
            .build()
      );
   private final Setting<Boolean> elytra = this.sgTotem
      .add(new BoolSetting.Builder().name("elytra").description("Will always hold a totem while flying with an elytra.").defaultValue(false).build());
   private final Setting<Boolean> falling = this.sgTotem
      .add(new BoolSetting.Builder().name("falling").description("Will hold a totem if fall damage could kill you.").defaultValue(false).build());
   private final Setting<Boolean> explosion = this.sgTotem
      .add(new BoolSetting.Builder().name("explosion").description("Will hold a totem when explosion damage could kill you.").defaultValue(true).build());
   private boolean isClicking;
   private boolean sentMessage;
   private Offhand.Item currentItem;
   public boolean locked;
   private int totems;
   private int ticks;

   public Offhand() {
      super(Categories.Combat, "offhand", "Allows you to hold specified items in your offhand.");
   }

   @Override
   public void onActivate() {
      this.ticks = 0;
      this.sentMessage = false;
      this.isClicking = false;
      this.currentItem = this.preferreditem.get();
   }

   @EventHandler(priority = 1199)
   private void onTick(TickEvent.Pre event) {
      FindItemResult result = InvUtils.find(Items.TOTEM_OF_UNDYING);
      this.totems = result.count();
      if (this.totems <= 0) {
         this.locked = false;
      } else if (this.ticks > this.delayTicks.get()) {
         boolean low = this.mc.player.getHealth()
               + this.mc.player.getAbsorptionAmount()
               - PlayerUtils.possibleHealthReductions(this.explosion.get(), this.falling.get())
            <= this.minHealth.get();
         boolean ely = this.elytra.get() && this.mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && this.mc.player.isFallFlying();
         FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == this.currentItem.item, 0, 35);
         this.locked = low || ely;
         if (this.locked && this.mc.player.getMainHandItem().getItem() != Items.TOTEM_OF_UNDYING) {
            InvUtils.move().from(result.slot()).toOffhand();
         }

         this.ticks = 0;
         return;
      }

      this.ticks++;
      AutoTotem autoTotem = Modules.get().get(AutoTotem.class);
      this.currentItem = this.preferreditem.get();
      if (this.rightgapple.get()) {
         if (!this.locked) {
            if (this.SwordGap.get() && this.mc.player.getMainHandItem().is(ItemTags.SWORDS) && this.isClicking) {
               this.currentItem = Offhand.Item.EGap;
            }

            if (!this.SwordGap.get() && this.isClicking) {
               this.currentItem = Offhand.Item.EGap;
            }
         }
      } else if ((this.mc.player.getMainHandItem().is(ItemTags.SWORDS) || this.mc.player.getMainHandItem().getItem() instanceof AxeItem)
         && this.alwaysSwordGap.get()) {
         this.currentItem = Offhand.Item.EGap;
      } else if (this.potionClick.get()) {
         if (!this.locked && this.mc.player.getMainHandItem().is(ItemTags.SWORDS) && this.isClicking) {
            this.currentItem = Offhand.Item.Potion;
         }
      } else if ((this.mc.player.getMainHandItem().is(ItemTags.SWORDS) || this.mc.player.getMainHandItem().getItem() instanceof AxeItem)
         && this.alwaysPot.get()) {
         this.currentItem = Offhand.Item.Potion;
      } else {
         this.currentItem = this.preferreditem.get();
      }

      if (this.mc.player.getOffhandItem().getItem() != this.currentItem.item && this.ticks >= this.delayTicks.get()) {
         if (!this.locked) {
            FindItemResult item = InvUtils.find(itemStack -> itemStack.getItem() == this.currentItem.item, this.hotbar.get() ? 0 : 9, 35);
            if (!item.found()) {
               if (!this.sentMessage) {
                  this.warning("Chosen item not found.");
                  this.sentMessage = true;
               }
            } else if (this.isClicking || !autoTotem.isLocked() && !item.isOffhand()) {
               InvUtils.move().from(item.slot()).toOffhand();
               this.sentMessage = false;
            }

            this.ticks = 0;
            return;
         }

         this.ticks++;
      }
   }

   @EventHandler
   private void onMouseClick(MouseClickEvent event) {
      this.isClicking = this.mc.screen == null
         && !Modules.get().get(AutoTotem.class).isLocked()
         && !this.usableItem()
         && !this.mc.player.isUsingItem()
         && event.action == KeyAction.Press
         && event.button() == 1;
   }

   private boolean usableItem() {
      return this.mc.player.getMainHandItem().getItem() == Items.BOW
         || this.mc.player.getMainHandItem().getItem() == Items.TRIDENT
         || this.mc.player.getMainHandItem().getItem() == Items.CROSSBOW
         || Utils.isFood(this.mc.player.getMainHandItem());
   }

   @Override
   public String getInfoString() {
      return this.preferreditem.get().name();
   }

   public enum Item {
      EGap(Items.ENCHANTED_GOLDEN_APPLE),
      Gap(Items.GOLDEN_APPLE),
      Crystal(Items.END_CRYSTAL),
      Totem(Items.TOTEM_OF_UNDYING),
      Shield(Items.SHIELD),
      Potion(Items.POTION);

      final net.minecraft.world.item.Item item;

      Item(net.minecraft.world.item.Item item) {
         this.item = item;
      }
   }
}
