/**
 * ============================================================
 * [DemonAntennaLayer.java]
 * Description: LayerDefinition des antennes DEMON (cornes courbees vers l'exterieur, style belier)
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
 * Cornes de demon: 3 segments avec courbe vers l'exterieur puis pointe vers l'avant.
 * Base 2x3x2 (15° out) → Mid 1x3x1 (25° out + 10° back) → Tip 1x2x1 (10° out + 30° forward).
 * La pointe se recourbe vers l'avant style belier/demon. Texture 32x32.
 */
public final class DemonAntennaLayer {

    private static final float DEG10 = (float) Math.toRadians(10);
    private static final float DEG15 = (float) Math.toRadians(15);
    private static final float DEG25 = (float) Math.toRadians(25);
    private static final float DEG30 = (float) Math.toRadians(30);

    private DemonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: curves outward (-X), tip curls forward
        PartDefinition leftAntenna = bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftBase = leftAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, DEG15));
        PartDefinition leftMid = leftBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 5)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, DEG10, 0, DEG25));
        leftMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG30, 0, DEG10));

        // Right horn: mirrored (outward = -Z rotation)
        PartDefinition rightAntenna = bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightBase = rightAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, -DEG15));
        PartDefinition rightMid = rightBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 5)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, DEG10, 0, -DEG25));
        rightMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG30, 0, -DEG10));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
