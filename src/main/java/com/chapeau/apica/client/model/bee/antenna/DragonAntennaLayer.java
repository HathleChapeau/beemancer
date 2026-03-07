/**
 * ============================================================
 * [DragonAntennaLayer.java]
 * Description: LayerDefinition des antennes DRAGON (cornes balayees vers l'arriere)
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
 * Cornes de dragon: base 1x4x1 montant droit + pointe 1x2x1 balayee vers l'arriere (+Z).
 * Texture 32x32.
 */
public final class DragonAntennaLayer {

    private DragonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: base goes up, tip sweeps back
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)     // Base (1x4x1) straight up
                        .texOffs(0, 5).addBox(-1.0F, -6.0F, 0.5F, 1.0F, 2.0F, 1.0F),      // Tip (1x2x1) swept back
                PartPose.ZERO);

        // Right horn: mirrored
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create()
                        .texOffs(8, 0).addBox(0.0F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                        .texOffs(8, 5).addBox(0.0F, -6.0F, 0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
