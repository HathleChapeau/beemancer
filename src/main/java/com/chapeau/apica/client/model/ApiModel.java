/**
 * ============================================================
 * [ApiModel.java]
 * Description: Modele articule pour Api avec bras et jambes animables
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | ModelLayerLocation             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApiRenderer.java (rendu)
 * - ClientSetup.java (enregistrement layer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import com.chapeau.apica.Apica;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Modele articule d'Api.
 * Systeme de coordonnees: (0,0,0) = centre du bloc au sol (pixel 8,0,8 en coords absolues).
 * Toutes les coordonnees sont en pixels (1 bloc = 16 pixels).
 * Le corps est tilte de 45 degres sur l'axe X (comme le JSON original).
 */
public class ApiModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "api"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/machines/api.png");

    private static final float ANGLE_45 = (float) (Math.PI / 4);

    // Pivot de rotation en coordonnees modele (centre bloc au sol = 0,0,0)
    // JSON origin [8, 2.75, 8.5] -> model coords (0, 2.75, 0.5)
    private static final float PIVOT_X = 0f;
    private static final float PIVOT_Y = 2.75f;
    private static final float PIVOT_Z = 0.5f;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;

    public ApiModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
        this.body = root.getChild("body");
        this.armLeft = root.getChild("arm_left");
        this.armRight = root.getChild("arm_right");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
    }

    /**
     * Convertit des coordonnees absolues JSON (0-16) vers coordonnees modele centrees.
     * Block center (8, 0, 8) devient (0, 0, 0).
     */
    private static float toModelX(float jsonX) { return jsonX - 8f; }
    private static float toModelY(float jsonY) { return jsonY; }
    private static float toModelZ(float jsonZ) { return jsonZ - 8f; }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootDef = mesh.getRoot();

        // === BODY ===
        // Pivot at model coords (0, 2.75, 0.5)
        // Cubes are relative to this pivot
        rootDef.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Layer 1a: from [1, 3.5, 0] to [15, 6.5, 10]
                        // Model coords: from (-7, 3.5, -8) to (7, 6.5, 2)
                        // Relative to pivot (0, 2.75, 0.5): from (-7, 0.75, -8.5)
                        .texOffs(0, 1).addBox(-7f, 0.75f, -8.5f, 14f, 3f, 10f)
                        // Layer 1b: from [3, 3.5, -2] to [13, 6.5, 12]
                        // Model: from (-5, 3.5, -10) to (5, 6.5, 4)
                        // Relative to pivot: from (-5, 0.75, -10.5)
                        .texOffs(0, 0).addBox(-5f, 0.75f, -10.5f, 10f, 3f, 14f)
                        // Layer 2a: from [2, 1.5, 1] to [14, 3.5, 9]
                        // Model: from (-6, 1.5, -7) to (6, 3.5, 1)
                        // Relative to pivot: from (-6, -1.25, -7.5)
                        .texOffs(0, 1).addBox(-6f, -1.25f, -7.5f, 12f, 2f, 8f)
                        // Layer 2b: from [4, 1.5, -1] to [12, 3.5, 11]
                        // Model: from (-4, 1.5, -9) to (4, 3.5, 3)
                        // Relative to pivot: from (-4, -1.25, -9.5)
                        .texOffs(0, 0).addBox(-4f, -1.25f, -9.5f, 8f, 2f, 12f)
                        // Layer 3a: from [3, 0.5, 2] to [13, 1.5, 8]
                        // Model: from (-5, 0.5, -6) to (5, 1.5, 0)
                        // Relative to pivot: from (-5, -2.25, -6.5)
                        .texOffs(0, 2).addBox(-5f, -2.25f, -6.5f, 10f, 1f, 6f)
                        // Layer 3b: from [5, 0.5, 0] to [11, 1.5, 10]
                        // Model: from (-3, 0.5, -8) to (3, 1.5, 2)
                        // Relative to pivot: from (-3, -2.25, -8.5)
                        .texOffs(0, 1).addBox(-3f, -2.25f, -8.5f, 6f, 1f, 10f)
                        // Layer 4a: from [4, -0.5, 3] to [12, 0.5, 7]
                        // Model: from (-4, -0.5, -5) to (4, 0.5, -1)
                        // Relative to pivot: from (-4, -3.25, -5.5)
                        .texOffs(0, 2).addBox(-4f, -3.25f, -5.5f, 8f, 1f, 4f)
                        // Layer 4b: from [6, -0.5, 1] to [10, 0.5, 9]
                        // Model: from (-2, -0.5, -7) to (2, 0.5, 1)
                        // Relative to pivot: from (-2, -3.25, -7.5)
                        .texOffs(0, 1).addBox(-2f, -3.25f, -7.5f, 4f, 1f, 8f),
                PartPose.offsetAndRotation(PIVOT_X, PIVOT_Y, PIVOT_Z, ANGLE_45, 0f, 0f));

        // === ARM LEFT: from [-2, 5.5, 3] to [1, 5.5, 4] (3x0x1) ===
        // JSON center: ((-2+1)/2, 5.5, (3+4)/2) = (-0.5, 5.5, 3.5)
        // Model coords: (-0.5-8, 5.5, 3.5-8) = (-8.5, 5.5, -4.5)
        rootDef.addOrReplaceChild("arm_left",
                CubeListBuilder.create()
                        .texOffs(0, 14).addBox(-1.5f, 0f, -0.5f, 3f, 0f, 1f),
                PartPose.offsetAndRotation(-8.5f, 5.5f, -4.5f, ANGLE_45, 0f, 0f));

        // === ARM RIGHT: from [15, 5.5, 3] to [18, 5.5, 4] (3x0x1) ===
        // JSON center: ((15+18)/2, 5.5, 3.5) = (16.5, 5.5, 3.5)
        // Model coords: (16.5-8, 5.5, 3.5-8) = (8.5, 5.5, -4.5)
        rootDef.addOrReplaceChild("arm_right",
                CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-1.5f, 0f, -0.5f, 3f, 0f, 1f),
                PartPose.offsetAndRotation(8.5f, 5.5f, -4.5f, ANGLE_45, 0f, 0f));

        // === LEG LEFT: from [4, 5.5, 12] to [5, 5.5, 15] (1x0x3) ===
        // JSON center: (4.5, 5.5, 13.5)
        // Model coords: (4.5-8, 5.5, 13.5-8) = (-3.5, 5.5, 5.5)
        rootDef.addOrReplaceChild("leg_left",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-0.5f, 0f, -1.5f, 1f, 0f, 3f),
                PartPose.offsetAndRotation(-3.5f, 5.5f, 5.5f, ANGLE_45, 0f, 0f));

        // === LEG RIGHT: from [11, 5.5, 12] to [12, 5.5, 15] (1x0x3) ===
        // JSON center: (11.5, 5.5, 13.5)
        // Model coords: (11.5-8, 5.5, 13.5-8) = (3.5, 5.5, 5.5)
        rootDef.addOrReplaceChild("leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-0.5f, 0f, -1.5f, 1f, 0f, 3f),
                PartPose.offsetAndRotation(3.5f, 5.5f, 5.5f, ANGLE_45, 0f, 0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    // === Accesseurs pour l'animation ===

    public ModelPart getBody() {
        return body;
    }

    public ModelPart getArmLeft() {
        return armLeft;
    }

    public ModelPart getArmRight() {
        return armRight;
    }

    public ModelPart getLegLeft() {
        return legLeft;
    }

    public ModelPart getLegRight() {
        return legRight;
    }

    /** Reset toutes les rotations a leur valeur par defaut. */
    public void resetPose() {
        body.xRot = ANGLE_45;
        body.yRot = 0f;
        body.zRot = 0f;
        body.x = PIVOT_X;
        body.y = PIVOT_Y;
        body.z = PIVOT_Z;

        armLeft.xRot = ANGLE_45;
        armLeft.yRot = 0f;
        armLeft.zRot = 0f;

        armRight.xRot = ANGLE_45;
        armRight.yRot = 0f;
        armRight.zRot = 0f;

        legLeft.xRot = ANGLE_45;
        legLeft.yRot = 0f;
        legLeft.zRot = 0f;

        legRight.xRot = ANGLE_45;
        legRight.yRot = 0f;
        legRight.zRot = 0f;
    }
}
