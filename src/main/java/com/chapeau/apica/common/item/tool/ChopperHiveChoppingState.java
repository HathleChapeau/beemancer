/**
 * ============================================================
 * [ChopperHiveChoppingState.java]
 * Description: Gestion server-side de la file de destruction du Chopper Hive
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Block               | Loot drops           | getDrops() avec outil          |
 * | MagazineData        | Consommation fluide  | consumeFluid() par buche       |
 * | ChopperHiveItem     | Vitesse fluide       | getTicksPerBlockForFluid()     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ChopperHiveItem.java (start, tick, clear)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.item.magazine.MagazineData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere la file de destruction de blocs pour le Chopper Hive.
 * Chaque joueur peut avoir une session active.
 * Phase warmup (1 seconde) avant la destruction, puis blocs detruits du haut vers le bas.
 * Consomme 5 mB de fluide par buche. Vitesse selon fluide (honey=12, royal_jelly=8, nectar=6).
 * Session s'arrete si fluide epuise.
 */
public final class ChopperHiveChoppingState {

    /** Cout fixe en mB par buche detruite. */
    private static final int COST_PER_LOG = 5;

    /** Ticks de warmup avant le debut de la destruction (1 seconde) */
    public static final int WARMUP_TICKS = 20;

    private static final Map<UUID, State> activeStates = new HashMap<>();

    private static class State {
        final List<BlockPos> queue;
        int currentIndex;
        int tickCounter;
        int warmupTicker;

        State(List<BlockPos> queue) {
            this.queue = queue;
            this.currentIndex = 0;
            this.tickCounter = 0;
            this.warmupTicker = 0;
        }
    }

    /**
     * Demarre une session de destruction pour le joueur.
     * Les positions doivent etre triees du haut vers le bas (Y decroissant).
     */
    public static void start(UUID playerId, List<BlockPos> sortedPositions) {
        if (sortedPositions.isEmpty()) return;
        activeStates.put(playerId, new State(sortedPositions));
    }

    /**
     * Tick la session du joueur. Appele chaque tick depuis inventoryTick().
     * Phase warmup (20 ticks) puis destruction avec consommation fluide.
     * Vitesse de destruction determinee par le fluide du magazine equipe.
     */
    public static void tick(Player player, ServerLevel level, ItemStack chopperStack) {
        State state = activeStates.get(player.getUUID());
        if (state == null) return;

        // Phase warmup
        if (state.warmupTicker < WARMUP_TICKS) {
            state.warmupTicker++;
            return;
        }

        if (state.currentIndex >= state.queue.size()) {
            activeStates.remove(player.getUUID());
            return;
        }

        BlockPos currentPos = state.queue.get(state.currentIndex);

        // Verifier que le bloc est toujours une buche
        BlockState blockState = level.getBlockState(currentPos);
        if (!blockState.is(BlockTags.LOGS)) {
            state.currentIndex++;
            state.tickCounter = 0;
            return;
        }

        // Vitesse selon le fluide equipe
        int ticksPerBlock = ChopperHiveItem.getTicksPerBlockForFluid(chopperStack);
        state.tickCounter++;

        if (state.tickCounter >= ticksPerBlock) {
            // Consommer le fluide avant destruction
            if (!MagazineData.consumeFluid(chopperStack, COST_PER_LOG)) {
                // Fluide epuise → arret, nouveau clic requis pour reload
                activeStates.remove(player.getUUID());
                return;
            }

            destroyAndCollect(level, player, currentPos, blockState);

            state.tickCounter = 0;
            state.currentIndex++;

            if (state.currentIndex >= state.queue.size()) {
                activeStates.remove(player.getUUID());
            }
        }
    }

    /**
     * Detruit un bloc et ajoute le loot a l'inventaire du joueur.
     * Le surplus est drop au sol.
     */
    private static void destroyAndCollect(ServerLevel level, Player player,
                                           BlockPos pos, BlockState blockState) {
        BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        ItemStack tool = player.getMainHandItem();

        List<ItemStack> drops = Block.getDrops(blockState, level, pos, blockEntity, player, tool);

        for (ItemStack drop : drops) {
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
        }

        level.destroyBlock(pos, false, player);
    }

    /** Verifie si le joueur a une session active. */
    public static boolean isActive(UUID playerId) {
        return activeStates.containsKey(playerId);
    }

    /** Annule la session du joueur. */
    public static void clear(UUID playerId) {
        activeStates.remove(playerId);
    }

    private ChopperHiveChoppingState() {
    }
}
