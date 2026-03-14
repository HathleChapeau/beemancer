/**
 * ============================================================
 * [SaddlePartModelC.java]
 * Description: Selle HoverBee - variante C (electrodes + cube central)
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
 * Selle variante C : selle + dossier + 2 electrodes + cube central entre les electrodes.
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


        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -0.0F, -2.5F, 6.0F, 0.5F, 5.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("backrest",
                CubeListBuilder.create()
                        .texOffs(1, 6)
                        .addBox(-2.5F, -0.0F, -0.5F, 5.0F, 0.5F, 0.5F),
                PartPose.offset(0.0F, -0.5F, 1.75F));


        root.addOrReplaceChild("electrode_left",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1F, -1.0F, -1F, 2.0F, 2.0F, 2F),
                PartPose.offset(-2.5F, -0.5F, 2.75F));


        root.addOrReplaceChild("electrode_right",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .mirror()
                        .addBox(-1F, -1.0F, -1F, 2.0F, 2.0F, 2F),
                PartPose.offset(2.5F, -0.5F, 2.75F));

        // Cube central: 2x2x2, positionne au-dessus de la selle
        root.addOrReplaceChild("center_cube",
                CubeListBuilder.create()
                        .texOffs(0, 14)
                        .addBox(-1.5F, -0.9F, -0.9F, 3F, 1.8F, 1.8F),
                PartPose.offset(0.0F, -0.5F, 2.75F));

        return LayerDefinition.create(mesh, 32, 32);
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
