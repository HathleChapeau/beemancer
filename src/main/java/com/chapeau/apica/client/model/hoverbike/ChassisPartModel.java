/**
 * ============================================================
 * [ChassisPartModel.java]
 * Description: Modele de la partie Chassis — cadre structural du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | CHASSIS                        |
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
 * Chassis : cadre structural du hoverbike.
 * 2 rails lateraux le long de la moto + 1 plaque inferieure.
 * Enveloppe le modele de base pour donner la structure.
 */
public class ChassisPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("chassis");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    public ChassisPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Rail lateral gauche : 2x4x32, longe toute la moto
        root.addOrReplaceChild("rail_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, -16.0F, 2.0F, 4.0F, 32.0F),
                PartPose.offset(-9.0F, 10.0F, 0.0F));

        // Rail lateral droit : miroir du gauche
        root.addOrReplaceChild("rail_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, 0.0F, -16.0F, 2.0F, 4.0F, 32.0F),
                PartPose.offset(9.0F, 10.0F, 0.0F));

        // Plaque inferieure : relie avant et arriere par dessous
        root.addOrReplaceChild("bottom_plate",
                CubeListBuilder.create()
                        .texOffs(0, 36)
                        .addBox(-8.0F, 0.0F, -16.0F, 16.0F, 2.0F, 32.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.CHASSIS;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(0, 1, 1);
    }
}
