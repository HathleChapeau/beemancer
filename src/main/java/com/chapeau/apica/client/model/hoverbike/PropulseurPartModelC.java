/**
 * ============================================================
 * [PropulseurPartModelC.java]
 * Description: Variante C — banniere decorative a l'arriere
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
 * Banniere : petit fanion fixe sur un mat derriere l'abdomen.
 * Aspect heraldique/chevaleresque.
 */
public class PropulseurPartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("propulseur_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/yellow_wool.png");

    public PropulseurPartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Mat : 1x8x1
        root.addOrReplaceChild("pole",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -8.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offset(0.0F, 17.0F, 6.5F));

        // Drapeau : 0x5x4, plan plat
        root.addOrReplaceChild("flag",
                CubeListBuilder.create()
                        .texOffs(4, 0)
                        .addBox(0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 4.0F),
                PartPose.offset(0.5F, 11.0F, 6.5F));

        // Pointe du mat : 1x1x1
        root.addOrReplaceChild("tip",
                CubeListBuilder.create()
                        .texOffs(0, 9)
                        .addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 9.0F, 6.5F));

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
