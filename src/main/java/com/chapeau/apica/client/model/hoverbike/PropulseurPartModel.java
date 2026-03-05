/**
 * ============================================================
 * [PropulseurPartModel.java]
 * Description: Noeud decoratif attache a l'arriere de l'abeille (queue)
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
 * - HoverbikePartLayer.java: Instanciation et rendu
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
 * Noeud papillon decoratif : petit noeud attache a l'arriere du corps,
 * juste au-dessus du dard. Aspect mignon.
 */
public class PropulseurPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("propulseur");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/red_wool.png");

    public PropulseurPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Noeud central : 2x2x1
        root.addOrReplaceChild("knot",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 17.0F, 5.5F));

        // Boucle gauche : 3x3x1
        root.addOrReplaceChild("loop_left",
                CubeListBuilder.create()
                        .texOffs(0, 3)
                        .addBox(-3.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(-1.0F, 17.0F, 5.5F));

        // Boucle droite : 3x3x1
        root.addOrReplaceChild("loop_right",
                CubeListBuilder.create()
                        .texOffs(0, 3)
                        .addBox(0.0F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(1.0F, 17.0F, 5.5F));

        // Ruban gauche : 1x3x1
        root.addOrReplaceChild("ribbon_left",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-1.5F, 18.5F, 5.5F));

        // Ruban droit : 1x3x1
        root.addOrReplaceChild("ribbon_right",
                CubeListBuilder.create()
                        .texOffs(0, 7)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(1.5F, 18.5F, 5.5F));

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
