/**
 * ============================================================
 * [ApicaBeeModel.java]
 * Description: Modele d'abeille customisable avec corps et rayures tintables separement
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
 * - BeeCreatorScreen (preview 3D tintee)
 * - ClientSetup (registerLayerDefinitions)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import com.chapeau.apica.Apica;
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
 * Deux cubes corps superposes (body_corpus et body_stripe) pour tinting separe.
 * Texture 64x64 avec UV standard: body pass a texOffs(0,0), stripe pass a texOffs(0,17).
 * Les zones transparentes dans chaque pass sont complementaires (corps = golden, rayures = dark bands).
 *
 * @param <T> Type d'entite compatible
 */
public class ApicaBeeModel<T extends Entity> extends HierarchicalModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "apica_bee"), "main");

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/entity/apica_bee.png");

    private final ModelPart root;
    public final ModelPart bodyCorpus;
    public final ModelPart bodyStripe;
    public final ModelPart frontLegs;
    public final ModelPart middleLegs;
    public final ModelPart backLegs;

    public ApicaBeeModel(ModelPart root) {
        this.root = root;
        ModelPart bone = root.getChild("bone");
        this.bodyCorpus = bone.getChild("body_corpus");
        this.bodyStripe = bone.getChild("body_stripe");
        this.frontLegs = bone.getChild("front_legs");
        this.middleLegs = bone.getChild("middle_legs");
        this.backLegs = bone.getChild("back_legs");
    }

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

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Pose statique pour la preview
    }

    /** Affiche uniquement le corps (pour tint pass corps). */
    public void showCorpusOnly() {
        bodyCorpus.visible = true;
        bodyStripe.visible = false;
        frontLegs.visible = false;
        middleLegs.visible = false;
        backLegs.visible = false;
    }

    /** Affiche uniquement les rayures (pour tint pass rayure). */
    public void showStripeOnly() {
        bodyCorpus.visible = false;
        bodyStripe.visible = true;
        frontLegs.visible = false;
        middleLegs.visible = false;
        backLegs.visible = false;
    }

    /** Affiche uniquement les parties non tintees (pattes, etc.). */
    public void showUntintedOnly() {
        bodyCorpus.visible = false;
        bodyStripe.visible = false;
        frontLegs.visible = true;
        middleLegs.visible = true;
        backLegs.visible = true;
    }

    /** Affiche toutes les parties. */
    public void showAll() {
        bodyCorpus.visible = true;
        bodyStripe.visible = true;
        frontLegs.visible = true;
        middleLegs.visible = true;
        backLegs.visible = true;
    }
}
