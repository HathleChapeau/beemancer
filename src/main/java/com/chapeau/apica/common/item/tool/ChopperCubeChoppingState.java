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
 * | Block               | Loot drops           | getDrops() avec outil          |
 * | ParticleHelper      | Particules rune      | Emission pendant destruction   |
 * | ApicaParticles      | Registre particules  | RUNE particle type             |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ChopperCubeItem.java (start, tick, clear)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.core.registry.ApicaParticles;
import com.chapeau.apica.core.util.ParticleHelper;
import net.minecraft.core.BlockPos;
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
 * Phase warmup (1 seconde) avant la destruction, puis blocs detruits du haut vers le bas.
 * Emet des particules rune pendant la phase de destruction active.
 */
public final class ChopperCubeChoppingState {

    /** Ticks entre chaque destruction de bloc */
    private static final int TICKS_PER_BLOCK = 12;

    /** Ticks de warmup avant le debut de la destruction (1 seconde) */
    public static final int WARMUP_TICKS = 20;

    /** Frequence de spawn des particules rune (toutes les N ticks) */
    private static final int PARTICLE_INTERVAL = 3;

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
     * Phase warmup (20 ticks) puis destruction avec particules rune.
     */
    public static void tick(Player player, ServerLevel level) {
        State state = activeStates.get(player.getUUID());
        if (state == null) return;

        // Phase warmup: attendre avant de commencer la destruction
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

        // Particules rune pendant la destruction active
        if (state.tickCounter % PARTICLE_INTERVAL == 0) {
            spawnRuneParticles(level, player);
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
     * Spawn quelques particules rune pres de la main du joueur.
     * Visible en first et third person (server-side broadcast).
     */
    private static void spawnRuneParticles(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 handPos = player.getEyePosition()
                .add(look.scale(0.5))
                .add(0, -0.4, 0);
        ParticleHelper.spawnParticles(level, ApicaParticles.RUNE.get(),
                handPos, 1, 0.1, 0.02);
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

    private ChopperCubeChoppingState() {
    }
}
