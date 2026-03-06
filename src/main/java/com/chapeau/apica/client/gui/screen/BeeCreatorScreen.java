/**
 * ============================================================
 * [BeeCreatorScreen.java]
 * Description: Ecran du Bee Creator — selection de parties et couleurs avec preview 3D
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | BeeCreatorMenu           | Menu associe         | ContainerData sync             |
 * | BeePart                  | Enum parties         | Liste des parties              |
 * | BeeCreatorUpdatePacket   | Packet C2S           | Envoi couleur au serveur       |
 * | AnimationController      | Rotation fluide      | Gestion animation turntable    |
 * | RotateAnimation          | Rotation LOOP        | Rotation Y continue            |
 * | ApicaBeeModel            | Modele customisable  | Preview 3D tintee              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup (registerScreens)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.animation.AnimationController;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.client.animation.RotateAnimation;
import com.chapeau.apica.client.animation.TimingEffect;
import com.chapeau.apica.client.animation.TimingType;
import com.chapeau.apica.client.model.ApicaBeeModel;
import com.chapeau.apica.common.block.beecreator.BeePart;
import com.chapeau.apica.common.menu.BeeCreatorMenu;
import com.chapeau.apica.core.network.packets.BeeCreatorUpdatePacket;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * GUI du Bee Creator. Layout large:
 * - Panneau gauche: 7 rangees (une par partie) avec label + champ hex couleur + preview couleur
 * - Panneau droit: carre avec preview 3D de l'abeille tintee qui tourne lentement
 */
public class BeeCreatorScreen extends AbstractContainerScreen<BeeCreatorMenu> {

    private static final int GUI_W = 310;
    private static final int GUI_H = 200;
    private static final int LEFT_PANEL_W = 180;
    private static final int PREVIEW_SIZE = 110;
    private static final int ROW_H = 22;
    private static final int PART_START_Y = 24;
    private static final int COL_BG = 0xCC1A1A2E;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_DARK = 0xFF111122;
    private static final int COL_LABEL = 0xFFDDDDDD;
    private static final int COL_TITLE = 0xFFE8A317;
    private static final int COL_FIELD_BORDER = 0xFF444466;

    /** Duree d'un tour complet en ticks (240 = 12 secondes). */
    private static final float SPIN_DURATION = 240f;

    private final EditBox[] hexFields = new EditBox[BeePart.COUNT];
    private final int[] localColors = new int[BeePart.COUNT];
    private ApicaBeeModel<?> beeModel;
    private final AnimationController animController = new AnimationController();

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

        // Bake le modele ApicaBee
        beeModel = new ApicaBeeModel<>(
                Minecraft.getInstance().getEntityModels().bakeLayer(ApicaBeeModel.LAYER_LOCATION));

        // Animation turntable: rotation Y continue en boucle
        animController.createAnimation("spin", RotateAnimation.builder()
                .axis(Axis.YP)
                .startAngle(0f)
                .endAngle(360f)
                .timingType(TimingType.NORMAL)
                .timingEffect(TimingEffect.LOOP)
                .duration(SPIN_DURATION)
                .build());
        animController.playAnimation("spin");

        int gx = this.leftPos;
        int gy = this.topPos;

        for (BeePart part : BeePart.values()) {
            int fieldX = gx + 80;
            int fieldY = gy + PART_START_Y + part.getIndex() * ROW_H + 2;

            EditBox field = new EditBox(font, fieldX, fieldY, 72, 16, Component.empty());
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
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int gx = this.leftPos;
        int gy = this.topPos;

        // Bordure exterieure
        gfx.fill(gx - 2, gy - 2, gx + GUI_W + 2, gy + GUI_H + 2, COL_BORDER);
        // Fond principal
        gfx.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);

        // Titre
        gfx.drawString(font, Component.translatable("block.apica.bee_creator"),
                gx + 8, gy + 8, COL_TITLE, false);

