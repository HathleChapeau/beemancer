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
 * Selle variante C : structure avec cubes inclines a 45° et effet de ring.
 * La ring est geree par HoverbikePartLayer.
 */
public class SaddlePartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("saddle_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_saddle_c.png");

    /** Position du centre de la ring pour l'effet de particules (coordonnees locales) */
    public static final Vec3 RING_CENTER = new Vec3(0, -0.5, 2.75);

    private static final float ANGLE_45 = (float)(Math.PI / 4);

    public SaddlePartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Assise: 6x1x5, centree a l'origine
        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -0.5F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.ZERO);

        // Dossier: 5x1x1, derriere le siege et legèrement plus haut
        root.addOrReplaceChild("backrest",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-2.5F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -0.5F, 1.75F));

        // Cube principal 3x3x1, tourne a 45° sur l'axe X
        root.addOrReplaceChild("main_cube",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0F, -0.5F, 2.75F, ANGLE_45, 0F, 0F));

        // Cube secondaire 2x2x1 (non tourne, devant le cube principal)
        root.addOrReplaceChild("secondary_cube",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0F, -0.5F, 3.75F));

        // Barre horizontale 3.5x1x0.5 tournee a 45° sur X (meme niveau que main_cube)
        root.addOrReplaceChild("bar_horizontal",
                CubeListBuilder.create()
                        .texOffs(0, 15)
                        .addBox(-1.75F, -0.5F, -0.25F, 3.5F, 1.0F, 0.5F),
                PartPose.offsetAndRotation(0F, -0.5F, 2.75F, ANGLE_45, 0F, 0F));

        // Barre verticale 1x3.5x0.5 tournee a 45° sur X (meme niveau que main_cube)
        root.addOrReplaceChild("bar_vertical",
                CubeListBuilder.create()
                        .texOffs(8, 15)
                        .addBox(-0.5F, -1.75F, -0.25F, 1.0F, 3.5F, 0.5F),
                PartPose.offsetAndRotation(0F, -0.5F, 2.75F, ANGLE_45, 0F, 0F));

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
