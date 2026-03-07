/**
 * ============================================================
 * [DemonAntennaLayer.java]
 * Description: LayerDefinition des antennes DEMON (cornes de belier/ram enroulees)
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
 * Cornes de demon style belier/ram: partent sur les cotes, s'enroulent
 * vers l'arriere puis vers le bas en formant un C.
 * Base 2x2x2 (30° out) → Mid 2x2x1 (45° back) → Tip 1x2x1 (40° more back+down).
 * Texture 32x32.
 */
public final class DemonAntennaLayer {

    private static final float DEG30 = (float) Math.toRadians(30);
    private static final float DEG40 = (float) Math.toRadians(40);
    private static final float DEG45 = (float) Math.toRadians(45);

    private DemonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: outward → backward → downward (ram curl)
        PartDefinition leftAntenna = bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition leftBase = leftAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, DEG30));
        PartDefinition leftMid = leftBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 4)
                        .addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, DEG45, 0, 0));
        leftMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 7)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, DEG40, 0, 0));

        // Right horn: mirrored Z rotation
        PartDefinition rightAntenna = bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition rightBase = rightAntenna.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0, 0, -DEG30));
        PartDefinition rightMid = rightBase.addOrReplaceChild("mid",
                CubeListBuilder.create().texOffs(0, 4)
                        .addBox(-1.0F, -2.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, DEG45, 0, 0));
        rightMid.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(0, 7)
                        .addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, DEG40, 0, 0));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
