/**
 * ============================================================
 * [BeeCreatorScreen.java]
 * Description: Ecran du Bee Creator — selection de parties, couleurs et types avec preview 3D
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | BeeCreatorMenu           | Menu associe         | ContainerData sync             |
 * | BeeCreatorBlockEntity    | Slot constants       | Body/Wing/Stinger type slots   |
 * | BeePart                  | Enum parties         | Liste des parties              |
 * | BeeBodyType              | Types de corps       | Selecteur body                 |
 * | BeeWingType              | Types d'ailes        | Selecteur ailes                |
 * | BeeStingerType           | Types de dard        | Selecteur dard                 |
 * | BeeCreatorUpdatePacket   | Packet C2S           | Envoi couleur/type au serveur  |
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
import com.chapeau.apica.common.block.beecreator.BeeStingerType;
import com.chapeau.apica.common.block.beecreator.BeeWingType;
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
 * GUI du Bee Creator. Layout (gauche a droite):
 * - Preview 3D (150x150) avec drag rotation
 * - Panel selecteurs (Body / Wings / Stinger) empiles verticalement
 * - Panel couleurs (7 champs hex)
 * Selecteurs: fire sur mouse release.
 */
public class BeeCreatorScreen extends AbstractContainerScreen<BeeCreatorMenu> {

    private static final int GUI_W = 360;
    private static final int GUI_H = 210;

    // Preview (gauche)
    private static final int PREVIEW_X = 6;
    private static final int PREVIEW_Y = 22;
    private static final int PREVIEW_SIZE = 150;
    private static final int VANILLA_SIZE = 50;

    // Panel selecteurs (milieu)
    private static final int SEL_X = PREVIEW_X + PREVIEW_SIZE + 8;
    private static final int SEL_W = 90;
    private static final int SEL_Y = 22;
    private static final int SEL_ROW_H = 28;
    private static final int SEL_ARROW_W = 14;
    private static final int SEL_ARROW_H = 14;

    // Panel couleurs (droite)
    private static final int COL_X = SEL_X + SEL_W + 6;
    private static final int COL_W = GUI_W - COL_X - 4;
    private static final int COL_Y = 22;
    private static final int COL_ROW_H = 20;

    // Couleurs UI
    private static final int C_BG = 0xCC1A1A2E;
    private static final int C_BORDER = 0xFF555555;
    private static final int C_DARK = 0xFF111122;
    private static final int C_LABEL = 0xFFDDDDDD;
    private static final int C_TITLE = 0xFFE8A317;
    private static final int C_SWATCH_BORDER = 0xFF444466;
    private static final int C_PANEL = 0xFF222244;
    private static final int C_ARROW = 0xFFAAAAAA;
    private static final int C_ARROW_HOVER = 0xFFE8A317;
    private static final int GIZMO_LENGTH = 20;

    private static final ResourceLocation VANILLA_BEE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/bee/bee.png");

    // Selector row indices
    private static final int SEL_BODY = 0;
    private static final int SEL_WING = 1;
    private static final int SEL_STINGER = 2;
    private static final int SEL_COUNT = 3;

    private final EditBox[] hexFields = new EditBox[BeePart.COUNT];
    private final int[] localColors = new int[BeePart.COUNT];
    private ApicaBeeModel<?> beeModel;
    private BeeModel<?> vanillaBeeModel;

    private BeeBodyType currentBodyType = BeeBodyType.DEFAULT;
    private BeeWingType currentWingType = BeeWingType.DEFAULT;
    private BeeStingerType currentStingerType = BeeStingerType.DEFAULT;

    private float dragRotationY = 25f;
    private float dragRotationX = 160f;
    private boolean isDraggingPreview;
    private double lastDragX;
    private double lastDragY;
    private boolean showGizmo;
    private boolean showVanilla;

    /** Pending selector action: which row was pressed, and direction. -1 = none. */
    private int pendingSelectorRow = -1;
    private int pendingSelectorDir = 0;

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
        currentWingType = menu.getWingType();
        currentStingerType = menu.getStingerType();
        rebuildBeeModel();

        var entityModels = Minecraft.getInstance().getEntityModels();
        vanillaBeeModel = new BeeModel<>(entityModels.bakeLayer(ModelLayers.BEE));

        int gx = this.leftPos;
        int gy = this.topPos;

