/**
 * ============================================================
 * [MagicBeeRenderer.java]
 * Description: Renderer custom pour MagicBeeEntity avec textures par espèce
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité à rendre      | Accès aux données (espèce)     |
 * | BeeModel            | Modèle vanilla       | Base du rendu                  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ClientSetup.java: Enregistrement du renderer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.renderer.entity;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Renderer custom pour les abeilles magiques.
 * Utilise le modele vanilla de l'abeille avec des textures par espece.
 *
 * Structure des textures (tout en minuscules):
 * - textures/entity/bee/{speciesid}_bee.png - Texture normale
 * - textures/entity/bee/{speciesid}_bee_nectar.png - Texture avec pollen (optionnel)
 *
 * Si la texture d'espece n'existe pas, utilise la texture vanilla.
 */
public class MagicBeeRenderer extends MobRenderer<MagicBeeEntity, BeeModel<MagicBeeEntity>> {

    // Cache des especes avec textures custom valides
    private static final Set<String> VALID_SPECIES_TEXTURES = new HashSet<>();
    private static final Set<String> CHECKED_SPECIES = new HashSet<>();

    // Textures vanilla fallback
    private static final ResourceLocation VANILLA_BEE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");
    private static final ResourceLocation VANILLA_BEE_NECTAR =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_nectar.png");

    public MagicBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new BeeModel<>(context.bakeLayer(ModelLayers.BEE)), 0.4f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MagicBeeEntity bee) {
        String speciesId = bee.getSpeciesId();
        boolean pollinated = bee.isPollinated();

        // Verifier si cette espece a une texture custom (avec cache)
        if (!CHECKED_SPECIES.contains(speciesId)) {
            checkSpeciesTexture(speciesId);
        }

        // Si l'espece a une texture valide, l'utiliser
        if (VALID_SPECIES_TEXTURES.contains(speciesId)) {
            // ResourceLocation requiert des minuscules uniquement
            String texturePath = "textures/entity/bee/" + speciesId + "_bee";

            // Essayer d'abord la texture nectar si pollinisee
            if (pollinated) {
                ResourceLocation nectarTexture = ResourceLocation.fromNamespaceAndPath(
                        Beemancer.MOD_ID,
                        texturePath + "_nectar.png"
                );
                if (resourceExists(nectarTexture)) {
                    return nectarTexture;
                }
            }

            // Texture normale
            return ResourceLocation.fromNamespaceAndPath(
                    Beemancer.MOD_ID,
                    texturePath + ".png"
            );
        }

        // Fallback vers vanilla
        return pollinated ? VANILLA_BEE_NECTAR : VANILLA_BEE;
    }

    /**
     * Verifie si une espece a une texture custom et met en cache le resultat.
     */
    private void checkSpeciesTexture(String speciesId) {
        CHECKED_SPECIES.add(speciesId);

        // ResourceLocation requiert des minuscules uniquement
        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                Beemancer.MOD_ID,
                "textures/entity/bee/" + speciesId + "_bee.png"
        );

        if (resourceExists(textureLocation)) {
            VALID_SPECIES_TEXTURES.add(speciesId);
        }
    }

    /**
     * Vérifie si une ressource existe.
     */
    private boolean resourceExists(ResourceLocation location) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Récupère la texture vanilla (fallback).
     */
    public static ResourceLocation getVanillaTexture(boolean pollinated) {
        return pollinated ? VANILLA_BEE_NECTAR : VANILLA_BEE;
    }

    /**
     * Vide le cache des textures (utile pour le rechargement des ressources).
     */
    public static void clearTextureCache() {
        VALID_SPECIES_TEXTURES.clear();
        CHECKED_SPECIES.clear();
    }
}
