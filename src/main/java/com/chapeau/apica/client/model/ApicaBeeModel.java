/**
 * ============================================================
 * [ApicaBeeModel.java]
 * Description: Modele d'abeille customisable avec corps, rayures, yeux, ailes et dard tintables
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Apica               | MOD_ID               | ModelLayerLocation, textures   |
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Modele d'abeille customisable pour le Bee Creator.
 * Trois LayerDefinitions separees pour supporter des textures de tailles differentes:
 * - Corps (64x64): body_corpus, body_stripe, yeux, pupilles, pattes
 * - Ailes (32x32): left_wing, right_wing
 * - Dard (32x32): stinger
 *
 * @param <T> Type d'entite compatible
 */
public class ApicaBeeModel<T extends Entity> extends HierarchicalModel<T> {

    // --- Layer locations ---
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee"), "main");
    public static final ModelLayerLocation WING_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee"), "wing");
    public static final ModelLayerLocation STINGER_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee"), "stinger");

    // --- Textures ---
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/entity/apica_bee.png");
    public static final ResourceLocation WING_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/entity/apica_bee_wing.png");
    public static final ResourceLocation STINGER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/entity/apica_bee_stinger.png");

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

    // --- Stinger (32x32 texture, separate root) ---
    private final ModelPart stingerRoot;

    public ApicaBeeModel(ModelPart bodyRoot, ModelPart wingRoot, ModelPart stingerRoot) {
        this.root = bodyRoot;
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
        this.stingerRoot = stingerRoot;
    }

    /** Corps principal: body, stripe, yeux, pupilles, pattes (64x64). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();

        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Corps — layer corpus (tinte avec couleur corps)
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);

        // Corps — layer rayure (tinte avec couleur rayure)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(0, 17)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);

        // Yeux: box 2x3x1 au coin du corps. 2 colonnes sur la face avant + 1 colonne sur le cote.
        // Legere protrusion (0.01) pour rendre par-dessus le corps. entityCutout masque le reste.
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 42)
                        .addBox(-3.51F, -5.0F, -5.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 42)
                        .addBox(1.51F, -5.0F, -5.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        // Pupille gauche: 1x1 en haut-droite de l'oeil gauche (coin interne)
        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create()
                        .texOffs(0, 46)
                        .addBox(-2.51F, -3.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Pupille droite: 1x1 en haut-gauche de l'oeil droit (coin interne, miroir)
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create()
                        .texOffs(4, 46)
                        .addBox(1.51F, -3.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Pattes (non tintees)
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create()
                        .texOffs(0, 35)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -2.0F));

        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create()
                        .texOffs(0, 37)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 0.0F));

        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create()
                        .texOffs(0, 39)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Ailes (32x32). Plan horizontal 8x16 pour couvrir plus de texture.
     * Les pixels transparents ne rendent pas (entityCutout), donc seule la
     * zone peinte dans la texture est visible. Peindre plus de pixels dans
     * le top face (cols 16-23, rows 0-15) agrandit visuellement l'aile.
     */
    public static LayerDefinition createWingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();

        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-8.0F, 0.0F, 0.0F, 8.0F, 0.0F, 16.0F),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -8.0F, 0.0F, -0.2618F, 0.0F));

        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .mirror()
                        .addBox(0.0F, 0.0F, 0.0F, 8.0F, 0.0F, 16.0F),
                PartPose.offsetAndRotation(1.5F, -4.0F, -8.0F, 0.0F, 0.2618F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Dard (32x32). Plan vertical depassant a l'arriere du corps. */
    public static LayerDefinition createStingerLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();

        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("stinger",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(0.0F, -1.0F, 5.0F, 0.0F, 1.0F, 2.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Pose statique pour la preview
    }

    // ========== Render helpers pour les textures separees ==========

    /** Rend les ailes avec leur propre texture. */
    public void renderWings(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        wingRoot.render(pose, vc, light, overlay, color);
    }

    /** Rend le dard avec sa propre texture. */
    public void renderStinger(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        stingerRoot.render(pose, vc, light, overlay, color);
    }

    // ========== Visibility toggles (body texture parts only) ==========

    /** Affiche uniquement le corps (pour tint pass corps). */
    public void showCorpusOnly() {
        setBodyPartsVisible(false);
        bodyCorpus.visible = true;
    }

    /** Affiche uniquement les rayures (pour tint pass rayure). */
    public void showStripeOnly() {
        setBodyPartsVisible(false);
        bodyStripe.visible = true;
    }

    /** Affiche uniquement les yeux (pour tint pass yeux). */
    public void showEyesOnly() {
        setBodyPartsVisible(false);
        eyes.visible = true;
    }

    /** Affiche uniquement les pupilles (pour tint pass pupilles). */
    public void showPupilsOnly() {
        setBodyPartsVisible(false);
        leftPupil.visible = true;
        rightPupil.visible = true;
    }

    /** Affiche uniquement les parties non tintees (pattes). */
    public void showUntintedOnly() {
        setBodyPartsVisible(false);
        frontLegs.visible = true;
        middleLegs.visible = true;
        backLegs.visible = true;
    }

    /** Affiche toutes les parties body. */
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