        // Color fields
        for (BeePart part : BeePart.values()) {
            int rowY = gy + COL_Y + part.getIndex() * COL_ROW_H + 2;
            int fieldX = gx + COL_X + 46;

            EditBox field = new EditBox(font, fieldX, rowY, 58, 14, Component.empty());
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

        // Toggle buttons sous la preview
        int btnsY = gy + PREVIEW_Y + PREVIEW_SIZE + 4;
        int btnsX = gx + PREVIEW_X;
        addRenderableWidget(Button.builder(Component.literal("Vanilla"), btn -> showVanilla = !showVanilla)
                .bounds(btnsX, btnsY, 42, 14).build());
        addRenderableWidget(Button.builder(Component.literal("XYZ"), btn -> showGizmo = !showGizmo)
                .bounds(btnsX + 46, btnsY, 24, 14).build());
    }

    // ========== Selector logic (mouse release) ==========

    private String getSelectorLabel(int row) {
        return switch (row) {
            case SEL_BODY -> currentBodyType.getDisplayName();
            case SEL_WING -> currentWingType.getDisplayName();
            case SEL_STINGER -> currentStingerType.getDisplayName();
            default -> "";
        };
    }

    private String getSelectorTitle(int row) {
        return switch (row) {
            case SEL_BODY -> "Body";
            case SEL_WING -> "Wings";
            case SEL_STINGER -> "Stinger";
            default -> "";
        };
    }

    private void cycleSelector(int row, int direction) {
        switch (row) {
            case SEL_BODY -> {
                currentBodyType = direction > 0 ? currentBodyType.next() : currentBodyType.prev();
                rebuildBeeModel();
                PacketDistributor.sendToServer(new BeeCreatorUpdatePacket(
                        menu.getBlockPos(), BeeCreatorBlockEntity.BODY_TYPE_SLOT, currentBodyType.getIndex()));
            }
            case SEL_WING -> {
                currentWingType = direction > 0 ? currentWingType.next() : currentWingType.prev();
                rebuildBeeModel();
                PacketDistributor.sendToServer(new BeeCreatorUpdatePacket(
                        menu.getBlockPos(), BeeCreatorBlockEntity.WING_TYPE_SLOT, currentWingType.getIndex()));
            }
            case SEL_STINGER -> {
                currentStingerType = direction > 0 ? currentStingerType.next() : currentStingerType.prev();
                rebuildBeeModel();
                PacketDistributor.sendToServer(new BeeCreatorUpdatePacket(
                        menu.getBlockPos(), BeeCreatorBlockEntity.STINGER_TYPE_SLOT, currentStingerType.getIndex()));
            }
        }
    }

    /** Returns {row, direction} or null if not over an arrow. */
    private int[] hitTestSelectorArrow(double mouseX, double mouseY) {
        int gx = this.leftPos;
        int gy = this.topPos;
        for (int row = 0; row < SEL_COUNT; row++) {
            int ry = gy + SEL_Y + row * SEL_ROW_H + 12;
            int lx = gx + SEL_X;
            int rx = gx + SEL_X + SEL_W - SEL_ARROW_W;
            // Left arrow
            if (mouseX >= lx && mouseX < lx + SEL_ARROW_W && mouseY >= ry && mouseY < ry + SEL_ARROW_H) {
                return new int[]{row, -1};
            }
            // Right arrow
            if (mouseX >= rx && mouseX < rx + SEL_ARROW_W && mouseY >= ry && mouseY < ry + SEL_ARROW_H) {
                return new int[]{row, 1};
            }
        }
        return null;
    }

    private void rebuildBeeModel() {
        var entityModels = Minecraft.getInstance().getEntityModels();
        ModelPart bodyRoot = entityModels.bakeLayer(ApicaBeeModel.getBodyLayer(currentBodyType));
        ModelPart wingRoot = entityModels.bakeLayer(ApicaBeeModel.getWingLayer(currentWingType));
        ModelPart stingerRoot = entityModels.bakeLayer(ApicaBeeModel.getStingerLayer(currentStingerType));
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
        BeeBodyType sb = menu.getBodyType();
        BeeWingType sw = menu.getWingType();
        BeeStingerType ss = menu.getStingerType();
        boolean changed = false;
        if (sb != currentBodyType) { currentBodyType = sb; changed = true; }
        if (sw != currentWingType) { currentWingType = sw; changed = true; }
        if (ss != currentStingerType) { currentStingerType = ss; changed = true; }
        if (changed) rebuildBeeModel();
    }

