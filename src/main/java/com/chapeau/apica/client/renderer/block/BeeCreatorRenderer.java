/**
 * ============================================================
 * [BeeCreatorRenderer.java]
 * Description: Renderer pour afficher le modele d'abeille au-dessus du Bee Creator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | BeeCreatorBlockEntity    | Donnees types/couleurs| getBodyTypeIndex(), etc.      |
 * | ApicaBeeModel            | Modele modulaire     | Multi-pass tinted render       |
 * | BeeBodyType/Wing/Stinger | Enum types           | Textures et layers             |
 * | BeeAntennaType           | Enum antennes        | Textures et layers             |
 * | BeePart                  | Enum parties         | Indices couleurs               |
 * | AnimationTimer           | Temps smooth         | Rotation                       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.block;

import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeePart;
import com.chapeau.apica.common.block.beecreator.BeeCreatorBlockEntity;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Rend le modele d'abeille personnalise au-dessus du bloc Bee Creator.
 * Utilise le meme systeme multi-pass que BeeCreatorScreen pour le tint par partie.
 */
public class BeeCreatorRenderer implements BlockEntityRenderer<BeeCreatorBlockEntity> {

    private ApicaBeeModel<?> model;
    private int cachedBodyIdx = -1;
    private int cachedWingIdx = -1;
    private int cachedStingerIdx = -1;
    private int cachedAntennaIdx = -1;

    public BeeCreatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BeeCreatorBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        int bodyIdx = be.getBodyTypeIndex();
        int wingIdx = be.getWingTypeIndex();
        int stingerIdx = be.getStingerTypeIndex();
        int antennaIdx = be.getAntennaTypeIndex();

        if (model == null || bodyIdx != cachedBodyIdx || wingIdx != cachedWingIdx
                || stingerIdx != cachedStingerIdx || antennaIdx != cachedAntennaIdx) {
            rebuildModel(bodyIdx, wingIdx, stingerIdx, antennaIdx);
        }
        if (model == null) return;

        BeeBodyType bodyType = BeeBodyType.byIndex(bodyIdx);
        BeeWingType wingType = BeeWingType.byIndex(wingIdx);
        BeeStingerType stingerType = BeeStingerType.byIndex(stingerIdx);
        BeeAntennaType antennaType = BeeAntennaType.byIndex(antennaIdx);

        ResourceLocation bodyTex = ApicaBeeModel.getBodyTexture(bodyType);
        ResourceLocation wingTex = ApicaBeeModel.getWingTexture(wingType);
        ResourceLocation stingerTex = ApicaBeeModel.getStingerTexture(stingerType);
        ResourceLocation antennaTex = ApicaBeeModel.getAntennaTexture(antennaType);

        float time = AnimationTimer.getRenderTime(partialTick);
        float bob = (float) Math.sin(time * 0.1) * 0.03f;
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());

        poseStack.pushPose();
        poseStack.translate(0.5, 2.2 + bob, 0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        float scale = 0.6f;
        poseStack.scale(scale, scale, scale);

        RenderType bodyRT = RenderType.entityCutout(bodyTex);
        VertexConsumer bodyVC = buffer.getBuffer(bodyRT);

        model.showCorpusOnly();
        model.renderToBuffer(poseStack, bodyVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.BODY)));

        model.showStripeOnly();
        model.renderToBuffer(poseStack, bodyVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.STRIPE)));

        model.showEyesOnly();
        model.renderToBuffer(poseStack, bodyVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.EYE)));

        model.showPupilsOnly();
        model.renderToBuffer(poseStack, bodyVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.PUPIL)));

        model.showUntintedOnly();
        model.renderToBuffer(poseStack, bodyVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.STRIPE)));

        VertexConsumer antennaVC = buffer.getBuffer(RenderType.entityCutout(antennaTex));
        model.renderAntenna(poseStack, antennaVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.ANTENNA)));

        VertexConsumer wingVC = buffer.getBuffer(RenderType.entityCutout(wingTex));
        model.renderWings(poseStack, wingVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.WING)));

        VertexConsumer stingerVC = buffer.getBuffer(RenderType.entityCutout(stingerTex));
        model.renderStinger(poseStack, stingerVC, light, OverlayTexture.NO_OVERLAY,
                toArgb(be.getPartColor(BeePart.STINGER)));

        poseStack.popPose();
    }

    private void rebuildModel(int bodyIdx, int wingIdx, int stingerIdx, int antennaIdx) {
        var entityModels = Minecraft.getInstance().getEntityModels();
        BeeBodyType bodyType = BeeBodyType.byIndex(bodyIdx);
        BeeWingType wingType = BeeWingType.byIndex(wingIdx);
        BeeStingerType stingerType = BeeStingerType.byIndex(stingerIdx);
        BeeAntennaType antennaType = BeeAntennaType.byIndex(antennaIdx);

        ModelPart bodyRoot = entityModels.bakeLayer(ApicaBeeModel.getBodyLayer(bodyType));
        ModelPart wingRoot = entityModels.bakeLayer(ApicaBeeModel.getWingLayer(wingType));
        ModelPart stingerRoot = entityModels.bakeLayer(ApicaBeeModel.getStingerLayer(stingerType));
        ModelPart antennaRoot = entityModels.bakeLayer(ApicaBeeModel.getAntennaLayer(antennaType));

        model = new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, bodyType);
        cachedBodyIdx = bodyIdx;
        cachedWingIdx = wingIdx;
        cachedStingerIdx = stingerIdx;
        cachedAntennaIdx = antennaIdx;
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    @Override
    public boolean shouldRenderOffScreen(BeeCreatorBlockEntity blockEntity) {
        return true;
    }
}
