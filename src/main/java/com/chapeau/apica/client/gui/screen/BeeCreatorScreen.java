/**
 * ============================================================
 * [BeeCreatorScreen.java]
 * Description: Ecran du Bee Creator — selection de parties, couleurs et body type avec preview 3D
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | BeeCreatorMenu           | Menu associe         | ContainerData sync             |
 * | BeeCreatorBlockEntity    | DATA_COUNT, slots    | Body type slot index           |
 * | BeePart                  | Enum parties         | Liste des parties              |
 * | BeeBodyType              | Types de corps       | Selecteur body                 |
 * | BeeCreatorUpdatePacket   | Packet C2S           | Envoi couleur/body au serveur  |
 * | ApicaBeeModel            | Modele customisable  | Preview 3D tintee              |
 * | BeeModel                 | Modele vanilla       | Preview reference              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (registerScreens)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.block.beecreator.BeeCreatorBlockEntity;
import com.chapeau.apica.common.block.beecreator.BeePart;
import com.chapeau.apica.common.menu.BeeCreatorMenu;
import com.chapeau.apica.core.network.packets.BeeCreatorUpdatePacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GUI du Bee Creator. Layout:
 * - Gauche: grande preview 3D (150x150) avec drag rotation
 * - Droite: selecteur body type + 7 champs hex couleur
 * - Bas droite: toggles Vanilla/XYZ
 */
public class BeeCreatorScreen extends AbstractContainerScreen<BeeCreatorMenu> {

    private static final int GUI_W = 320;
    private static final int GUI_H = 210;

    // Preview (gauche)
    private static final int PREVIEW_X = 8;
    private static final int PREVIEW_Y = 22;
    private static final int PREVIEW_SIZE = 150;
    private static final int VANILLA_SIZE = 50;

    // Panel couleurs (droite)
    private static final int PANEL_X = PREVIEW_X + PREVIEW_SIZE + 10;
    private static final int PANEL_W = GUI_W - PANEL_X - 6;
    private static final int ROW_H = 20;
    private static final int BODY_SELECTOR_Y = 22;
    private static final int COLORS_START_Y = 44;

    // Couleurs UI
    private static final int COL_BG = 0xCC1A1A2E;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_DARK = 0xFF111122;
    private static final int COL_LABEL = 0xFFDDDDDD;
    private static final int COL_TITLE = 0xFFE8A317;
    private static final int COL_FIELD_BORDER = 0xFF444466;
    private static final int COL_ACCENT = 0xFF333355;
    private static final int GIZMO_LENGTH = 20;

    private static final ResourceLocation VANILLA_BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    private final EditBox[] hexFields = new EditBox[BeePart.COUNT];
    private final int[] localColors = new int[BeePart.COUNT];
    private ApicaBeeModel<?> beeModel;
    private BeeModel<?> vanillaBeeModel;
    private BeeBodyType currentBodyType = BeeBodyType.DEFAULT;

    private float dragRotationY = 25f;
    private float dragRotationX = 160f;
    private boolean isDraggingPreview;
    private double lastDragX;
    private double lastDragY;
    private boolean showGizmo;
    private boolean showVanilla;

    public BeeCreatorScreen(BeeCreatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
        this.inventoryLabelY = GUI_H + 1;
        this.titleLabelY = -999;
        for (BeePart part : BeePart.values()) {
            localColors[part.getIndex()] = part.getDefaultColor();
        }
    }

    @Override
    protected void init() {
        super.init();

        currentBodyType = menu.getBodyType();
        rebuildBeeModel();
        var entityModels = Minecraft.getInstance().getEntityModels();
        vanillaBeeModel = new BeeModel<>(entityModels.bakeLayer(ModelLayers.BEE));

        int gx = this.leftPos;
        int gy = this.topPos;

        // Body selector: [<] Default [>]
        int selectorX = gx + PANEL_X;
        int selectorY = gy + BODY_SELECTOR_Y;
        addRenderableWidget(Button.builder(Component.literal("<"), btn -> cycleBody(-1))
                .bounds(selectorX, selectorY, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), btn -> cycleBody(1))
                .bounds(selectorX + PANEL_W - 14, selectorY, 14, 14).build());

