/**
 * ============================================================
 * [DragonAntennaLayer.java]
 * Description: LayerDefinition des antennes DRAGON (cornes courbees vers l'arriere, 3 segments)
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
 * Cornes de dragon: 3 segments empiles avec rotation cumulative vers l'arriere.
 * Base 2x3x2 (epaisse) → Mid 1x3x1 (25° arriere) → Tip 1x2x1 (20° arriere, effile).
 * Courbe totale ~45° vers l'arriere. Texture 32x32.
 */
public final class DragonAntennaLayer {

    private static final float DEG25 = (float) Math.toRadians(25);
    private static final float DEG20 = (float) Math.toRadians(20);

    private DragonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: base → mid (25° back) → tip (20° more back)
        PartDefinition leftAntenna = bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftBase = leftAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        PartDefinition leftMid = leftBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 5)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, DEG25, 0, 0));
        leftMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, DEG20, 0, 0));

        // Right horn: same geometry and rotation (dragon horns are symmetric)
        PartDefinition rightAntenna = bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightBase = rightAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        PartDefinition rightMid = rightBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 5)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, DEG25, 0, 0));
        rightMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 9)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 0.0F, DEG20, 0, 0));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
