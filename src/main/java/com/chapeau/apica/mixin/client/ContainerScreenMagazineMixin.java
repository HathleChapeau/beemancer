/**
 * ============================================================
 * [ContainerScreenMagazineMixin.java]
 * Description: Mixin client pour afficher un slot magazine bonus au survol d'un IMagazineHolder
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IMagazineHolder     | Detection items      | instanceof check               |
 * | MagazineData        | Lecture magazine     | Affichage icone equipee         |
 * | MagazineItem        | Type check           | Validation curseur              |
 * | MagazineEquipPacket | Reseau               | Envoi equip/unequip            |
 * | MagazineFluidData   | Lecture fluide       | Tooltip magazine               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - apica.mixins.json (client mixin registration)
 *
 * ============================================================
 */
package com.chapeau.apica.mixin.client;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.magazine.MagazineFluidData;
import com.chapeau.apica.common.item.magazine.MagazineItem;
import com.chapeau.apica.core.network.packets.MagazineEquipPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ajoute un slot magazine bonus au-dessus des items IMagazineHolder dans l'inventaire.
 * Le slot reste visible si le curseur passe de l'item au slot bonus.
 * Clic gauche pour equiper (magazine en curseur) ou desequiper (curseur vide).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMagazineMixin {

    private static final ResourceLocation MAGAZINE_SLOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Apica.MOD_ID, "textures/gui/magazine_slot.png");

    /** Taille du slot magazine bonus (meme taille qu'un slot vanilla). */
    @Unique private static final int SLOT_SIZE = 18;
    /** Offset Y au-dessus du slot hovered. */
    @Unique private static final int SLOT_Y_OFFSET = 20;

    /** Position ecran du slot magazine bonus actuellement visible (-1 si cache). */
    @Unique private int apica$magSlotScreenX = -1;
    @Unique private int apica$magSlotScreenY = -1;
    @Unique private boolean apica$magSlotVisible = false;
    /** Index du slot inventaire associe au bonus slot. */
    @Unique private int apica$magSlotIndex = -1;

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Shadow protected abstract Slot findSlot(double mouseX, double mouseY);

    @Inject(method = "render", at = @At("TAIL"))
    private void apica$renderMagazineSlot(GuiGraphics graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;

        // Chercher le slot sous la souris
        Slot hoveredSlot = findSlot(mouseX, mouseY);
        boolean hoveringHolder = false;

        if (hoveredSlot != null && hoveredSlot.hasItem()) {
            ItemStack stack = hoveredSlot.getItem();
            if (stack.getItem() instanceof IMagazineHolder) {
                hoveringHolder = true;
                int slotScreenX = leftPos + hoveredSlot.x;
                int slotScreenY = topPos + hoveredSlot.y;
                apica$magSlotScreenX = slotScreenX - 1;
                apica$magSlotScreenY = slotScreenY - SLOT_Y_OFFSET + 2;
                apica$magSlotVisible = true;
                apica$magSlotIndex = hoveredSlot.index;
            }
        }

        // Si le curseur n'est pas sur un holder, verifier s'il est sur le bonus slot
        if (!hoveringHolder && apica$magSlotVisible) {
            boolean onBonusSlot = mouseX >= apica$magSlotScreenX
                    && mouseX < apica$magSlotScreenX + SLOT_SIZE
                    && mouseY >= apica$magSlotScreenY
                    && mouseY < apica$magSlotScreenY + SLOT_SIZE;
            if (!onBonusSlot) {
                apica$magSlotVisible = false;
            }
        }

        // Rendre le slot bonus si visible
        if (apica$magSlotVisible) {
            graphics.blit(MAGAZINE_SLOT_TEXTURE,
                    apica$magSlotScreenX, apica$magSlotScreenY,
                    0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

            // Rendre l'icone du magazine equipe
            if (apica$magSlotIndex >= 0 && apica$magSlotIndex < self.getMenu().slots.size()) {
                ItemStack holderStack = self.getMenu().slots.get(apica$magSlotIndex).getItem();
                if (MagazineData.hasMagazine(holderStack)) {
                    String fluidId = MagazineData.getFluidId(holderStack);
                    int amount = MagazineData.getFluidAmount(holderStack);
                    ItemStack displayMag = MagazineItem.createFilled(fluidId, amount);
                    graphics.renderItem(displayMag,
                            apica$magSlotScreenX + 1, apica$magSlotScreenY + 1);
                }
            }

            // Highlight si survol du bonus slot
            boolean onBonusSlot = mouseX >= apica$magSlotScreenX
                    && mouseX < apica$magSlotScreenX + SLOT_SIZE
                    && mouseY >= apica$magSlotScreenY
                    && mouseY < apica$magSlotScreenY + SLOT_SIZE;
            if (onBonusSlot) {
                graphics.fill(apica$magSlotScreenX + 1, apica$magSlotScreenY + 1,
                        apica$magSlotScreenX + SLOT_SIZE - 1, apica$magSlotScreenY + SLOT_SIZE - 1,
                        0x80FFFFFF);
            }
        }
    }

    /**
     * Intercepte mouseClicked AVANT que vanilla ne demarre le quickcraft mode.
     * Quand le curseur porte un item, vanilla ne passe PAS par slotClicked — il demarre
     * le quickcraft (drag-split). On doit donc intercepter ici pour:
     * - Clic gauche sur le bonus slot: equip/unequip/swap
     * - Clic droit sur un IMagazineHolder: equip/unequip/swap
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void apica$onMouseClicked(double mouseX, double mouseY, int button,
                                       CallbackInfoReturnable<Boolean> cir) {
        // Clic gauche sur le bonus slot
        if (button == 0 && apica$magSlotVisible) {
            boolean onBonusSlot = mouseX >= apica$magSlotScreenX
                    && mouseX < apica$magSlotScreenX + SLOT_SIZE
                    && mouseY >= apica$magSlotScreenY
                    && mouseY < apica$magSlotScreenY + SLOT_SIZE;

            if (onBonusSlot) {
                AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
                if (apica$tryMagazineAction(self)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // Clic droit sur un slot contenant un IMagazineHolder
        if (button == 1) {
            Slot hoveredSlot = findSlot(mouseX, mouseY);
            if (hoveredSlot != null && hoveredSlot.hasItem()
                    && hoveredSlot.getItem().getItem() instanceof IMagazineHolder) {
                AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
                apica$magSlotIndex = hoveredSlot.index;
                if (apica$tryMagazineAction(self)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /** Tente equip/unequip/swap sur le slot apica$magSlotIndex. Retourne true si action effectuee. */
    @Unique
    private boolean apica$tryMagazineAction(AbstractContainerScreen<?> self) {
        if (apica$magSlotIndex < 0 || apica$magSlotIndex >= self.getMenu().slots.size()) return false;

        ItemStack holderStack = self.getMenu().slots.get(apica$magSlotIndex).getItem();
        if (!(holderStack.getItem() instanceof IMagazineHolder)) return false;

        ItemStack cursorStack = self.getMenu().getCarried();
        boolean hasMagazine = MagazineData.hasMagazine(holderStack);

        if (cursorStack.getItem() instanceof MagazineItem && !MagazineFluidData.isEmpty(cursorStack)) {
            PacketDistributor.sendToServer(new MagazineEquipPacket(apica$magSlotIndex, true));
            return true;
        } else if (hasMagazine && cursorStack.isEmpty()) {
            PacketDistributor.sendToServer(new MagazineEquipPacket(apica$magSlotIndex, false));
            return true;
        }
        return false;
    }
}
