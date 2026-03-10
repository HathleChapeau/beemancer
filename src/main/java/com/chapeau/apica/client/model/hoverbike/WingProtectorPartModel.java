/**
 * ============================================================
 * [WingProtectorPartModel.java]
 * Description: Protection d'aile HoverBee - variante A (centree a l'origine)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | WING_PROTECTOR                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu
 * - ClientSetup.java: Enregistrement du layer
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
 * Protection d'aile variante A : deux plaques protectrices (une par aile).
 * Les offsets relatifs (-1.5 et +1.5 en X) servent de pivot pour la rotation
 * synchronisee avec les ailes. Le positionnement Y/Z est gere par
 * HoverbikePartLayer via getPartPosition() selon le body type.
 */
public class WingProtectorPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("wing_protector");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_wing_protector_a.png");

    public WingProtectorPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Protection aile droite: 6x1x5, offset X=-1.5 pour pivot de rotation
        root.addOrReplaceChild("protector_right",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6.0F, -0.5F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(-1.5F, 0.0F, 0.0F));

        // Protection aile gauche: 6x1x5, offset X=+1.5 pour pivot de rotation
        root.addOrReplaceChild("protector_left",
                CubeListBuilder.create()
                        .texOffs(0, 6)
                        .addBox(0.0F, -0.5F, -2.5F, 6.0F, 1.0F, 5.0F),
                PartPose.offset(1.5F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.WING_PROTECTOR;
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
