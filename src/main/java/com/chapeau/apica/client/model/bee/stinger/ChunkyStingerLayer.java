/**
 * ============================================================
 * [ChunkyStingerLayer.java]
 * Description: LayerDefinition du dard CHUNKY (2x2x1 base + 1x1x2 pointe)
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

/**
 * Dard CHUNKY: base epaisse 2x2x1 suivie d'une pointe 1x1x2.
 * Layout Z: Base(0..1) → Pointe(1..3). Centre X/Y sur la base.
 * Texture 32x32.
 */
public final class ChunkyStingerLayer {

    private ChunkyStingerLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("stinger",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 2.0F, 1.0F)    // Base (2x2x1)
                        .texOffs(6, 0).addBox(-0.5F, -1.0F, 1.0F, 1.0F, 1.0F, 2.0F),    // Pointe (1x1x2)
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
