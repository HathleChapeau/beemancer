/**
 * ============================================================
 * [LeafBlowerProjectileModel.java]
 * Description: Modèle cube simple pour le projectile orbe du Leaf Blower
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | LeafBlowerProjectileEntity    | Entité rendue        | Paramètre générique du modèle  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - LeafBlowerProjectileRenderer.java (rendu de l'orbe)
 * - ClientSetup.java (registration du layer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.entity;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.entity.projectile.LeafBlowerProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class LeafBlowerProjectileModel extends EntityModel<LeafBlowerProjectileEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "leaf_blower_orb"), "main");

    private final ModelPart cube;

    public LeafBlowerProjectileModel(ModelPart root) {
        this.cube = root.getChild("cube");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();
        partDefinition.addOrReplaceChild("cube",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.ZERO);
        return LayerDefinition.create(meshDefinition, 32, 16);
    }

    @Override
    public void setupAnim(LeafBlowerProjectileEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        cube.yRot = ageInTicks * 0.15F;
        cube.xRot = ageInTicks * 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               int color) {
        cube.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
