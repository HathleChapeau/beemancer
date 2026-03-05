/**
 * ============================================================
 * [CoeurPartModel.java]
 * Description: Petit chapeau cylindrique pose sur la tete de l'abeille
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | COEUR                          |
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
 * Petit chapeau cylindrique (top hat) pose sur la tete de l'abeille.
 * Se place au-dessus des antennes, legerement en avant.
 */
public class CoeurPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("coeur");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/black_wool.png");

    public CoeurPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Bord du chapeau : 6x1x6
        root.addOrReplaceChild("brim",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 14.5F, -2.0F));

        // Cylindre du chapeau : 4x4x4
        root.addOrReplaceChild("crown",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 14.5F, -2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.COEUR;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, -1, -1);
    }
}
