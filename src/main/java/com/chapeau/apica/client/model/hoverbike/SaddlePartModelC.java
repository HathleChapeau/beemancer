/**
 * ============================================================
 * [SaddlePartModelC.java]
 * Description: Selle HoverBee - variante C (2 cubes lateraux relies par une barre)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | SADDLE                         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartVariants.java: Enregistrement variante
 * - ClientSetup.java: Enregistrement du layer
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.hoverbike;

import com.chapeau.apica.Apica;
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
 * Selle variante C : 2 cubes 1x2.25x2.25 aux extremites relies par une barre centrale.
 */
public class SaddlePartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("saddle_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_saddle_c.png");

    public SaddlePartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cube gauche: 1x2.25x2.25, centre a -3 sur X
        root.addOrReplaceChild("cube_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.125F, -1.125F, 1.0F, 2.25F, 2.25F),
                PartPose.offset(-3.0F, 0F, 0F));

        // Cube droit: 1x2.25x2.25, centre a +3 sur X
        root.addOrReplaceChild("cube_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.125F, -1.125F, 1.0F, 2.25F, 2.25F),
                PartPose.offset(3.0F, 0F, 0F));

        // Barre de liaison: relie les deux cubes (5 de large, 2 de haut, 2 de profond)
        root.addOrReplaceChild("connector",
                CubeListBuilder.create()
                        .texOffs(0, 5)
                        .addBox(-2.5F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F),
                PartPose.offset(0F, 0F, 0F));

        return LayerDefinition.create(mesh, 16, 8);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.SADDLE;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, -0.35, 0);
    }
}
