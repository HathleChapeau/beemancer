/**
 * ============================================================
 * [HoneyPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport de fluide
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire le fluide du bloc connecte
 * - Chaque pipe pousse independamment vers ses voisins toutes les 0.5s
 * - Egalisation entre pipes: pousse la moitie de la difference
 * - Push vers conteneurs: pousse jusqu'au transferRate
 * - Capacites: T1=1000mb, T2=2000mb, T3=4000mb, T4=8000mb
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class HoneyPipeBlockEntity extends BlockEntity {
    // --- TIER CONFIG ---
    public static final int TIER1_BUFFER = 1000;
    public static final int TIER2_BUFFER = 2000;
    public static final int TIER3_BUFFER = 4000;
    public static final int TIER4_BUFFER = 8000;

    public static final int TIER1_TRANSFER = 250;
    public static final int TIER2_TRANSFER = 500;
    public static final int TIER3_TRANSFER = 1000;
    public static final int TIER4_TRANSFER = 2000;

    private static final int SHARE_INTERVAL = 10; // 0.5 secondes

    private final FluidTank buffer;
    private final int transferRate;

    private int shareCooldown = 0;

    // Directions manuellement deconnectees par le joueur
    private final EnumSet<Direction> disconnectedDirections = EnumSet.noneOf(Direction.class);

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
        // Extraction depuis les blocs marques pour extraction (chaque tick)
        be.processExtractions(level, pos, state);

        // Push vers voisins toutes les 0.5 sec
        be.shareCooldown--;
        if (be.shareCooldown <= 0) {
            be.shareCooldown = SHARE_INTERVAL;
            be.pushToNeighbors(level, pos, state);
        }
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
                int maxExtract = buffer.getCapacity() - buffer.getFluidAmount();
                FluidStack toDrain = cap.drain(maxExtract, IFluidHandler.FluidAction.SIMULATE);
                if (!toDrain.isEmpty() && buffer.isFluidValid(toDrain)) {
                    int canFill = buffer.fill(toDrain, IFluidHandler.FluidAction.SIMULATE);
                    if (canFill > 0) {
                        FluidStack drained = cap.drain(canFill, IFluidHandler.FluidAction.EXECUTE);
                        buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    /**
     * Pousse le fluide vers les voisins connectes.
     * - Vers une pipe: egalise (pousse la moitie de la difference)
     * - Vers un conteneur: pousse jusqu'au transferRate
     */
    private void pushToNeighbors(Level level, BlockPos pos, BlockState state) {
        if (buffer.isEmpty()) {
            return;
        }

        boolean changed = false;

        for (Direction dir : Direction.values()) {
            if (buffer.isEmpty()) break;
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            if (HoneyPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);

            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe) {
                changed |= pushToPipe(neighborPipe);
            } else {
                var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (cap != null) {
                    changed |= pushToContainer(cap);
                }
            }
        }

        if (changed) {
            syncToClient();
        }
    }

    /**
     * Egalise le fluide avec une pipe voisine.
     * Pousse la moitie de la difference si on a plus qu'elle.
     */
    private boolean pushToPipe(HoneyPipeBlockEntity neighborPipe) {
        FluidStack myFluid = buffer.getFluid();
        FluidStack theirFluid = neighborPipe.buffer.getFluid();

        // Verifier compatibilite de fluide
        if (!theirFluid.isEmpty() && !myFluid.getFluid().isSame(theirFluid.getFluid())) {
            return false;
        }

        int myAmount = buffer.getFluidAmount();
        int theirAmount = neighborPipe.buffer.getFluidAmount();
        int diff = myAmount - theirAmount;

        if (diff <= 1) {
            return false;
        }

        int toTransfer = Math.min(diff / 2, transferRate);
        toTransfer = Math.min(toTransfer, neighborPipe.buffer.getCapacity() - theirAmount);

        if (toTransfer <= 0) {
            return false;
        }

        FluidStack drained = buffer.drain(toTransfer, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            neighborPipe.buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            neighborPipe.syncToClient();
            return true;
        }

        return false;
    }

    /**
     * Pousse le fluide vers un conteneur (tank, machine, etc.).
     */
    private boolean pushToContainer(IFluidHandler handler) {
        int toTransfer = Math.min(buffer.getFluidAmount(), transferRate);
        if (toTransfer <= 0) {
            return false;
        }

        FluidStack toFill = new FluidStack(buffer.getFluid().getFluid(), toTransfer);
        int accepted = handler.fill(toFill, IFluidHandler.FluidAction.SIMULATE);

        if (accepted > 0) {
            FluidStack drained = buffer.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                return true;
            }
        }

        return false;
    }

    public FluidTank getBuffer() {
        return buffer;
    }

    public boolean isDisconnected(Direction dir) {
        return disconnectedDirections.contains(dir);
    }

    public void setDisconnected(Direction dir, boolean disconnected) {
        if (disconnected) {
            disconnectedDirections.add(dir);
        } else {
            disconnectedDirections.remove(dir);
        }
        setChanged();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Buffer", buffer.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ShareCooldown", shareCooldown);
        int disconnectedBits = 0;
        for (Direction dir : disconnectedDirections) {
            disconnectedBits |= (1 << dir.ordinal());
        }
        if (disconnectedBits != 0) {
            tag.putInt("DisconnectedDirs", disconnectedBits);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) {
            buffer.readFromNBT(registries, tag.getCompound("Buffer"));
        }
        shareCooldown = tag.getInt("ShareCooldown");
        disconnectedDirections.clear();
        if (tag.contains("DisconnectedDirs")) {
            int bits = tag.getInt("DisconnectedDirs");
            for (Direction dir : Direction.values()) {
                if ((bits & (1 << dir.ordinal())) != 0) {
                    disconnectedDirections.add(dir);
                }
            }
        }
    }
}
