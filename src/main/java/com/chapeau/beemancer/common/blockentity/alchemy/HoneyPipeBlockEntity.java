/**
 * ============================================================
 * [HoneyPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport de fluide
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire le fluide du bloc connecte
 * - Mode normal: pousse le fluide vers les pipes connectees
 * - Round-robin entre les pipes disponibles
 * - Evite les va-et-vient en trackant l'origine du fluide
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.alchemy;

import com.chapeau.beemancer.common.block.alchemy.HoneyPipeBlock;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HoneyPipeBlockEntity extends BlockEntity {
    // --- TIER CONFIG ---
    public static final int TIER1_BUFFER = 500;
    public static final int TIER1_TRANSFER = 100;

    public static final int TIER2_BUFFER = 1000;
    public static final int TIER2_TRANSFER = 250;

    public static final int TIER3_BUFFER = 2000;
    public static final int TIER3_TRANSFER = 500;

    public static final int TIER4_BUFFER = 4000;
    public static final int TIER4_TRANSFER = 1000;

    private final int transferRate;
    private final FluidTank buffer;

    private int transferCooldown = 0;
    private int roundRobinIndex = 0;

    // Position de la pipe d'ou vient le fluide actuel (pour eviter les va-et-vient)
    @Nullable
    private BlockPos sourcePos = null;

    public HoneyPipeBlockEntity(BlockPos pos, BlockState state) {
        this(BeemancerBlockEntities.HONEY_PIPE.get(), pos, state, TIER1_BUFFER, TIER1_TRANSFER);
    }

    public HoneyPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                int bufferCapacity, int transferRate) {
        super(type, pos, state);
        this.transferRate = transferRate;
        this.buffer = new FluidTank(bufferCapacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return stack.getFluid() == BeemancerFluids.HONEY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.ROYAL_JELLY_SOURCE.get()
                    || stack.getFluid() == BeemancerFluids.NECTAR_SOURCE.get();
            }

            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    // Factory methods for tiered versions
    public static HoneyPipeBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new HoneyPipeBlockEntity(BeemancerBlockEntities.HONEY_PIPE_TIER2.get(), pos, state,
            TIER2_BUFFER, TIER2_TRANSFER);
    }

    public static HoneyPipeBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new HoneyPipeBlockEntity(BeemancerBlockEntities.HONEY_PIPE_TIER3.get(), pos, state,
            TIER3_BUFFER, TIER3_TRANSFER);
    }

    public static HoneyPipeBlockEntity createTier4(BlockPos pos, BlockState state) {
        return new HoneyPipeBlockEntity(BeemancerBlockEntities.HONEY_PIPE_TIER4.get(), pos, state,
            TIER4_BUFFER, TIER4_TRANSFER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoneyPipeBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        // Reset source tracking si buffer est vide
        if (be.buffer.isEmpty()) {
            be.sourcePos = null;
        }

        // Process extraction from neighbors marked for extraction
        be.processExtractions(level, pos, state);

        // Push to neighbors (not in extraction mode)
        be.pushToNeighbors(level, pos, state);

        be.transferCooldown = 2;
    }

    private void processExtractions(Level level, BlockPos pos, BlockState state) {
        if (buffer.getFluidAmount() >= buffer.getCapacity()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            if (!HoneyPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Don't extract from other pipes
            if (level.getBlockEntity(neighborPos) instanceof HoneyPipeBlockEntity) continue;

            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (cap != null) {
                FluidStack toDrain = cap.drain(transferRate, IFluidHandler.FluidAction.SIMULATE);
                if (!toDrain.isEmpty() && buffer.isFluidValid(toDrain)) {
                    int canFill = buffer.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
                    if (canFill > 0) {
                        FluidStack drained = cap.drain(canFill, IFluidHandler.FluidAction.EXECUTE);
                        buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                        // Mark source as non-pipe (null = from machine)
                        sourcePos = null;
                    }
                }
            }
        }
    }

    private void pushToNeighbors(Level level, BlockPos pos, BlockState state) {
        if (buffer.isEmpty()) {
            return;
        }

        // Construire la liste des destinations disponibles
        List<PipeDestination> availableDestinations = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            if (HoneyPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Skip si c'est la source (evite va-et-vient)
            if (neighborPos.equals(sourcePos)) continue;

            // Check si c'est une pipe
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe) {
                // Skip si la pipe voisine est pleine
                if (neighborPipe.buffer.getFluidAmount() >= neighborPipe.buffer.getCapacity()) continue;
                // Skip si le fluide n'est pas compatible
                if (!neighborPipe.buffer.isEmpty() && !neighborPipe.buffer.getFluid().getFluid().isSame(buffer.getFluid().getFluid())) continue;

                availableDestinations.add(new PipeDestination(dir, neighborPos, true));
            } else {
                // C'est un bloc normal avec capacite fluide
                var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (cap != null) {
                    FluidStack toTransfer = buffer.drain(transferRate, IFluidHandler.FluidAction.SIMULATE);
                    if (!toTransfer.isEmpty()) {
                        int canFill = cap.fill(toTransfer, IFluidHandler.FluidAction.SIMULATE);
                        if (canFill > 0) {
                            availableDestinations.add(new PipeDestination(dir, neighborPos, false));
                        }
                    }
                }
            }
        }

        if (availableDestinations.isEmpty()) return;

        // Round-robin: choisir la prochaine destination
        roundRobinIndex = roundRobinIndex % availableDestinations.size();
        PipeDestination dest = availableDestinations.get(roundRobinIndex);
        roundRobinIndex++;

        // Transferer vers la destination choisie
        if (dest.isPipe) {
            BlockEntity neighborBe = level.getBlockEntity(dest.pos);
            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe) {
                FluidStack toTransfer = buffer.drain(transferRate, IFluidHandler.FluidAction.SIMULATE);
                if (!toTransfer.isEmpty()) {
                    int filled = neighborPipe.buffer.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        buffer.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                        // Marquer notre position comme source pour la pipe voisine
                        neighborPipe.sourcePos = pos;
                    }
                }
            }
        } else {
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, dest.pos, dest.direction.getOpposite());
            if (cap != null) {
                FluidStack toTransfer = buffer.drain(transferRate, IFluidHandler.FluidAction.SIMULATE);
                if (!toTransfer.isEmpty()) {
                    int filled = cap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        buffer.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    /**
     * Recoit du fluide d'une autre pipe
     */
    public int receiveFluid(FluidStack fluid, BlockPos fromPos) {
        if (buffer.getFluidAmount() >= buffer.getCapacity()) return 0;
        if (!buffer.isFluidValid(fluid)) return 0;

        int filled = buffer.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            sourcePos = fromPos;
        }
        return filled;
    }

    public FluidTank getBuffer() {
        return buffer;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Buffer", buffer.writeToNBT(registries, new CompoundTag()));
        tag.putInt("RoundRobin", roundRobinIndex);
        if (sourcePos != null) {
            tag.put("SourcePos", NbtUtils.writeBlockPos(sourcePos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) {
            buffer.readFromNBT(registries, tag.getCompound("Buffer"));
        }
        roundRobinIndex = tag.getInt("RoundRobin");
        if (tag.contains("SourcePos")) {
            sourcePos = NbtUtils.readBlockPos(tag, "SourcePos").orElse(null);
        }
    }

    private record PipeDestination(Direction direction, BlockPos pos, boolean isPipe) {}
}
