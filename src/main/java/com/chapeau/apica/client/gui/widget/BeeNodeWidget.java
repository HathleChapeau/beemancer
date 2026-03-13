/**
 * ============================================================
 * [BeeNodeWidget.java]
 * Description: Widget pour afficher une abeille en 3D dans le Codex
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Donnees du node      | Affichage et interaction       |
 * | BeeModel            | Modele vanilla       | Rendu 3D de l'abeille          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CodexScreen (creation et rendu des nodes bees)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeAntennaType;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.quest.NodeState;
import com.chapeau.apica.common.quest.QuestPlayerData;
import com.chapeau.apica.core.bee.BeeSpeciesManager;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeeNodeWidget extends AbstractWidget {
    public static final int NODE_SIZE = 24;
    private static final int FRAME_SIZE = 26;
    private static final int BEE_RENDER_SIZE = 16;

    // Vanilla advancement frame textures
    private static final ResourceLocation TASK_FRAME_OBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/task_frame_obtained.png");
    private static final ResourceLocation TASK_FRAME_UNOBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/task_frame_unobtained.png");
    private static final ResourceLocation GOAL_FRAME_OBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/goal_frame_obtained.png");
    private static final ResourceLocation GOAL_FRAME_UNOBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/goal_frame_unobtained.png");
    private static final ResourceLocation CHALLENGE_FRAME_OBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/challenge_frame_obtained.png");
    private static final ResourceLocation CHALLENGE_FRAME_UNOBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/challenge_frame_unobtained.png");

    /** Cache des modeles ApicaBee par cle de combinaison de types. */
    private static final Map<String, ApicaBeeModel<?>> MODEL_CACHE = new HashMap<>();

    private final CodexNode node;
    private final String speciesId;
    private final Component displayTitle;
    private final Component displayDescription;
    private final NodeState nodeState;
    private final boolean harmonized;
    private boolean unlocked;
    private boolean canUnlock;
    private boolean hovered;


    public BeeNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock) {
        this(node, screenX, screenY, unlocked, canUnlock, null);
    }

    public BeeNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock, NodeState state) {
        super(screenX, screenY, NODE_SIZE, NODE_SIZE, node.getTitle());
        this.node = node;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;
        this.nodeState = state != null ? state : (unlocked ? NodeState.UNLOCKED : (canUnlock ? NodeState.DISCOVERED : NodeState.LOCKED));

        // Extract species ID from node ID (e.g., "meadow_bee" -> "meadow")
        String nodeId = node.getId();
        if (nodeId.endsWith("_bee")) {
            this.speciesId = nodeId.substring(0, nodeId.length() - 4);
        } else {
            this.speciesId = nodeId;
        }

        // Vérifier si l'espèce est harmonisée via les données de species
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesManager.BeeSpeciesData speciesData = BeeSpeciesManager.getSpecies(this.speciesId);
        this.harmonized = speciesData != null && speciesData.harmonized;

        // Titre toujours le vrai nom (la decouverte du nom se fait via l'injecteur, pas le codex)
        Component baseTitle = CodexManager.getDisplayTitle(node, this.nodeState);
        this.displayTitle = baseTitle;
        this.displayDescription = CodexManager.getDisplayDescription(node, this.nodeState);
    }

    private static boolean isSpeciesKnownByPlayer(String speciesId) {
        if (Minecraft.getInstance().player != null) {
            CodexPlayerData data = Minecraft.getInstance().player.getData(ApicaAttachments.CODEX_DATA);
            return data.isSpeciesKnown(speciesId);
        }
        return false;
    }

    /**
     * Vérifie si un parent breeding est connu : espèce apprise via injecteur
     * OU quête du node parent complétée (le joueur a déjà obtenu cette abeille).
     */
    private static boolean isParentKnown(String parentNodeId, String speciesId) {
        if (isSpeciesKnownByPlayer(speciesId)) return true;
        if (Minecraft.getInstance().player == null) return false;
        CodexNode parentNode = CodexManager.getNode("bees:" + parentNodeId);
        if (parentNode != null && parentNode.hasQuest()) {
            QuestPlayerData questData = Minecraft.getInstance().player.getData(ApicaAttachments.QUEST_DATA);
            return questData.isCompleted(parentNode.getQuestId());
        }
        return false;
    }

    private static ApicaBeeModel<?> getOrBuildModel(BeeBodyType body, BeeWingType wing,
                                                      BeeStingerType stinger, BeeAntennaType antenna) {
        String key = body.getId() + "_" + wing.getId() + "_" + stinger.getId() + "_" + antenna.getId();
        return MODEL_CACHE.computeIfAbsent(key, k -> {
            try {
                var entityModels = Minecraft.getInstance().getEntityModels();
                ModelPart bodyRoot = entityModels.bakeLayer(ApicaBeeModel.getBodyLayer(body));
                ModelPart wingRoot = entityModels.bakeLayer(ApicaBeeModel.getWingLayer(wing));
                ModelPart stingerRoot = entityModels.bakeLayer(ApicaBeeModel.getStingerLayer(stinger));
                ModelPart antennaRoot = entityModels.bakeLayer(ApicaBeeModel.getAntennaLayer(antenna));
                return new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, antennaRoot, body);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public CodexNode getNode() {
        return node;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setCanUnlock(boolean canUnlock) {
        this.canUnlock = canUnlock;
    }

    public boolean canUnlock() {
        return canUnlock;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = isMouseOver(mouseX, mouseY);

        // Draw achievement-style frame background
        renderFrame(graphics);

        // Draw 3D bee model
        renderBee3D(graphics, partialTick);

        // Draw glow effect if hovered and can unlock
        if (hovered && canUnlock && !unlocked) {
            renderGlow(graphics);
        }

        // Badge tier en haut à gauche
        renderTierBadge(graphics);

        // Badge "New" si DISCOVERED
        if (nodeState == NodeState.DISCOVERED) {
            renderNewBadge(graphics);
        }
    }

    private void renderNewBadge(GuiGraphics graphics) {
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        String text = "New";
        int badgeX = getX() + NODE_SIZE - 2;
        int badgeY = getY() - 4;

        // Encadré vert
        int textWidth = font.width(text);
        int padding = 2;
        int bgColor = 0xFF228B22; // Vert forêt
        int borderColor = 0xFF32CD32; // Vert lime

        graphics.pose().pushPose();
        graphics.pose().translate(badgeX, badgeY, 0);
        graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(15)); // Penché

        // Fond + bordure
        graphics.fill(-padding, -padding, textWidth + padding, font.lineHeight + padding, borderColor);
        graphics.fill(-padding + 1, -padding + 1, textWidth + padding - 1, font.lineHeight + padding - 1, bgColor);

        // Texte
        graphics.drawString(font, text, 0, 0, 0xFFFFFFFF, false);

        graphics.pose().popPose();
    }

    private void renderTierBadge(GuiGraphics graphics) {
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);
        if (data == null) {
            return;
        }

        String tier = data.tier;
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;

        int badgeX = getX() - 2;
        int badgeY = getY() - 2;
        int textWidth = font.width(tier);
        int padding = 1;

        // Fond semi-transparent noir avec bordure dorée
        int bgColor = 0xCC000000;
        int borderColor = 0xFFDAA520; // Goldenrod

        graphics.fill(badgeX - padding, badgeY - padding,
                badgeX + textWidth + padding + 1, badgeY + font.lineHeight + padding, borderColor);
        graphics.fill(badgeX - padding + 1, badgeY - padding + 1,
                badgeX + textWidth + padding, badgeY + font.lineHeight + padding - 1, bgColor);

        // Texte doré (décalé +1px droite, +1px bas par rapport au fond)
        graphics.drawString(font, tier, badgeX + 1, badgeY + 1, 0xFFFFD700, false);
    }

    private void renderFrame(GuiGraphics graphics) {
        int x = getX();
        int y = getY();

        // Choisir le type de frame : challenge > harmonized (goal) > task
        boolean challenge = node.isChallenge();

        ResourceLocation frame;
        float r, g, b;

        if (unlocked && node.isDefault()) {
            frame = challenge ? CHALLENGE_FRAME_OBTAINED
                    : harmonized ? GOAL_FRAME_OBTAINED
                    : TASK_FRAME_OBTAINED;
            r = 0.45f; g = 0.50f; b = 0.55f;   // Bleu/gris terne (default)
        } else if (unlocked) {
            frame = challenge ? CHALLENGE_FRAME_OBTAINED
                    : harmonized ? GOAL_FRAME_OBTAINED
                    : TASK_FRAME_OBTAINED;
            r = 1.0f; g = 1.0f; b = 1.0f;       // Pas de teinte
        } else if (canUnlock) {
            frame = challenge ? CHALLENGE_FRAME_UNOBTAINED
                    : harmonized ? GOAL_FRAME_UNOBTAINED
                    : TASK_FRAME_UNOBTAINED;
            r = 1.0f; g = 1.0f; b = 1.0f;       // Blanc
        } else {
            frame = challenge ? CHALLENGE_FRAME_UNOBTAINED
                    : harmonized ? GOAL_FRAME_UNOBTAINED
                    : TASK_FRAME_UNOBTAINED;
            r = 0.27f; g = 0.27f; b = 0.27f;   // Sombre (#444444)
        }

        RenderSystem.setShaderColor(r, g, b, 1.0f);
        graphics.blit(frame, x - 1, y - 1, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderBee3D(GuiGraphics graphics, float partialTick) {
        BeeSpeciesManager.ensureClientLoaded();
        BeeSpeciesManager.BeeSpeciesData data = BeeSpeciesManager.getSpecies(speciesId);

        String bodyId = data != null ? data.modelBody : "default";
        String wingId = data != null ? data.modelWing : "default";
        String stingerId = data != null ? data.modelStinger : "default";
        String antennaId = data != null ? data.modelAntenna : "default";

        BeeBodyType bodyType = resolveBodyType(bodyId);
        BeeWingType wingType = resolveWingType(wingId);
        BeeStingerType stingerType = resolveStingerType(stingerId);
        BeeAntennaType antennaType = resolveAntennaType(antennaId);

        ApicaBeeModel<?> model = getOrBuildModel(bodyType, wingType, stingerType, antennaType);
        if (model == null) return;

        // Couleurs par partie (grisees si pas unlocked)
        float dimFactor = unlocked ? 1.0f : 0.4f;
        int bodyColor = dimColor(data != null ? data.partColorBody : 0xCC8800, dimFactor);
        int stripeColor = dimColor(data != null ? data.partColorStripe : 0x1A1A1A, dimFactor);
        int wingColor = dimColor(data != null ? data.partColorWing : 0xAADDFF, dimFactor);
        int antennaColor = dimColor(data != null ? data.partColorAntenna : 0x1A1A1A, dimFactor);
        int stingerColor = dimColor(data != null ? data.partColorStinger : 0xDDAA00, dimFactor);
        int eyeColor = dimColor(data != null ? data.partColorEye : 0x1A1A1A, dimFactor);
        int pupilColor = dimColor(data != null ? data.partColorPupil : 0xFFFFFF, dimFactor);

        ResourceLocation bodyTex = ApicaBeeModel.getBodyTexture(bodyType);
        ResourceLocation wingTex = ApicaBeeModel.getWingTexture(wingType);
        ResourceLocation stingerTex = ApicaBeeModel.getStingerTexture(stingerType);
        ResourceLocation antennaTex = ApicaBeeModel.getAntennaTexture(antennaType);

        int centerX = getX() + NODE_SIZE / 2;
        int centerY = getY() + NODE_SIZE / 2;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(centerX, centerY - 17, 100);
        float scale = -16;
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(160));
        poseStack.mulPose(Axis.YP.rotationDegrees(219));

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        // Multi-pass body render
        VertexConsumer bodyVC = bufferSource.getBuffer(RenderType.entityCutout(bodyTex));

        model.showCorpusOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(bodyColor));

        model.showStripeOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(stripeColor));

        model.showEyesOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(eyeColor));

        model.showPupilsOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(pupilColor));

        model.showUntintedOnly();
        model.renderToBuffer(poseStack, bodyVC, light, overlay, toArgb(stripeColor));

        // Antenna
        VertexConsumer antennaVC = bufferSource.getBuffer(RenderType.entityCutout(antennaTex));
        model.renderAntenna(poseStack, antennaVC, light, overlay, toArgb(antennaColor));

        // Wings
        VertexConsumer wingVC = bufferSource.getBuffer(RenderType.entityCutout(wingTex));
        model.renderWings(poseStack, wingVC, light, overlay, toArgb(wingColor));

        // Stinger
        VertexConsumer stingerVC = bufferSource.getBuffer(RenderType.entityCutout(stingerTex));
        model.renderStinger(poseStack, stingerVC, light, overlay, toArgb(stingerColor));

        bufferSource.endBatch();
        poseStack.popPose();
    }

    private void renderGlow(GuiGraphics graphics) {
        // Golden glow effect
        graphics.fill(getX() - 1, getY() - 1, getX() + NODE_SIZE + 1, getY() + NODE_SIZE + 1, 0x40FFAA00);
    }

    private static BeeBodyType resolveBodyType(String id) {
        for (BeeBodyType t : BeeBodyType.values()) if (t.getId().equals(id)) return t;
        return BeeBodyType.DEFAULT;
    }

    private static BeeWingType resolveWingType(String id) {
        for (BeeWingType t : BeeWingType.values()) if (t.getId().equals(id)) return t;
        return BeeWingType.DEFAULT;
    }

    private static BeeStingerType resolveStingerType(String id) {
        for (BeeStingerType t : BeeStingerType.values()) if (t.getId().equals(id)) return t;
        return BeeStingerType.DEFAULT;
    }

    private static BeeAntennaType resolveAntennaType(String id) {
        for (BeeAntennaType t : BeeAntennaType.values()) if (t.getId().equals(id)) return t;
        return BeeAntennaType.DEFAULT;
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Assombrit une couleur RGB par un facteur (0.0 = noir, 1.0 = original). */
    private static int dimColor(int rgb, float factor) {
        int r = (int)(((rgb >> 16) & 0xFF) * factor);
        int g = (int)(((rgb >> 8) & 0xFF) * factor);
        int b = (int)((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hovered) {
            Minecraft mc = Minecraft.getInstance();

            List<Component> tooltipLines = new ArrayList<>();

            // Titre (bold si unlocked)
            tooltipLines.add(displayTitle.copy().withStyle(style -> style.withBold(unlocked)));

            // Parents (toujours affichés si disponibles, sauf si SECRET+LOCKED)
            Component breedingLine = getBreedingParentsLine();
            if (breedingLine != null) {
                tooltipLines.add(breedingLine);
            }

            // Description
            tooltipLines.add(displayDescription);

            graphics.renderTooltip(mc.font, tooltipLines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    private Component getBreedingParentsLine() {
        // Ne pas afficher les parents si SECRET et LOCKED (affiche "???")
        if (node.getVisibility() == com.chapeau.apica.common.quest.NodeVisibility.SECRET
                && nodeState == NodeState.LOCKED) {
            return null;
        }

        String parent1 = node.getBreedingParent1();
        String parent2 = node.getBreedingParent2();

        if (parent1 == null || parent2 == null) {
            return null; // Base bees have no parents
        }

        String parent1Species = parent1.endsWith("_bee") ? parent1.substring(0, parent1.length() - 4) : parent1;
        String parent2Species = parent2.endsWith("_bee") ? parent2.substring(0, parent2.length() - 4) : parent2;

        String parent1Display = isParentKnown(parent1, parent1Species)
            ? formatBeeName(parent1)
            : "???";
        String parent2Display = isParentKnown(parent2, parent2Species)
            ? formatBeeName(parent2)
            : "???";

        return Component.literal(parent1Display + " + " + parent2Display).withStyle(style -> style.withColor(0xAAAA55));
    }

    private String formatBeeName(String beeId) {
        // Convert "meadow_bee" to "Meadow Bee"
        String[] parts = beeId.replace("_", " ").split(" ");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + NODE_SIZE
            && mouseY >= getY() && mouseY < getY() + NODE_SIZE;
    }

    /** Vide le cache des modeles (rechargement des ressources). */
    public static void clearModelCache() {
        MODEL_CACHE.clear();
    }
}
