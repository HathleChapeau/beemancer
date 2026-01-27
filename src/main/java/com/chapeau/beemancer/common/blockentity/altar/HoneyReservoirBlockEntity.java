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
 * | AltarHeartBlockEntity   | Multiblock check    | isPartOfFormedMultiblock |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyReservoirBlock.java
 * - HoneyCrystalBlockEntity.java (query reservoirs)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.altar;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
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
     * Calcule le ratio de remplissage (0.0 à 1.0) pour le renderer.
     */
    public float getFillRatio() {
        if (fluidTank.isEmpty()) return 0f;
        return (float) fluidTank.getFluidAmount() / CAPACITY;
    }

    /**
     * Vérifie si ce réservoir fait partie d'un multiblock formé.
     * Cherche l'AltarHeart (contrôleur) 2 blocs en dessous.
     */
    public boolean isPartOfFormedMultiblock() {
        if (level == null) return false;

        // Le réservoir est à Y+2 relatif à l'AltarHeart (Y+0)
        BlockPos heartPos = worldPosition.below(2);
        BlockEntity be = level.getBlockEntity(heartPos);
        if (be instanceof AltarHeartBlockEntity heart) {
            return heart.isFormed();
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
        return fluidTank.fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return fluidTank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return fluidTank.drain(maxDrain, action);
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
