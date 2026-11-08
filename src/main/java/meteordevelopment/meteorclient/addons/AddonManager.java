package meteordevelopment.meteorclient.addons;

import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.MeteorClient;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

public class AddonManager {
   public static final List<MeteorAddon> ADDONS = new ArrayList<>();

   public static void init() {
      MeteorClient.ADDON = new MeteorAddon() {
         @Override
         public void onInitialize() {
         }

         @Override
         public String getPackage() {
            return "meteordevelopment.meteorclient";
         }

         @Override
         public String getWebsite() {
            return "https://meteorclient.com";
         }

         @Override
         public GithubRepo getRepo() {
            return new GithubRepo("MeteorDevelopment", "meteor-client");
         }

         @Override
         public String getCommit() {
            String commit = MeteorClient.MOD_META.getCustomValue("meteor-client:commit").getAsString();
            return commit.isEmpty() ? null : commit;
         }
      };
      ModMetadata metadata = FabricLoader.getInstance().getModContainer("meteor-client").get().getMetadata();
      MeteorClient.ADDON.name = metadata.getName();
      MeteorClient.ADDON.authors = new String[metadata.getAuthors().size()];
      if (metadata.containsCustomValue("meteor-client:color")) {
         MeteorClient.ADDON.color.parse(metadata.getCustomValue("meteor-client:color").getAsString());
      }

      int i = 0;

      for (Person author : metadata.getAuthors()) {
         MeteorClient.ADDON.authors[i++] = author.getName();
      }

      ADDONS.add(MeteorClient.ADDON);

      for (EntrypointContainer<MeteorAddon> entrypoint : FabricLoader.getInstance().getEntrypointContainers("meteor", MeteorAddon.class)) {
         ModMetadata metadatax = entrypoint.getProvider().getMetadata();

         MeteorAddon addon;
         try {
            addon = entrypoint.getEntrypoint();
         } catch (Throwable throwable) {
            throw new RuntimeException("Exception during addon init \"%s\".".formatted(metadatax.getName()), throwable);
         }

         addon.name = metadatax.getName();
         if (metadatax.getAuthors().isEmpty()) {
            throw new RuntimeException(
               "Addon \"%s\" requires at least 1 author to be defined in it's fabric.mod.json. See https://fabricmc.net/wiki/documentation:fabric_mod_json_spec"
                  .formatted(addon.name)
            );
         }

         addon.authors = new String[metadatax.getAuthors().size()];
         if (metadatax.containsCustomValue("meteor-client:color")) {
            addon.color.parse(metadatax.getCustomValue("meteor-client:color").getAsString());
         }

         int ix = 0;

         for (Person author : metadatax.getAuthors()) {
            addon.authors[ix++] = author.getName();
         }

         ADDONS.add(addon);
      }
   }
}
