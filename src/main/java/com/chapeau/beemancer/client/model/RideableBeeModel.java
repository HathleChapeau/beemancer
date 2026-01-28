/**
 * ============================================================
 * [RideableBeeModel.java]
 * Description: Modèle simple pour RideableBee (placeholder cube)
 * ============================================================
 *
 * NOTE: Modèle temporaire en attendant un vrai modèle d'abeille.
 * Utilise un cube simple pour le debug et les tests.
 *
 * UTILISÉ PAR:
 * - RideableBeeRenderer.java: Rendu de l'entité
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.model;

import com.chapeau.beemancer.common.entity.mount.RideableBeeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Modèle simple cube pour RideableBee.
 * Placeholder en attendant un vrai modèle animé.
 */
public class RideableBeeModel extends HierarchicalModel<RideableBeeEntity> {

    private final ModelPart root;
    private final ModelPart body;

    public RideableBeeModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    /**
     * Crée la définition du layer pour ce modèle.
     * À appeler lors de l'enregistrement des layers.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // Corps principal - cube simple représentant une abeille
        // Taille: 14x10x14 (similaire à une abeille vanilla mais plus grand)
        partDefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -7.0F, 14.0F, 10.0F, 14.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(RideableBeeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Pas d'animation pour le moment
        // On peut ajouter une légère oscillation plus tard
    }
}
