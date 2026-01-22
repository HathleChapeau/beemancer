/**
 * ============================================================
 * [WandTrackedData.java]
 * Description: Définition d'une donnée trackée par la baguette
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeeWandItem.java: Configuration des données à tracker
 * - WandOverlayRenderer.java: Affichage des données
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import java.util.function.Function;

/**
 * Représente une donnée trackée pour une classe spécifique.
 *
 * @param <T> Le type de l'objet source
 */
public class WandTrackedData<T> {
    private final String name;
    private final Function<T, Object> valueGetter;
    
    public WandTrackedData(String name, Function<T, Object> valueGetter) {
        this.name = name;
        this.valueGetter = valueGetter;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Récupère la valeur de la donnée depuis l'objet source.
     * Retourne "" si null.
     */
    public String getValue(T source) {
        Object value = valueGetter.apply(source);
        return value != null ? value.toString() : "";
    }
    
    /**
     * Récupère la valeur brute.
     */
    public Object getRawValue(T source) {
        return valueGetter.apply(source);
    }
}
