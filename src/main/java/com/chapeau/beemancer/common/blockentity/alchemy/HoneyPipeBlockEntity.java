/**
 * ============================================================
 * [HoneyPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport de fluide
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire le fluide du bloc connecte
 * - Chaque pipe pousse independamment vers ses voisins toutes les 0.5s
 * - Egalisation equitable: calcule la cible, puis transfere simultanement
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
import net.minecraft.network.Connection;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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

    // Couleur de teinte du core (-1 = pas de teinte)
    private int tintColor = -1;

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
     * Utilise une approche en deux phases pour une repartition equitable:
     * Phase 1: Calculer la cible d'egalisation entre toutes les pipes voisines
     * Phase 2: Executer les transferts simultanement
     * Les conteneurs recoivent ensuite jusqu'au transferRate.
     */
    private void pushToNeighbors(Level level, BlockPos pos, BlockState state) {
        if (buffer.isEmpty()) {
            return;
        }

        boolean changed = false;
        FluidStack myFluid = buffer.getFluid();

        // --- Phase 1: Collecter les voisins et calculer la cible ---
        List<PipeTarget> pipeTargets = new ArrayList<>();
        List<ContainerTarget> containerTargets = new ArrayList<>();
        int myAmount = buffer.getFluidAmount();

        for (Direction dir : Direction.values()) {
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            if (HoneyPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);

            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe) {
                FluidStack theirFluid = neighborPipe.buffer.getFluid();
                // Verifier compatibilite de fluide
                if (!theirFluid.isEmpty() && !myFluid.getFluid().isSame(theirFluid.getFluid())) {
                    continue;
                }
                int theirAmount = neighborPipe.buffer.getFluidAmount();
                // Inclure seulement les voisins avec moins de fluide
                if (myAmount > theirAmount) {
                    pipeTargets.add(new PipeTarget(neighborPipe, theirAmount));
                }
            } else {
                var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (cap != null) {
                    containerTargets.add(new ContainerTarget(cap, dir));
                }
            }
        }

        // --- Phase 2: Egalisation equitable entre pipes ---
        if (!pipeTargets.isEmpty()) {
            // Calculer la cible: total / (participants)
            // Exclure iterativement les voisins deja au-dessus de la cible
            int targetAmount = computeEqualTarget(myAmount, pipeTargets);

            // Calculer les transferts pour chaque voisin
            int totalTransfer = 0;
            for (PipeTarget target : pipeTargets) {
                int toGive = targetAmount - target.amount;
                if (toGive <= 0) {
                    target.transfer = 0;
                    continue;
                }
                int capped = Math.min(toGive, transferRate);
                capped = Math.min(capped, target.pipe.buffer.getCapacity() - target.amount);
                target.transfer = Math.max(capped, 0);
                totalTransfer += target.transfer;
            }

            // Securite: si le total depasse ce qu'on peut donner, scaler proportionnellement
            int maxGive = myAmount - targetAmount;
            if (totalTransfer > maxGive && totalTransfer > 0) {
                for (PipeTarget target : pipeTargets) {
                    target.transfer = (int) ((long) target.transfer * maxGive / totalTransfer);
                }
            }

            // Executer les transferts
            for (PipeTarget target : pipeTargets) {
                if (target.transfer <= 0) continue;

                FluidStack drained = buffer.drain(target.transfer, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    target.pipe.buffer.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    target.pipe.syncToClient();
                    changed = true;
                }
            }
        }

        // --- Phase 3: Push vers conteneurs ---
        for (ContainerTarget ct : containerTargets) {
            if (buffer.isEmpty()) break;
            changed |= pushToContainer(ct.handler);
        }

        if (changed) {
            syncToClient();
        }
    }

    /**
     * Calcule la cible d'egalisation equitable.
     * Exclut iterativement les voisins dont le niveau est deja au-dessus de la cible.
     *
     * Exemple: me=1000, voisins=[0, 0, 800]
     * - Tentative avec 4 participants: cible = 1800/4 = 450
     * - Voisin a 800 > 450 → exclure
     * - Recalcul avec 3 participants: cible = 1000/3 = 333
     * - Tous les restants < 333 → valide
     */
    private int computeEqualTarget(int myAmount, List<PipeTarget> targets) {
        // Trier par quantite croissante pour l'exclusion iterative
        targets.sort((a, b) -> Integer.compare(a.amount, b.amount));

        int totalFluid = myAmount;
        int participantCount = 1; // moi

        for (PipeTarget target : targets) {
            totalFluid += target.amount;
            participantCount++;
        }

        // Exclure iterativement les voisins au-dessus de la cible
        for (int i = targets.size() - 1; i >= 0; i--) {
            int target = totalFluid / participantCount;
            if (targets.get(i).amount < target) {
                break; // Tous les restants sont en dessous de la cible
            }
            // Ce voisin est au-dessus de la cible, l'exclure
            totalFluid -= targets.get(i).amount;
            participantCount--;
        }

        return participantCount > 0 ? totalFluid / participantCount : myAmount;
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

    private static class PipeTarget {
        final HoneyPipeBlockEntity pipe;
        final int amount;
        int transfer;

        PipeTarget(HoneyPipeBlockEntity pipe, int amount) {
            this.pipe = pipe;
            this.amount = amount;
            this.transfer = 0;
        }
    }

    private record ContainerTarget(IFluidHandler handler, Direction direction) {}

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

    public int getTintColor() {
        return tintColor;
    }

    public void setTintColor(int color) {
        this.tintColor = color;
        setChanged();
        syncToClient();
    }

    public boolean hasTint() {
        return tintColor != -1;
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
        if (tintColor != -1) {
            tag.putInt("TintColor", tintColor);
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
        tintColor = tag.contains("TintColor") ? tag.getInt("TintColor") : -1;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        requestModelDataUpdate();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
        // Force re-render côté client pour mettre à jour le BlockColor
        if (level != null && level.isClientSide()) {
            level.invalidateCapabilities(worldPosition);
            requestModelDataUpdate();
        }
    }

    @Override
    public void requestModelDataUpdate() {
        super.requestModelDataUpdate();
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
