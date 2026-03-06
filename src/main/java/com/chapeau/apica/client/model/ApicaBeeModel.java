/**
 * ============================================================
 * [ApicaBeeModel.java]
 * Description: Modele d'abeille modulaire — corps interchangeables avec attaches pour ailes/dard
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | ModelLayerLocation, textures   |
 * | BeeBodyType         | Types de corps       | Layer factories, attachments   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeCreatorScreen (preview 3D tintee multi-pass)
 * - ClientSetup (registerLayerDefinitions)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Modele d'abeille modulaire pour le Bee Creator.
 * Corps interchangeable (DEFAULT, ROUND, ...) avec points d'attache
 * pour les parties separees (ailes, dard).
 *
 * Architecture:
 * - Body layer (64x64): corpus, stripe, eyes, pupils, legs — per body type
 * - Wing layer (32x32): shared geometry, positions ajustees par body type
 * - Stinger layer (32x32): shared geometry, position ajustee par body type
 *
 * @param <T> Type d'entite compatible
 */
public class ApicaBeeModel<T extends Entity> extends HierarchicalModel<T> {

    // --- Shared layer locations (wings/stinger geometry) ---
    public static final ModelLayerLocation WING_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee"), "wing");
    public static final ModelLayerLocation STINGER_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee"), "stinger");

    // --- Body parts (64x64 texture) ---
    private final ModelPart root;
    public final ModelPart bodyCorpus;
    public final ModelPart bodyStripe;
    public final ModelPart eyes;
    public final ModelPart leftPupil;
    public final ModelPart rightPupil;
    public final ModelPart frontLegs;
    public final ModelPart middleLegs;
    public final ModelPart backLegs;

    // --- Wing parts (32x32 texture, separate root) ---
    private final ModelPart wingRoot;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    // --- Stinger (32x32 texture, separate root) ---
    private final ModelPart stingerRoot;
    private final ModelPart stinger;

    // --- Current body type ---
    private final BeeBodyType bodyType;

    public ApicaBeeModel(ModelPart bodyRoot, ModelPart wingRoot, ModelPart stingerRoot, BeeBodyType bodyType) {
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

        applyAttachments();
    }

    /** Repositionne ailes et dard selon les points d'attache du body type. */
    private void applyAttachments() {
        float[] rw = getRightWingAttach(bodyType);
        rightWing.x = rw[0];
        rightWing.y = rw[1];
        rightWing.z = rw[2];
        rightWing.yRot = rw[3];

        leftWing.x = -rw[0];
        leftWing.y = rw[1];
        leftWing.z = rw[2];
        leftWing.yRot = -rw[3];

        float[] st = getStingerAttach(bodyType);
        stinger.x = st[0];
        stinger.y = st[1];
        stinger.z = st[2];
    }

    // ========== Attachment points per body type ==========

    /** Right wing: {x, y, z, rotY}. Left wing is mirrored. */
    private static float[] getRightWingAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{-1.5f, -4.0f, -3.0f, -0.2618f};
            case ROUND -> new float[]{-2.0f, -3.0f, -2.0f, -0.35f};
        };
    }

    /** Stinger: {x, y, z} relative to bone. */
    private static float[] getStingerAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{0.0f, -1.0f, 5.0f};
            case ROUND -> new float[]{0.0f, -0.5f, 4.0f};
        };
    }

    // ========== Layer locations per body type ==========

    public static ModelLayerLocation getBodyLayer(BeeBodyType type) {
        return new ModelLayerLocation(
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee_" + type.getId()), "body");
    }

    // ========== Textures per body type ==========

    public static ResourceLocation getBodyTexture(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/apica_bee.png");
            case ROUND -> ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/apica_bee_round.png");
        };
    }

    public static ResourceLocation getWingTexture(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/apica_bee_wing.png");
            case ROUND -> ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/apica_bee_wing_round.png");
        };
    }

    public static ResourceLocation getStingerTexture(BeeBodyType type) {
        return ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/entity/apica_bee_stinger.png");
    }

    public BeeBodyType getBodyType() { return bodyType; }

    // ========== Body layer factories ==========

    /** Dispatch: retourne la LayerDefinition du body type donne. */
    public static LayerDefinition createBodyLayerFor(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> createDefaultBodyLayer();
            case ROUND -> createRoundBodyLayer();
        };
    }

    /** Corps DEFAULT: vanilla-like (7x7x10), texture 64x64. */
    public static LayerDefinition createDefaultBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 42).addBox(-3.51F, -1.0F, -5.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 42).addBox(1.51F, -1.0F, -5.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 46)
                        .addBox(-2.51F, -1.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 46)
                        .addBox(1.51F, -1.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 35)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -2.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 37)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 0.0F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 39)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Corps ROUND: plus large et court (9x6x8), texture 64x64. */
    public static LayerDefinition createRoundBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.5F, -3.0F, -4.0F, 9.0F, 6.0F, 8.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create().texOffs(0, 14)
                        .addBox(-4.5F, -3.0F, -4.0F, 9.0F, 6.0F, 8.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 42).addBox(-4.51F, -1.0F, -4.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 42).addBox(2.51F, -1.0F, -4.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 46)
                        .addBox(-3.51F, -1.0F, -4.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 46)
                        .addBox(2.51F, -1.0F, -4.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 35)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(2.0F, 3.0F, -1.5F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 37)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(2.0F, 3.0F, 0.5F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 39)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(2.0F, 3.0F, 2.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Ailes (32x32). Geometrie partagee, positions ajustees par body type au runtime. */
    public static LayerDefinition createWingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        CubeDeformation inflate = new CubeDeformation(0.001F);

        // Positions par defaut (overrides par applyAttachments)
        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(2, 20)
                        .addBox(-18.0F, 0.0F, 0.0F, 18.0F, 0.0F, 12.0F, inflate),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));

        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(-12, 7)
                        .addBox(0.0F, 0.0F, 0.0F, 18.0F, 0.0F, 12.0F, inflate),
                PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Dard (32x32). Position ajustee par body type au runtime. */
    public static LayerDefinition createStingerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("stinger",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
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

    // ========== Visibility toggles ==========

    public void showCorpusOnly() {
        setBodyPartsVisible(false);
        bodyCorpus.visible = true;
    }

    public void showStripeOnly() {
        setBodyPartsVisible(false);
        bodyStripe.visible = true;
    }

    public void showEyesOnly() {
        setBodyPartsVisible(false);
        eyes.visible = true;
    }

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

    public void showAll() {
        setBodyPartsVisible(true);
    }

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
