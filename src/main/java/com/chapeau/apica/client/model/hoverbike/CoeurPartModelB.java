/**
 * ============================================================
 * [CoeurPartModelB.java]
 * Description: Variante B — couronne doree sur la tete de l'abeille
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
 * - HoverbikePartVariants.java: Enregistrement variante
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
 * Couronne doree : anneau avec 4 pointes, posee sur la tete.
 * Aspect royal.
 */
public class CoeurPartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("coeur_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/gold_block.png");

    public CoeurPartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Anneau avant : 6x2x1
        root.addOrReplaceChild("band_front",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -2.0F, -0.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, -7.5F));

        // Anneau arriere : 6x2x1
        root.addOrReplaceChild("band_back",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -2.0F, -0.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, -2.5F));

        // Anneau gauche : 1x2x5
        root.addOrReplaceChild("band_left",
                CubeListBuilder.create()
                        .texOffs(0, 3)
                        .addBox(-0.5F, -2.0F, -2.5F, 1.0F, 2.0F, 5.0F),
                PartPose.offset(-3.0F, 14.0F, -5.0F));

        // Anneau droit : 1x2x5
        root.addOrReplaceChild("band_right",
                CubeListBuilder.create()
                        .texOffs(0, 3)
                        .addBox(-0.5F, -2.0F, -2.5F, 1.0F, 2.0F, 5.0F),
                PartPose.offset(3.0F, 14.0F, -5.0F));

        // Pointe avant : 1x2x1
        root.addOrReplaceChild("spike_front",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, -7.5F));

        // Pointe arriere : 1x2x1
        root.addOrReplaceChild("spike_back",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, -2.5F));

        // Pointe gauche : 1x2x1
        root.addOrReplaceChild("spike_left",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(-3.0F, 14.0F, -5.0F));

        // Pointe droite : 1x2x1
        root.addOrReplaceChild("spike_right",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(3.0F, 14.0F, -5.0F));

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
