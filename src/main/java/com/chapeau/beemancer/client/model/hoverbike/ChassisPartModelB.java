/**
 * ============================================================
 * [ChassisPartModelB.java]
 * Description: Variante B du chassis — cadre large et epais
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
 * Chassis variante B : cadre lourd avec rails larges et barre transversale.
 * Donne un aspect plus massif et industriel.
 */
public class ChassisPartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("chassis_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/gold_block.png");

    public ChassisPartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Rail large gauche : 4x6x32
        root.addOrReplaceChild("rail_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, -16.0F, 4.0F, 6.0F, 32.0F),
                PartPose.offset(-8.0F, 8.0F, 0.0F));

        // Rail large droit
        root.addOrReplaceChild("rail_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, -16.0F, 4.0F, 6.0F, 32.0F),
                PartPose.offset(8.0F, 8.0F, 0.0F));

        // Barre transversale avant : 16x3x3
        root.addOrReplaceChild("cross_front",
                CubeListBuilder.create()
                        .texOffs(0, 38)
                        .addBox(-8.0F, 0.0F, -1.5F, 16.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, -10.0F));

        // Barre transversale arriere : 16x3x3
        root.addOrReplaceChild("cross_rear",
                CubeListBuilder.create()
                        .texOffs(0, 44)
                        .addBox(-8.0F, 0.0F, -1.5F, 16.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, 10.0F));

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
