/**
 * ============================================================
 * [ChassisPartModel.java]
 * Description: Selle simple posee sur le dos de l'abeille
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | CHASSIS                        |
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
 * Selle simple : assise plate sur le dos de l'abeille avec un petit dossier.
 * Se place sur le thorax (haut du body, Y=15).
 */
public class ChassisPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("chassis");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");

    public ChassisPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Assise de la selle : 5x1x6, posee sur le dos
        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, -0.5F, -3.0F, 5.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 14.5F, 1.0F));

        // Dossier arriere : 5x3x1
        root.addOrReplaceChild("backrest",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-2.5F, -3.0F, 0.0F, 5.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 14.5F, 4.0F));

        // Rebord gauche : 1x2x6
        root.addOrReplaceChild("side_left",
                CubeListBuilder.create()
                        .texOffs(0, 11)
                        .addBox(-0.5F, -1.5F, -3.0F, 1.0F, 2.0F, 6.0F),
                PartPose.offset(-3.0F, 14.5F, 1.0F));

        // Rebord droit : 1x2x6
        root.addOrReplaceChild("side_right",
                CubeListBuilder.create()
                        .texOffs(0, 11)
                        .addBox(-0.5F, -1.5F, -3.0F, 1.0F, 2.0F, 6.0F),
                PartPose.offset(3.0F, 14.5F, 1.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.CHASSIS;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, -1, 0);
    }
}