    // ========== Rendering ==========

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int gx = this.leftPos;
        int gy = this.topPos;

        // Fond
        gfx.fill(gx - 2, gy - 2, gx + GUI_W + 2, gy + GUI_H + 2, C_BORDER);
        gfx.fill(gx, gy, gx + GUI_W, gy + GUI_H, C_BG);

        // Titre
        gfx.drawString(font, Component.translatable("block.apica.bee_creator"),
                gx + 8, gy + 8, C_TITLE, false);

        // === PREVIEW ===
        int px = gx + PREVIEW_X;
        int py = gy + PREVIEW_Y;
        gfx.fill(px - 1, py - 1, px + PREVIEW_SIZE + 1, py + PREVIEW_SIZE + 1, C_BORDER);
        gfx.fill(px, py, px + PREVIEW_SIZE, py + PREVIEW_SIZE, C_DARK);
        renderBeePreview(gfx, px, py, PREVIEW_SIZE, PREVIEW_SIZE, partialTick);

        if (showVanilla) {
            int vx = px + PREVIEW_SIZE - VANILLA_SIZE;
            int vy = py;
            gfx.fill(vx - 1, vy - 1, vx + VANILLA_SIZE + 1, vy + VANILLA_SIZE + 1, C_BORDER);
            gfx.fill(vx, vy, vx + VANILLA_SIZE, vy + VANILLA_SIZE, C_DARK);
            renderVanillaPreview(gfx, vx, vy, VANILLA_SIZE, VANILLA_SIZE);
        }

        // === SELECTOR PANEL ===
        int sx = gx + SEL_X;
        int sy = gy + SEL_Y;
        gfx.fill(sx - 1, sy - 1, sx + SEL_W + 1, sy + SEL_COUNT * SEL_ROW_H + 1, C_BORDER);
        gfx.fill(sx, sy, sx + SEL_W, sy + SEL_COUNT * SEL_ROW_H, C_PANEL);

        int[] hover = hitTestSelectorArrow(mouseX, mouseY);

        for (int row = 0; row < SEL_COUNT; row++) {
            int ry = sy + row * SEL_ROW_H;
            // Title
            gfx.drawCenteredString(font, getSelectorTitle(row), sx + SEL_W / 2, ry + 2, 0xFF888888);
            // Arrows
            int arrowY = ry + 12;
            boolean hoverLeft = hover != null && hover[0] == row && hover[1] == -1;
            boolean hoverRight = hover != null && hover[0] == row && hover[1] == 1;
            gfx.drawString(font, "<", sx + 3, arrowY + 2, hoverLeft ? C_ARROW_HOVER : C_ARROW, false);
            gfx.drawString(font, ">", sx + SEL_W - 10, arrowY + 2, hoverRight ? C_ARROW_HOVER : C_ARROW, false);
            // Value
            gfx.drawCenteredString(font, getSelectorLabel(row), sx + SEL_W / 2, arrowY + 2, C_LABEL);
            // Separator
            if (row < SEL_COUNT - 1) {
                gfx.fill(sx + 4, ry + SEL_ROW_H - 1, sx + SEL_W - 4, ry + SEL_ROW_H, 0x30FFFFFF);
            }
        }

