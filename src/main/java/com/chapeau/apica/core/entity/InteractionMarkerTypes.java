/**
 * ============================================================
 * [InteractionMarkerTypes.java]
 * Description: Registre de types de marqueurs d'interaction réutilisables
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | InteractionMarkerEntity   | Entité de marqueur   | Paramètre du callback          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - InteractionMarkerEntity.java: Délégation des interactions et validité
 * - InteractionMarkerManager.java: Offset de spawn et taille
 * - Tout système enregistrant un nouveau type de marqueur
 *
 * ============================================================
 */
package com.chapeau.apica.core.entity;

import com.chapeau.apica.common.blockentity.mount.AssemblyTableBlockEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeStatRoller;
import com.chapeau.apica.common.item.mount.CreativeFocusItem;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Registre centralisé des types de marqueurs d'interaction.
 * Chaque type définit une condition de validité, un handler d'interaction,
 * un offset de spawn et une taille de hitbox.
 * Architecture ouverte : tout système peut enregistrer un nouveau type via register().
 */
public final class InteractionMarkerTypes {

    private static final Map<String, MarkerType> REGISTRY = new HashMap<>();

    /**
     * Définition d'un type de marqueur d'interaction.
     *
     * @param validityCheck vérifie si le marqueur est encore valide (appelé chaque tick server)
     * @param handler       callback d'interaction quand un joueur clique droit sur le marqueur
     * @param spawnOffset   offset par rapport au bloc ancre pour positionner l'entité
     * @param width         largeur de la hitbox
     * @param height        hauteur de la hitbox
     */
    public record MarkerType(
            BiPredicate<Level, BlockPos> validityCheck,
            InteractionCallback handler,
            Vec3 spawnOffset,
            float width,
            float height
    ) {}

    /**
     * Callback d'interaction pour un marqueur.
     */
    @FunctionalInterface
    public interface InteractionCallback {
        InteractionResult handle(InteractionMarkerEntity entity, Player player, InteractionHand hand);
    }

    /**
     * Enregistre un nouveau type de marqueur.
     */
    public static void register(String id, MarkerType type) {
        REGISTRY.put(id, type);
    }

    /**
     * Récupère un type de marqueur par son identifiant.
     */
    @Nullable
    public static MarkerType get(String id) {
        return REGISTRY.get(id);
    }

    /**
     * Retourne tous les types enregistrés (lecture seule).
     */
    public static Map<String, MarkerType> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    /**
     * Initialise les types built-in du mod.
     * Appelé au démarrage depuis Apica.java (FMLCommonSetupEvent).
     */
    public static void init() {
        register("assembly_focus", new MarkerType(
                // Validity: la table existe et contient une pièce
                (level, pos) -> level.getBlockEntity(pos) instanceof AssemblyTableBlockEntity table
                        && !table.isEmpty(),
                // Handler: ajouter une stat avec le Creative Focus
                InteractionMarkerTypes::handleAssemblyFocus,
                // Spawn offset: au-dessus de la pièce qui tourne
                new Vec3(0.5, 1.3, 0.5),
                0.5F, 0.5F
        ));
    }

    /**
     * Handler d'interaction pour le marqueur "assembly_focus".
     * Vérifie que le joueur tient un Creative Focus, puis roll une stat sur la pièce.
     */
    private static InteractionResult handleAssemblyFocus(
            InteractionMarkerEntity entity, Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return CreativeFocusItem.isCreativeFocus(player.getItemInHand(hand))
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!CreativeFocusItem.isCreativeFocus(held)) return InteractionResult.PASS;

        BlockPos tablePos = entity.getAnchorPos();
        if (!(player.level().getBlockEntity(tablePos) instanceof AssemblyTableBlockEntity table)) {
            return InteractionResult.FAIL;
        }

        ItemStack storedItem = table.getStoredItem();
        if (storedItem.isEmpty() || !HoverbikePartItem.isHoverbikePart(storedItem)) {
            return InteractionResult.FAIL;
        }

        boolean success = HoverbikeStatRoller.rollAndApply(storedItem, player.getRandom());
        if (success) {
            table.syncToClient();
            // Particules de succès
            if (player.level() instanceof ServerLevel serverLevel) {
                Vec3 center = Vec3.atCenterOf(tablePos).add(0, 1.0, 0);
                ParticleHelper.burst(serverLevel, center, ParticleHelper.EffectType.MAGIC, 12);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
