/**
 * ============================================================
 * [RadiateurPartModelC.java]
 * Description: Variante C — armure lourde avec plaques epaisses
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
 * Armure lourde : plaques epaisses couvrant les flancs et le dessous
 * du thorax. Maximum de protection, aspect tank.
 */
public class RadiateurPartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("radiateur_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/netherite_block.png");

    public RadiateurPartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Plaque gauche : 2x6x9
        root.addOrReplaceChild("armor_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -4.5F, 2.0F, 6.0F, 9.0F),
                PartPose.offset(-4.5F, 18.0F, 0.0F));

        // Plaque droite : 2x6x9
        root.addOrReplaceChild("armor_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -4.5F, 2.0F, 6.0F, 9.0F),
                PartPose.offset(4.5F, 18.0F, 0.0F));

        // Plaque ventrale : 7x1x8
        root.addOrReplaceChild("belly_plate",
                CubeListBuilder.create()
                        .texOffs(0, 15)
                        .addBox(-3.5F, 0.0F, -4.0F, 7.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

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
