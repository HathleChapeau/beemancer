/**
 * ============================================================
 * [ApicaBeeModel.java]
 * Description: Modele d'abeille modulaire — corps, ailes, dard et antennes interchangeables
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | ModelLayerLocation, textures   |
 * | BeeBodyType         | Types de corps       | Attachments, dispatch          |
 * | BeeWingType          | Types d'ailes        | Dispatch                       |
 * | BeeStingerType       | Types de dard        | Dispatch                       |
 * | BeeAntennaType       | Types d'antennes     | Dispatch                       |
 * | bee/body/*           | Layer definitions    | Geometrie par body type        |
 * | bee/wing/*           | Layer definitions    | Geometrie par wing type        |
 * | bee/stinger/*        | Layer definitions    | Geometrie par stinger type     |
 * | bee/antenna/*        | Layer definitions    | Geometrie par antenna type     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeCreatorScreen (preview 3D tintee multi-pass)
 * - BeeCreatorRenderer (rendu au-dessus du bloc)
 * - ClientSetup (registerLayerDefinitions)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.bee.antenna.ButterflyAntennaLayer;
import com.chapeau.apica.client.model.bee.antenna.DefaultAntennaLayer;
import com.chapeau.apica.client.model.bee.antenna.DemonAntennaLayer;
import com.chapeau.apica.client.model.bee.antenna.DragonAntennaLayer;
import com.chapeau.apica.client.model.bee.antenna.GlassesAntennaLayer;
import com.chapeau.apica.client.model.bee.antenna.LongAntennaLayer;
import com.chapeau.apica.client.model.bee.body.ArmoredBodyLayer;
import com.chapeau.apica.client.model.bee.body.DefaultBodyLayer;
import com.chapeau.apica.client.model.bee.body.PuffyBodyLayer;
import com.chapeau.apica.client.model.bee.body.SegmentedBodyLayer;
import com.chapeau.apica.client.model.bee.body.RoyalBodyLayer;
import com.chapeau.apica.client.model.bee.stinger.ChunkyStingerLayer;
import com.chapeau.apica.client.model.bee.stinger.DefaultStingerLayer;
import com.chapeau.apica.client.model.bee.stinger.SharpStingerLayer;
import com.chapeau.apica.client.model.bee.wing.DefaultWingLayer;
import com.chapeau.apica.client.model.bee.wing.RoundWingLayer;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Modele d'abeille modulaire pour le Bee Creator.
 * Corps, ailes, dard et antennes sont independamment interchangeables.
 * Chaque type a sa propre geometrie, texture et layer.
 * Les points d'attache (ailes/dard/antennes) dependent du body type.
 */
public class ApicaBeeModel<T extends Entity> extends HierarchicalModel<T> {

    private final ModelPart root;
    public final ModelPart bodyCorpus;
    public final ModelPart bodyStripe;
    public final ModelPart eyes;
    public final ModelPart leftPupil;
    public final ModelPart rightPupil;
    public final ModelPart frontLegs;
    public final ModelPart middleLegs;
    public final ModelPart backLegs;

    private final ModelPart wingRoot;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    private final ModelPart stingerRoot;
    private final ModelPart stinger;

    private final ModelPart antennaRoot;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;

    private final BeeBodyType bodyType;

    public ApicaBeeModel(ModelPart bodyRoot, ModelPart wingRoot, ModelPart stingerRoot,
                         ModelPart antennaRoot, BeeBodyType bodyType) {
        this.root = bodyRoot;
        this.bodyType = bodyType;

        ModelPart bone = bodyRoot.getChild("bone");
        this.bodyCorpus = bone.getChild("body_corpus");
        this.bodyStripe = bone.getChild("body_stripe");
        this.eyes = bone.getChild("eyes");
        this.leftPupil = bone.getChild("left_pupil");
        this.rightPupil = bone.getChild("right_pupil");
        this.frontLegs = bone.getChild("front_legs");
        this.middleLegs = bone.getChild("middle_legs");
        this.backLegs = bone.getChild("back_legs");

        this.wingRoot = wingRoot;
        ModelPart wingBone = wingRoot.getChild("bone");
        this.rightWing = wingBone.getChild("right_wing");
        this.leftWing = wingBone.getChild("left_wing");

        this.stingerRoot = stingerRoot;
        this.stinger = stingerRoot.getChild("bone").getChild("stinger");

        this.antennaRoot = antennaRoot;
        ModelPart antennaBone = antennaRoot.getChild("bone");
        this.leftAntenna = antennaBone.getChild("left_antenna");
        this.rightAntenna = antennaBone.getChild("right_antenna");

        applyAttachments();
    }

