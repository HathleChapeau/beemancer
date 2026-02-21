/**
 * ============================================================
 * [PipeEndpoint.java]
 * Description: Point d'entrée/sortie du réseau de pipes (machine connectée)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BlockPos            | Position monde       | Localisation pipe et machine   |
 * | Direction           | Face de connexion    | Côté de la machine connectée   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PipeNetwork.java (collecte des endpoints du réseau)
 * - ItemPipeBlockEntity.java (routage des items)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Représente une connexion entre un pipe et une machine (inventaire).
 * Un endpoint EXTRACT signifie que le pipe peut tirer des items de la machine.
 * Un endpoint INSERT signifie que le pipe peut pousser des items dans la machine.
 */
public record PipeEndpoint(
    BlockPos pipePos,
    Direction face,
    BlockPos machinePos,
    EndpointType type
) {

    /**
     * Type de point de connexion dans le réseau.
     */
    public enum EndpointType {
        /** Le pipe extrait des items de la machine connectée. */
        EXTRACT,
        /** Le pipe insère des items dans la machine connectée. */
        INSERT
    }
}
