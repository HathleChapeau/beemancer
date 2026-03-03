/**
 * ============================================================
 * [HoneyLampBlockEntity.java]
 * Description: BlockEntity pour la lampe à miel avec luminosité dynamique
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
 * PATTERN: Create FluidTankBlockEntity
 * - luminosity stocké dans le BE, lu par Block.getLightEmission()
 * - checkBlock() pour notifier le light engine coté client
 * - Blocks.LIGHT helpers pour étendre la portée au-delà de 15 blocs
 *
 * UTILISÉ PAR:
 * - HoneyLampBlock (getLightEmission, onRemove)
 * - Apica.java (capability fluide)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.artifacts;

import com.chapeau.apica.common.block.artifacts.HoneyLampBlock;
import com.chapeau.apica.common.block.artifacts.HoneyLampBlock.LampState;
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
import java.util.ArrayList;
import java.util.List;

public class HoneyLampBlockEntity extends BlockEntity {
    public static final int CAPACITY = 4000;

    private static final int HONEY_CONSUMPTION = 1;
    private static final int ROYAL_JELLY_CONSUMPTION = 2;
    private static final int NECTAR_CONSUMPTION = 4;

    private static final int HELPER_DISTANCE_HONEY = 2;       // 2 + 14 = 16 blocks range
    private static final int HELPER_DISTANCE_ROYAL_JELLY = 4; // 4 + 14 = 18 blocks range
    private static final int HELPER_DISTANCE_NECTAR = 6;      // 6 + 14 = 20 blocks range

    private static final Direction[] SPREAD_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    };

    private final FluidTank fluidTank;
    private final List<BlockPos> helperPositions = new ArrayList<>();
    private LampState currentState = LampState.OFF;

    /** Luminosité dynamique lue par HoneyLampBlock.getLightEmission() — pattern Create */
    protected int luminosity;

    public int getLuminosity() { return luminosity; }

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

    // =========================================================================
    // LUMINOSITY — pattern Create FluidTankBlockEntity.setLuminosity()
    // =========================================================================

    protected void setLuminosity(int luminosity) {
        if (level == null || level.isClientSide()) return;
        if (this.luminosity == luminosity) return;
        this.luminosity = luminosity;
        sendData();
    }

    private void sendData() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // =========================================================================
    // SERVER TICK
    // =========================================================================

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

            // Mettre à jour blockstate visuel
            BlockState current = level.getBlockState(pos);
            if (current.getValue(HoneyLampBlock.LAMP_STATE) != desiredState) {
                level.setBlock(pos, current.setValue(HoneyLampBlock.LAMP_STATE, desiredState), 3);
            }

            // Mettre à jour luminosité dynamique (pattern Create)
            be.setLuminosity(be.getLuminosityForState(desiredState));

            // Mettre à jour helpers de lumière
            be.removeAllHelpers();
            be.placeHelpersForState(desiredState);
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

    private int getLuminosityForState(LampState state) {
        return switch (state) {
            case HONEY -> 15;
            case ROYAL_JELLY -> 15;
            case NECTAR -> 15;
            default -> 0;
        };
    }

    // =========================================================================
    // LIGHT HELPERS — vanilla Blocks.LIGHT pour étendre la portée
    // =========================================================================

    private void placeHelpersForState(LampState state) {
        if (level == null) return;
        switch (state) {
            case HONEY -> placeHelperRing(HELPER_DISTANCE_HONEY);
            case ROYAL_JELLY -> placeHelperRing(HELPER_DISTANCE_ROYAL_JELLY);
            case NECTAR -> placeHelperRing(HELPER_DISTANCE_NECTAR);
            default -> { /* OFF: pas de helpers */ }
        }
        setChanged();
    }

    private void placeHelperRing(int distance) {
        BlockState lightState = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (Direction dir : SPREAD_DIRECTIONS) {
            BlockPos target = worldPosition.relative(dir, distance);
            if (level.isLoaded(target) && level.getBlockState(target).isAir()) {
                level.setBlock(target, lightState, 3);
                helperPositions.add(target);
            }
        }
    }

    /**
     * Supprime tous les blocs LIGHT placés par cette lampe.
     * Appelé lors du changement d'état et de la destruction du bloc.
     */
    public void removeAllHelpers() {
        if (level == null) return;
        for (BlockPos hp : helperPositions) {
            if (level.isLoaded(hp) && level.getBlockState(hp).is(Blocks.LIGHT)) {
                level.removeBlock(hp, false);
            }
        }
        helperPositions.clear();
        setChanged();
    }

    // =========================================================================
    // PERSISTENCE — pattern Create: Luminosity + Helpers en NBT
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putString("LampState", currentState.getSerializedName());
        tag.putInt("Luminosity", luminosity);

        if (!helperPositions.isEmpty()) {
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        int prevLum = luminosity;

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

        luminosity = tag.getInt("Luminosity");

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

        // Pattern Create: forcer le light engine client à recalculer si luminosity a changé
        if (luminosity != prevLum && level != null && level.isClientSide()) {
            level.getChunkSource()
                    .getLightEngine()
                    .checkBlock(worldPosition);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Fluid", fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putString("LampState", currentState.getSerializedName());
        tag.putInt("Luminosity", luminosity);
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
