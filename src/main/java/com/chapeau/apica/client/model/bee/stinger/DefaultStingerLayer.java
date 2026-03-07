/**
 * ============================================================
 * [DefaultStingerLayer.java]
 * Description: LayerDefinition du dard DEFAULT (court, 0x1x2)
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
 * - ApicaBeeModel (createStingerLayerFor)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.bee.stinger;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class DefaultStingerLayer {

    private DefaultStingerLayer() {}

    /** Dard DEFAULT: court (0x1x2), texture 32x32. */
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("stinger",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
