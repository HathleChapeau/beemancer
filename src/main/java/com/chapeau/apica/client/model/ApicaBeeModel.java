/**
 * ============================================================
 * [ApicaBeeModel.java]
 * Description: Modele d'abeille modulaire — corps, ailes et dard interchangeables
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | ModelLayerLocation, textures   |
 * | BeeBodyType         | Types de corps       | Layer factories, attachments   |
 * | BeeWingType          | Types d'ailes        | Layer factories, textures      |
 * | BeeStingerType       | Types de dard        | Layer factories, textures      |
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
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
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
 * Corps, ailes et dard sont independamment interchangeables.
 * Chaque type a sa propre geometrie, texture et layer.
 * Les points d'attache (position des ailes/dard) dependent du body type.
 *
 * @param <T> Type d'entite compatible
 */
public class ApicaBeeModel<T extends Entity> extends HierarchicalModel<T> {

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
    public final ModelPart leftAntenna;
    public final ModelPart rightAntenna;

    // --- Wing parts (32x32 texture, separate root) ---
    private final ModelPart wingRoot;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    // --- Stinger (32x32 texture, separate root) ---
    private final ModelPart stingerRoot;
    private final ModelPart stinger;

    // --- Current types ---
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
        this.leftAntenna = bone.getChild("left_antenna");
        this.rightAntenna = bone.getChild("right_antenna");

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
            case THICK -> new float[]{-1.5f, -4.0f, -3.0f, -0.2618f};
        };
    }

    /** Stinger: {x, y, z} relative to bone. */
    private static float[] getStingerAttach(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> new float[]{0.0f, -1.0f, 5.0f};
            case THICK -> new float[]{0.0f, -1.0f, 12.0f};
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

    public BeeBodyType getBodyType() { return bodyType; }

    // ========== Body layer factories ==========

    public static LayerDefinition createBodyLayerFor(BeeBodyType type) {
        return switch (type) {
            case DEFAULT -> createDefaultBodyLayer();
            case THICK -> createThickBodyLayer();
        };
    }

    /** Corps DEFAULT: vanilla-like (7x7x10), texture 64x64. */
    private static LayerDefinition createDefaultBodyLayer() {
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
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(14, 35)
                        .addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(-1.5F, -4.0F, -5.0F));
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(22, 35)
                        .addBox(-1.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(1.5F, -4.0F, -5.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Corps THICK: segmente (body 7x7x10 + thorax 8x8x5 + waist 6x6x1 + tail 7x7x6).
     * Thorax a l'avant (3px depuis le front du body), waist a l'arriere du body, tail derriere.
     * Double couche (corpus + stripe) sur tous les cubes sauf waist.
     * Texture 64x64.
     */
    private static LayerDefinition createThickBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: body + thorax + waist + tail (all solid geometry)
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F)      // Body (7x7x10)
                        .texOffs(34, 0).addBox(-4.0F, -4.5F, -7.0F, 8.0F, 8.0F, 5.0F)       // Thorax (8x8x5)
                        .texOffs(0, 34).addBox(-3.0F, -3.5F, 5.0F, 6.0F, 6.0F, 1.0F)        // Waist (6x6x1)
                        .texOffs(34, 26).addBox(-3.5F, -4.0F, 6.0F, 7.0F, 7.0F, 6.0F),      // Tail (7x7x6)
                PartPose.ZERO);

        // body_stripe: body + thorax + tail (NO waist)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F)     // Body stripe
                        .texOffs(34, 13).addBox(-4.0F, -4.5F, -7.0F, 8.0F, 8.0F, 5.0F)      // Thorax stripe
                        .texOffs(34, 39).addBox(-3.5F, -4.0F, 6.0F, 7.0F, 7.0F, 6.0F),      // Tail stripe
                PartPose.ZERO);

        // Eyes on thorax front face (Z=-7)
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 47).addBox(-4.01F, -1.0F, -7.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 47).addBox(2.01F, -1.0F, -7.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 51)
                        .addBox(-3.01F, -1.0F, -7.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 51)
                        .addBox(2.01F, -1.0F, -7.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Legs under thorax (thorax Z: -7 to -2, bottom Y: 3.5)
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 41)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.5F, -6.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 43)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.5F, -4.5F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 45)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.5F, -3.0F));

        // Antennae on thorax front (Z=-7), top (Y=-4.5)
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(0, 53)
                        .addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(-2.0F, -4.5F, -7.0F));
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(10, 53)
                        .addBox(-1.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(2.0F, -4.5F, -7.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    // ========== Wing layer factories ==========

    public static LayerDefinition createWingLayerFor(BeeWingType type) {
        return switch (type) {
            case DEFAULT -> createDefaultWingLayer();
            case ROUND -> createRoundWingLayer();
        };
    }

    /** Ailes DEFAULT: grandes (18x12), texture 32x32. */
    private static LayerDefinition createDefaultWingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        CubeDeformation inflate = new CubeDeformation(0.001F);

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

    /** Ailes ROUND: plus courtes et larges (12x10), texture 32x32. */
    private static LayerDefinition createRoundWingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        CubeDeformation inflate = new CubeDeformation(0.001F);

        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(2, 22)
                        .addBox(-12.0F, 0.0F, 0.0F, 12.0F, 0.0F, 10.0F, inflate),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));

        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(-10, 6)
                        .addBox(0.0F, 0.0F, 0.0F, 12.0F, 0.0F, 10.0F, inflate),
                PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    // ========== Stinger layer factories ==========

    public static LayerDefinition createStingerLayerFor(BeeStingerType type) {
        return switch (type) {
            case DEFAULT -> createDefaultStingerLayer();
            case SHARP -> createSharpStingerLayer();
        };
    }

    /** Dard DEFAULT: court (0x1x2), texture 32x32. */
    private static LayerDefinition createDefaultStingerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("stinger",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Dard SHARP: long (0x1x4), texture 32x32. */
    private static LayerDefinition createSharpStingerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("stinger",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, -1.0F, 0.0F, 0.0F, 1.0F, 4.0F),
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

    public void showAntennaOnly() {
        setBodyPartsVisible(false);
        leftAntenna.visible = true;
        rightAntenna.visible = true;
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
        leftAntenna.visible = visible;
        rightAntenna.visible = visible;
    }
}
