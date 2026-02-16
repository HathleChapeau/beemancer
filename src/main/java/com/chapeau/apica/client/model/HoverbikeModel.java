/**
 * ============================================================
 * [HoverbikeModel.java]
 * Description: Modele test du Hoverbike — deux cubes 16x16x16 avant/arriere
 * ============================================================
 *
 * UTILISÉ PAR:
 * - HoverbikeRenderer.java: Rendu de l'entite
 * - ClientSetup.java: Enregistrement du layer
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Modele de test : deux cubes de 1 bloc (16x16x16 pixels) places bout a bout.
 * Le joueur est assis sur le cube arriere.
 * Cube arriere : centre sous le joueur.
 * Cube avant : directement devant.
 */
public class HoverbikeModel extends HierarchicalModel<HoverbikeEntity> {

    private final ModelPart root;

    public HoverbikeModel(ModelPart root) {
        this.root = root;
    }

    /**
     * Cree la definition du layer.
     * Texture 128x64 pour accueillir les deux cubes.
     */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        // Cube arriere (sous le joueur) — centre en X/Z, base au sol
        // from(-8, 0, -8) to (8, 16, 8) = 16x16x16
        partDefinition.addOrReplaceChild("rear",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-8.0F, 0.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 8.0F, 8.0F));

        // Cube avant — devant le cube arriere
        // from(-8, 0, -24) to (8, 16, -8) = 16x16x16
        partDefinition.addOrReplaceChild("front",
                CubeListBuilder.create()
                        .texOffs(64, 0)
                        .addBox(-8.0F, 0.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 8.0F, -8.0F));

        return LayerDefinition.create(meshDefinition, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Pas d'animation pour le modele de test
    }
}
