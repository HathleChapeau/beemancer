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
 * | GuiRenderHelper     | Rendu vanilla        | Fond container, slots, boutons |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEditModeHandler.java: Ouverture sur clic droit
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.client.model.hoverbike.ChassisPartModel;
import com.chapeau.beemancer.client.model.hoverbike.CoeurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.beemancer.client.model.hoverbike.PropulseurPartModel;
import com.chapeau.beemancer.client.model.hoverbike.RadiateurPartModel;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.chapeau.beemancer.common.entity.mount.HoverbikePart;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
 * Utilise les widgets et couleurs vanilla Minecraft (fond container gris,
 * boutons standards, inset slot). Le joueur peut maintenir clic gauche
 * pour faire tourner le modele de la piece.
 */
public class HoverbikePartScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 170;
    private static final int INSET_SIZE = 100;
    private static final int TITLE_Y_OFFSET = 7;
    private static final int BUTTON_HEIGHT = 20;

    // Couleurs vanilla container
    private static final int CONTAINER_BG = 0xFFC6C6C6;
    private static final int TITLE_COLOR = 0x404040;
    private static final int INSET_BG = 0xFF8B8B8B;
    private static final int INSET_BORDER_DARK = 0xFF373737;
    private static final int INSET_BORDER_LIGHT = 0xFFFFFFFF;

    private final HoverbikePart partType;
    private final HoverbikeEntity hoverbike;
    private HoverbikePartModel partModel;

    private float rotationY = 30f;
    private float rotationX = -20f;
    private boolean dragging = false;

    // Layout calcule dans init()
    private int panelX, panelY;
    private int insetX, insetY;

    public HoverbikePartScreen(HoverbikePart partType, HoverbikeEntity hoverbike) {
        super(Component.literal(formatPartName(partType)));
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

        // Layout centre
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        insetX = panelX + (PANEL_WIDTH - INSET_SIZE) / 2;
        insetY = panelY + 22;

        // Fleche gauche (vanilla Button)
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            // Navigation modele precedent (futur)
        }).bounds(panelX + 8, insetY + INSET_SIZE / 2 - BUTTON_HEIGHT / 2, 20, BUTTON_HEIGHT).build());

        // Fleche droite (vanilla Button)
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            // Navigation modele suivant (futur)
        }).bounds(panelX + PANEL_WIDTH - 28, insetY + INSET_SIZE / 2 - BUTTON_HEIGHT / 2, 20, BUTTON_HEIGHT).build());

        // Bouton retour (vanilla Button, translatable)
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> {
            onClose();
        }).bounds(width / 2 - 40, panelY + PANEL_HEIGHT - 30, 80, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Fond assombri vanilla
        renderBackground(graphics, mouseX, mouseY, partialTick);

        // Panneau container vanilla (fond gris + bordures 3D)
        GuiRenderHelper.renderContainerBackgroundNoTitle(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Titre centre (couleur vanilla 0x404040 sur fond gris)
        graphics.drawCenteredString(font, title, width / 2, panelY + TITLE_Y_OFFSET, TITLE_COLOR);

        // Separateur sous le titre
        graphics.fill(panelX + 7, panelY + 18, panelX + PANEL_WIDTH - 7, panelY + 19, 0xFF8B8B8B);

        // Zone inset pour le modele (style slot agrandi)
        renderInset(graphics, insetX, insetY, INSET_SIZE, INSET_SIZE);

        // Rendu du modele 3D
        renderPartModel(graphics, partialTick);

        // Widgets (boutons vanilla) rendus par super
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /**
     * Rend une zone inset style slot Minecraft (bord sombre en haut-gauche,
     * bord clair en bas-droite, fond gris fonce).
     */
    private void renderInset(GuiGraphics g, int x, int y, int w, int h) {
        // Fond sombre
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INSET_BG);
        // Bord haut et gauche (sombre = inset)
        g.fill(x, y, x + w, y + 1, INSET_BORDER_DARK);
        g.fill(x, y, x + 1, y + h, INSET_BORDER_DARK);
        // Bord bas et droit (clair = inset)
        g.fill(x + 1, y + h - 1, x + w, y + h, INSET_BORDER_LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, INSET_BORDER_LIGHT);
    }

    /**
     * Rend le modele 3D de la partie au centre de la zone inset.
     * Utilise le lighting vanilla pour les entites en inventaire.
     */
    private void renderPartModel(GuiGraphics graphics, float partialTick) {
        if (partModel == null) return;

        int centerX = insetX + INSET_SIZE / 2;
        int centerY = insetY + INSET_SIZE / 2 + 10;
        float scale = 50f;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Positionner au centre de l'inset
        poseStack.translate(centerX, centerY, 100);
        poseStack.scale(scale, scale, -scale);

        // Rotation utilisateur
        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        // Centrer le modele (decaler vers son centre approximatif)
        poseStack.translate(0, -0.7, 0);

        // Lighting vanilla pour entites en inventaire
        Lighting.setupForEntityInInventory();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        ResourceLocation texture = partModel.getTextureLocation();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        partModel.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);

        bufferSource.endBatch();

        // Restaurer le lighting
        Lighting.setupFor3DItems();

        poseStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Laisser les widgets vanilla gerer leurs clics d'abord
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        // Clic gauche dans l'inset : commencer drag rotation
        if (button == 0 && isInsideInset((int) mouseX, (int) mouseY)) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
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

    private boolean isInsideInset(int mx, int my) {
        return mx >= insetX && mx < insetX + INSET_SIZE
                && my >= insetY && my < insetY + INSET_SIZE;
    }

    /**
     * Formatte le nom de la partie avec majuscule initiale.
     */
    private static String formatPartName(HoverbikePart part) {
        String name = part.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
