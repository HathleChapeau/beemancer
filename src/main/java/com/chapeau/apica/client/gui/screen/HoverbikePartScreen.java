/**
 * ============================================================
 * [HoverbikePartScreen.java]
 * Description: Ecran de visualisation d'une partie du Hoverbike avec stats et swap inventaire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoverbikePart           | Enum partie          | Identification partie          |
 * | HoverbikePartModel      | Modele 3D            | Rendu preview                  |
 * | HoverbikePartVariants   | Registre variantes   | Liste dynamique des modeles    |
 * | HoverbikeEntity         | Entite source        | Lecture piece equipee          |
 * | HoverbikePartData       | Stats de la piece    | Base stats + modifiers         |
 * | HoverbikePartItem       | Item de piece        | Detection pieces inventaire    |
 * | HoverbikePartSwapPacket | Packet swap          | Echange piece                  |
 * | HoverbikePartRemovePacket| Packet remove       | Retrait piece                  |
 * | GuiRenderHelper         | Rendu vanilla        | Fond container, slots, boutons |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeEditModeHandler.java: Ouverture sur clic droit
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.GuiRenderHelper;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import com.chapeau.apica.client.model.hoverbike.HoverbikePartVariants;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.item.mount.AppliedModifier;
import com.chapeau.apica.common.item.mount.HoverbikePartData;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.chapeau.apica.core.network.packets.HoverbikePartRemovePacket;
import com.chapeau.apica.core.network.packets.HoverbikePartSwapPacket;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecran de visualisation d'une partie equipee du hoverbike.
 * Affiche la preview 3D, les stats (base + modifiers + MK), un bouton Remove,
 * et des fleches de swap si le joueur a des pieces compatibles en inventaire.
 */
public class HoverbikePartScreen extends Screen {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 270;
    private static final int INSET_SIZE = 100;
    private static final int TITLE_Y_OFFSET = 7;
    private static final int BUTTON_HEIGHT = 20;

    private static final int TITLE_COLOR = 0x404040;
    private static final int INSET_BG = 0xFF8B8B8B;
    private static final int INSET_BORDER_DARK = 0xFF373737;
    private static final int INSET_BORDER_LIGHT = 0xFFFFFFFF;
    private static final int VARIANT_NAME_COLOR = 0xFF333333;
    private static final int BASE_STAT_COLOR = 0xFFFFFFFF;
    private static final int PREFIX_COLOR = 0xFFFFAA00;
    private static final int SUFFIX_COLOR = 0xFF55FFFF;
    private static final int MK_COLOR = 0xFFFFFF55;
    private static final int LABEL_COLOR = 0xFFAAAAAA;

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

    private List<Integer> compatibleSlots = new ArrayList<>();
    private int currentSwapIndex = 0;

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

        currentVariantIndex = Math.floorMod(currentVariantIndex, variants.size());
        bakeCurrentModel();

        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        insetX = panelX + (PANEL_WIDTH - INSET_SIZE) / 2;
        insetY = panelY + 22;

        scanCompatiblePieces();

