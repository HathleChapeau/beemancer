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
 * | HoverbikePartVariants| Registre variantes  | Liste dynamique des modeles    |
 * | HoverbikeEntity     | Entite source        | Lecture/ecriture variante      |
 * | HoverbikeVariantPacket| Packet reseau      | Envoi selection au serveur     |
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
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.beemancer.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.beemancer.common.entity.mount.HoverbikeEntity;
import com.chapeau.beemancer.common.entity.mount.HoverbikePart;
import com.chapeau.beemancer.core.network.packets.HoverbikeVariantPacket;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Ecran de visualisation/selection d'un modele de partie du hoverbike.
 * Le nom de la variante est affiche sous la zone de preview.
 * Les fleches gauche/droite changent la variante et envoient un packet au serveur.
 */
public class HoverbikePartScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 190;
    private static final int INSET_SIZE = 100;
    private static final int TITLE_Y_OFFSET = 7;
    private static final int BUTTON_HEIGHT = 20;

    private static final int TITLE_COLOR = 0x404040;
    private static final int INSET_BG = 0xFF8B8B8B;
    private static final int INSET_BORDER_DARK = 0xFF373737;
    private static final int INSET_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int VARIANT_NAME_COLOR = 0xFF333333;

    private final HoverbikePart partType;
    private final HoverbikeEntity hoverbike;

    private int currentVariantIndex;
    private HoverbikePartModel partModel;
    private List<HoverbikePartVariants.VariantEntry> variants;

    private float rotationY = 30f;
    private float rotationX = -20f;
    private boolean dragging = false;

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

        variants = HoverbikePartVariants.getVariants(partType);
        currentVariantIndex = hoverbike.getPartVariant(partType);
        if (variants.isEmpty()) return;

        // Clamp l'index
        currentVariantIndex = Math.floorMod(currentVariantIndex, variants.size());

        bakeCurrentModel();

        // Layout
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        insetX = panelX + (PANEL_WIDTH - INSET_SIZE) / 2;
        insetY = panelY + 22;

        // Fleche gauche
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
            cycleVariant(-1);
        }).bounds(panelX + 8, insetY + INSET_SIZE / 2 - BUTTON_HEIGHT / 2, 20, BUTTON_HEIGHT).build());

        // Fleche droite
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
            cycleVariant(1);
        }).bounds(panelX + PANEL_WIDTH - 28, insetY + INSET_SIZE / 2 - BUTTON_HEIGHT / 2, 20, BUTTON_HEIGHT).build());

        // Bouton retour
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> {
            onClose();
        }).bounds(width / 2 - 40, panelY + PANEL_HEIGHT - 30, 80, BUTTON_HEIGHT).build());
    }

    /**
     * Change la variante de modele et envoie le packet au serveur.
     */
    private void cycleVariant(int direction) {
        if (variants.isEmpty()) return;
        currentVariantIndex = Math.floorMod(currentVariantIndex + direction, variants.size());
        bakeCurrentModel();

        // Envoyer au serveur
        PacketDistributor.sendToServer(new HoverbikeVariantPacket(
                hoverbike.getId(), partType.ordinal(), currentVariantIndex));
    }

    /**
     * Bake le modele de la variante courante pour le preview.
     */
    private void bakeCurrentModel() {
        if (variants.isEmpty()) return;
        HoverbikePartVariants.VariantEntry entry = variants.get(currentVariantIndex);
        Minecraft mc = Minecraft.getInstance();
        ModelPart root = mc.getEntityModels().bakeLayer(entry.factory().layerLocation());
        partModel = entry.factory().constructor().apply(root);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Widgets (boutons) rendus en premier par super
        super.render(graphics, mouseX, mouseY, partialTick);

        // Panneau container vanilla
        GuiRenderHelper.renderContainerBackgroundNoTitle(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        // Titre centre
        graphics.drawCenteredString(font, title, width / 2, panelY + TITLE_Y_OFFSET, TITLE_COLOR);

        // Separateur sous le titre
        graphics.fill(panelX + 7, panelY + 18, panelX + PANEL_WIDTH - 7, panelY + 19, 0xFF8B8B8B);

        // Zone inset pour le modele
        renderInset(graphics, insetX, insetY, INSET_SIZE, INSET_SIZE);

        // Rendu du modele 3D
        renderPartModel(graphics, partialTick);

        // Nom de la variante sous l'inset
        if (!variants.isEmpty()) {
            String variantName = variants.get(currentVariantIndex).name();
            String displayText = variantName + " (" + (currentVariantIndex + 1) + "/" + variants.size() + ")";
            graphics.drawCenteredString(font, displayText, width / 2, insetY + INSET_SIZE + 4, VARIANT_NAME_COLOR);
        }
    }

    private void renderInset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, INSET_BG);
        g.fill(x, y, x + w, y + 1, INSET_BORDER_DARK);
        g.fill(x, y, x + 1, y + h, INSET_BORDER_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, INSET_BORDER_LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, INSET_BORDER_LIGHT);
    }

    private void renderPartModel(GuiGraphics graphics, float partialTick) {
        if (partModel == null) return;

        int centerX = insetX + INSET_SIZE / 2;
        int centerY = insetY + INSET_SIZE / 2 + 10;
        float scale = 50f;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(centerX, centerY, 100);
        poseStack.scale(scale, scale, -scale);

        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        poseStack.translate(0, -0.7, 0);

        Lighting.setupForEntityInInventory();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        ResourceLocation texture = partModel.getTextureLocation();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));

        partModel.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY);

        bufferSource.endBatch();

        Lighting.setupFor3DItems();

        poseStack.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
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
            rotationY -= (float) dragX * 0.8f;
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

    private static String formatPartName(HoverbikePart part) {
        String name = part.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
