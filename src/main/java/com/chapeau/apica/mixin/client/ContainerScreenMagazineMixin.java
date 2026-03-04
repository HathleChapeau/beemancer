/**
 * ============================================================
 * [ContainerScreenMagazineMixin.java]
 * Description: Mixin client pour le clic droit equip/unequip magazine sur un IMagazineHolder
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

import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.magazine.MagazineFluidData;
import com.chapeau.apica.common.item.magazine.MagazineItem;
import com.chapeau.apica.core.network.packets.MagazineEquipPacket;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clic droit sur un IMagazineHolder pour equiper/desequiper un magazine.
 * L'indicateur visuel du magazine equipe est gere par IItemDecorator (ClientSetup).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenMagazineMixin {

    /**
     * Intercepte le clic droit sur un IMagazineHolder pour equiper/desequiper un magazine.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void apica$onMouseClicked(double mouseX, double mouseY, int button,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (button != 1) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        Slot hoveredSlot = self.getSlotUnderMouse();

        if (hoveredSlot != null && hoveredSlot.hasItem()
                && hoveredSlot.getItem().getItem() instanceof IMagazineHolder) {
            // Toujours bloquer le clic droit vanilla sur un holder (empeche de prendre l'item)
            cir.setReturnValue(true);

            ItemStack cursorStack = self.getMenu().getCarried();
            boolean hasMagazine = MagazineData.hasMagazine(hoveredSlot.getItem());
            boolean isMagItem = cursorStack.getItem() instanceof MagazineItem;
            boolean magNotEmpty = !MagazineFluidData.isEmpty(cursorStack);

            if (isMagItem && magNotEmpty) {
                PacketDistributor.sendToServer(new MagazineEquipPacket(hoveredSlot.index, true));
            } else if (hasMagazine && cursorStack.isEmpty()) {
                PacketDistributor.sendToServer(new MagazineEquipPacket(hoveredSlot.index, false));
            }
        }
    }
}