        // === COLOR PANEL ===
        int cx = gx + COL_X;
        int cy = gy + COL_Y;
        for (BeePart part : BeePart.values()) {
            int rowY = cy + part.getIndex() * COL_ROW_H;
            if (part.getIndex() % 2 == 0) {
                gfx.fill(cx, rowY, cx + COL_W, rowY + COL_ROW_H, 0x15FFFFFF);
            }
            gfx.drawString(font, part.getDisplayName(), cx + 2, rowY + 4, C_LABEL, false);

            // Swatch
            int swX = cx + COL_W - 16;
            int swY = rowY + 2;
            int color = localColors[part.getIndex()];
            gfx.fill(swX, swY, swX + 14, swY + 14, C_SWATCH_BORDER);
            gfx.fill(swX + 1, swY + 1, swX + 13, swY + 13, 0xFF000000 | color);
        }
    }

    // ========== Preview ==========

    private void renderBeePreview(GuiGraphics gfx, int x, int y, int w, int h, float partialTick) {
        if (beeModel == null) return;

        int centerX = x + w / 2;
        int centerY = y + h / 2;
        float scale = 48f;

        ResourceLocation bodyTex = ApicaBeeModel.getBodyTexture(currentBodyType);
        ResourceLocation wingTex = ApicaBeeModel.getWingTexture(currentWingType);
        ResourceLocation stingerTex = ApicaBeeModel.getStingerTexture(currentStingerType);

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

        beeModel.showAntennaOnly();
        beeModel.renderToBuffer(gfx.pose(), bufferSource.getBuffer(bodyRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.ANTENNA.getIndex()]));

        RenderType wingRT = RenderType.entityCutout(wingTex);
        beeModel.renderWings(gfx.pose(), bufferSource.getBuffer(wingRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.WING.getIndex()]));

        RenderType stingerRT = RenderType.entityCutout(stingerTex);
        beeModel.renderStinger(gfx.pose(), bufferSource.getBuffer(stingerRT),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                toArgb(localColors[BeePart.STINGER.getIndex()]));

        bufferSource.endBatch();
        Lighting.setupFor3DItems();
        gfx.pose().popPose();

        if (showGizmo) renderGizmo(gfx, centerX, centerY);

        gfx.disableScissor();
    }

    private void renderVanillaPreview(GuiGraphics gfx, int x, int y, int w, int h) {
        if (vanillaBeeModel == null) return;

        int centerX = x + w / 2;
        int centerY = y + h / 2;

        gfx.enableScissor(x, y, x + w, y + h);
        gfx.pose().pushPose();
        gfx.pose().translate(centerX, centerY, 50.0f);
        gfx.pose().scale(38f, 38f, 38f);
        gfx.pose().mulPose(Axis.XP.rotationDegrees(dragRotationX));
        gfx.pose().mulPose(Axis.YP.rotationDegrees(dragRotationY));
        gfx.pose().translate(0.0f, -1.15625f, 0.0f);

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource buf = Minecraft.getInstance().renderBuffers().bufferSource();
        vanillaBeeModel.renderToBuffer(gfx.pose(), buf.getBuffer(RenderType.entityCutout(VANILLA_BEE_TEXTURE)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        buf.endBatch();
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

        drawLine(gfx, cx, cy, cx + (int) rxSX, cy + (int) rxSY, 0xFFFF4444, "X");
        drawLine(gfx, cx, cy, cx + (int) rySX, cy + (int) rySY, 0xFF44FF44, "Y");
        drawLine(gfx, cx, cy, cx + (int) rzSX, cy + (int) rzSY, 0xFF4488FF, "Z");
    }

    private void drawLine(GuiGraphics gfx, int x0, int y0, int x1, int y1, int color, String label) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy, px = x0, py = y0;
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
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    // ========== Input ==========

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check selector arrows — record pending, fire on release
            int[] hit = hitTestSelectorArrow(mouseX, mouseY);
            if (hit != null) {
                pendingSelectorRow = hit[0];
                pendingSelectorDir = hit[1];
                return true;
            }
            // Preview drag
            if (isInsidePreview(mouseX, mouseY)) {
                isDraggingPreview = true;
                lastDragX = mouseX;
                lastDragY = mouseY;
                return true;
            }
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
        if (button == 0) {
            // Fire pending selector on release
            if (pendingSelectorRow >= 0) {
                int[] hit = hitTestSelectorArrow(mouseX, mouseY);
                if (hit != null && hit[0] == pendingSelectorRow && hit[1] == pendingSelectorDir) {
                    cycleSelector(pendingSelectorRow, pendingSelectorDir);
                }
                pendingSelectorRow = -1;
                pendingSelectorDir = 0;
                return true;
            }
            if (isDraggingPreview) {
                isDraggingPreview = false;
                return true;
            }
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
                if (keyCode == 256) { field.setFocused(false); return true; }
                return field.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (EditBox field : hexFields) {
            if (field != null && field.isFocused()) return field.charTyped(chr, modifiers);
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
        try { return Integer.parseUnsignedInt(cleaned, 16); }
        catch (NumberFormatException e) { return -1; }
    }

    private static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }
}