        // Color fields
        for (BeePart part : BeePart.values()) {
            int rowY = gy + COLORS_START_Y + part.getIndex() * ROW_H + 2;
            int fieldX = gx + PANEL_X + 50;

            EditBox field = new EditBox(font, fieldX, rowY, 68, 14, Component.empty());
            field.setMaxLength(7);
            field.setBordered(true);
            int syncedColor = menu.getPartColor(part);
            int color = syncedColor != 0 ? syncedColor : part.getDefaultColor();
            localColors[part.getIndex()] = color;
            field.setValue(toHex(color));
            field.setTextColor(0xFFFFFF);

            final int partIndex = part.getIndex();
            field.setResponder(text -> onHexFieldChanged(partIndex, text));
            hexFields[part.getIndex()] = field;
            addRenderableWidget(field);
        }

        // Toggle buttons sous les couleurs
        int btnsY = gy + COLORS_START_Y + BeePart.COUNT * ROW_H + 6;
        addRenderableWidget(Button.builder(Component.literal("Vanilla"), btn -> showVanilla = !showVanilla)
                .bounds(gx + PANEL_X, btnsY, 42, 14).build());
        addRenderableWidget(Button.builder(Component.literal("XYZ"), btn -> showGizmo = !showGizmo)
                .bounds(gx + PANEL_X + 46, btnsY, 24, 14).build());
    }

    private void cycleBody(int direction) {
        BeeBodyType next = direction > 0 ? currentBodyType.next() : currentBodyType.prev();
        currentBodyType = next;
        rebuildBeeModel();
        PacketDistributor.sendToServer(new BeeCreatorUpdatePacket(
                menu.getBlockPos(), BeeCreatorBlockEntity.BODY_TYPE_SLOT, next.getIndex()));
    }

    private void rebuildBeeModel() {
        var entityModels = Minecraft.getInstance().getEntityModels();
        ModelPart bodyRoot = entityModels.bakeLayer(ApicaBeeModel.getBodyLayer(currentBodyType));
        ModelPart wingRoot = entityModels.bakeLayer(ApicaBeeModel.WING_LAYER);
        ModelPart stingerRoot = entityModels.bakeLayer(ApicaBeeModel.STINGER_LAYER);
        beeModel = new ApicaBeeModel<>(bodyRoot, wingRoot, stingerRoot, currentBodyType);
    }

    private void onHexFieldChanged(int partIndex, String text) {
        int parsed = parseHex(text);
        if (parsed >= 0) {
            localColors[partIndex] = parsed;
            PacketDistributor.sendToServer(new BeeCreatorUpdatePacket(
                    menu.getBlockPos(), partIndex, parsed));
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        for (BeePart part : BeePart.values()) {
            int serverColor = menu.getPartColor(part);
            if (serverColor != 0 && serverColor != localColors[part.getIndex()]) {
                localColors[part.getIndex()] = serverColor;
                EditBox field = hexFields[part.getIndex()];
                if (field != null && !field.isFocused()) {
                    field.setValue(toHex(serverColor));
                }
            }
        }
        BeeBodyType serverBodyType = menu.getBodyType();
        if (serverBodyType != currentBodyType) {
            currentBodyType = serverBodyType;
            rebuildBeeModel();
        }
    }

    // ========== Rendering ==========

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int gx = this.leftPos;
        int gy = this.topPos;

        // Fond principal
        gfx.fill(gx - 2, gy - 2, gx + GUI_W + 2, gy + GUI_H + 2, COL_BORDER);
        gfx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);

        // Titre
        gfx.drawString(font, Component.translatable("block.apica.bee_creator"),
                gx + 8, gy + 8, COL_TITLE, false);

        // === PREVIEW PANEL (gauche) ===
        int px = gx + PREVIEW_X;
        int py = gy + PREVIEW_Y;
        gfx.fill(px - 1, py - 1, px + PREVIEW_SIZE + 1, py + PREVIEW_SIZE + 1, COL_BORDER);
        gfx.fill(px, py, px + PREVIEW_SIZE, py + PREVIEW_SIZE, COL_DARK);

        renderBeePreview(gfx, px, py, PREVIEW_SIZE, PREVIEW_SIZE, partialTick);

        // Mini vanilla overlay (coin haut-droit de la preview)
        if (showVanilla) {
            int vx = px + PREVIEW_SIZE - VANILLA_SIZE;
            int vy = py;
            gfx.fill(vx - 1, vy - 1, vx + VANILLA_SIZE + 1, vy + VANILLA_SIZE + 1, COL_BORDER);
            gfx.fill(vx, vy, vx + VANILLA_SIZE, vy + VANILLA_SIZE, COL_DARK);
            renderVanillaPreview(gfx, vx, vy, VANILLA_SIZE, VANILLA_SIZE);
        }

        // === CONTROLS PANEL (droite) ===
        int cpx = gx + PANEL_X;

        // Body selector label
        gfx.drawCenteredString(font, currentBodyType.getDisplayName(),
                cpx + PANEL_W / 2, gy + BODY_SELECTOR_Y + 3, COL_LABEL);

        // Separateur sous body selector
        gfx.fill(cpx, gy + BODY_SELECTOR_Y + 16, cpx + PANEL_W, gy + BODY_SELECTOR_Y + 17, COL_ACCENT);

        // Color rows
        for (BeePart part : BeePart.values()) {
            int rowY = gy + COLORS_START_Y + part.getIndex() * ROW_H;

            // Alternance fond
            if (part.getIndex() % 2 == 0) {
                gfx.fill(cpx, rowY, cpx + PANEL_W, rowY + ROW_H, 0x15FFFFFF);
            }

            // Label
            gfx.drawString(font, part.getDisplayName(), cpx + 2, rowY + 4, COL_LABEL, false);

            // Color swatch apres le champ hex
            int swatchX = cpx + 50 + 70;
            int swatchY = rowY + 2;
            int color = localColors[part.getIndex()];
            gfx.fill(swatchX, swatchY, swatchX + 14, swatchY + 14, COL_FIELD_BORDER);
            gfx.fill(swatchX + 1, swatchY + 1, swatchX + 13, swatchY + 13, 0xFF000000 | color);
        }

        // Info sous la preview
        gfx.drawString(font, "Drag to rotate", px + 2, py + PREVIEW_SIZE + 3, 0xFF666666, false);
    }

    // ========== Preview ApicaBee ==========

    private void renderBeePreview(GuiGraphics gfx, int x, int y, int w, int h, float partialTick) {
        if (beeModel == null) return;

        int centerX = x + w / 2;
        int centerY = y + h / 2;
        float scale = 48f;

        ResourceLocation bodyTex = ApicaBeeModel.getBodyTexture(currentBodyType);

        gfx.enableScissor(x, y, x + w, y + h);
        gfx.pose().pushPose();
        gfx.pose().translate(centerX, centerY, 50.0f);
        gfx.pose().scale(scale, scale, scale);
        gfx.pose().mulPose(Axis.XP.rotationDegrees(dragRotationX));
        gfx.pose().mulPose(Axis.YP.rotationDegrees(dragRotationY));
        gfx.pose().translate(0.0f, -1.15625f, 0.0f);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderType bodyRT = RenderType.entityCutout(bodyTex);

        beeModel.showCorpusOnly();
        beeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(bodyRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.BODY.getIndex()]));

        beeModel.showStripeOnly();
        beeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(bodyRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.STRIPE.getIndex()]));

        beeModel.showEyesOnly();
        beeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(bodyRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.EYE.getIndex()]));

        beeModel.showPupilsOnly();
        beeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(bodyRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.PUPIL.getIndex()]));

        beeModel.showUntintedOnly();
        beeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(bodyRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.STRIPE.getIndex()]));

        RenderType wingRT = RenderType.entityCutout(ApicaBeeModel.WING_TEXTURE);
        beeModel.renderWings(gfx.pose(), bufferSource.getBuffer(wingRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.WING.getIndex()]));

        RenderType stingerRT = RenderType.entityCutout(ApicaBeeModel.STINGER_TEXTURE);
        beeModel.renderStinger(gfx.pose(), bufferSource.getBuffer(stingerRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.STINGER.getIndex()]));

        bufferSource.endBatch();
        Lighting.setupFor3DItems();
        gfx.pose().popPose();

        if (showGizmo) {
            renderGizmo(gfx, centerX, centerY);
        }

        gfx.disableScissor();
    }

    // ========== Preview Vanilla ==========

    private void renderVanillaPreview(GuiGraphics gfx, int x, int y, int w, int h) {
        if (vanillaBeeModel == null) return;

        int centerX = x + w / 2;
        int centerY = y + h / 2;
        float scale = 38f;

        gfx.enableScissor(x, y, x + w, y + h);
        gfx.pose().pushPose();
        gfx.pose().translate(centerX, centerY, 50.0f);
        gfx.pose().scale(scale, scale, scale);
        gfx.pose().mulPose(Axis.XP.rotationDegrees(dragRotationX));
        gfx.pose().mulPose(Axis.YP.rotationDegrees(dragRotationY));
        gfx.pose().translate(0.0f, -1.15625f, 0.0f);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderType rt = RenderType.entityCutout(VANILLA_BEE_TEXTURE);
        vanillaBeeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(rt),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        bufferSource.endBatch();
        Lighting.setupFor3DItems();
        gfx.pose().popPose();
        gfx.disableScissor();
    }

    // ========== Gizmo ==========

    private void renderGizmo(GuiGraphics gfx, int cx, int cy) {
        gfx.pose().pushPose();
        gfx.pose().setIdentity();
        gfx.pose().mulPose(Axis.XP.rotationDegrees(dragRotationX));
        gfx.pose().mulPose(Axis.YP.rotationDegrees(dragRotationY));
        org.joml.Matrix4f mat = gfx.pose().last().pose();

        float rxSX = mat.m00() * GIZMO_LENGTH, rxSY = mat.m10() * GIZMO_LENGTH;
        float rySX = mat.m01() * GIZMO_LENGTH, rySY = mat.m11() * GIZMO_LENGTH;
        float rzSX = mat.m02() * GIZMO_LENGTH, rzSY = mat.m12() * GIZMO_LENGTH;

        gfx.pose().popPose();

        drawGizmoLine(gfx, cx, cy, cx + (int) rxSX, cy + (int) rxSY, 0xFFFF4444, "X");
        drawGizmoLine(gfx, cx, cy, cx + (int) rySX, cy + (int) rySY, 0xFF44FF44, "Y");
        drawGizmoLine(gfx, cx, cy, cx + (int) rzSX, cy + (int) rzSY, 0xFF4488FF, "Z");
    }

    private void drawGizmoLine(GuiGraphics gfx, int x0, int y0, int x1, int y1, int color, String label) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int px = x0, py = y0;
        while (true) {
            gfx.fill(px, py, px + 1, py + 1, color);
            if (px == x1 && py == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; px += sx; }
            if (e2 < dx) { err += dx; py += sy; }
        }
        gfx.drawString(font, label, x1 + 2, y1 - 4, color, false);
    }

    // ========== Labels / Render ==========

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    // ========== Input ==========

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInsidePreview(mouseX, mouseY)) {
            isDraggingPreview = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && isDraggingPreview) {
            dragRotationY -= (float) (mouseX - lastDragX);
            dragRotationX += (float) (mouseY - lastDragY);
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingPreview) {
            isDraggingPreview = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isInsidePreview(double mouseX, double mouseY) {
        int px = this.leftPos + PREVIEW_X;
        int py = this.topPos + PREVIEW_Y;
        return mouseX >= px && mouseX < px + PREVIEW_SIZE
                && mouseY >= py && mouseY < py + PREVIEW_SIZE;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox field : hexFields) {
            if (field != null && field.isFocused()) {
                if (keyCode == 256) {
                    field.setFocused(false);
                    return true;
                }
                return field.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (EditBox field : hexFields) {
            if (field != null && field.isFocused()) {
                return field.charTyped(chr, modifiers);
            }
        }
        return super.charTyped(chr, modifiers);
    }

    // ========== Utilities ==========

    private static String toHex(int color) {
        return String.format("#%06X", color & 0xFFFFFF);
    }

    private static int parseHex(String text) {
        String cleaned = text.startsWith("#") ? text.substring(1) : text;
        if (cleaned.length() != 6) return -1;
        try {
            return Integer.parseUnsignedInt(cleaned, 16);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
