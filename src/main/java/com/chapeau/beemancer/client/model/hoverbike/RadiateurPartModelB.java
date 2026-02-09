/**
 * ============================================================
 * [RadiateurPartModelB.java]
 * Description: Variante B du radiateur — ailettes multiples
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
 * - HoverbikePartVariants.java: Enregistrement variante
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
import net.minecraft.world.phys.Vec3;

/**
 * Radiateur variante B : 6 ailettes fines (3 par cote).
 * Style grille d'aeration, haute surface de dissipation.
 */
public class RadiateurPartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("radiateur_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    public RadiateurPartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 3 ailettes gauche : 1x6x6, espacees sur Z
        root.addOrReplaceChild("fin_l1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(-10.0F, 13.0F, -8.0F));

        root.addOrReplaceChild("fin_l2",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(-10.0F, 13.0F, 0.0F));

        root.addOrReplaceChild("fin_l3",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(-10.0F, 13.0F, 8.0F));

        // 3 ailettes droite
        root.addOrReplaceChild("fin_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(10.0F, 13.0F, -8.0F));

        root.addOrReplaceChild("fin_r2",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(10.0F, 13.0F, 0.0F));

        root.addOrReplaceChild("fin_r3",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, 0.0F, -3.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(10.0F, 13.0F, 8.0F));

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
        return new Vec3(0, 0, -1);
    }
}
