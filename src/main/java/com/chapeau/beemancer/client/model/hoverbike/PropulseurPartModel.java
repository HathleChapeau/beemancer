/**
 * ============================================================
 * [PropulseurPartModel.java]
 * Description: Modele de la partie Propulseur — tuyeres d'echappement du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | PROPULSEUR                     |
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
 * Propulseur : tuyeres d'echappement a l'arriere du hoverbike.
 * Deux blocs exhaust places symetriquement derriere le cube rear,
 * representant les moteurs de propulsion.
 */
public class PropulseurPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("propulseur");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/magma.png");

    public PropulseurPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Exhaust gauche : 4x4x6, derriere le cube rear
        root.addOrReplaceChild("exhaust_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(-4.0F, 12.0F, 16.0F));

        // Exhaust droit : miroir du gauche
        root.addOrReplaceChild("exhaust_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, 0.0F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(4.0F, 12.0F, 16.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.PROPULSEUR;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }
}
