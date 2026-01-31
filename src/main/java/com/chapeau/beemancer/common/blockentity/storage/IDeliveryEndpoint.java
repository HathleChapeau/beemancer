/**
 * ============================================================
 * [IDeliveryEndpoint.java]
 * Description: Interface pour les blocs recevant des livraisons d'abeilles
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance     | Raison                | Utilisation                    |
 * |----------------|----------------------|--------------------------------|
 * | ItemStack      | Items transportes    | Reception de livraison         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DeliveryPhaseGoal.java (livraison polymorphe)
 * - StorageTerminalBlockEntity.java (implements)
 * - ImportInterfaceBlockEntity.java (implements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import net.minecraft.world.item.ItemStack;

/**
 * Interface implementee par les blocs pouvant recevoir des items
 * livres par une DeliveryBeeEntity (terminal, import interface, etc.).
 */
public interface IDeliveryEndpoint {

    /**
     * Recoit les items livres par une abeille de livraison.
     *
     * @param items les items a inserer
     * @return le reste non insere (vide si tout a ete insere)
     */
    ItemStack receiveDeliveredItems(ItemStack items);
}
