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
 * Reproduit la geometrie du modele JSON avec parties separees pour l'animation.
 * Le corps est tilte de 45 degres sur l'axe X (comme le JSON original).
 */
public class ApiModel extends Model {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "api"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/block/machines/api.png");

    private static final float ANGLE_45 = (float) (Math.PI / 4);

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

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootDef = mesh.getRoot();

        // Pivot pour la rotation 45 degres (comme le JSON: origin 8, 2.75, 8.5)
        float pivotX = 8f, pivotY = 2.75f, pivotZ = 8.5f;

        // === BODY (toutes les couches Comby fusionnees) ===
        PartDefinition bodyDef = rootDef.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Layer 1a: 14x3x10
                        .texOffs(0, 1).addBox(-7f, -1.5f, -5f, 14f, 3f, 10f)
                        // Layer 1b: 10x3x14
                        .texOffs(0, 0).addBox(-5f, -1.5f, -7f, 10f, 3f, 14f)
                        // Layer 2a: 12x2x8
                        .texOffs(0, 1).addBox(-6f, -3.5f, -4f, 12f, 2f, 8f)
                        // Layer 2b: 8x2x12
                        .texOffs(0, 0).addBox(-4f, -3.5f, -6f, 8f, 2f, 12f)
                        // Layer 3a: 10x1x6
                        .texOffs(0, 2).addBox(-5f, -4.5f, -3f, 10f, 1f, 6f)
                        // Layer 3b: 6x1x10
                        .texOffs(0, 1).addBox(-3f, -4.5f, -5f, 6f, 1f, 10f)
                        // Layer 4a: 8x1x4
                        .texOffs(0, 2).addBox(-4f, -5.5f, -2f, 8f, 1f, 4f)
                        // Layer 4b: 4x1x8
                        .texOffs(0, 1).addBox(-2f, -5.5f, -4f, 4f, 1f, 8f),
                PartPose.offsetAndRotation(pivotX, pivotY, pivotZ, ANGLE_45, 0f, 0f));

        // === ARM LEFT (3x0x1 - tres plat) ===
        rootDef.addOrReplaceChild("arm_left",
                CubeListBuilder.create()
                        .texOffs(0, 14).addBox(-1.5f, 0f, -0.5f, 3f, 0f, 1f),
                PartPose.offsetAndRotation(-2f + pivotX, pivotY, 3f + pivotZ - 5f, ANGLE_45, 0f, 0f));

        // === ARM RIGHT (3x0x1 - tres plat) ===
        rootDef.addOrReplaceChild("arm_right",
                CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-1.5f, 0f, -0.5f, 3f, 0f, 1f),
                PartPose.offsetAndRotation(18f - 8f, pivotY, 3f + pivotZ - 5f, ANGLE_45, 0f, 0f));

        // === LEG LEFT (1x0x3 - tres plat) ===
        rootDef.addOrReplaceChild("leg_left",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-0.5f, 0f, -1.5f, 1f, 0f, 3f),
                PartPose.offsetAndRotation(4.5f, pivotY, 13.5f - 5f, ANGLE_45, 0f, 0f));

        // === LEG RIGHT (1x0x3 - tres plat) ===
        rootDef.addOrReplaceChild("leg_right",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-0.5f, 0f, -1.5f, 1f, 0f, 3f),
                PartPose.offsetAndRotation(11.5f, pivotY, 13.5f - 5f, ANGLE_45, 0f, 0f));

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
        body.x = 8f;
        body.y = 2.75f;
        body.z = 8.5f;

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
