/**
 * ============================================================
 * [BeeCreatorScreen.java]
 * Description: Screen GUI pour le BeeCreator
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.BeeCreatorMenu;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.network.packets.BeeCreatorActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class BeeCreatorScreen extends AbstractContainerScreen<BeeCreatorMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/bee_creator.png");

    private static final int GENE_ROW_START_Y = 45;
    private static final int GENE_ROW_HEIGHT = 20;
    
    private Button editButton;
    private final List<GeneRowButtons> geneRows = new ArrayList<>();

    public BeeCreatorScreen(BeeCreatorMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 184;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        geneRows.clear();
        
        // Create gene row buttons for each category
        List<GeneCategory> categories = menu.getCategories();
        for (int i = 0; i < categories.size(); i++) {
            GeneCategory category = categories.get(i);
            int rowY = GENE_ROW_START_Y + i * GENE_ROW_HEIGHT;
            
            // Left arrow
            Button leftBtn = Button.builder(Component.literal("<"), btn -> {
                PacketDistributor.sendToServer(new BeeCreatorActionPacket(
                        menu.containerId, category.getId(), false));
                menu.cycleGenePrevious(category);
            }).bounds(leftPos + 10, topPos + rowY, 20, 18).build();
            
            // Right arrow
            Button rightBtn = Button.builder(Component.literal(">"), btn -> {
                PacketDistributor.sendToServer(new BeeCreatorActionPacket(
                        menu.containerId, category.getId(), true));
                menu.cycleGeneNext(category);
            }).bounds(leftPos + 146, topPos + rowY, 20, 18).build();
            
            this.addRenderableWidget(leftBtn);
            this.addRenderableWidget(rightBtn);
            
            geneRows.add(new GeneRowButtons(category, leftBtn, rightBtn, rowY));
        }
        
        // Edit button
        editButton = Button.builder(Component.translatable("gui.beemancer.bee_creator.edit"), btn -> {
            if (menu.canApplyChanges()) {
                PacketDistributor.sendToServer(new BeeCreatorActionPacket(
                        menu.containerId, "APPLY", true));
                menu.applyChanges();
            }
        }).bounds(leftPos + 60, topPos + GENE_ROW_START_Y + categories.size() * GENE_ROW_HEIGHT + 5, 56, 20).build();
        
        this.addRenderableWidget(editButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Update edit button state
        editButton.active = menu.canApplyChanges();
        
        // Reload genes when bee changes
        if (menu.hasBee()) {
            //menu.loadGenesFromBee();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Render gene category names and values
        for (GeneRowButtons row : geneRows) {
            int textY = topPos + row.y + 5;
            
            // Category name (left side)
            graphics.drawString(font, row.category.getDisplayName(), 
                    leftPos + 32, textY, 0xFFFFFF, false);
            
            // Gene value (center)
            Gene gene = menu.getSelectedGene(row.category);
            if (gene != null) {
                String geneName = gene.getDisplayName().getString();
                int textWidth = font.width(geneName);
                graphics.drawString(font, geneName, 
                        leftPos + 88 - textWidth / 2, textY, 0xFFFFFF, false);
            }
        }
        
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    private record GeneRowButtons(GeneCategory category, Button left, Button right, int y) {}
}
