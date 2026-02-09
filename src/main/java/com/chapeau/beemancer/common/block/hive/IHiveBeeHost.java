/**
 * ============================================================
 * [IHiveBeeHost.java]
 * Description: Interface publique pour l'interaction abeille-ruche
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                     | Raison                | Utilisation               |
 * |--------------------------------|----------------------|---------------------------|
 * | MagicBeeEntity                 | Entite abeille       | Parametre des methodes    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagicBeeEntity.java (handleBeePing, onBeeKilled)
 * - ForagingBehaviorGoal.java (getAndAssignFlower, returnFlower)
 * - MagicHiveBlockEntity.java (implements)
 * - HiveMultiblockBlockEntity.java (implements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Interface publique qu'une ruche expose aux entites abeilles.
 * Implementee par MagicHiveBlockEntity et HiveMultiblockBlockEntity.
 */
public interface IHiveBeeHost {

    /**
     * Traite un ping periodique d'une abeille dehors.
     * @return true si l'abeille doit rester en vie, false pour la discard
     */
    boolean handleBeePing(MagicBeeEntity bee);

    /**
     * Notifie la ruche qu'une abeille est morte.
     */
    void onBeeKilled(UUID beeUUID);

    /**
     * Assigne une fleur aleatoire du pool a un slot d'abeille.
     * @return la position de la fleur, ou null si aucune disponible
     */
    @Nullable
    BlockPos getAndAssignFlower(int slot);

    /**
     * Retourne une fleur au pool apres utilisation par l'abeille.
     */
    void returnFlower(int slot, BlockPos flower);

    /**
     * Position du bloc ruche dans le monde.
     */
    BlockPos getBlockPos();
}