        // Rangees des parties
        for (BeePart part : BeePart.values()) {
            int rowY = gy + PART_START_Y + part.getIndex() * ROW_H;

            // Fond alterne
            if (part.getIndex() % 2 == 0) {
                gfx.fill(gx + 4, rowY, gx + LEFT_PANEL_W - 4, rowY + ROW_H, 0x20FFFFFF);
            }

            // Label
            gfx.drawString(font, part.getDisplayName(), gx + 10, rowY + 6, COL_LABEL, false);

            // Preview couleur (petit carre)
            int previewX = gx + 158;
            int previewY = rowY + 3;
            int color = localColors[part.getIndex()];
            gfx.fill(previewX, previewY, previewX + 14, previewY + 14, COL_FIELD_BORDER);
            gfx.fill(previewX + 1, previewY + 1, previewX + 13, previewY + 13, 0xFF000000 | color);
        }

        // Panneau droit: preview
        int previewX = gx + LEFT_PANEL_W + 10;
        int previewY = gy + 24;
        int previewRight = previewX + PREVIEW_SIZE;
        int previewBottom = previewY + PREVIEW_SIZE;

        // Fond du carre de preview
        gfx.fill(previewX - 1, previewY - 1, previewRight + 1, previewBottom + 1, COL_BORDER);
        gfx.fill(previewX, previewY, previewRight, previewBottom, COL_DARK);

        // Label "Preview"
        gfx.drawCenteredString(font, "Preview",
                previewX + PREVIEW_SIZE / 2, previewY - 14, 0xFF888888);

        // Rendu de l'abeille tintee qui tourne
        renderBeePreview(gfx, previewX, previewY, PREVIEW_SIZE, PREVIEW_SIZE, partialTick);

        // Legende couleurs sous la preview
        int legendY = previewBottom + 8;
        for (BeePart part : BeePart.values()) {
            if (part.getIndex() >= 4) break;
            int lx = previewX + (part.getIndex() % 2) * 56;
            int ly = legendY + (part.getIndex() / 2) * 12;
            int c = localColors[part.getIndex()];
            gfx.fill(lx, ly, lx + 8, ly + 8, 0xFF000000 | c);
            gfx.drawString(font, part.getDisplayName().substring(0, Math.min(3, part.getDisplayName().length())),
                    lx + 10, ly, 0xFF999999, false);
        }
    }

    private void renderBeePreview(GuiGraphics gfx, int x, int y, int w, int h, float partialTick) {
        if (beeModel == null) return;

        // Tick l'animation avec le temps fluide
        float renderTime = AnimationTimer.getRenderTime(partialTick);
        animController.tick(renderTime);

        int centerX = x + w / 2;
        int centerY = y + h / 2 + 10;
        int scale = 38;

        gfx.enableScissor(x, y, x + w, y + h);
        gfx.pose().pushPose();
        gfx.pose().translate(centerX, centerY, 50.0f);
        gfx.pose().scale(scale, -scale, scale);

        // Inclinaison X fixe pour voir le dessus de l'abeille
        gfx.pose().mulPose(Axis.XP.rotationDegrees(15f));
        // Rotation Y via le systeme d'animation Apica
        animController.applyAnimation("spin", gfx.pose());

        Lighting.setupForEntityInInventory();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderType renderType = RenderType.entityCutout(ApicaBeeModel.TEXTURE);

        // Pass 1: Couleur corps
        beeModel.showCorpusOnly();
        VertexConsumer vc1 = bufferSource.getBuffer(renderType);
        beeModel.renderToBuffer(gfx.pose(), vc1, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, toArgb(localColors[BeePart.BODY.getIndex()]));

        // Pass 2: Couleur rayure
        beeModel.showStripeOnly();
        VertexConsumer vc2 = bufferSource.getBuffer(renderType);
        beeModel.renderToBuffer(gfx.pose(), vc2, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, toArgb(localColors[BeePart.STRIPE.getIndex()]));

        // Pass 3: Parties non tintees (pattes)
        beeModel.showUntintedOnly();
        VertexConsumer vc3 = bufferSource.getBuffer(renderType);
        beeModel.renderToBuffer(gfx.pose(), vc3, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        bufferSource.endBatch();
        Lighting.setupFor3DItems();

        gfx.pose().popPose();
        gfx.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Pas de labels par defaut
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
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
