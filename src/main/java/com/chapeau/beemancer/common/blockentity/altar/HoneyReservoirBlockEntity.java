/**
 * ============================================================
 * [HoneyReservoirBlockEntity.java]
 * Description: BlockEntity pour le réservoir de fluide de l'Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | BeemancerBlockEntities  | Type registration   | super()               |
 * | BeemancerFluids         | Fluides acceptés    | isFluidValid          |
 * | HoneyReservoirBlock     | BlockState update   | FLUID_LEVEL           |
 * | HoneyCrystalBlockEntity | Multiblock check    | isPartOfFormedMultiblock |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyReservoirBlock.java
 * - HoneyCrystalBlockEntity.java (query reservoirs)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.altar;

import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

/**
 * Stocke jusqu'à 4000mB de miel, royal jelly ou nectar.
 * Un seul type de fluide à la fois.
 */
public class HoneyReservoirBlockEntity extends BlockEntity implements IFluidHandler {
    public static final int CAPACITY = 4000;

    private final FluidTank fluidTank;

    public HoneyReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.HONEY_RESERVOIR.get(), pos, state);
        this.fluidTank = new FluidTank(CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide()) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }
        };
    }

    public void serverTick() {
        // Pas de logique tick pour l'instant
    }

    /**
     * Met à jour le blockstate FLUID_LEVEL basé sur le contenu du tank.
     */
    public void updateFluidLevel() {
        if (level == null || level.isClientSide()) return;

        int newLevel = calculateFluidLevel();
        BlockState currentState = getBlockState();
        int currentLevel = currentState.getValue(HoneyReservoirBlock.FLUID_LEVEL);

        if (newLevel != currentLevel) {
            level.setBlock(worldPosition, currentState.setValue(HoneyReservoirBlock.FLUID_LEVEL, newLevel), 3);
        }
    }

    /**
     * Calcule le niveau de fluide (0-4) basé sur le remplissage.
     */
    private int calculateFluidLevel() {
        if (fluidTank.isEmpty()) return 0;
        float ratio = (float) fluidTank.getFluidAmount() / CAPACITY;
        if (ratio <= 0.25f) return 1;
        if (ratio <= 0.50f) return 2;
        if (ratio <= 0.75f) return 3;
        return 4;
    }

    /**
     * Vérifie si ce réservoir fait partie d'un multiblock formé.
     * Cherche le HoneyCrystal (contrôleur) 2 blocs en dessous.
     */
    public boolean isPartOfFormedMultiblock() {
        if (level == null) return false;

        // Le réservoir est à Y+2 relatif au crystal (Y+0)
        BlockPos crystalPos = worldPosition.below(2);
        BlockEntity be = level.getBlockEntity(crystalPos);
        if (be instanceof HoneyCrystalBlockEntity crystal) {
            return crystal.isFormed();
        }
        return false;
    }

    // --- IFluidHandler implementation ---

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluidTank.getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return CAPACITY;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return fluidTank.isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        int filled = fluidTank.fill(resource, action);
        if (action.execute() && filled > 0) {
            updateFluidLevel();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack drained = fluidTank.drain(resource, action);
        if (action.execute() && !drained.isEmpty()) {
            updateFluidLevel();
        }
        return drained;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack drained = fluidTank.drain(maxDrain, action);
        if (action.execute() && !drained.isEmpty()) {
            updateFluidLevel();
        }
        return drained;
    }

    // --- Accessors ---

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
