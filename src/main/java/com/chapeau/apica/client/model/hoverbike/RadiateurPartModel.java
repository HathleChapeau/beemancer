/**
 * ============================================================
 * [RadiateurPartModel.java]
 * Description: Plastrons lateraux legers sur les flancs de l'abeille
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
package com.chapeau.apica.client.model.hoverbike;

import com.chapeau.apica.common.entity.mount.HoverbikePart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Plastrons legers : plaques fines sur chaque flanc du thorax.
 * Protection minimale, aspect discret.
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

        // Plastron gauche : 1x5x8
        root.addOrReplaceChild("plate_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -2.5F, -4.0F, 1.0F, 5.0F, 8.0F),
                PartPose.offset(-4.0F, 18.0F, 0.0F));

        // Plastron droit : 1x5x8
        root.addOrReplaceChild("plate_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -2.5F, -4.0F, 1.0F, 5.0F, 8.0F),
                PartPose.offset(4.0F, 18.0F, 0.0F));

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

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(1, 0, 0);
    }
}
