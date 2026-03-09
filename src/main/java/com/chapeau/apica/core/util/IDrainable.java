/**
 * ============================================================
 * [IDrainable.java]
 * Description: Interface pour les BlockEntities dont le fluide peut etre vide par shift+clic
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FluidTank           | Tank NeoForge        | Acces au fluide a drainer      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - LaunchpadBlockEntity
 * - HoneyLampBlockEntity
 * - HoneyReservoirBlockEntity
 * - LiquidPipeBlockEntity
 *
 * ============================================================
 */
package com.chapeau.apica.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public interface IDrainable {

    FluidTank getDrainableTank();

    String getDrainableEmptyName();

    static boolean tryDrain(Level level, BlockPos pos, Player player, IDrainable drainable) {
        FluidTank tank = drainable.getDrainableTank();
        int drained = tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE).getAmount();
        if (drained > 0) {
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5f, 1.0f);
            player.displayClientMessage(Component.literal("Drained " + drained + " mB"), true);
            return true;
        }
        player.displayClientMessage(Component.literal(drainable.getDrainableEmptyName() + " is empty"), true);
        return false;
    }
}
