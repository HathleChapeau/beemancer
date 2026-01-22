/**
 * ============================================================
 * [WandClassTracker.java]
 * Description: Configuration de tracking pour une classe spécifique
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | WandTrackedData     | Données trackées     | Liste des données à afficher   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeeWandItem.java: Configuration des classes trackées
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Configuration de tracking pour une classe d'objets spécifique.
 *
 * @param <T> Le type de la classe trackée
 */
public class WandClassTracker<T> {
    private final Class<T> trackedClass;
    private final List<WandTrackedData<T>> trackedData = new ArrayList<>();
    
    public WandClassTracker(Class<T> trackedClass) {
        this.trackedClass = trackedClass;
    }
    
    /**
     * Ajoute une donnée à tracker.
     *
     * @param name Nom affiché de la donnée
     * @param valueGetter Fonction pour récupérer la valeur
     * @return this pour chaînage
     */
    public WandClassTracker<T> track(String name, Function<T, Object> valueGetter) {
        trackedData.add(new WandTrackedData<>(name, valueGetter));
        return this;
    }
    
    public Class<T> getTrackedClass() {
        return trackedClass;
    }
    
    public List<WandTrackedData<T>> getTrackedData() {
        return trackedData;
    }
    
    /**
     * Vérifie si l'objet est une instance de la classe trackée.
     */
    public boolean matches(Object obj) {
        return trackedClass.isInstance(obj);
    }
    
    /**
     * Récupère toutes les valeurs pour un objet donné.
     * Retourne une liste de paires (nom, valeur).
     */
    @SuppressWarnings("unchecked")
    public List<TrackedValue> getValues(Object obj) {
        if (!matches(obj)) {
            return List.of();
        }
        
        T typedObj = (T) obj;
        List<TrackedValue> values = new ArrayList<>();
        
        for (WandTrackedData<T> data : trackedData) {
            values.add(new TrackedValue(data.getName(), data.getValue(typedObj)));
        }
        
        return values;
    }
    
    /**
     * Représente une paire nom-valeur trackée.
     */
    public record TrackedValue(String name, String value) {}
}
