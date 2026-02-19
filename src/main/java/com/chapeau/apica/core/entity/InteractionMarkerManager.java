/**
 * ============================================================
 * [InteractionMarkerManager.java]
 * Description: Helper statique pour spawn/despawn/find des marqueurs d'interaction
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InteractionMarkerEntity | Entité gérée         | Création et recherche          |
 * | InteractionMarkerTypes  | Définition des types | Offset et taille               |
 * | ApicaEntities           | EntityType registré  | Instanciation                  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - AssemblyTableBlockEntity.java: Spawn/despawn au place/remove
 * - AssemblyTableBlock.java: Despawn au onRemove
 * - Tout système utilisant des marqueurs d'interaction
 *
 * ============================================================
 */
package com.chapeau.apica.core.entity;

import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Gestion centralisée du cycle de vie des InteractionMarkerEntity.
 * Fournit des méthodes statiques pour spawn, despawn et recherche.
 */
public final class InteractionMarkerManager {

    private InteractionMarkerManager() {}

    /**
     * Spawn un marqueur d'interaction au-dessus du bloc ancre.
     * Retourne l'entité créée, ou null si le type n'est pas enregistré.
     */
    @Nullable
    public static InteractionMarkerEntity spawn(ServerLevel level, BlockPos anchor, String markerType) {
        InteractionMarkerTypes.MarkerType type = InteractionMarkerTypes.get(markerType);
        if (type == null) return null;

        InteractionMarkerEntity entity = ApicaEntities.INTERACTION_MARKER.get().create(level);
        if (entity == null) return null;

        Vec3 offset = type.spawnOffset();
        entity.setPos(anchor.getX() + offset.x, anchor.getY() + offset.y, anchor.getZ() + offset.z);
        entity.setAnchorPos(anchor);
        entity.setMarkerType(markerType);
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Supprime tous les marqueurs du type donné ancrés à la position spécifiée.
     */
    public static void despawn(ServerLevel level, BlockPos anchor, String markerType) {
        List<InteractionMarkerEntity> markers = findAll(level, anchor, markerType);
        for (InteractionMarkerEntity marker : markers) {
            marker.discard();
        }
    }

    /**
     * Trouve un marqueur du type donné ancré à la position spécifiée.
     * Retourne le premier trouvé, ou null.
     */
    @Nullable
    public static InteractionMarkerEntity find(Level level, BlockPos anchor, String markerType) {
        List<InteractionMarkerEntity> markers = findAll(level, anchor, markerType);
        return markers.isEmpty() ? null : markers.getFirst();
    }

    /**
     * Trouve tous les marqueurs du type donné ancrés à la position spécifiée.
     */
    public static List<InteractionMarkerEntity> findAll(Level level, BlockPos anchor, String markerType) {
        AABB searchBox = new AABB(anchor).inflate(2.0);
        return level.getEntitiesOfClass(InteractionMarkerEntity.class, searchBox,
                e -> e.getAnchorPos().equals(anchor) && markerType.equals(e.getMarkerType()));
    }
}
