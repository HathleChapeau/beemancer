/**
 * ============================================================
 * [HoverbikePartScreen.java]
 * Description: Ecran de selection de modele pour une partie du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePart       | Enum partie          | Identification partie          |
 * | HoverbikePartModel  | Modele 3D            | Rendu preview                  |
 * | HoverbikeEntity     | Entite source        | Contexte edit mode             |
 * | ClientSetup         | Layer definitions    | Bake du modele pour preview    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEditModeHandler.java: Ouverture sur clic droit
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.client.model.hoverbike.ChassisPartModel;
import com.chapeau.beemancer.client.model.hoverbike.CoeurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.beemancer.client.model.hoverbike.PropulseurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.RadiateurPartModel;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.chapeau.beemancer.common.entity.mount.HoverbikePart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Ecran de visualisation/selection d'un modele de partie du hoverbike.
 * Affiche le modele 3D de la partie au centre, avec des fleches
 * gauche/droite (futures: navigation entre modeles) et un bouton retour.
 * Le joueur peut maintenir clic gauche pour faire tourner le modele.
 */
public class HoverbikePartScreen extends Screen {

    private static final int BG_COLOR = 0xCC000000;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int TITLE_COLOR = 0xFFFFAA00;
    private static final int ARROW_COLOR = 0xFFAAAAAA;
    private static final int ARROW_HOVER_COLOR = 0xFFFFFFFF;
    private static final int CLOSE_COLOR = 0xFFFF5555;
    private static final int CLOSE_HOVER_COLOR = 0xFFFF8888;

    private final HoverbikePart partType;
    private final HoverbikeEntity hoverbike;
    private HoverbikePartModel partModel;

    private float rotationY = 30f;
    private float rotationX = -20f;
    private boolean dragging = false;

    // Layout
    private int panelX, panelY, panelW, panelH;
    private int arrowLeftX, arrowRightX, arrowY, arrowSize;
    private int closeX, closeY, closeW, closeH;

    public HoverbikePartScreen(HoverbikePart partType, HoverbikeEntity hoverbike) {
        super(Component.literal(partType.name()));
        this.partType = partType;
        this.hoverbike = hoverbike;
    }

    @Override
    protected void init() {
        super.init();

        // Bake le modele de la partie
        Minecraft mc = Minecraft.getInstance();
        ModelPart root = switch (partType) {
            case CHASSIS -> mc.getEntityModels().bakeLayer(ChassisPartModel.LAYER_LOCATION);
            case COEUR -> mc.getEntityModels().bakeLayer(CoeurPartModel.LAYER_LOCATION);
            case PROPULSEUR -> mc.getEntityModels().bakeLayer(PropulseurPartModel.LAYER_LOCATION);
            case RADIATEUR -> mc.getEntityModels().bakeLayer(RadiateurPartModel.LAYER_LOCATION);
        };
        partModel = switch (partType) {
            case CHASSIS -> new ChassisPartModel(root);
            case COEUR -> new CoeurPartModel(root);
            case PROPULSEUR -> new PropulseurPartModel(root);
            case RADIATEUR -> new RadiateurPartModel(root);
        };

        // Layout du panneau central (60% de la hauteur, 40% de la largeur)
        panelW = Math.min(300, (int) (width * 0.4));
        panelH = Math.min(350, (int) (height * 0.6));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        // Fleches gauche/droite
        arrowSize = 30;
        arrowLeftX = panelX + 15;
        arrowRightX = panelX + panelW - 15 - arrowSize;
        arrowY = panelY + panelH / 2 - arrowSize / 2;

        // Bouton fermer en bas au centre
        closeW = 40;
        closeH = 20;
        closeX = width / 2 - closeW / 2;
        closeY = panelY + panelH - 35;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // Fond semi-transparent
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        drawBorder(graphics, panelX, panelY, panelW, panelH, BORDER_COLOR);

        // Titre
        String title = partType.name();
        int titleWidth = font.width(title);
        graphics.drawString(font, title, width / 2 - titleWidth / 2, panelY + 8, TITLE_COLOR, false);

        // Ligne separatrice sous le titre
        graphics.fill(panelX + 4, panelY + 20, panelX + panelW - 4, panelY + 21, BORDER_COLOR);

        // Rendu du modele 3D au centre
        renderPartModel(graphics, partialTick);

        // Fleche gauche
        boolean hoverLeft = isInside(mouseX, mouseY, arrowLeftX, arrowY, arrowSize, arrowSize);
        int leftColor = hoverLeft ? ARROW_HOVER_COLOR : ARROW_COLOR;
        graphics.drawString(font, "<", arrowLeftX + arrowSize / 2 - 3, arrowY + arrowSize / 2 - 4, leftColor, true);

        // Fleche droite
        boolean hoverRight = isInside(mouseX, mouseY, arrowRightX, arrowY, arrowSize, arrowSize);
        int rightColor = hoverRight ? ARROW_HOVER_COLOR : ARROW_COLOR;
        graphics.drawString(font, ">", arrowRightX + arrowSize / 2 - 3, arrowY + arrowSize / 2 - 4, rightColor, true);

        // Bouton retour (fleche gauche en bas)
        boolean hoverClose = isInside(mouseX, mouseY, closeX, closeY, closeW, closeH);
        int closeBg = hoverClose ? CLOSE_HOVER_COLOR : CLOSE_COLOR;
        graphics.fill(closeX, closeY, closeX + closeW, closeY + closeH, closeBg);
        drawBorder(graphics, closeX, closeY, closeW, closeH, BORDER_COLOR);
        String closeText = "<-";
        int closeTextW = font.width(closeText);
        graphics.drawString(font, closeText, closeX + closeW / 2 - closeTextW / 2,
                closeY + closeH / 2 - 4, 0xFFFFFFFF, false);
    }

    /**
     * Rend le modele 3D de la partie au centre du panneau.
     */
    private void renderPartModel(GuiGraphics graphics, float partialTick) {
        if (partModel == null) return;

        int centerX = width / 2;
        int centerY = panelY + panelH / 2 + 10;
        float scale = 4.0f;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Positionner au centre de l'ecran
        poseStack.translate(centerX, centerY, 150);
        poseStack.scale(scale, scale, scale);

        // Appliquer la rotation utilisateur
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        // Flip Y pour correspondre a la convention modele
        poseStack.scale(-1, -1, 1);

        // Rendre le modele
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        ResourceLocation texture = partModel.getTextureLocation();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        partModel.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);

        bufferSource.endBatch();
        poseStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Clic gauche : commencer a drag pour rotation
        if (button == 0) {
            dragging = true;
            return true;
        }
        // Clic droit sur bouton fermer
        if (button == 0 && isInside((int) mouseX, (int) mouseY, closeX, closeY, closeW, closeH)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Verifier si on a clique sans drag sur le bouton fermer
            if (isInside((int) mouseX, (int) mouseY, closeX, closeY, closeW, closeH)) {
                onClose();
                return true;
            }
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragging) {
            rotationY += (float) dragX * 0.8f;
            rotationX += (float) dragY * 0.8f;
            rotationX = Math.max(-90f, Math.min(90f, rotationX));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
