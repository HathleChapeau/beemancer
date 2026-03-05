/**
 * ============================================================
 * [RadiateurPartModelB.java]
 * Description: Variante B — epaulettes decoratives sur les flancs
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
 * Epaulettes : petites extensions rigides sur les flancs du thorax,
 * comme des epaulettes de chevalier. 3 ailettes par cote.
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

        // Ailette gauche avant : 2x3x1
        root.addOrReplaceChild("fin_l1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(-4.0F, 17.0F, -2.0F));

        // Ailette gauche milieu : 3x3x1
        root.addOrReplaceChild("fin_l2",
                CubeListBuilder.create()
                        .texOffs(6, 0)
                        .addBox(-3.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(-4.0F, 17.0F, 0.0F));

        // Ailette gauche arriere : 2x3x1
        root.addOrReplaceChild("fin_l3",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(-4.0F, 17.0F, 2.0F));

        // Ailette droite avant : 2x3x1
        root.addOrReplaceChild("fin_r1",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(0.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(4.0F, 17.0F, -2.0F));

        // Ailette droite milieu : 3x3x1
        root.addOrReplaceChild("fin_r2",
                CubeListBuilder.create()
                        .texOffs(6, 0)
                        .addBox(0.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(4.0F, 17.0F, 0.0F));

        // Ailette droite arriere : 2x3x1
        root.addOrReplaceChild("fin_r3",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(0.0F, -1.5F, -0.5F, 2.0F, 3.0F, 1.0F),
                PartPose.offset(4.0F, 17.0F, 2.0F));

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
