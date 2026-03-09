/**
 * ============================================================
 * [ControlLeftPartModelC.java]
 * Description: Variante C — cube de controle gauche, texture differente
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
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Cube de controle gauche variante C : meme geometrie que A, texture differente.
 */
public class ControlLeftPartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("control_left_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_control_left_c.png");

    public ControlLeftPartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        return ControlLeftPartModel.createLayerDefinition();
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