    private void applyAttachments() {
        float[] rw = getRightWingAttach(bodyType);
        rightWing.x = rw[0]; rightWing.y = rw[1]; rightWing.z = rw[2]; rightWing.yRot = rw[3];
        leftWing.x = -rw[0]; leftWing.y = rw[1]; leftWing.z = rw[2]; leftWing.yRot = -rw[3];

        float[] st = getStingerAttach(bodyType);
        stinger.x = st[0]; stinger.y = st[1]; stinger.z = st[2];

        float[] la = getLeftAntennaAttach(bodyType);
        leftAntenna.x = la[0]; leftAntenna.y = la[1]; leftAntenna.z = la[2];
        float[] ra = getRightAntennaAttach(bodyType);
        rightAntenna.x = ra[0]; rightAntenna.y = ra[1]; rightAntenna.z = ra[2];
    }

    // ========== Attachment points per body type ==========

    private static float[] getRightWingAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{-1.5f, -4.0f, -3.0f, -0.2618f};
            case ROYAL -> new float[]{-1.5f, -4.5f, -3.0f, -0.2618f};
            case SEGMENTED -> new float[]{-1.5f, -4.0f, -1.0f, -0.2618f};
            case ARMORED, PUFFY -> new float[]{-1.5f, -4.5f, -2.0f, -0.2618f};
        };
    }

    private static float[] getStingerAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{0.0f, 0.0f, 5.0f};
            case ROYAL -> new float[]{0.0f, 0.0f, 8.5f};
            case SEGMENTED -> new float[]{0.0f, 0.0f, 9.0f};
            case ARMORED, PUFFY -> new float[]{0.0f, 1.0f, 7.5f};
        };
    }

    private static float[] getLeftAntennaAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{-1.5f, -2.0f, -5.0f};
            case ROYAL -> new float[]{-1.5f, -2.0f, -8.5f};
            case SEGMENTED -> new float[]{-1.5f, -2.0f, -9.0f};
            case ARMORED, PUFFY -> new float[]{-1.5f, -1.5f, -7.5f};
        };
    }

    private static float[] getRightAntennaAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{1.5f, -2.0f, -5.0f};
            case ROYAL -> new float[]{1.5f, -2.0f, -8.5f};
            case SEGMENTED -> new float[]{1.5f, -2.0f, -9.0f};
            case ARMORED, PUFFY -> new float[]{1.5f, -1.5f, -7.5f};
        };
    }

    // ========== Layer factories (dispatch) ==========

    public static LayerDefinition createBodyLayerFor(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> DefaultBodyLayer.create();
            case ROYAL -> RoyalBodyLayer.create();
            case SEGMENTED -> SegmentedBodyLayer.create();
            case ARMORED -> ArmoredBodyLayer.create();
            case PUFFY -> PuffyBodyLayer.create();
        };
    }

    public static LayerDefinition createWingLayerFor(BeeWingType type) {
        return switch (type) {
            case DEFAULT -> DefaultWingLayer.create();
            case ROUND -> RoundWingLayer.create();
        };
    }

    public static LayerDefinition createStingerLayerFor(BeeStingerType type) {
        return switch (type) {
            case DEFAULT -> DefaultStingerLayer.create();
            case SHARP -> SharpStingerLayer.create();
            case CHUNKY -> ChunkyStingerLayer.create();
        };
    }

    public static LayerDefinition createAntennaLayerFor(BeeAntennaType type) {
        return switch (type) {
            case DEFAULT -> DefaultAntennaLayer.create();
            case LONG -> LongAntennaLayer.create();
            case DRAGON -> DragonAntennaLayer.create();
            case DEMON -> DemonAntennaLayer.create();
            case GLASSES -> GlassesAntennaLayer.create();
            case BUTTERFLY -> ButterflyAntennaLayer.create();
        };
    }

    // ========== Layer locations ==========

    public static ModelLayerLocation getBodyLayer(BeeBodyType type) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee_body_" + type.getId()), "main");
    }

    public static ModelLayerLocation getWingLayer(BeeWingType type) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee_wing_" + type.getId()), "main");
    }

    public static ModelLayerLocation getStingerLayer(BeeStingerType type) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee_stinger_" + type.getId()), "main");
    }

    public static ModelLayerLocation getAntennaLayer(BeeAntennaType type) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee_antenna_" + type.getId()), "main");
    }

    // ========== Textures ==========

    public static ResourceLocation getBodyTexture(BeeBodyType type) {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID,
                "textures/entity/apica_bee_body_" + type.getId() + ".png");
    }

    public static ResourceLocation getWingTexture(BeeWingType type) {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID,
                "textures/entity/apica_bee_wing_" + type.getId() + ".png");
    }

    public static ResourceLocation getStingerTexture(BeeStingerType type) {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID,
                "textures/entity/apica_bee_stinger_" + type.getId() + ".png");
    }

    public static ResourceLocation getAntennaTexture(BeeAntennaType type) {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID,
                "textures/entity/apica_bee_antenna_" + type.getId() + ".png");
    }

    public BeeBodyType getBodyType() { return bodyType; }

    public ModelPart getRightWing() { return rightWing; }
    public ModelPart getLeftWing() { return leftWing; }

    /** Anime les ailes avec vitesse et amplitude configurables. */
    public void animateWings(float ageInTicks, float speed, float amplitude, float upBias) {
        if (speed <= 0.001F || amplitude <= 0.001F) {
            rightWing.zRot = -upBias;
            leftWing.zRot = upBias;
            return;
        }
        float flap = Mth.cos(ageInTicks * speed) * amplitude;
        rightWing.zRot = flap - upBias;
        leftWing.zRot = -flap + upBias;
    }

    /** Anime les pattes avec un angle de repli. */
    public void animateLegs(float tuckAngle) {
        frontLegs.xRot = tuckAngle;
        middleLegs.xRot = tuckAngle;
        backLegs.xRot = tuckAngle;
    }

    // ========== HierarchicalModel ==========

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
    }

    // ========== Render helpers ==========

    public void renderWings(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        wingRoot.render(pose, vc, light, overlay, color);
    }

    public void renderStinger(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        stingerRoot.render(pose, vc, light, overlay, color);
    }

    public void renderAntenna(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        antennaRoot.render(pose, vc, light, overlay, color);
    }

    // ========== Visibility toggles ==========

    public void showCorpusOnly() { setBodyPartsVisible(false); bodyCorpus.visible = true; }
    public void showStripeOnly() { setBodyPartsVisible(false); bodyStripe.visible = true; }
    public void showEyesOnly() { setBodyPartsVisible(false); eyes.visible = true; }

    public void showPupilsOnly() {
        setBodyPartsVisible(false);
        leftPupil.visible = true;
        rightPupil.visible = true;
    }

    public void showUntintedOnly() {
        setBodyPartsVisible(false);
        frontLegs.visible = true;
        middleLegs.visible = true;
        backLegs.visible = true;
    }

    public void showAll() { setBodyPartsVisible(true); }

    private void setBodyPartsVisible(boolean visible) {
        bodyCorpus.visible = visible;
        bodyStripe.visible = visible;
        eyes.visible = visible;
        leftPupil.visible = visible;
        rightPupil.visible = visible;
        frontLegs.visible = visible;
        middleLegs.visible = visible;
        backLegs.visible = visible;
    }
}
