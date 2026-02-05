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
package com.chapeau.beemancer.client.gui.widget;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.codex.CodexPlayerData;
import com.chapeau.beemancer.common.quest.NodeState;
import com.chapeau.beemancer.core.registry.BeemancerAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
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
import java.util.Set;

public class BeeNodeWidget extends AbstractWidget {
    public static final int NODE_SIZE = 20;
    private static final int BEE_RENDER_SIZE = 16;

    // Vanilla bee texture fallback
    private static final ResourceLocation VANILLA_BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    // Texture cache
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new HashMap<>();

    private final CodexNode node;
    private final String speciesId;
    private final Component displayTitle;
    private final Component displayDescription;
    private final NodeState nodeState;
    private boolean unlocked;
    private boolean canUnlock;
    private boolean hovered;

    private static BeeModel<?> beeModel;

    public BeeNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock) {
        this(node, screenX, screenY, unlocked, canUnlock, null);
    }

    public BeeNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock, NodeState state) {
        super(screenX, screenY, NODE_SIZE, NODE_SIZE, node.getTitle());
        this.node = node;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;
        this.nodeState = state != null ? state : (unlocked ? NodeState.UNLOCKED : (canUnlock ? NodeState.DISCOVERED : NodeState.LOCKED));

        // Calculer le texte à afficher (??? si SECRET et LOCKED)
        this.displayTitle = CodexManager.getDisplayTitle(node, this.nodeState);
        this.displayDescription = CodexManager.getDisplayDescription(node, this.nodeState);

        // Extract species ID from node ID (e.g., "meadow_bee" -> "meadow")
        String nodeId = node.getId();
        if (nodeId.endsWith("_bee")) {
            this.speciesId = nodeId.substring(0, nodeId.length() - 4);
        } else {
            this.speciesId = nodeId;
        }
    }

    private static BeeModel<?> getOrCreateModel() {
        if (beeModel == null) {
            beeModel = new BeeModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BEE));
        }
        return beeModel;
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
    }

    private void renderFrame(GuiGraphics graphics) {
        // Simple colored background frame
        int bgColor = unlocked ? 0xFF333333 : 0xFF222222;
        int borderColor;

        switch (node.getCategory().name()) {
            case "ROOT" -> borderColor = unlocked ? 0xFFFFAA00 : 0xFF886600; // Golden for base bees
            case "GOAL" -> borderColor = unlocked ? 0xFF00FF00 : 0xFF006600; // Green for goals
            case "CHALLENGE" -> borderColor = unlocked ? 0xFFFF00FF : 0xFF660066; // Purple for challenges
            default -> borderColor = unlocked ? 0xFFAAAAAA : 0xFF555555; // Gray for normal
        }

        // Draw border
        graphics.fill(getX() - 1, getY() - 1, getX() + NODE_SIZE + 1, getY() + NODE_SIZE + 1, borderColor);
        // Draw background
        graphics.fill(getX(), getY(), getX() + NODE_SIZE, getY() + NODE_SIZE, bgColor);
    }

    private void renderBee3D(GuiGraphics graphics, float partialTick) {
        ResourceLocation texture = getTextureForSpecies(speciesId);

        int centerX = getX() + NODE_SIZE / 2;
        int centerY = getY() + NODE_SIZE / 2;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Position at center of widget
        poseStack.translate(centerX, centerY - 50, 100);

        // Scale up bee model (5x zoom)
        float scale = -40;
        poseStack.scale(scale, scale, scale);

        // Flip and rotate for display
        poseStack.mulPose(Axis.XP.rotationDegrees(160));
        poseStack.mulPose(Axis.YP.rotationDegrees(144 + (hovered ? partialTick * 2 : 0)));

        // Get buffer source and render
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutout(texture));

        BeeModel<?> model = getOrCreateModel();

        model.renderToBuffer(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, unlocked ? 0xFFFFFFFF : 0xFF666666);

        bufferSource.endBatch();

        poseStack.popPose();
    }

    private void renderGlow(GuiGraphics graphics) {
        // Golden glow effect
        graphics.fill(getX() - 1, getY() - 1, getX() + NODE_SIZE + 1, getY() + NODE_SIZE + 1, 0x40FFAA00);
    }

    private ResourceLocation getTextureForSpecies(String speciesId) {
        return TEXTURE_CACHE.computeIfAbsent(speciesId, id -> {
            ResourceLocation customTexture = ResourceLocation.fromNamespaceAndPath(
                    Beemancer.MOD_ID,
                    "textures/entity/bee/" + id + "_bee.png"
            );

            // Check if texture exists
            if (Minecraft.getInstance().getResourceManager().getResource(customTexture).isPresent()) {
                return customTexture;
            }

            return VANILLA_BEE_TEXTURE;
        });
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hovered) {
            Minecraft mc = Minecraft.getInstance();

            if (unlocked) {
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(displayTitle.copy().withStyle(style -> style.withBold(true)));

                // Add breeding parents line if bee has parents
                Component breedingLine = getBreedingParentsLine();
                if (breedingLine != null) {
                    tooltipLines.add(breedingLine);
                }

                tooltipLines.add(displayDescription);

                graphics.renderTooltip(mc.font, tooltipLines, java.util.Optional.empty(), mouseX, mouseY);
            } else if (canUnlock) {
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(displayTitle);

                // Add breeding parents line if bee has parents
                Component breedingLine = getBreedingParentsLine();
                if (breedingLine != null) {
                    tooltipLines.add(breedingLine);
                }

                tooltipLines.add(Component.translatable("codex.beemancer.click_to_unlock"));

                graphics.renderTooltip(mc.font, tooltipLines, java.util.Optional.empty(), mouseX, mouseY);
            } else {
                // Node LOCKED - afficher "???" si SECRET, sinon le titre
                graphics.renderTooltip(mc.font, java.util.List.of(
                    displayTitle,
                    Component.translatable("codex.beemancer.complete_quest_first").withStyle(style -> style.withColor(0xFF6666))
                ), java.util.Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private Component getBreedingParentsLine() {
        String parent1 = node.getBreedingParent1();
        String parent2 = node.getBreedingParent2();

        if (parent1 == null || parent2 == null) {
            return null; // Base bees have no parents
        }

        // Get player's unlocked nodes
        Set<String> unlockedNodes = getUnlockedNodes();

        String parent1Display = isParentUnlocked(parent1, unlockedNodes)
            ? formatBeeName(parent1)
            : "???";
        String parent2Display = isParentUnlocked(parent2, unlockedNodes)
            ? formatBeeName(parent2)
            : "???";

        return Component.literal(parent1Display + " + " + parent2Display).withStyle(style -> style.withColor(0xAAAA55));
    }

    private Set<String> getUnlockedNodes() {
        if (Minecraft.getInstance().player != null) {
            CodexPlayerData data = Minecraft.getInstance().player.getData(BeemancerAttachments.CODEX_DATA);
            return data.getUnlockedNodes();
        }
        return Set.of();
    }

    private boolean isParentUnlocked(String parentId, Set<String> unlockedNodes) {
        // The node ID in unlockedNodes includes the page prefix (e.g., "bees:meadow_bee")
        String fullId = "bees:" + parentId;
        return unlockedNodes.contains(fullId);
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

    public static void clearTextureCache() {
        TEXTURE_CACHE.clear();
    }
}
