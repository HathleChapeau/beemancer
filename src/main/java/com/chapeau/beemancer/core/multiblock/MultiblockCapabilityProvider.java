/**
 * ============================================================
 * [MultiblockCapabilityProvider.java]
 * Description: Interface pour les contrôleurs multibloc qui délèguent des capabilities aux blocs structurels
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IFluidHandler       | Capability fluide    | Délégation fluid handler       |
 * | IItemHandler        | Capability item      | Délégation item handler        |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CentrifugeHeartBlockEntity (implémente pour la centrifuge multibloc)
 * - HoneyReservoirBlockEntity (query via findCapabilityProvider)
 * - Beemancer.java (capability registration lambdas)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Interface implémentée par les contrôleurs de multibloc qui exposent des capabilities
 * sur leurs blocs structurels. Quand un bloc structurel (ex: réservoir) reçoit une requête
 * de capability, il délègue au contrôleur via cette interface.
 */
public interface MultiblockCapabilityProvider {

    /**
     * Retourne le fluid handler à exposer pour un bloc structurel donné.
     * @param worldPos Position monde du bloc structurel querié
     * @param face Face du bloc querié (null si pas de direction)
     * @return Le handler à exposer, ou null si pas de fluid handler pour cette position/face
     */
    @Nullable
    default IFluidHandler getFluidHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        return null;
    }

    /**
     * Retourne l'item handler à exposer pour un bloc structurel donné.
     * @param worldPos Position monde du bloc structurel querié
     * @param face Face du bloc querié (null si pas de direction)
     * @return Le handler à exposer, ou null si pas d'item handler pour cette position/face
     */
    @Nullable
    default IItemHandler getItemHandlerForBlock(BlockPos worldPos, @Nullable Direction face) {
        return null;
    }
}
