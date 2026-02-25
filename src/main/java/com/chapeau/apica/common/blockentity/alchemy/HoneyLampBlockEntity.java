/**
 * ============================================================
 * [HoneyLampBlockEntity.java]
 * Description: BlockEntity pour la lampe à miel avec tank de fluide
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities      | Registre BE          | Type de block entity           |
 * | ApicaFluids             | Fluides Apica        | Validation des fluides         |
 * | HoneyLampBlock          | Bloc associé         | Mise à jour blockstate         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyLampBlock (création, interaction)
 * - Apica.java (capability fluide)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.common.block.alchemy.HoneyLampBlock;
import com.chapeau.apica.common.block.alchemy.HoneyLampBlock.LampState;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class HoneyLampBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4000;

    private static final int HONEY_CONSUMPTION = 1;
    private static final int ROYAL_JELLY_CONSUMPTION = 2;
    private static final int NECTAR_CONSUMPTION = 4;

    private final FluidTank fluidTank;
    private LampState currentState = LampState.OFF;

    public HoneyLampBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.HONEY_LAMP.get(), pos, state);
        this.fluidTank = new FluidTank(CAPACITY) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == ApicaFluids.HONEY_SOURCE.get()
                        || stack.getFluid() == ApicaFluids.ROYAL_JELLY_SOURCE.get()
                        || stack.getFluid() == ApicaFluids.NECTAR_SOURCE.get();
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

    public FluidTank getFluidTank() { return fluidTank; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoneyLampBlockEntity be) {
        LampState desiredState = be.computeDesiredState();

        if (desiredState != LampState.OFF) {
            int consumption = be.getConsumptionRate(desiredState);
            be.fluidTank.drain(consumption, IFluidHandler.FluidAction.EXECUTE);

            if (be.fluidTank.isEmpty()) {
                desiredState = LampState.OFF;
            }
        }

        if (desiredState != be.currentState) {
            be.currentState = desiredState;
            BlockState current = level.getBlockState(pos);
            if (current.getValue(HoneyLampBlock.LAMP_STATE) != desiredState) {
                level.setBlock(pos, current.setValue(HoneyLampBlock.LAMP_STATE, desiredState), 3);
            }
        }
    }

    private LampState computeDesiredState() {
        if (fluidTank.isEmpty()) return LampState.OFF;

        Fluid fluid = fluidTank.getFluid().getFluid();
        if (fluid == ApicaFluids.NECTAR_SOURCE.get()) return LampState.NECTAR;
        if (fluid == ApicaFluids.ROYAL_JELLY_SOURCE.get()) return LampState.ROYAL_JELLY;
        if (fluid == ApicaFluids.HONEY_SOURCE.get()) return LampState.HONEY;
        return LampState.OFF;
    }

    private int getConsumptionRate(LampState state) {
        return switch (state) {
            case HONEY -> HONEY_CONSUMPTION;
            case ROYAL_JELLY -> ROYAL_JELLY_CONSUMPTION;
            case NECTAR -> NECTAR_CONSUMPTION;
            default -> 0;
        };
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putString("LampState", currentState.getSerializedName());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Fluid")) {
            fluidTank.readFromNBT(registries, tag.getCompound("Fluid"));
        }
        if (tag.contains("LampState")) {
            String stateName = tag.getString("LampState");
            for (LampState ls : LampState.values()) {
                if (ls.getSerializedName().equals(stateName)) {
                    currentState = ls;
                    break;
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putString("LampState", currentState.getSerializedName());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
