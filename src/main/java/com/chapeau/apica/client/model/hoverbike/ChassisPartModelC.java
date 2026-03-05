/**
 * ============================================================
 * [ChassisPartModelC.java]
 * Description: Variante C — harnais complet avec sangles autour du corps
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
 * Harnais complet : sangles qui entourent le thorax de l'abeille
 * avec une assise integree. Aspect utilitaire et securise.
 */
public class ChassisPartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("chassis_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/netherite_block.png");

    public ChassisPartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Assise : 5x1x6
        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, -0.5F, -3.0F, 5.0F, 1.0F, 6.0F),
                PartPose.offset(0.0F, 14.5F, 1.0F));

        // Sangle dorsale (haut) : 8x1x1
        root.addOrReplaceChild("strap_top",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 14.5F, -1.0F));

        // Sangle ventrale gauche : 1x7x1
        root.addOrReplaceChild("strap_left",
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(-4.0F, 14.5F, -1.0F));

        // Sangle ventrale droite : 1x7x1
        root.addOrReplaceChild("strap_right",
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offset(4.0F, 14.5F, -1.0F));

        // Sangle ventrale (bas) : 8x1x1
        root.addOrReplaceChild("strap_bottom",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-4.0F, -0.5F, -0.5F, 8.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 21.5F, -1.0F));

        // Sangle longitudinale avant : 1x1x4
        root.addOrReplaceChild("strap_front",
                CubeListBuilder.create()
                        .texOffs(4, 9)
                        .addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 14.5F, -3.0F));

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
