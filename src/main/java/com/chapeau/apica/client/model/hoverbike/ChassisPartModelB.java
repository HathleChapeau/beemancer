/**
 * ============================================================
 * [ChassisPartModelB.java]
 * Description: Variante B — selle royale avec pommeaux et assise large
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
 * Selle royale : assise plus large avec pommeaux avant et arriere.
 * Donne un aspect monte plus confortable et noble.
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

        // Assise large : 7x1x8
        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.5F, -0.5F, -4.0F, 7.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 14.5F, 1.0F));

        // Pommeau avant : 2x2x1
        root.addOrReplaceChild("pommel_front",
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 14.5F, -3.0F));

        // Pommeau arriere : 3x3x1
        root.addOrReplaceChild("pommel_rear",
                CubeListBuilder.create()
                        .texOffs(6, 9)
                        .addBox(-1.5F, -3.0F, 0.0F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 14.5F, 5.0F));

        // Decoration laterale gauche : 1x1x8
        root.addOrReplaceChild("trim_left",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(-0.5F, -1.0F, -4.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offset(-4.0F, 14.5F, 1.0F));

        // Decoration laterale droite : 1x1x8
        root.addOrReplaceChild("trim_right",
                CubeListBuilder.create()
                        .texOffs(0, 13)
                        .addBox(-0.5F, -1.0F, -4.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offset(4.0F, 14.5F, 1.0F));

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
