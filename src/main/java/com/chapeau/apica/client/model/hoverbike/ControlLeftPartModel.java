/**
 * ============================================================
 * [ControlLeftPartModel.java]
 * Description: Cube de controle gauche HoverBee (centre a l'origine)
 * ============================================================
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
 * Cube 1x3x3 de controle gauche, centre a l'origine.
 * Le positionnement final est gere par HoverbikePartLayer selon le body type.
 * Variante A — texture de base.
 */
public class ControlLeftPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("control_left");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_control_left_a.png");

    public ControlLeftPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cube de controle gauche: 1x3x3, centre a l'origine
        root.addOrReplaceChild("control_left",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public HoverbikePart getPartType() {
        return HoverbikePart.CONTROL_LEFT;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        return new Vec3(-0.5, 0, 0);
    }
}
