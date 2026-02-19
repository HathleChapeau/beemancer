/**
 * ============================================================
 * [AppliedStat.java]
 * Description: Record représentant une base stat fixe d'une pièce de hoverbike
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeStatType   | Enum des stats       | Identifie la stat              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoverbikePartData.java: Lecture des base stats
 * - HoverbikeSettingsComputer.java: Calcul des stats effectives
 * - AssemblyTableStatsRenderer.java: Affichage billboard
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikeStatType;

/**
 * Base stat fixe d'une pièce de hoverbike.
 * Les base stats sont définies par variant dans le JSON et lues dynamiquement.
 * Elles ne sont PAS stockées en NBT sur l'ItemStack.
 *
 * @param statType le type de statistique
 * @param value    la valeur fixe
 */
public record AppliedStat(HoverbikeStatType statType, double value) {
}
