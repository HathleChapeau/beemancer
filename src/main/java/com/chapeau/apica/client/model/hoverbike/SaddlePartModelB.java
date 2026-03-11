/**
 * ============================================================
 * [SaddlePartModelB.java]
 * Description: Selle HoverBee - variante B (2 electrodes avec arc electrique)
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
 * - HoverbikePartLayer.java: Spawn particules lightning
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
 * Selle variante B : 2 electrodes aux extremites avec arc electrique entre elles.
 * Les particules lightning sont gerees par HoverbikePartLayer.
 */
public class SaddlePartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("saddle_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_saddle_b.png");

    /** Position des electrodes pour le lightning (coordonnees locales) */
    public static final Vec3 LEFT_ELECTRODE = new Vec3(-3.0, 0, 0);
    public static final Vec3 RIGHT_ELECTRODE = new Vec3(3.0, 0, 0);

    public SaddlePartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Electrode gauche: 1x2x2, centree a -3 sur X (separation de 5 entre les deux)
        root.addOrReplaceChild("electrode_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(-3.0F, 0F, 0F));

        // Electrode droite: 1x2x2, centree a +3 sur X (separation de 5 entre les deux)
        root.addOrReplaceChild("electrode_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(3.0F, 0F, 0F));

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
