/**
 * ============================================================
 * [PropulseurPartModelB.java]
 * Description: Variante B du propulseur — 4 micro-tuyeres
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
 * Propulseur variante B : quatre micro-tuyeres aux coins.
 * Aspect agile, distribue la poussee sur 4 points.
 */
public class PropulseurPartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("propulseur_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/redstone_block.png");

    public PropulseurPartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Tuyere haut-gauche : 2x2x4
        root.addOrReplaceChild("nozzle_tl",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(-5.0F, 10.0F, 16.0F));

        // Tuyere haut-droite
        root.addOrReplaceChild("nozzle_tr",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(5.0F, 10.0F, 16.0F));

        // Tuyere bas-gauche
        root.addOrReplaceChild("nozzle_bl",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(-5.0F, 14.0F, 16.0F));

        // Tuyere bas-droite
        root.addOrReplaceChild("nozzle_br",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(5.0F, 14.0F, 16.0F));

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

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, 0, 1);
    }
}
