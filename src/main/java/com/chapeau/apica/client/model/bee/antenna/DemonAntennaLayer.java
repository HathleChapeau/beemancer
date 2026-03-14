/**
 * ============================================================
 * [DemonAntennaLayer.java]
 * Description: LayerDefinition des antennes DEMON (cornes du diable classiques)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBeeModel (createAntennaLayerFor)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.bee.antenna;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Cornes du diable classiques: montent droit vers le haut avec leger ecart,
 * pointe effilee legererement penchee vers l'avant.
 * Base 2x3x2 (15° out) → Tip 1x2x1 (15° forward).
 * Texture 32x32.
 */
public final class DemonAntennaLayer {

    private static final float DEG15 = (float) Math.toRadians(15);

    private DemonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: up + slightly outward, tip leans forward
        PartDefinition leftAntenna = bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftBase = leftAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(1, 0)
                        .addBox(-0.9F, -3.0F, -0.9F, 1.8F, 3.0F, 1.8F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, -DEG15));
        leftBase.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(1, 8)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG15, 0, 0));

        // Right horn: mirrored Z rotation
        PartDefinition rightAntenna = bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightBase = rightAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(1, 0)
                        .addBox(-0.9F, -3.0F, -0.9F, 1.8F, 3.0F, 1.8F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, DEG15));
        rightBase.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(1, 8)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.5F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG15, 0, 0));

        return LayerDefinition.create(mesh, 16, 16);
    }
}
