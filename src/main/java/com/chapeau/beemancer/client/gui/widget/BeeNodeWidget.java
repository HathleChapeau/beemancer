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
import com.chapeau.beemancer.common.codex.CodexNode;
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

import java.util.HashMap;
import java.util.Map;

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
    private boolean unlocked;
    private boolean canUnlock;
    private boolean hovered;

    private static BeeModel<?> beeModel;

    public BeeNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock) {
        super(screenX, screenY, NODE_SIZE, NODE_SIZE, node.getTitle());
        this.node = node;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;

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
        poseStack.translate(centerX, centerY + 3, 100);

        // Scale up bee model (5x zoom)
        float scale = 40.0f;
        poseStack.scale(scale, scale, scale);

        // Flip and rotate for display
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-20));
        poseStack.mulPose(Axis.YP.rotationDegrees(45 + (hovered ? partialTick * 2 : 0)));

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

            // Format species name nicely
            String displayName = speciesId.substring(0, 1).toUpperCase() + speciesId.substring(1) + " Bee";

            if (unlocked) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.literal(displayName),
                    node.getDescription()
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else if (canUnlock) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.literal(displayName),
                    Component.translatable("codex.beemancer.click_to_unlock")
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    Component.translatable("codex.beemancer.locked"),
                    Component.translatable("codex.beemancer.unlock_parent_first")
                ), java.util.Optional.empty(), mouseX, mouseY);
            }
        }
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