        if (!compatibleSlots.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("<"), btn -> {
                cycleSwap(-1);
            }).bounds(panelX + 8, insetY + INSET_SIZE / 2 - BUTTON_HEIGHT / 2, 20, BUTTON_HEIGHT).build());

            addRenderableWidget(Button.builder(Component.literal(">"), btn -> {
                cycleSwap(1);
            }).bounds(panelX + PANEL_WIDTH - 28, insetY + INSET_SIZE / 2 - BUTTON_HEIGHT / 2, 20, BUTTON_HEIGHT).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.apica.hoverbike.remove"), btn -> {
            PacketDistributor.sendToServer(new HoverbikePartRemovePacket(
                    hoverbike.getId(), partType.ordinal()));
            onClose();
        }).bounds(width / 2 - 40, panelY + PANEL_HEIGHT - 52, 80, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.back"), btn -> {
            onClose();
        }).bounds(width / 2 - 40, panelY + PANEL_HEIGHT - 28, 80, BUTTON_HEIGHT).build());
    }

    /**
     * Scanne l'inventaire du joueur pour trouver les pieces compatibles (meme categorie).
     */
    private void scanCompatiblePieces() {
        compatibleSlots.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        List<ItemStack> items = mc.player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof HoverbikePartItem partItem)) continue;
            if (partItem.getCategory() == partType) {
                compatibleSlots.add(i);
            }
        }
        currentSwapIndex = 0;
    }

    /**
     * Cycle dans les pieces compatibles et envoie un swap au serveur.
     */
    private void cycleSwap(int direction) {
        if (compatibleSlots.isEmpty()) return;
        currentSwapIndex = Math.floorMod(currentSwapIndex + direction, compatibleSlots.size());
        int slot = compatibleSlots.get(currentSwapIndex);

        PacketDistributor.sendToServer(new HoverbikePartSwapPacket(
                hoverbike.getId(), partType.ordinal(), slot));

        // Rescan apres swap (le slot inventaire a change)
        scanCompatiblePieces();
    }

    private void bakeCurrentModel() {
        if (variants.isEmpty()) return;
        HoverbikePartVariants.VariantEntry entry = variants.get(currentVariantIndex);
        Minecraft mc = Minecraft.getInstance();
        ModelPart root = mc.getEntityModels().bakeLayer(entry.factory().layerLocation());
        partModel = entry.factory().constructor().apply(root);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        GuiRenderHelper.renderContainerBackgroundNoTitle(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawCenteredString(font, title, width / 2, panelY + TITLE_Y_OFFSET, TITLE_COLOR);
        graphics.fill(panelX + 7, panelY + 18, panelX + PANEL_WIDTH - 7, panelY + 19, 0xFF8B8B8B);

        renderInset(graphics, insetX, insetY, INSET_SIZE, INSET_SIZE);
        renderPartModel(graphics, partialTick);

        int textY = insetY + INSET_SIZE + 4;

        if (!variants.isEmpty()) {
            HoverbikePartVariants.VariantEntry entry = variants.get(currentVariantIndex);
            ItemStack equippedStack = hoverbike.getPartStack(partType);
            int mk = HoverbikePartData.getMK(equippedStack);

            String variantName = entry.name();
            String mkStr = mk > 0 ? " MK " + toRoman(mk) : "";
            graphics.drawCenteredString(font, variantName + mkStr, width / 2, textY, mk > 0 ? MK_COLOR : VARIANT_NAME_COLOR);
            textY += 12;

            textY = renderStats(graphics, entry, equippedStack, textY);
        }
    }

    /**
     * Affiche les stats (base + modifiers) sous le nom de la variante.
     */
    private int renderStats(GuiGraphics graphics, HoverbikePartVariants.VariantEntry entry,
                             ItemStack equippedStack, int startY) {
        int y = startY;
        int leftX = panelX + 10;

        graphics.drawString(font, "--- Base Stats ---", leftX, y, LABEL_COLOR, false);
        y += 10;

        if (entry.stat1Name() != null && !entry.stat1Name().isEmpty()) {
            graphics.drawString(font, "  " + entry.stat1Name() + ": +" + formatStat(entry.stat1Value()), leftX, y, BASE_STAT_COLOR, false);
            y += 10;
        }
        if (entry.stat2Name() != null && !entry.stat2Name().isEmpty()) {
            graphics.drawString(font, "  " + entry.stat2Name() + ": +" + formatStat(entry.stat2Value()), leftX, y, BASE_STAT_COLOR, false);
            y += 10;
        }

        List<AppliedModifier> prefixes = HoverbikePartData.getPrefixes(equippedStack);
        List<AppliedModifier> suffixes = HoverbikePartData.getSuffixes(equippedStack);

        if (!prefixes.isEmpty()) {
            graphics.drawString(font, "--- Prefix ---", leftX, y, LABEL_COLOR, false);
            y += 10;
            for (AppliedModifier mod : prefixes) {
                String line = "  T" + mod.tier() + " " +
                        mod.statType().getJsonKey() + " " + formatModValue(mod);
                graphics.drawString(font, line, leftX, y, PREFIX_COLOR, false);
                y += 10;
            }
        }

        if (!suffixes.isEmpty()) {
            graphics.drawString(font, "--- Suffix ---", leftX, y, LABEL_COLOR, false);
            y += 10;
            for (AppliedModifier mod : suffixes) {
                String line = "  T" + mod.tier() + " " +
                        mod.statType().getJsonKey() + " " + formatModValue(mod);
                graphics.drawString(font, line, leftX, y, SUFFIX_COLOR, false);
                y += 10;
            }
        }

        return y;
    }

    private static String formatStat(double value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.format("%.3f", value);
    }

    private static String formatModValue(AppliedModifier mod) {
        String sign = mod.value() >= 0 ? "+" : "";
        if ("%".equals(mod.valueType())) {
            return sign + String.format("%.1f", mod.value()) + "%";
        }
        return sign + formatStat(mod.value());
    }

    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> String.valueOf(num);
        };
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
