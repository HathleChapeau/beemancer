/**
 * ============================================================
 * [SplitFluidHandler.java]
 * Description: IFluidHandler wrapper qui route fill() vers un tank input et drain() vers un tank output
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | FluidTank           | Tanks wrappés        | Délégation fill/drain          |
 * | IFluidHandler       | Interface implémentée| Exposition capability          |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CentrifugeHeartBlockEntity (fuelTank input, outputTank output)
 * - Tout multibloc nécessitant une séparation fill/drain
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.util;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Wrapper IFluidHandler qui sépare les opérations d'entrée et de sortie.
 * fill() est routé vers le tank d'entrée (ex: fuel).
 * drain() est routé vers le tank de sortie (ex: produit).
 */
public class SplitFluidHandler implements IFluidHandler {

    private final FluidTank inputTank;
    private final FluidTank outputTank;

    public SplitFluidHandler(FluidTank inputTank, FluidTank outputTank) {
        this.inputTank = inputTank;
        this.outputTank = outputTank;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? inputTank.getFluid() : outputTank.getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? inputTank.getCapacity() : outputTank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 ? inputTank.isFluidValid(stack) : outputTank.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return inputTank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return outputTank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return outputTank.drain(maxDrain, action);
    }
}
