/**
 * ============================================================
 * [InteractionMarkerManager.java]
 * Description: Helper statique pour spawn/despawn/find des marqueurs d'interaction
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | InteractionMarkerEntity | Entite geree         | Creation et recherche          |
 * | InteractionMarkerTypes  | Definition des types | Offset et taille               |
 * | ApicaEntities           | EntityType registre  | Instanciation                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AssemblyTableBlockEntity.java: Spawn/despawn au place/remove (bloc anchor)
 * - HoverbikeEntity.java: Spawn/despawn en edit mode (entity anchor)
 *
 * ============================================================
 */
package com.chapeau.apica.core.entity;

import com.chapeau.apica.core.registry.ApicaEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Gestion centralisee du cycle de vie des InteractionMarkerEntity.
 * Supporte deux modes d'ancrage: bloc (BlockPos) et entite (entity ID).
 */
public final class InteractionMarkerManager {

    private InteractionMarkerManager() {}

    // =========================================================================
    // BLOCK ANCHOR (pour Assembly Table, etc.)
    // =========================================================================

    /**
     * Spawn un marqueur d'interaction au-dessus du bloc ancre.
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
     * Supprime tous les marqueurs du type donne ancres a la position specifiee.
     */
    public static void despawn(ServerLevel level, BlockPos anchor, String markerType) {
        List<InteractionMarkerEntity> markers = findAll(level, anchor, markerType);
        for (InteractionMarkerEntity marker : markers) {
            marker.discard();
        }
    }

    /**
     * Trouve un marqueur du type donne ancre a la position specifiee.
     */
    @Nullable
    public static InteractionMarkerEntity find(Level level, BlockPos anchor, String markerType) {
        List<InteractionMarkerEntity> markers = findAll(level, anchor, markerType);
        return markers.isEmpty() ? null : markers.getFirst();
    }

    /**
     * Trouve tous les marqueurs du type donne ancres a la position specifiee.
     */
    public static List<InteractionMarkerEntity> findAll(Level level, BlockPos anchor, String markerType) {
        AABB searchBox = new AABB(anchor).inflate(2.0);
        return level.getEntitiesOfClass(InteractionMarkerEntity.class, searchBox,
                e -> e.getAnchorPos().equals(anchor) && markerType.equals(e.getMarkerType()));
    }

    // =========================================================================
    // ENTITY ANCHOR (pour Hoverbike edit mode, etc.)
    // =========================================================================

    /**
     * Spawn un marqueur d'interaction ancre a une entite.
     * Le marqueur est place a la position de l'entite + l'offset fourni.
     *
     * @param level le ServerLevel
     * @param anchorEntity l'entite a laquelle le marqueur est ancre
     * @param markerType le type de marqueur enregistre
     * @param partOrdinal l'ordinal de la piece (pour les hoverbike parts)
     * @param worldOffset offset en espace monde (deja calcule avec rotation)
     */
    @Nullable
    public static InteractionMarkerEntity spawnForEntity(ServerLevel level, Entity anchorEntity,
                                                          String markerType, int partOrdinal,
                                                          Vec3 worldOffset) {
        InteractionMarkerTypes.MarkerType type = InteractionMarkerTypes.get(markerType);
        if (type == null) return null;

        InteractionMarkerEntity entity = ApicaEntities.INTERACTION_MARKER.get().create(level);
        if (entity == null) return null;

        Vec3 anchorPos = anchorEntity.position();
        entity.setPos(anchorPos.x + worldOffset.x, anchorPos.y + worldOffset.y, anchorPos.z + worldOffset.z);
        entity.setAnchorPos(anchorEntity.blockPosition());
        entity.setAnchorEntityId(anchorEntity.getId());
        entity.setMarkerType(markerType);
        entity.setPartOrdinal(partOrdinal);
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Supprime tous les marqueurs du type donne ancres a l'entite specifiee.
     */
    public static void despawnForEntity(ServerLevel level, int anchorEntityId, String markerType) {
        List<InteractionMarkerEntity> markers = findAllForEntity(level, anchorEntityId, markerType);
        for (InteractionMarkerEntity marker : markers) {
            marker.discard();
        }
    }

    /**
     * Trouve tous les marqueurs ancres a une entite donnee, d'un type donne.
     * Recherche dans un rayon de 10 blocs autour de la position de l'entite.
     */
    public static List<InteractionMarkerEntity> findAllForEntity(Level level, int anchorEntityId, String markerType) {
        Entity anchor = level.getEntity(anchorEntityId);
        if (anchor == null) return List.of();
        AABB searchBox = anchor.getBoundingBox().inflate(10.0);
        return level.getEntitiesOfClass(InteractionMarkerEntity.class, searchBox,
                e -> e.getAnchorEntityId() == anchorEntityId && markerType.equals(e.getMarkerType()));
    }
}
