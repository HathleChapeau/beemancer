/**
 * ============================================================
 * [ControlPartModel.java]
 * Description: Modele de controle unifie HoverBee - variante A
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Classe parent        | Heritage modele partie         |
 * | HoverbikePart       | Enum type            | CONTROL_LEFT / CONTROL_RIGHT   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartVariants.java: Enregistrement variante (gauche + droite)
 * - HoverbikePartLayer.java: Rendu avec flip horizontal pour droite
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
 * Modele de controle unifie : cube 1x3x3 centre a l'origine.
 * Utilise pour les deux cotes (gauche et droite).
 * Le flip horizontal est applique par HoverbikePartLayer pour le cote droit.
 */
public class ControlPartModel extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("control");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_control_a.png");

    /** Le type de partie est defini dynamiquement lors du rendu. */
    private HoverbikePart partType = HoverbikePart.CONTROL_LEFT;

    public ControlPartModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cube de controle: 1x3x3, centre a l'origine
        root.addOrReplaceChild("control",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.5F, -1.5F, 1.0F, 3.0F, 3.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public HoverbikePart getPartType() {
        return partType;
    }

    public void setPartType(HoverbikePart type) {
        this.partType = type;
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return TEXTURE;
    }

    @Override
    public Vec3 getEditModeOffset() {
        // Offset vers l'exterieur (sera inverse pour le cote droit)
        return new Vec3(-0.5, 0, 0);
    }
}
