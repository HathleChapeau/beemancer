/**
 * ============================================================
 * [RadiateurPartModel.java]
 * Description: Modele de la partie Radiateur — ailettes de refroidissement du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | RADIATEUR                      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Instanciation et rendu
 * - ClientSetup.java: Enregistrement du layer
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.model.hoverbike;

import com.chapeau.beemancer.common.entity.mount.HoverbikePart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Radiateur : ailettes de refroidissement sur les cotes du hoverbike.
 * Deux panneaux plats sur les flancs gauche et droit,
 * representant les grilles de dissipation thermique.
 */
public class RadiateurPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("radiateur");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/copper_block.png");

    public RadiateurPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Panneau gauche : 1x8x20, sur le flanc gauche
        root.addOrReplaceChild("panel_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -10.0F, 1.0F, 8.0F, 20.0F),
                PartPose.offset(-10.0F, 12.0F, -2.0F));

        // Panneau droit : miroir du gauche
        root.addOrReplaceChild("panel_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -10.0F, 1.0F, 8.0F, 20.0F),
                PartPose.offset(10.0F, 12.0F, -2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.RADIATEUR;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }
}
