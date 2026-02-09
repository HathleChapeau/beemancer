/**
 * ============================================================
 * [HoverbikePartModel.java]
 * Description: Classe abstraite parent pour tous les modeles de parties du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite source        | Donnees pour animation         |
 * | HoverbikePart       | Enum type partie     | Identification                 |
 * | Beemancer           | MOD_ID               | Construction ModelLayerLocation|
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ChassisPartModel, CoeurPartModel, PropulseurPartModel, RadiateurPartModel
 * - HoverbikePartLayer.java: Rendu des parties
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.model.hoverbike;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.mount.HoverbikePart;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

/**
 * Base abstraite pour les modeles de parties du Hoverbike.
 * Chaque partie concrete definit sa propre geometrie (createLayerDefinition),
 * sa texture, et son type. Le systeme est concu pour etre extensible :
 * ajouter une nouvelle partie = creer 1 nouvelle classe qui extends celle-ci.
 */
public abstract class HoverbikePartModel extends HierarchicalModel<HoverbikeEntity> {

    protected final ModelPart root;

    protected HoverbikePartModel(ModelPart root) {
        this.root = root;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /** Type de partie (CHASSIS, COEUR, etc.). */
    public abstract HoverbikePart getPartType();

    /** Texture utilisee pour rendre cette partie. */
    public abstract ResourceLocation getTextureLocation();

    @Override
    public void setupAnim(HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Par defaut, pas d'animation. Les parties concretes peuvent override.
    }

    /**
     * Fabrique un ModelLayerLocation pour une partie du hoverbike.
     * Convention: beemancer:hoverbike_[partName] / main
     */
    public static ModelLayerLocation createLayerLocation(String partName) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "hoverbike_" + partName),
                "main"
        );
    }
}
