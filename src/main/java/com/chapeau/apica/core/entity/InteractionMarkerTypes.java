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
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.entity.mount.HoverbikeStatRoller;
import com.chapeau.apica.common.item.mount.CreativeFocusItem;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
                // Validity: la table existe et contient une piece
                (level, pos) -> level.getBlockEntity(pos) instanceof AssemblyTableBlockEntity table
                        && !table.isEmpty(),
                // Handler: ajouter une stat avec le Creative Focus
                InteractionMarkerTypes::handleAssemblyFocus,
                // Spawn offset: au-dessus de la piece qui tourne
                new Vec3(0.5, 1.3, 0.5),
                0.5F, 0.5F
        ));

        // 4 types pour les pieces du hoverbike en edit mode
        for (HoverbikePart part : HoverbikePart.values()) {
            String typeId = "hoverbike_part_" + part.name().toLowerCase();
            register(typeId, new MarkerType(
                    // Validity: l'entite ancre existe (verifie par isEntityAnchored dans tick)
                    (level, pos) -> true,
                    // Handler: ouvre le menu de la piece
                    (marker, player, hand) -> handleHoverbikePart(marker, player, hand, part),
                    // Spawn offset: calcule dynamiquement par HoverbikeEntity
                    Vec3.ZERO,
                    0.5F, 0.5F
            ));
        }
    }

    /**
     * Handler d'interaction pour les marqueurs "hoverbike_part_*".
     * Si le joueur tient une piece compatible: swap. Sinon: ouvre le menu.
     */
    private static InteractionResult handleHoverbikePart(
            InteractionMarkerEntity marker, Player player, InteractionHand hand, HoverbikePart part) {
        Entity anchorEntity = marker.level().getEntity(marker.getAnchorEntityId());
        if (!(anchorEntity instanceof HoverbikeEntity hoverbike)) return InteractionResult.FAIL;
        if (!hoverbike.isEditMode()) return InteractionResult.FAIL;
        if (!hoverbike.isOwner(player)) return InteractionResult.PASS;

        if (player.level().isClientSide()) {
            openPartScreen(part, hoverbike);
            return InteractionResult.SUCCESS;
        }

        // Server-side: check if player holds a compatible piece for quick swap
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof HoverbikePartItem heldPart
                && heldPart.getCategory() == part) {
            ItemStack currentOnBike = hoverbike.getPartStack(part).copy();
            hoverbike.setPartStack(part, held.copy());
            player.setItemInHand(hand, currentOnBike);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * Ouvre le HoverbikePartScreen cote client.
     * Methode separee pour eviter les references client dans le code commun.
     */
    private static void openPartScreen(HoverbikePart part, HoverbikeEntity hoverbike) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            Minecraft.getInstance().setScreen(
                    new com.chapeau.apica.client.gui.screen.HoverbikePartScreen(part, hoverbike));
        }
    }

    /**
     * Handler d'interaction pour le marqueur "assembly_focus".
     * Verifie que le joueur tient un Creative Focus, puis roll une stat sur la piece.
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
