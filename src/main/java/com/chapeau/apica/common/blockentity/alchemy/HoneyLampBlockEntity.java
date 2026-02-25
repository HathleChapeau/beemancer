/**
 * ============================================================
 * [HoneyLampBlockEntity.java]
 * Description: BlockEntity pour la lampe à miel avec tank et helpers lumineux
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities      | Registre BE          | Type de block entity           |
 * | ApicaFluids             | Fluides Apica        | Validation des fluides         |
 * | HoneyLampBlock          | Bloc associé         | Mise à jour blockstate         |
 * | Blocks.LIGHT            | Vanilla LightBlock   | Helpers lumineux invisibles    |
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
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class HoneyLampBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4000;

    private static final int HONEY_CONSUMPTION = 1;
    private static final int ROYAL_JELLY_CONSUMPTION = 2;
    private static final int NECTAR_CONSUMPTION = 4;

    private static final int HELPER_DISTANCE_NEAR = 5;
    private static final int HELPER_DISTANCE_FAR = 10;

    private static final Direction[] CARDINAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    };

    private final FluidTank fluidTank;
    private final Set<BlockPos> helperPositions = new HashSet<>();
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
            be.updateBlockState(level, pos, desiredState);
            be.updateHelpers(level, pos, desiredState);
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

    private void updateBlockState(Level level, BlockPos pos, LampState lampState) {
        BlockState current = level.getBlockState(pos);
        if (current.getValue(HoneyLampBlock.LAMP_STATE) != lampState) {
            level.setBlock(pos, current.setValue(HoneyLampBlock.LAMP_STATE, lampState), 3);
        }
    }

    private void updateHelpers(Level level, BlockPos pos, LampState lampState) {
        removeAllHelpers();

        switch (lampState) {
            case ROYAL_JELLY -> placeHelperRing(level, pos, HELPER_DISTANCE_NEAR);
            case NECTAR -> {
                placeHelperRing(level, pos, HELPER_DISTANCE_NEAR);
                placeHelperRing(level, pos, HELPER_DISTANCE_FAR);
            }
            default -> { /* HONEY et OFF: pas de helpers */ }
        }
    }

    /**
     * Place un anneau de blocs de lumière vanilla (level 15) autour du centre.
     */
    private void placeHelperRing(Level level, BlockPos center, int distance) {
        BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (Direction dir : CARDINAL_DIRECTIONS) {
            BlockPos helperPos = center.relative(dir, distance);
            if (level.isLoaded(helperPos) && level.getBlockState(helperPos).isAir()) {
                level.setBlock(helperPos, lightState, 3);
                helperPositions.add(helperPos);
            }
        }
    }

    /**
     * Supprime tous les blocs helpers lumineux placés par cette lampe.
     */
    public void removeAllHelpers() {
        if (level == null) return;
        for (BlockPos helperPos : helperPositions) {
            if (level.isLoaded(helperPos)) {
                BlockState state = level.getBlockState(helperPos);
                if (state.is(Blocks.LIGHT)) {
                    level.removeBlock(helperPos, false);
                }
            }
        }
        helperPositions.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putString("LampState", currentState.getSerializedName());

        ListTag helpers = new ListTag();
        for (BlockPos hp : helperPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", hp.getX());
            posTag.putInt("Y", hp.getY());
            posTag.putInt("Z", hp.getZ());
            helpers.add(posTag);
        }
        tag.put("Helpers", helpers);
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
        helperPositions.clear();
        if (tag.contains("Helpers")) {
            ListTag helpers = tag.getList("Helpers", Tag.TAG_COMPOUND);
            for (int i = 0; i < helpers.size(); i++) {
                CompoundTag posTag = helpers.getCompound(i);
                helperPositions.add(new BlockPos(
                        posTag.getInt("X"),
                        posTag.getInt("Y"),
                        posTag.getInt("Z")
                ));
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
