/**
 * ============================================================
 * [DemonAntennaLayer.java]
 * Description: LayerDefinition des antennes DEMON (cornes devil classiques)
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
 * Cornes de demon classiques: montent en s'ecartant, pointe recourbee vers l'interieur/avant.
 * Base 2x3x2 (12° out) → Mid 1x3x1 (5° out, 12° forward) → Tip 1x2x1 (-8° inward, 18° forward).
 * L'arc d'abord vers l'exterieur puis la pointe revient vers l'interieur donne la silhouette devil.
 * Texture 32x32.
 */
public final class DemonAntennaLayer {

    private static final float DEG5  = (float) Math.toRadians(5);
    private static final float DEG8  = (float) Math.toRadians(8);
    private static final float DEG12 = (float) Math.toRadians(12);
    private static final float DEG18 = (float) Math.toRadians(18);

    private DemonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: up and outward, tip curves inward + forward
        PartDefinition leftAntenna = bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftBase = leftAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, DEG12));
        PartDefinition leftMid = leftBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 5)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG12, 0, DEG5));
        leftMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG18, 0, -DEG8));

        // Right horn: mirrored Z rotations
        PartDefinition rightAntenna = bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightBase = rightAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, -DEG12));
        PartDefinition rightMid = rightBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 5)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG12, 0, -DEG5));
        rightMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, -DEG18, 0, DEG8));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
