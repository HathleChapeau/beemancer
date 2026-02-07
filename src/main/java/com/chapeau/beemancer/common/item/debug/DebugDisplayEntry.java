/**
 * ============================================================
 * [DebugDisplayEntry.java]
 * Description: Entrée d'affichage debug enregistrée via DebugWandItem.addDisplay()
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Vec3                | Position + offset    | Coordonnées monde              |
 * | Supplier            | Lazy evaluation      | Position et texte dynamiques   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - DebugWandItem.java: Stockage dans la liste statique
 * - CustomDebugDisplayRenderer.java: Lecture pour rendu
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.debug;

import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Entrée d'affichage debug: position dynamique + texte dynamique + offset + couleur.
 * Si positionSupplier renvoie null, l'entrée est considérée invalide et sera retirée.
 */
public record DebugDisplayEntry(
    Supplier<Vec3> positionSupplier,
    Supplier<String> textSupplier,
    Vec3 offset,
    int color
) {}
