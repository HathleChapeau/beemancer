/**
 * ============================================================
 * [InventoryScreenAccessoryMixin.java]
 * Description: Mixin pour ajouter 2 slots accessoire + tabs sur l'inventaire joueur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance            | Raison                | Utilisation                    |
 * |-----------------------|----------------------|--------------------------------|
 * | AccessoryClientCache  | Cache client         | Lecture items equipes + tabs   |
 * | AccessoryEquipPacket  | Reseau               | Envoi equip/unequip            |
 * | IAccessory            | Type check           | Validation + tab click         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - apica.mixins.json (client mixin registration)
 *
 * ============================================================
 */
package com.chapeau.apica.mixin.client;

import com.chapeau.apica.client.gui.AccessoryClientCache;
import com.chapeau.apica.common.item.accessory.IAccessory;
import com.chapeau.apica.core.network.packets.AccessoryEquipPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ajoute 2 slots accessoire au-dessus du shield dans l'inventaire joueur.
 * Ajoute des tabs dynamiques en haut du GUI pour chaque accessoire avec hasInventoryTab().
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenAccessoryMixin {

    // Accessory slot positions (container-relative, above shield slot at 77,62)
    @Unique private static final int SLOT_0_X = 76;
    @Unique private static final int SLOT_0_Y = 25;
    @Unique private static final int SLOT_1_X = 76;
    @Unique private static final int SLOT_1_Y = 7;
    @Unique private static final int SLOT_SIZE = 18;

    // Tab sprites (vanilla advancement above-type tabs)
    @Unique private static final ResourceLocation TAB_SELECTED_LEFT =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_left_selected");
    @Unique private static final ResourceLocation TAB_UNSELECTED_LEFT =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_left");
    @Unique private static final ResourceLocation TAB_SELECTED_MIDDLE =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_middle_selected");
    @Unique private static final ResourceLocation TAB_UNSELECTED_MIDDLE =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_middle");

    // Tab dimensions (above orientation: narrow width, taller height)
    @Unique private static final int TAB_W = 28;
    @Unique private static final int TAB_H = 32;
    @Unique private static final int TAB_PROTRUDE = 28;
    @Unique private static final int TAB_GAP = 1;

    // Lazy icon stacks
    @Unique private static ItemStack apica$playerIcon;
    @Unique private static ItemStack apica$getPlayerIcon() {
        if (apica$playerIcon == null) apica$playerIcon = new ItemStack(Items.CRAFTING_TABLE);
        return apica$playerIcon;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void apica$renderAccessories(GuiGraphics graphics, int mouseX, int mouseY,
                                          float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int leftPos = self.getGuiLeft();
        int topPos = self.getGuiTop();

        // --- Accessory slot backgrounds ---
        apica$renderSlotBg(graphics, leftPos + SLOT_0_X, topPos + SLOT_0_Y);
        apica$renderSlotBg(graphics, leftPos + SLOT_1_X, topPos + SLOT_1_Y);

        // --- Items in slots ---
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < 2; i++) {
            ItemStack stack = AccessoryClientCache.getSlot(i);
            if (!stack.isEmpty()) {
                int sx = leftPos + (i == 0 ? SLOT_0_X : SLOT_1_X) + 1;
                int sy = topPos + (i == 0 ? SLOT_0_Y : SLOT_1_Y) + 1;
                graphics.renderItem(stack, sx, sy);
                graphics.renderItemDecorations(font, stack, sx, sy);
            }
        }

        // --- Highlight on hover ---
        int hoveredSlot = apica$getHoveredSlot(mouseX, mouseY, leftPos, topPos);
        if (hoveredSlot >= 0) {
            int sx = leftPos + (hoveredSlot == 0 ? SLOT_0_X : SLOT_1_X);
            int sy = topPos + (hoveredSlot == 0 ? SLOT_0_Y : SLOT_1_Y);
            graphics.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0x80FFFFFF);
        }

        // --- Tabs (dynamiques: Player + un par accessoire avec tab) ---
        List<Integer> tabSlots = AccessoryClientCache.getTabSlots();
        if (!tabSlots.isEmpty()) {
            int tabY = topPos - TAB_PROTRUDE;
            int tabX = leftPos;

            // Player tab (selected — we're on InventoryScreen)
            graphics.blitSprite(TAB_SELECTED_LEFT, tabX, tabY, TAB_W, TAB_H);
            graphics.renderItem(apica$getPlayerIcon(), tabX + 6, tabY + 9);
            tabX += TAB_W + TAB_GAP;

            // Accessory tabs (unselected)
            for (int slot : tabSlots) {
                graphics.blitSprite(TAB_UNSELECTED_MIDDLE, tabX, tabY, TAB_W, TAB_H);
                graphics.renderItem(AccessoryClientCache.getSlot(slot), tabX + 6, tabY + 9);
                tabX += TAB_W + TAB_GAP;
            }
        }

        // --- Tooltip for hovered accessory slot ---
        if (hoveredSlot >= 0) {
            ItemStack hovered = AccessoryClientCache.getSlot(hoveredSlot);
            if (!hovered.isEmpty()) {
                graphics.renderTooltip(font, hovered, mouseX, mouseY);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void apica$onMouseClicked(double mouseX, double mouseY, int button,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int leftPos = self.getGuiLeft();
        int topPos = self.getGuiTop();

        // --- Tab click: accessory tabs ---
        List<Integer> tabSlots = AccessoryClientCache.getTabSlots();
        if (!tabSlots.isEmpty()) {
            int tabY = topPos - TAB_PROTRUDE;
            int tabX = leftPos + TAB_W + TAB_GAP;
            for (int slot : tabSlots) {
                if (mouseX >= tabX && mouseX < tabX + TAB_W
                        && mouseY >= tabY && mouseY < tabY + TAB_H) {
                    ItemStack stack = AccessoryClientCache.getSlot(slot);
                    if (stack.getItem() instanceof IAccessory acc) {
                        acc.onInventoryTabClicked(slot);
                    }
                    cir.setReturnValue(true);
                    return;
                }
                tabX += TAB_W + TAB_GAP;
            }
        }

        // --- Accessory slot click: equip/unequip ---
        int hoveredSlot = apica$getHoveredSlot(mouseX, mouseY, leftPos, topPos);
        if (hoveredSlot >= 0) {
            ItemStack carried = self.getMenu().getCarried();
            if (!carried.isEmpty() && carried.getItem() instanceof IAccessory) {
                // Equip
                PacketDistributor.sendToServer(new AccessoryEquipPacket(hoveredSlot, true));
                cir.setReturnValue(true);
            } else if (carried.isEmpty() && !AccessoryClientCache.getSlot(hoveredSlot).isEmpty()) {
                // Unequip
                PacketDistributor.sendToServer(new AccessoryEquipPacket(hoveredSlot, false));
                cir.setReturnValue(true);
            }
        }
    }

    /** Render a vanilla-style slot background (18x18). */
    @Unique
    private void apica$renderSlotBg(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF373737);
        graphics.fill(x, y + 1, x + 1, y + SLOT_SIZE - 1, 0xFF373737);
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE - 1, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
    }

    /** Returns the accessory slot index (0 or 1) under the mouse, or -1. */
    @Unique
    private int apica$getHoveredSlot(double mouseX, double mouseY, int leftPos, int topPos) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int s0x = leftPos + SLOT_0_X;
        int s0y = topPos + SLOT_0_Y;
        if (mx >= s0x && mx < s0x + SLOT_SIZE && my >= s0y && my < s0y + SLOT_SIZE) return 0;

        int s1x = leftPos + SLOT_1_X;
        int s1y = topPos + SLOT_1_Y;
        if (mx >= s1x && mx < s1x + SLOT_SIZE && my >= s1y && my < s1y + SLOT_SIZE) return 1;

        return -1;
    }
}
