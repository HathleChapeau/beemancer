/**
 * ============================================================
 * [PropulseurPartModelB.java]
 * Description: Variante B — sacoches laterales arriere
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | PROPULSEUR                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartVariants.java: Enregistrement variante
 * - ClientSetup.java: Enregistrement du layer
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.hoverbike;

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
 * Sacoches : deux petits sacs accroches de chaque cote de l'abdomen arriere.
 * Pour transporter du butin. Aspect utilitaire.
 */
public class PropulseurPartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("propulseur_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");

    public PropulseurPartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Sacoche gauche : 2x4x4
        root.addOrReplaceChild("bag_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -2.0F, 2.0F, 4.0F, 4.0F),
                PartPose.offset(-4.5F, 18.0F, 3.0F));

        // Rabat sacoche gauche : 2x1x4
        root.addOrReplaceChild("flap_left",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(-4.5F, 17.0F, 3.0F));

        // Sacoche droite : 2x4x4
        root.addOrReplaceChild("bag_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -2.0F, 2.0F, 4.0F, 4.0F),
                PartPose.offset(4.5F, 18.0F, 3.0F));

        // Rabat sacoche droite : 2x1x4
        root.addOrReplaceChild("flap_right",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.0F, -0.5F, -2.0F, 2.0F, 1.0F, 4.0F),
                PartPose.offset(4.5F, 17.0F, 3.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.PROPULSEUR;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, 0, 1);
    }
}
