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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
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

    @Unique
    private static final Logger APICA_LOG = LoggerFactory.getLogger("ApicaMagazineMixin");

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

    @Inject(method = "render", at = @At("TAIL"))
    private void apica$renderMagazineSlot(GuiGraphics graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int leftPos = self.getGuiLeft();
        int topPos = self.getGuiTop();

        // Chercher le slot sous la souris (getSlotUnderMouse est public NeoForge)
        Slot hoveredSlot = self.getSlotUnderMouse();
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
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        APICA_LOG.info("[MAG] mouseClicked FIRED: button={} screen={}", button,
                self.getClass().getSimpleName());

        // Clic gauche sur le bonus slot
        if (button == 0 && apica$magSlotVisible) {
            boolean onBonusSlot = mouseX >= apica$magSlotScreenX
                    && mouseX < apica$magSlotScreenX + SLOT_SIZE
                    && mouseY >= apica$magSlotScreenY
                    && mouseY < apica$magSlotScreenY + SLOT_SIZE;

            if (onBonusSlot) {
                if (apica$tryMagazineAction(self)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // Clic droit sur un slot contenant un IMagazineHolder
        if (button == 1) {
            // Utiliser getSlotUnderMouse (public NeoForge) au lieu de findSlot (private)
            Slot hoveredSlot = self.getSlotUnderMouse();
            APICA_LOG.info("[MAG] Right-click: hoveredSlot={}, hasItem={}, isHolder={}",
                    hoveredSlot != null ? hoveredSlot.index : "null",
                    hoveredSlot != null && hoveredSlot.hasItem(),
                    hoveredSlot != null && hoveredSlot.hasItem()
                            && hoveredSlot.getItem().getItem() instanceof IMagazineHolder);
            if (hoveredSlot != null && hoveredSlot.hasItem()
                    && hoveredSlot.getItem().getItem() instanceof IMagazineHolder) {
                apica$magSlotIndex = hoveredSlot.index;
                boolean result = apica$tryMagazineAction(self);
                APICA_LOG.info("[MAG] tryMagazineAction result={}", result);
                if (result) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    /** Tente equip/unequip/swap sur le slot apica$magSlotIndex. Retourne true si action effectuee. */
    @Unique
    private boolean apica$tryMagazineAction(AbstractContainerScreen<?> self) {
        if (apica$magSlotIndex < 0 || apica$magSlotIndex >= self.getMenu().slots.size()) {
            APICA_LOG.info("[MAG] tryAction: invalid index {}", apica$magSlotIndex);
            return false;
        }

        ItemStack holderStack = self.getMenu().slots.get(apica$magSlotIndex).getItem();
        if (!(holderStack.getItem() instanceof IMagazineHolder)) {
            APICA_LOG.info("[MAG] tryAction: slot {} not IMagazineHolder (item={})",
                    apica$magSlotIndex, holderStack.getItem().getClass().getSimpleName());
            return false;
        }

        ItemStack cursorStack = self.getMenu().getCarried();
        boolean hasMagazine = MagazineData.hasMagazine(holderStack);
        boolean isMagItem = cursorStack.getItem() instanceof MagazineItem;
        boolean magNotEmpty = !MagazineFluidData.isEmpty(cursorStack);
        String fluidId = isMagItem ? MagazineFluidData.getFluidId(cursorStack) : "N/A";

        APICA_LOG.info("[MAG] tryAction: cursor={} isMagItem={} magNotEmpty={} fluidId='{}' hasMagazine={} cursorEmpty={}",
                cursorStack.getItem().getClass().getSimpleName(),
                isMagItem, magNotEmpty, fluidId, hasMagazine, cursorStack.isEmpty());

        if (isMagItem && magNotEmpty) {
            APICA_LOG.info("[MAG] SENDING EQUIP packet slotIndex={}", apica$magSlotIndex);
            PacketDistributor.sendToServer(new MagazineEquipPacket(apica$magSlotIndex, true));
            return true;
        } else if (hasMagazine && cursorStack.isEmpty()) {
            APICA_LOG.info("[MAG] SENDING UNEQUIP packet slotIndex={}", apica$magSlotIndex);
            PacketDistributor.sendToServer(new MagazineEquipPacket(apica$magSlotIndex, false));
            return true;
        }
        APICA_LOG.info("[MAG] tryAction: no action taken");
        return false;
    }
}
