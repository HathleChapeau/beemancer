/**
 * ============================================================
 * [CodexNodeWidget.java]
 * Description: Widget pour afficher un node individuel du Codex avec icône
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexNode           | Données du node      | Affichage et interaction       |
 * | CodexNodeCategory   | Style visuel         | Sélection du background        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (création et rendu des nodes)
 * - StandardPageRenderer (rendu des nodes)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.codex.CodexManager;
import com.chapeau.apica.common.codex.CodexNode;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.common.quest.NodeState;
import com.chapeau.apica.common.quest.NodeVisibility;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.HashMap;
import java.util.Map;

public class CodexNodeWidget extends AbstractWidget {
    public static final int NODE_SIZE = 24;
    private static final int FRAME_SIZE = 26;

    // Vanilla advancement frame textures
    private static final ResourceLocation TASK_FRAME_OBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/task_frame_obtained.png");
    private static final ResourceLocation TASK_FRAME_UNOBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/task_frame_unobtained.png");
    private static final ResourceLocation CHALLENGE_FRAME_OBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/challenge_frame_obtained.png");
    private static final ResourceLocation CHALLENGE_FRAME_UNOBTAINED = ResourceLocation.withDefaultNamespace(
            "textures/gui/sprites/advancements/challenge_frame_unobtained.png");

    private static final Map<String, String> NODE_TO_ITEM = new HashMap<>();
    private static final Map<String, ResourceLocation> NODE_TO_TEXTURE = new HashMap<>();

    static {
        // Mapping des nodes vers les items/blocs du mod
        NODE_TO_ITEM.put("1st_bee", "apica:scoop");
        NODE_TO_ITEM.put("hive", "apica:magic_hive");
        NODE_TO_ITEM.put("hive_multibloc", "apica:hive_multiblock");
        NODE_TO_ITEM.put("manual_centrifuge", "apica:manual_centrifuge");
        NODE_TO_ITEM.put("crystallyzer", "apica:crystallizer");
        NODE_TO_ITEM.put("altar", "apica:altar_heart");
        NODE_TO_ITEM.put("extractor", "apica:extractor");
        NODE_TO_ITEM.put("anti_breeding_crystal", "apica:anti_breeding_crystal");
        NODE_TO_ITEM.put("liquid_pipe", "apica:liquid_pipe");
        NODE_TO_ITEM.put("item_pipe", "apica:item_pipe");
        NODE_TO_ITEM.put("centrifuge_t1", "apica:powered_centrifuge");
        NODE_TO_ITEM.put("centrifuge_t2", "apica:powered_centrifuge_tier2");
        NODE_TO_ITEM.put("centrifuge_t3", "apica:centrifuge_heart");
        NODE_TO_ITEM.put("portable_tank", "apica:honey_tank");
        NODE_TO_ITEM.put("tank", "apica:multiblock_tank");
        NODE_TO_ITEM.put("infuser_t1", "apica:infuser");
        NODE_TO_ITEM.put("alembic", "apica:alembic_heart");
        NODE_TO_ITEM.put("incubator", "apica:incubator");
        NODE_TO_ITEM.put("pollen_pot", "apica:pollen_pot");
        NODE_TO_ITEM.put("storage_controller_heart", "apica:storage_controller");
        NODE_TO_ITEM.put("relay", "apica:storage_relay");
        NODE_TO_ITEM.put("interface", "apica:storage_terminal");
        NODE_TO_ITEM.put("import", "apica:import_interface");
        NODE_TO_ITEM.put("export", "apica:export_interface");
        NODE_TO_ITEM.put("pipe_t2", "apica:liquid_pipe");
        NODE_TO_ITEM.put("pipe_t3", "apica:liquid_pipe");
        NODE_TO_ITEM.put("pipe_t4", "apica:liquid_pipe");
        NODE_TO_ITEM.put("resonator", "apica:resonator");
        NODE_TO_ITEM.put("essence_injector", "apica:injector");
        NODE_TO_ITEM.put("storage_barrel", "apica:storage_barrel_mk1");
        NODE_TO_ITEM.put("storage_barrel_mk2", "apica:storage_barrel_mk2");
        NODE_TO_ITEM.put("storage_barrel_mk3", "apica:storage_barrel_mk3");
        NODE_TO_ITEM.put("storage_barrel_mk4", "apica:storage_barrel_mk4");
        NODE_TO_ITEM.put("void_upgrade", "apica:void_upgrade");
        NODE_TO_ITEM.put("trash_can", "apica:trash_can");
        NODE_TO_ITEM.put("liquid_trash_can", "apica:liquid_trash_can");
        NODE_TO_ITEM.put("storage_hive", "apica:storage_hive");
        NODE_TO_ITEM.put("storage_hive_mk2", "apica:storage_hive_tier2");
        NODE_TO_ITEM.put("storage_hive_mk3", "apica:storage_hive_tier3");

        // Textures pour les nodes sans item correspondant
        NODE_TO_TEXTURE.put("apica", ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/gui/codex/icon_unknown.png"));
    }

    private final CodexNode node;
    private final String displayName;
    private final Component displayTitle;
    private final Component displayDescription;
    private final ItemStack iconItem;
    private final ResourceLocation iconTexture;
    private boolean unlocked;
    private boolean canUnlock;
    private boolean hovered;
    private final boolean isHeader;
    private final NodeState nodeState;

    public CodexNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock) {
        this(node, screenX, screenY, unlocked, canUnlock, false, null);
    }

    public CodexNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock, NodeState state) {
        this(node, screenX, screenY, unlocked, canUnlock, false, state);
    }

    public CodexNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock, boolean isHeader) {
        this(node, screenX, screenY, unlocked, canUnlock, isHeader, null);
    }

    public CodexNodeWidget(CodexNode node, int screenX, int screenY, boolean unlocked, boolean canUnlock, boolean isHeader, NodeState state) {
        super(screenX, screenY, NODE_SIZE, NODE_SIZE, node.getTitle());
        this.node = node;
        this.unlocked = unlocked;
        this.canUnlock = canUnlock;
        this.isHeader = isHeader;
        this.nodeState = state != null ? state : (unlocked ? NodeState.UNLOCKED : (canUnlock ? NodeState.DISCOVERED : NodeState.LOCKED));

        // Calculer le texte à afficher (??? si SECRET et LOCKED)
        this.displayTitle = CodexManager.getDisplayTitle(node, this.nodeState);
        this.displayDescription = CodexManager.getDisplayDescription(node, this.nodeState);

        String id = node.getId();
        this.displayName = formatDisplayName(id);

        // Chercher l'item correspondant
        String itemId = NODE_TO_ITEM.get(id);
        if (itemId != null) {
            ResourceLocation itemLoc = ResourceLocation.parse(itemId);
            this.iconItem = new ItemStack(BuiltInRegistries.ITEM.get(itemLoc));
            this.iconTexture = null;
        } else {
            this.iconItem = ItemStack.EMPTY;
            this.iconTexture = NODE_TO_TEXTURE.getOrDefault(id,
                ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "textures/gui/codex/icon_unknown.png"));
        }
    }

    private String formatDisplayName(String id) {
        String[] parts = id.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (result.length() > 0) result.append(" ");
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        return result.toString();
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

    public boolean isHeader() {
        return isHeader;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.hovered = isMouseOver(mouseX, mouseY);

        // Dessiner le fond du node
        renderBackground(graphics);

        // Dessiner l'icône
        renderIcon(graphics);

        // Effet de survol
        if (hovered && canUnlock && !unlocked) {
            renderGlow(graphics);
        }

        // Badge "New" si DISCOVERED
        if (nodeState == NodeState.DISCOVERED) {
            renderNewBadge(graphics);
        }

        // Debug: afficher le quest_id dans l'encadré du node
        if (DebugWandItem.displayDebug && node.hasQuest()) {
            renderDebugQuestId(graphics);
        }
    }

    private void renderNewBadge(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
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

    private void renderDebugQuestId(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        String questId = node.getQuestId();

        int x = getX();
        int y = getY() + NODE_SIZE + 1;

        // Fond sombre pour lisibilité
        int textWidth = font.width(questId);
        graphics.fill(x - 1, y - 1, x + textWidth + 1, y + font.lineHeight + 1, 0xCC000000);

        // Texte quest_id en cyan
        graphics.drawString(font, questId, x, y, 0xFF00FFFF, false);
    }

    private void renderBackground(GuiGraphics graphics) {
        int x = getX();
        int y = getY();

        // Choisir le type de frame selon les tags du node
        boolean challenge = node.isChallenge();

        // Choisir la texture et la teinte selon l'etat
        ResourceLocation frame;
        float r, g, b;

        if (isHeader) {
            frame = challenge ? CHALLENGE_FRAME_OBTAINED : TASK_FRAME_OBTAINED;
            r = 0.2f; g = 0.58f; b = 0.86f;   // Bleu (#3498DB)
        } else if (unlocked) {
            frame = challenge ? CHALLENGE_FRAME_OBTAINED : TASK_FRAME_OBTAINED;
            r = 0.95f; g = 0.77f; b = 0.06f;   // Or (#F1C40F)
        } else if (canUnlock) {
            frame = challenge ? CHALLENGE_FRAME_UNOBTAINED : TASK_FRAME_UNOBTAINED;
            r = 1.0f; g = 1.0f; b = 1.0f;       // Blanc
        } else {
            frame = challenge ? CHALLENGE_FRAME_UNOBTAINED : TASK_FRAME_UNOBTAINED;
            r = 0.27f; g = 0.27f; b = 0.27f;   // Sombre (#444444)
        }

        // Rendre le frame vanilla teinte (26x26 centre sur le node 24x24)
        RenderSystem.setShaderColor(r, g, b, 1.0f);
        graphics.blit(frame, x - 1, y - 1, 0, 0, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static final ResourceLocation ICON_UNKNOWN = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/codex/icon_unknown.png");

    private void renderIcon(GuiGraphics graphics) {
        int x = getX();
        int y = getY();

        // SECRET + pas encore DISCOVERED/UNLOCKED → icône "?"
        boolean secretHidden = node.getVisibility() == NodeVisibility.SECRET
                && nodeState == NodeState.LOCKED;

        if (secretHidden) {
            int iconX = x + (NODE_SIZE - 16) / 2;
            int iconY = y + (NODE_SIZE - 16) / 2;
            graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            graphics.blit(ICON_UNKNOWN, iconX, iconY, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        if (!iconItem.isEmpty()) {
            int iconX = x + (NODE_SIZE - 16) / 2;
            int iconY = y + (NODE_SIZE - 16) / 2;

            if (!unlocked && !canUnlock) {
                graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else if (!unlocked) {
                graphics.setColor(0.6f, 0.6f, 0.6f, 1.0f);
            }

            graphics.renderItem(iconItem, iconX, iconY);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else if (iconTexture != null) {
            int iconX = x + (NODE_SIZE - 16) / 2;
            int iconY = y + (NODE_SIZE - 16) / 2;

            if (!unlocked && !canUnlock) {
                graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else if (!unlocked) {
                graphics.setColor(0.6f, 0.6f, 0.6f, 1.0f);
            }

            graphics.blit(iconTexture, iconX, iconY, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            Font font = Minecraft.getInstance().font;
            int textColor = unlocked ? 0xFFFFFFFF : 0xFF888888;
            graphics.drawCenteredString(font, "?", x + NODE_SIZE / 2, y + (NODE_SIZE - font.lineHeight) / 2, textColor);
        }
    }

    private void renderGlow(GuiGraphics graphics) {
        int x = getX();
        int y = getY();
        graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, 0x40FFFF00);
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hovered) {
            Minecraft mc = Minecraft.getInstance();

            // Simple: titre (bold si unlocked) + description
            // displayTitle/displayDescription sont déjà "???" si SECRET et LOCKED
            graphics.renderTooltip(mc.font, java.util.List.of(
                displayTitle.copy().withStyle(style -> style.withBold(unlocked)),
                displayDescription
            ), java.util.Optional.empty(), mouseX, mouseY);
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
}
