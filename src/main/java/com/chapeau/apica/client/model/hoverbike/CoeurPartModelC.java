/**
 * ============================================================
 * [CoeurPartModelC.java]
 * Description: Variante C — casque protecteur sur la tete de l'abeille
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | COEUR                          |
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
 * Casque protecteur : coque arrondie qui couvre le dessus de la tete.
 * Aspect guerrier/chevalier.
 */
public class CoeurPartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("coeur_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    public CoeurPartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Dome du casque : 8x3x7, couvre le haut de la tete
        root.addOrReplaceChild("dome",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -3.5F, 8.0F, 3.0F, 7.0F),
                PartPose.offset(0.0F, 14.0F, -5.0F));

        // Visiere : 6x1x1, devant la tete
        root.addOrReplaceChild("visor",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-3.0F, -1.0F, -0.5F, 6.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -8.5F));

        // Protection joue gauche : 1x3x5
        root.addOrReplaceChild("cheek_left",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-0.5F, 0.0F, -2.5F, 1.0F, 3.0F, 5.0F),
                PartPose.offset(-4.0F, 14.0F, -5.0F));

        // Protection joue droite : 1x3x5
        root.addOrReplaceChild("cheek_right",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-0.5F, 0.0F, -2.5F, 1.0F, 3.0F, 5.0F),
                PartPose.offset(4.0F, 14.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.COEUR;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, -1, -1);
    }
}
