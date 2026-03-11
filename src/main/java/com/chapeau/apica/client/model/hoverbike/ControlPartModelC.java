/**
 * ============================================================
 * [ControlPartModelC.java]
 * Description: Modele de controle HoverBee - variante C (cubes inclines + ring)
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
 * - HoverbikePartVariants.java: Enregistrement variante
 * - HoverbikePartLayer.java: Rendu avec flip + ring effect
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
 * Controle variante C : structure avec cubes inclines a 45° et effet de ring.
 * - Cube principal 3x3x1 tourne a 45° sur X
 * - Cube secondaire 2x2x1 devant (non tourne)
 * - Barres croisees 3.5x1x0.5 et 1x3.5x0.5 tournees a 45° sur X
 * La ring est geree par HoverbikePartLayer.
 */
public class ControlPartModelC extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("control_c");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_control_c.png");

    /** Position du centre de la ring pour l'effet (coordonnees locales) */
    public static final Vec3 RING_CENTER = Vec3.ZERO;

    private static final float ANGLE_45 = (float)(Math.PI / 4);

    private HoverbikePart partType = HoverbikePart.CONTROL_LEFT;

    public ControlPartModelC(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cube principal 3x3x1, tourne a 45° sur l'axe X
        root.addOrReplaceChild("main_cube",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(0F, 0F, 0F, ANGLE_45, 0F, 0F));

        // Cube secondaire 2x2x1 (non tourne, devant le cube principal)
        root.addOrReplaceChild("secondary_cube",
                CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0F, 0F, 1.0F));

        // Barre horizontale 3.5x1x0.5 tournee a 45° sur X
        root.addOrReplaceChild("bar_horizontal",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.75F, -0.5F, -0.25F, 3.5F, 1.0F, 0.5F),
                PartPose.offsetAndRotation(0F, 0F, 0F, ANGLE_45, 0F, 0F));

        // Barre verticale 1x3.5x0.5 tournee a 45° sur X
        root.addOrReplaceChild("bar_vertical",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-0.5F, -1.75F, -0.25F, 1.0F, 3.5F, 0.5F),
                PartPose.offsetAndRotation(0F, 0F, 0F, ANGLE_45, 0F, 0F));

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
        return new Vec3(-0.5, 0, 0);
    }
}
