/**
 * ============================================================
 * [SaddlePartModel.java]
 * Description: Selle HoverBee - variante A
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | SADDLE                         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu
 * - ClientSetup.java: Enregistrement du layer
 * - SaddlePartModelB.java: Reutilisation de la geometrie
 * - SaddlePartModelC.java: Reutilisation de la geometrie
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.hoverbike;

import com.chapeau.apica.Apica;
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
 * Selle variante A : assise plate sur le dos de l'abeille avec un petit dossier.
 * Geometrie partagee par les variantes B et C (seule la texture change).
 */
public class SaddlePartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("saddle");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_saddle_a.png");

    public SaddlePartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Assise: 6x1x5 on the bee's back
        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -0.5F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 14.5F, 4.25F));

        // Dossier: 5x1x1 behind seat, slightly higher
        root.addOrReplaceChild("backrest",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(-2.5F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 14.0F, 6.5F));

        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.SADDLE;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, -0.35, 0);
    }
}
