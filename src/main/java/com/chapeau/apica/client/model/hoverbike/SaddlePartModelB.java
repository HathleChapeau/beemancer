/**
 * ============================================================
 * [SaddlePartModelB.java]
 * Description: Selle HoverBee - variante B (2 cubes + connecteur avec arc electrique)
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
 * - HoverbikePartVariants.java: Enregistrement variante
 * - ClientSetup.java: Enregistrement du layer
 * - HoverbikePartLayer.java: Spawn particules lightning
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
 * Selle variante B : 2 cubes lateraux relies par un connecteur avec arc electrique.
 * Les particules lightning sont gerees par HoverbikePartLayer.
 */
public class SaddlePartModelB extends HoverbikePartModel {

    public static final ModelLayerLocation LAYER_LOCATION = createLayerLocation("saddle_b");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_saddle_b.png");

    /** Texture placeholder rose pour le connecteur (cutout) */
    public static final ResourceLocation CONNECTOR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/hoverbee/hoverbee_connector_placeholder.png");

    /** Position des electrodes pour le lightning (coordonnees locales) */
    public static final Vec3 LEFT_ELECTRODE = new Vec3(-2.5F, -0.5F, 2.75F);
    public static final Vec3 RIGHT_ELECTRODE = new Vec3(2.5F, -0.5F, 2.75F);

    private final ModelPart connector;

    public SaddlePartModelB(ModelPart root) {
        super(root);
        this.connector = root.getChild("connector");
    }

    /** Retourne le ModelPart du connecteur pour rendu separe. */
    public ModelPart getConnector() {
        return connector;
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("seat",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -0.0F, -2.5F, 6.0F, 0.5F, 5.0F),
                PartPose.ZERO);

        root.addOrReplaceChild("backrest",
                CubeListBuilder.create()
                        .texOffs(1, 6)
                        .addBox(-2.5F, -0.0F, -0.5F, 5.0F, 0.5F, 0.5F),
                PartPose.offset(0.0F, -0.5F, 1.75F));

        root.addOrReplaceChild("electrode_left",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1F, -1.0F, -1F, 2.0F, 2.0F, 2F),
                PartPose.offset(-2.5F, -0.5F, 2.75F));


        root.addOrReplaceChild("electrode_right",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .mirror()
                        .addBox(-1F, -1.0F, -1F, 2.0F, 2.0F, 2F),
                PartPose.offset(2.5F, -0.5F, 2.75F));

        // 4 tiges 0.25x0.25 reliant les 4 coins des electrodes
        // Longueur X = 2.15 (distance entre les faces internes des electrodes)
        // Coins relatifs au connector offset: Y = ±0.875 (coins electrodes), Z = ±0.95
        root.addOrReplaceChild("connector",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        // Tige haut avant
                        .addBox(-1.5F, 0.875F, -1.125F, 3F, 0.25F, 0.25F)
                        // Tige haut arriere
                        .addBox(-1.5F, 0.875F, 0.875F, 3F, 0.25F, 0.25F)
                        // Tige bas avant
                        .addBox(-1.5F, -1.125F, -1.125F, 3F, 0.25F, 0.25F)
                        // Tige bas arriere
                        .addBox(-1.5F, -1.125F, 0.875F, 3F, 0.25F, 0.25F),
                PartPose.offset(0.0F, -0.5F, 2.75F));

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
