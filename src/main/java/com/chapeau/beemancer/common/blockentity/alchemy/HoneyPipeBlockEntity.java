/**
 * ============================================================
 * [HoneyPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport de fluide
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire le fluide du bloc connecte
 * - Partage équitable toutes les 0.5 sec entre toutes les pipes connectées
 * - Seule la pipe "master" (plus petite position) exécute le partage
 * - Inclut les conteneurs à fluide dans le réseau de partage
 * - Capacités: T1=1000mb, T2=2000mb, T3=4000mb, T4=8000mb
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HoneyPipeBlockEntity extends BlockEntity {
    // --- TIER CONFIG ---
    public static final int TIER1_BUFFER = 1000;
    public static final int TIER2_BUFFER = 2000;
    public static final int TIER3_BUFFER = 4000;
    public static final int TIER4_BUFFER = 8000;

    private static final int SHARE_INTERVAL = 10; // 0.5 secondes
    private static final int MAX_NETWORK_SIZE = 256; // Limite pour éviter stack overflow

    private final FluidTank buffer;

    private int shareCooldown = 0;
    private boolean isSharing = false;

    // Directions manuellement deconnectees par le joueur
    private final EnumSet<Direction> disconnectedDirections = EnumSet.noneOf(Direction.class);

    public HoneyPipeBlockEntity(BlockPos pos, BlockState state) {
        this(BeemancerBlockEntities.HONEY_PIPE.get(), pos, state, TIER1_BUFFER);
    }

    public HoneyPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int bufferCapacity) {
        super(type, pos, state);
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
                if (!isSharing) {
                    syncToClient();
                }
            }
        };
    }

    // Factory methods for tiered versions
    public static HoneyPipeBlockEntity createTier2(BlockPos pos, BlockState state) {
        return new HoneyPipeBlockEntity(BeemancerBlockEntities.HONEY_PIPE_TIER2.get(), pos, state, TIER2_BUFFER);
    }

    public static HoneyPipeBlockEntity createTier3(BlockPos pos, BlockState state) {
        return new HoneyPipeBlockEntity(BeemancerBlockEntities.HONEY_PIPE_TIER3.get(), pos, state, TIER3_BUFFER);
    }

    public static HoneyPipeBlockEntity createTier4(BlockPos pos, BlockState state) {
        return new HoneyPipeBlockEntity(BeemancerBlockEntities.HONEY_PIPE_TIER4.get(), pos, state, TIER4_BUFFER);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HoneyPipeBlockEntity be) {
        // Extraction depuis les blocs marqués pour extraction (chaque tick)
        be.processExtractions(level, pos, state);

        // Partage équitable toutes les 0.5 sec
        be.shareCooldown--;
        if (be.shareCooldown <= 0) {
            be.shareCooldown = SHARE_INTERVAL;
            be.tryShareFluidWithNetwork(level, pos, state);
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
     * Tente de partager le fluide. Seul le "master" du réseau exécute le partage.
     */
    private void tryShareFluidWithNetwork(Level level, BlockPos pos, BlockState state) {
        // Déterminer le type de fluide du réseau (chercher dans cette pipe ou les voisines)
        Fluid fluidType = findNetworkFluidType(level, pos, state);
        if (fluidType == null) {
            return; // Aucun fluide dans le réseau proche
        }

        // Collecter toutes les pipes du réseau
        List<HoneyPipeBlockEntity> allPipes = new ArrayList<>();
        List<ContainerParticipant> containers = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(pos); // Évite que les cycles re-visitent la pipe de départ

        collectAllNetworkMembers(level, pos, state, fluidType, allPipes, containers, visited);

        // Déterminer le master (plus petite position)
        BlockPos masterPos = pos;
        for (HoneyPipeBlockEntity pipe : allPipes) {
            if (comparePosForMaster(pipe.getBlockPos(), masterPos) < 0) {
                masterPos = pipe.getBlockPos();
            }
        }

        // Seul le master exécute le partage
        if (!pos.equals(masterPos)) {
            return;
        }

        // Ajouter cette pipe à la liste
        allPipes.add(this);

        // Désactiver le sync pendant le partage pour éviter le spam de packets
        for (HoneyPipeBlockEntity pipe : allPipes) {
            pipe.isSharing = true;
        }

        // Exécuter le partage
        executeFluidSharing(fluidType, allPipes, containers);

        // Réactiver et sync une seule fois par pipe modifiée
        for (HoneyPipeBlockEntity pipe : allPipes) {
            pipe.isSharing = false;
            pipe.syncToClient();
        }
    }

    /**
     * Trouve le type de fluide du réseau. Cherche d'abord dans cette pipe, puis les voisines.
     */
    @Nullable
    private Fluid findNetworkFluidType(Level level, BlockPos pos, BlockState state) {
        if (!buffer.isEmpty()) {
            return buffer.getFluid().getFluid();
        }

        // Chercher dans les pipes voisines directes
        for (Direction dir : Direction.values()) {
            if (!HoneyPipeBlock.isConnected(state, dir)) continue;
            if (HoneyPipeBlock.isExtracting(state, dir)) continue;

            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);

            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe) {
                if (!neighborPipe.buffer.isEmpty()) {
                    return neighborPipe.buffer.getFluid().getFluid();
                }
            }
        }

        return null;
    }

    /**
     * Compare deux positions pour déterminer le master (X, puis Y, puis Z).
     */
    private int comparePosForMaster(BlockPos a, BlockPos b) {
        int cmp = Integer.compare(a.getX(), b.getX());
        if (cmp != 0) return cmp;
        cmp = Integer.compare(a.getY(), b.getY());
        if (cmp != 0) return cmp;
        return Integer.compare(a.getZ(), b.getZ());
    }

    /**
     * Collecte récursivement toutes les pipes et conteneurs du réseau.
     * Continue la récursion même pour les pipes pleines.
     */
    private void collectAllNetworkMembers(Level level, BlockPos fromPos, BlockState fromState,
                                          Fluid fluidType,
                                          List<HoneyPipeBlockEntity> pipes,
                                          List<ContainerParticipant> containers,
                                          Set<BlockPos> visited) {
        if (visited.size() >= MAX_NETWORK_SIZE) {
            return; // Limite atteinte
        }

        for (Direction dir : Direction.values()) {
            if (!HoneyPipeBlock.isConnected(fromState, dir)) continue;
            if (HoneyPipeBlock.isExtracting(fromState, dir)) continue;

            BlockPos neighborPos = fromPos.relative(dir);
            if (visited.contains(neighborPos)) continue;
            visited.add(neighborPos);

            BlockEntity neighborBe = level.getBlockEntity(neighborPos);

            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe) {
                // Vérifier compatibilité de fluide
                if (!neighborPipe.buffer.isEmpty() &&
                    !neighborPipe.buffer.getFluid().getFluid().isSame(fluidType)) {
                    continue; // Fluide différent, ignorer complètement
                }

                // Ajouter la pipe (même si pleine, pour le calcul du master)
                pipes.add(neighborPipe);

                // Continuer la récursion (toujours, même si pleine)
                BlockState neighborState = level.getBlockState(neighborPos);
                collectAllNetworkMembers(level, neighborPos, neighborState, fluidType, pipes, containers, visited);
            } else {
                // Conteneur à fluide (tank, etc.)
                var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
                if (cap != null) {
                    // Vérifier compatibilité
                    boolean compatible = true;
                    for (int i = 0; i < cap.getTanks(); i++) {
                        FluidStack tankFluid = cap.getFluidInTank(i);
                        if (!tankFluid.isEmpty() && !tankFluid.getFluid().isSame(fluidType)) {
                            compatible = false;
                            break;
                        }
                    }

                    if (compatible) {
                        // Vérifier s'il peut accepter notre fluide
                        FluidStack testStack = new FluidStack(fluidType, 1);
                        int canFill = cap.fill(testStack, IFluidHandler.FluidAction.SIMULATE);
                        if (canFill > 0) {
                            containers.add(new ContainerParticipant(cap));
                        }
                    }
                }
            }
        }
    }

    /**
     * Exécute le partage équitable du fluide entre tous les participants.
     */
    private void executeFluidSharing(Fluid fluidType, List<HoneyPipeBlockEntity> pipes,
                                     List<ContainerParticipant> containers) {
        // Calculer le total de fluide et la capacité totale
        int totalFluid = 0;
        int totalCapacity = 0;

        for (HoneyPipeBlockEntity pipe : pipes) {
            totalFluid += pipe.buffer.getFluidAmount();
            totalCapacity += pipe.buffer.getCapacity();
        }

        for (ContainerParticipant container : containers) {
            totalFluid += container.getCurrentAmount(fluidType);
            totalCapacity += container.getCapacity();
        }

        if (totalFluid == 0 || totalCapacity == 0) {
            return;
        }

        // Calculer le ratio de remplissage cible
        float targetRatio = (float) totalFluid / totalCapacity;

        // Séparer les donneurs et receveurs
        List<FluidSource> donors = new ArrayList<>();
        List<FluidReceiver> receivers = new ArrayList<>();

        for (HoneyPipeBlockEntity pipe : pipes) {
            int current = pipe.buffer.getFluidAmount();
            int capacity = pipe.buffer.getCapacity();
            int target = Math.round(capacity * targetRatio);

            if (current > target) {
                donors.add(new PipeSource(pipe, current - target));
            } else if (current < target) {
                receivers.add(new PipeReceiver(pipe, target - current, fluidType));
            }
        }

        for (ContainerParticipant container : containers) {
            int current = container.getCurrentAmount(fluidType);
            int capacity = container.getCapacity();
            int target = Math.round(capacity * targetRatio);

            if (current > target) {
                donors.add(new ContainerSource(container, current - target, fluidType));
            } else if (current < target) {
                receivers.add(new ContainerReceiver(container, target - current, fluidType));
            }
        }

        // Transférer des donneurs vers les receveurs
        for (FluidSource donor : donors) {
            for (FluidReceiver receiver : receivers) {
                int toTransfer = Math.min(donor.getAvailable(), receiver.getNeeded());
                if (toTransfer > 0) {
                    int drained = donor.drain(toTransfer);
                    receiver.fill(drained);
                }
            }
        }
    }

    // --- Interfaces pour le partage ---

    private interface FluidSource {
        int getAvailable();
        int drain(int amount);
    }

    private interface FluidReceiver {
        int getNeeded();
        void fill(int amount);
    }

    private static class PipeSource implements FluidSource {
        private final HoneyPipeBlockEntity pipe;
        private int available;

        PipeSource(HoneyPipeBlockEntity pipe, int available) {
            this.pipe = pipe;
            this.available = available;
        }

        @Override
        public int getAvailable() { return available; }

        @Override
        public int drain(int amount) {
            int toDrain = Math.min(amount, available);
            FluidStack drained = pipe.buffer.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
            available -= drained.getAmount();
            return drained.getAmount();
        }
    }

    private static class PipeReceiver implements FluidReceiver {
        private final HoneyPipeBlockEntity pipe;
        private final Fluid fluidType;
        private int needed;

        PipeReceiver(HoneyPipeBlockEntity pipe, int needed, Fluid fluidType) {
            this.pipe = pipe;
            this.needed = needed;
            this.fluidType = fluidType;
        }

        @Override
        public int getNeeded() { return needed; }

        @Override
        public void fill(int amount) {
            int filled = pipe.buffer.fill(
                new FluidStack(fluidType, amount),
                IFluidHandler.FluidAction.EXECUTE
            );
            needed -= filled;
        }
    }

    private static class ContainerParticipant {
        private final IFluidHandler handler;

        ContainerParticipant(IFluidHandler handler) {
            this.handler = handler;
        }

        int getCurrentAmount(Fluid fluidType) {
            int amount = 0;
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack stack = handler.getFluidInTank(i);
                if (stack.getFluid().isSame(fluidType)) {
                    amount += stack.getAmount();
                }
            }
            return amount;
        }

        int getCapacity() {
            int capacity = 0;
            for (int i = 0; i < handler.getTanks(); i++) {
                capacity += handler.getTankCapacity(i);
            }
            return capacity;
        }

        IFluidHandler getHandler() { return handler; }
    }

    private static class ContainerSource implements FluidSource {
        private final ContainerParticipant container;
        private int available;
        private final Fluid fluidType;

        ContainerSource(ContainerParticipant container, int available, Fluid fluidType) {
            this.container = container;
            this.available = available;
            this.fluidType = fluidType;
        }

        @Override
        public int getAvailable() { return available; }

        @Override
        public int drain(int amount) {
            int toDrain = Math.min(amount, available);
            FluidStack drained = container.getHandler().drain(
                new FluidStack(fluidType, toDrain),
                IFluidHandler.FluidAction.EXECUTE
            );
            available -= drained.getAmount();
            return drained.getAmount();
        }
    }

    private static class ContainerReceiver implements FluidReceiver {
        private final ContainerParticipant container;
        private int needed;
        private final Fluid fluidType;

        ContainerReceiver(ContainerParticipant container, int needed, Fluid fluidType) {
            this.container = container;
            this.needed = needed;
            this.fluidType = fluidType;
        }

        @Override
        public int getNeeded() { return needed; }

        @Override
        public void fill(int amount) {
            int filled = container.getHandler().fill(
                new FluidStack(fluidType, amount),
                IFluidHandler.FluidAction.EXECUTE
            );
            needed -= filled;
        }
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
