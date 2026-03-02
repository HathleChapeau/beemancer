/**
 * ============================================================
 * [ChopperCubeChoppingState.java]
 * Description: Gestion server-side de la file de destruction du Chopper Cube
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ParticleHelper      | Effets visuels       | Particules burst destruction   |
 * | Block               | Loot drops           | getDrops() avec outil          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ChopperCubeItem.java (start, tick, clear)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gere la file de destruction de blocs pour le Chopper Cube.
 * Chaque joueur peut avoir une session active.
 * Les blocs sont detruits du haut vers le bas, un par un.
 */
public final class ChopperCubeChoppingState {

    /** Ticks entre chaque destruction de bloc */
    private static final int TICKS_PER_BLOCK = 6;

    private static final Map<UUID, State> activeStates = new HashMap<>();

    private static class State {
        final List<BlockPos> queue;
        int currentIndex;
        int tickCounter;

        State(List<BlockPos> queue) {
            this.queue = queue;
            this.currentIndex = 0;
            this.tickCounter = 0;
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
     * Gere l'animation des abeilles et la destruction des blocs.
     */
    public static void tick(Player player, ServerLevel level) {
        State state = activeStates.get(player.getUUID());
        if (state == null) return;

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

        state.tickCounter++;

        if (state.tickCounter >= TICKS_PER_BLOCK) {
            // Detruire le bloc et collecter le loot
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

        // Burst de particules a la destruction
        ParticleHelper.burst(level, Vec3.atCenterOf(pos), ParticleTypes.WAX_ON, 6);
    }

    /** Verifie si le joueur a une session active. */
    public static boolean isActive(UUID playerId) {
        return activeStates.containsKey(playerId);
    }

    /** Annule la session du joueur. */
    public static void clear(UUID playerId) {
        activeStates.remove(playerId);
    }

    private ChopperCubeChoppingState() {
    }
}
