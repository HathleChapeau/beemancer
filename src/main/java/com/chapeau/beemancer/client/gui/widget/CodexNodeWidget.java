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
package com.chapeau.beemancer.client.gui.widget;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexManager;
import com.chapeau.beemancer.common.codex.CodexNode;
import com.chapeau.beemancer.common.quest.NodeState;
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

    private static final Map<String, String> NODE_TO_ITEM = new HashMap<>();
    private static final Map<String, ResourceLocation> NODE_TO_TEXTURE = new HashMap<>();

    static {
        // Mapping des nodes vers les items/blocs du mod
        NODE_TO_ITEM.put("the_beginning", "beemancer:codex");
        NODE_TO_ITEM.put("1st_bee", "beemancer:bee_jar");
        NODE_TO_ITEM.put("hive", "beemancer:controlled_hive");
        NODE_TO_ITEM.put("hive_multibloc", "beemancer:magic_hive");
        NODE_TO_ITEM.put("manual_centrifuge", "beemancer:manual_centrifuge");
        NODE_TO_ITEM.put("crystallyzer", "beemancer:crystallizer");
        NODE_TO_ITEM.put("altar", "beemancer:altar");
        NODE_TO_ITEM.put("extractor", "beemancer:extractor");
        NODE_TO_ITEM.put("anti_breeding_crystal", "beemancer:anti_breeding_crystal");
        NODE_TO_ITEM.put("honey_pipe", "beemancer:honey_pipe");
        NODE_TO_ITEM.put("item_pipe", "beemancer:item_pipe");
        NODE_TO_ITEM.put("centrifuge_t1", "beemancer:centrifuge");
        NODE_TO_ITEM.put("centrifuge_t2", "beemancer:centrifuge");
        NODE_TO_ITEM.put("centrifuge_t3", "beemancer:centrifuge");
        NODE_TO_ITEM.put("portable_tank", "beemancer:portable_tank");
        NODE_TO_ITEM.put("tank", "beemancer:tank");
        NODE_TO_ITEM.put("infuser_t1", "beemancer:infuser");
        NODE_TO_ITEM.put("infuser_t2", "beemancer:infuser");
        NODE_TO_ITEM.put("infuser_t3", "beemancer:infuser");
        NODE_TO_ITEM.put("alembic", "beemancer:alembic");
        NODE_TO_ITEM.put("incubator", "beemancer:incubator");
        NODE_TO_ITEM.put("storage_controller_heart", "beemancer:storage_controller");
        NODE_TO_ITEM.put("relay", "beemancer:storage_relay");
        NODE_TO_ITEM.put("interface", "beemancer:storage_terminal");
        NODE_TO_ITEM.put("import", "beemancer:import_interface");
        NODE_TO_ITEM.put("export", "beemancer:export_interface");
        NODE_TO_ITEM.put("craft_auto", "beemancer:auto_crafter");
        NODE_TO_ITEM.put("pipe_t2", "beemancer:controller_pipe");
        NODE_TO_ITEM.put("pipe_t3", "beemancer:controller_pipe");
        NODE_TO_ITEM.put("pipe_t4", "beemancer:controller_pipe");

        // Textures pour les nodes sans item correspondant
        NODE_TO_TEXTURE.put("the_beginning", ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/icon_start.png"));
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
                ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/icon_unknown.png"));
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
    }

    private void renderBackground(GuiGraphics graphics) {
        int bgColor;
        int borderColor;

        if (isHeader) {
            bgColor = 0xFF1A3A5C;      // Fond bleu foncé
            borderColor = 0xFF3498DB;  // Bordure bleue
        } else if (unlocked) {
            bgColor = 0xFF2C2C2C;      // Fond sombre
            borderColor = 0xFFF1C40F;  // Bordure dorée
        } else if (canUnlock) {
            bgColor = 0xFF1A1A1A;      // Fond très sombre
            borderColor = 0xFF888888;  // Bordure grise
        } else {
            bgColor = 0xFF0F0F0F;      // Fond noir
            borderColor = 0xFF444444;  // Bordure sombre
        }

        int x = getX();
        int y = getY();

        // Fond
        graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, bgColor);

        // Bordure (2px)
        graphics.fill(x, y, x + NODE_SIZE, y + 2, borderColor);                         // Haut
        graphics.fill(x, y + NODE_SIZE - 2, x + NODE_SIZE, y + NODE_SIZE, borderColor); // Bas
        graphics.fill(x, y, x + 2, y + NODE_SIZE, borderColor);                         // Gauche
        graphics.fill(x + NODE_SIZE - 2, y, x + NODE_SIZE, y + NODE_SIZE, borderColor); // Droite
    }

    private void renderIcon(GuiGraphics graphics) {
        int x = getX();
        int y = getY();

        if (!iconItem.isEmpty()) {
            // Rendre l'item (16x16 centré dans 24x24)
            int iconX = x + (NODE_SIZE - 16) / 2;
            int iconY = y + (NODE_SIZE - 16) / 2;

            if (!unlocked && !canUnlock) {
                // Assombrir l'item si verrouillé
                graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            } else if (!unlocked) {
                graphics.setColor(0.6f, 0.6f, 0.6f, 1.0f);
            }

            graphics.renderItem(iconItem, iconX, iconY);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else if (iconTexture != null) {
            // Rendre la texture (16x16 centré dans 24x24)
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
            // Fallback: afficher un point d'interrogation
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

            if (unlocked) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    displayTitle.copy().withStyle(style -> style.withBold(true)),
                    displayDescription
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else if (canUnlock) {
                graphics.renderTooltip(mc.font, java.util.List.of(
                    displayTitle,
                    Component.translatable("codex.beemancer.click_to_unlock").withStyle(style -> style.withColor(0xAAAAAAFF))
                ), java.util.Optional.empty(), mouseX, mouseY);
            } else {
                // Node LOCKED - afficher "???" si SECRET, sinon le titre
                graphics.renderTooltip(mc.font, java.util.List.of(
                    displayTitle,
                    Component.translatable("codex.beemancer.complete_quest_first").withStyle(style -> style.withColor(0xFF6666))
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
}
