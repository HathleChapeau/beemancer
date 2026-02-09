/**
 * ============================================================
 * [ChassisPartModelC.java]
 * Description: Variante C du chassis — plaques blindees
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
 * Chassis variante C : plaques blindees avec protection laterale et dessus.
 * Aspect lourd, tank-like.
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

        // Plaque laterale gauche : 3x10x30
        root.addOrReplaceChild("plate_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, 0.0F, -15.0F, 3.0F, 10.0F, 30.0F),
                PartPose.offset(-9.0F, 7.0F, 0.0F));

        // Plaque laterale droite
        root.addOrReplaceChild("plate_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, 0.0F, -15.0F, 3.0F, 10.0F, 30.0F),
                PartPose.offset(9.0F, 7.0F, 0.0F));

        // Plaque superieure : 18x2x30
        root.addOrReplaceChild("top_plate",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-9.0F, 0.0F, -15.0F, 18.0F, 2.0F, 30.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
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
        return new Vec3(0, 1, 1);
    }
}
