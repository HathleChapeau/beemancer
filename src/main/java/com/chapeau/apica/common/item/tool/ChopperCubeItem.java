/**
 * ============================================================
 * [ChopperCubeItem.java]
 * Description: Cube qui detecte, surligne et abat les buches connectees
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | BlockTags                 | Detection buches     | Verification tag logs          |
 * | ChopperCubeChoppingState  | Destruction queue    | Gestion server-side            |
 * | ChopperCubeLockHelper     | Preview client       | Verrouillage glow              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java (registration)
 * - ChopperCubePreviewRenderer.java (lecture des positions)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Chopper Cube — outil d'abattage d'arbres.
 *
 * Quand le joueur regarde une buche en tenant cet item,
 * toutes les buches connectees du meme type au-dessus (y >= cible) sont surlignees.
 * Clic droit sur une buche: demarre la destruction du haut vers le bas,
 * avec 2 abeilles orbitantes autour de chaque bloc. Le loot va dans l'inventaire.
 */
public class ChopperCubeItem extends Item {

    /** Nombre max de blocs scannes pour eviter les lags */
    public static final int MAX_SCAN = 256;

    public ChopperCubeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (!clickedState.is(BlockTags.LOGS)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            // Ignorer si deja en cours de destruction
            if (ChopperCubeChoppingState.isActive(player.getUUID())) {
                return InteractionResult.CONSUME;
            }

            // Scanner et demarrer la destruction (haut→bas, puis gauche→droite)
            List<BlockPos> logs = findConnectedLogs(level, clickedPos);
            logs.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ));
            ChopperCubeChoppingState.start(player.getUUID(), logs);
        } else {
            // Lock le preview client-side
            List<BlockPos> logs = findConnectedLogs(level, clickedPos);
            ChopperCubeLockHelper.lockWith(logs);
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Ignorer si deja en cours de destruction
        if (!level.isClientSide() && ChopperCubeChoppingState.isActive(player.getUUID())) {
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        // Verifier que le joueur tient l'item (main ou offhand)
        boolean holding = selected || player.getOffhandItem() == stack;
        if (!holding) {
            ChopperCubeChoppingState.clear(player.getUUID());
            return;
        }

        ChopperCubeChoppingState.tick(player, (ServerLevel) level);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    /**
     * Trouve toutes les buches connectees (26-way) du meme type,
     * a partir d'une position de depart, en ne descendant jamais en dessous du startY.
     *
     * @param level    Le Level (client ou server)
     * @param startPos La position de la buche ciblee
     * @return Liste des positions de buches connectees (inclut startPos)
     */
    public static List<BlockPos> findConnectedLogs(Level level, BlockPos startPos) {
        List<BlockPos> result = new ArrayList<>();
        BlockState startState = level.getBlockState(startPos);

        if (!startState.is(BlockTags.LOGS)) {
            return result;
        }

        Block targetBlock = startState.getBlock();
        int startY = startPos.getY();

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && result.size() < MAX_SCAN) {
            BlockPos current = queue.poll();
            result.add(current);

            // Scanner les 26 voisins (3x3x3 - centre)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);

                        if (visited.contains(neighbor)) continue;
                        if (neighbor.getY() < startY) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.is(targetBlock)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return result;
    }
}
