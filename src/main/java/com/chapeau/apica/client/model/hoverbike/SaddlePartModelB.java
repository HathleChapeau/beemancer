/**
 * ============================================================
 * [SaddlePartModelB.java]
 * Description: Selle HoverBee - variante B (2 cubes + connecteur avec arc electrique)
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
 * Selle variante B : 2 cubes lateraux relies par un connecteur avec arc electrique.
 * Les particules lightning sont gerees par HoverbikePartLayer.
 */
public class SaddlePartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("saddle_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_saddle_b.png");

    /** Position des electrodes pour le lightning (coordonnees locales) */
    public static final Vec3 LEFT_ELECTRODE = new Vec3(2.75, -0.5, -1.575);
    public static final Vec3 RIGHT_ELECTRODE = new Vec3(2.75, -0.5, 1.575);

    public SaddlePartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Assise: 5x1x6, centree a l'origine
        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, -0.5F, -3.0F, 5.0F, 1.0F, 6.0F),
                PartPose.ZERO);

        // Dossier: 1x1x5, sur le cote et legèrement plus haut
        root.addOrReplaceChild("backrest",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 5.0F),
                PartPose.offset(1.75F, -0.5F, 0.0F));

        // Cube avant: 1x2x1.15, separe de 2 du cube arriere
        root.addOrReplaceChild("cube_left",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-0.5F, -1.0F, -0.575F, 1.0F, 2.0F, 1.15F),
                PartPose.offset(2.75F, -0.5F, -1.575F));

        // Cube arriere: flip du cube avant
        root.addOrReplaceChild("cube_right",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .mirror()
                        .addBox(-0.5F, -1.0F, -0.575F, 1.0F, 2.0F, 1.15F),
                PartPose.offset(2.75F, -0.5F, 1.575F));

        return LayerDefinition.create(mesh, 32, 16);
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
