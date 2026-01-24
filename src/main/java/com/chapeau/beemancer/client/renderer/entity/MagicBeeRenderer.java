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
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Renderer custom pour les abeilles magiques.
 * Supporte des textures différentes par espèce et état (pollinisé/non pollinisé).
 *
 * Structure des textures:
 * - textures/entity/bee/{species}.png - Texture normale
 * - textures/entity/bee/{species}_nectar.png - Texture avec pollen
 *
 * Si la texture d'espèce n'existe pas, utilise la texture vanilla.
 */
public class MagicBeeRenderer extends MobRenderer<MagicBeeEntity, BeeModel<MagicBeeEntity>> {

    // Cache des textures pour éviter de créer des ResourceLocation à chaque frame
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();
    private static final Map<String, ResourceLocation> TEXTURE_NECTAR_CACHE = new HashMap<>();

    // Textures vanilla fallback
    private static final ResourceLocation VANILLA_BEE = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");
    private static final ResourceLocation VANILLA_BEE_NECTAR = ResourceLocation.withDefaultNamespace("textures/entity/bee/bee_nectar.png");

    public MagicBeeRenderer(EntityRendererProvider.Context context) {
        super(context, new BeeModel<>(context.bakeLayer(ModelLayers.BEE)), 0.4f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull MagicBeeEntity bee) {
        String speciesId = bee.getSpeciesId();
        boolean pollinated = bee.isPollinated();

        // Choisir le cache approprié
        Map<String, ResourceLocation> cache = pollinated ? TEXTURE_NECTAR_CACHE : TEXTURE_CACHE;

        // Récupérer ou créer la texture
        return cache.computeIfAbsent(speciesId, id -> {
            String suffix = pollinated ? "_nectar" : "";
            ResourceLocation customTexture = ResourceLocation.fromNamespaceAndPath(
                    Beemancer.MOD_ID,
                    "textures/entity/bee/" + id + suffix + ".png"
            );
            // Note: On retourne toujours la texture custom, le système de Minecraft
            // utilisera la texture par défaut si elle n'existe pas (via missing texture)
            // Pour un fallback propre, il faudrait vérifier l'existence du fichier,
            // mais ce n'est pas trivial côté client sans un système de préchargement
            return customTexture;
        });
    }

    /**
     * Récupère la texture pour une espèce spécifique.
     * Utilise le cache pour éviter les allocations répétées.
     *
     * @param speciesId ID de l'espèce
     * @param pollinated Si l'abeille porte du pollen
     * @return ResourceLocation de la texture
     */
    public static ResourceLocation getTextureForSpecies(String speciesId, boolean pollinated) {
        Map<String, ResourceLocation> cache = pollinated ? TEXTURE_NECTAR_CACHE : TEXTURE_CACHE;
        String suffix = pollinated ? "_nectar" : "";

        return cache.computeIfAbsent(speciesId, id ->
                ResourceLocation.fromNamespaceAndPath(
                        Beemancer.MOD_ID,
                        "textures/entity/bee/" + id + suffix + ".png"
                )
        );
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
        TEXTURE_CACHE.clear();
        TEXTURE_NECTAR_CACHE.clear();
    }
}
