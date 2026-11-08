package meteordevelopment.meteorclient.systems.modules.render;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.joml.Vector3d;

public class EntityOwner extends Module {
   private static final Color BACKGROUND = new Color(0, 0, 0, 75);
   private static final Color TEXT = new Color(255, 255, 255);
   private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
   private final Setting<Double> scale = this.sgGeneral
      .add(new DoubleSetting.Builder().name("scale").description("The scale of the text.").defaultValue(1.0).min(0.0).build());
   private final Vector3d pos = new Vector3d();
   private final Map<UUID, String> uuidToName = new HashMap<>();

   public EntityOwner() {
      super(Categories.Render, "entity-owner", "Displays the name of the player who owns the entity you're looking at.");
   }

   @Override
   public void onDeactivate() {
      this.uuidToName.clear();
   }

   @EventHandler
   private void onRender2D(Render2DEvent event) {
      for (Entity entity : this.mc.level.entitiesForRendering()) {
         EntityReference<LivingEntity> owner;
         switch (entity) {
            case TamableAnimal tameable:
               owner = tameable.getOwnerReference();
               break;
            case ThrownEnderpearl pearl:
               owner = EntityReference.of((LivingEntity)pearl.getOwner());
               break;
            default:
               continue;
         }

         if (owner != null) {
            Utils.set(this.pos, entity, event.tickDelta);
            this.pos.add(0.0, entity.getEyeHeight(entity.getPose()) + 0.75, 0.0);
            if (NametagUtils.to2D(this.pos, this.scale.get())) {
               this.renderNametag(this.getOwnerName(owner));
            }
         }
      }
   }

   private void renderNametag(String name) {
      TextRenderer text = TextRenderer.get();
      NametagUtils.begin(this.pos);
      text.beginBig();
      double w = text.getWidth(name);
      double x = -w / 2.0;
      double y = -text.getHeight();
      Renderer2D.COLOR.begin();
      Renderer2D.COLOR.quad(x - 1.0, y - 1.0, w + 2.0, text.getHeight() + 2.0, BACKGROUND);
      Renderer2D.COLOR.render();
      text.render(name, x, y, TEXT);
      text.end();
      NametagUtils.end();
   }

   private String getOwnerName(EntityReference<LivingEntity> owner) {
      LivingEntity ownerEntity = EntityReference.get(owner, this.mc.level, LivingEntity.class);
      if (ownerEntity instanceof Player playerEntity) {
         return playerEntity.getName().getString();
      } else {
         UUID uuid = owner.getUUID();
         String name = this.uuidToName.get(uuid);
         if (name != null) {
            return name;
         }

         MeteorExecutor.execute(
            () -> {
               if (this.isActive()) {
                  EntityOwner.ProfileResponse res = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", ""))
                     .sendJson(EntityOwner.ProfileResponse.class);
                  if (this.isActive()) {
                     if (res == null) {
                        this.uuidToName.put(uuid, "Failed to get name");
                     } else {
                        this.uuidToName.put(uuid, res.name);
                     }
                  }
               }
            }
         );
         name = "Retrieving";
         this.uuidToName.put(uuid, name);
         return name;
      }
   }

   private static class ProfileResponse {
      public String name;
   }
}
