/**
 * ============================================================
 * [ControlPartModelB.java]
 * Description: Modele de controle HoverBee - variante B (2 cubes + connecteur + lightning)
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
 * - HoverbikePartLayer.java: Rendu avec flip + lightning arcs
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
 * Controle variante B : 2 cubes 1.15x2x1 separes de 2, relies par connecteur 2x2x1.
 * Arc electrique entre les deux cubes (gere par HoverbikePartLayer).
 */
public class ControlPartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("control_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_control_b.png");

    private HoverbikePart partType = HoverbikePart.CONTROL_LEFT;

    public ControlPartModelB(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cube avant: 1x2x1.15, centre a Z=-1.575 (separation de 2 avec l'arriere)
        root.addOrReplaceChild("cube_front",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-0.5F, -1.0F, -0.575F, 1.0F, 2.0F, 1.15F),
                PartPose.offset(0F, 0F, -1.575F));

        // Cube arriere: flip du cube avant
        root.addOrReplaceChild("cube_back",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .mirror()
                        .addBox(-0.5F, -1.0F, -0.575F, 1.0F, 2.0F, 1.15F),
                PartPose.offset(0F, 0F, 1.575F));

        // Connecteur: 1x2x2 (pont entre les deux cubes)
        root.addOrReplaceChild("connector",
                CubeListBuilder.create()
                        .texOffs(0, 4)
                        .addBox(-0.5F, -1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
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
        return new Vec3(-0.5, 0, 0);
    }
}
