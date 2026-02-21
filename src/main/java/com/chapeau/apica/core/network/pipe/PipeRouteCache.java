/**
 * ============================================================
 * [PipeRouteCache.java]
 * Description: Cache de routes BFS pour le réseau de pipes
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BlockPos            | Clé de cache         | Source et destination           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PipeNetwork.java (cache des routes calculées)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.pipe;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache de routes pré-calculées entre positions de pipes.
 * Invalidé entièrement à chaque changement topologique du réseau (ajout/retrait de pipe, toggle connexion).
 * Les routes sont calculées à la demande par BFS et mises en cache pour réutilisation.
 */
public class PipeRouteCache {
    private final Map<Long, List<BlockPos>> cache = new HashMap<>();

    /**
     * Génère une clé unique pour le couple source-destination.
     * Utilise le hash des deux positions combiné en un long.
     */
    private static long cacheKey(BlockPos from, BlockPos to) {
        return ((long) from.hashCode() << 32) | (to.hashCode() & 0xFFFFFFFFL);
    }

    /**
     * Récupère une route cachée, ou null si absente.
     */
    @Nullable
    public List<BlockPos> getCachedRoute(BlockPos from, BlockPos to) {
        return cache.get(cacheKey(from, to));
    }

    /**
     * Stocke une route dans le cache.
     */
    public void putRoute(BlockPos from, BlockPos to, List<BlockPos> route) {
        cache.put(cacheKey(from, to), route);
    }

    /**
     * Invalide tout le cache. Appelé à chaque changement topologique.
     */
    public void invalidateAll() {
        cache.clear();
    }

    /**
     * Invalide toutes les routes passant par une position donnée.
     * Plus ciblé que invalidateAll() mais plus coûteux à calculer.
     */
    public void invalidateRoutesThrough(BlockPos pos) {
        cache.entrySet().removeIf(entry ->
            entry.getValue().contains(pos)
        );
    }

    public int size() {
        return cache.size();
    }
}
