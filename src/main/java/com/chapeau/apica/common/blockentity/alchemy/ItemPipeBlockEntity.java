/**
 * ============================================================
 * [ItemPipeBlockEntity.java]
 * Description: BlockEntity pour les pipes de transport d'items avec routage réseau
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Mode extraction (par direction): tire les items du bloc connecté
 * - Pre-validation: vérifie qu'une destination existe AVANT d'extraire
 * - Items en transit: suivent une route pré-calculée (BFS) hop par hop
 * - Anti-loss: les items ne sont JAMAIS droppés — backpressure si pas de destination
 * - Round-robin global: distribution équitable entre toutes les destinations du réseau
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ItemPipeNetworkManager  | Réseau de pipes      | Routage, pre-validation        |
 * | PipeNetwork             | Réseau connexe       | findDestination                |
 * | PipeTransitItem         | Item en transit      | Stockage route + avancement    |
 * | AbstractPipeBlock       | Blockstate connexions| Détection extract/connect      |
 * | DebugWandItem           | Debug display        | Affichage buffer au-dessus     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ItemPipeBlock.java (création, ticker)
 * - Apica.java (capability registration)
 *
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.alchemy;

import com.chapeau.apica.common.block.alchemy.AbstractPipeBlock;
import com.chapeau.apica.common.block.alchemy.ItemPipeBlock;
import com.chapeau.apica.common.data.ItemFilterData;
import com.chapeau.apica.common.item.debug.DebugWandItem;
import com.chapeau.apica.core.network.pipe.ItemPipeNetworkManager;
import com.chapeau.apica.core.network.pipe.PipeNetwork;
import com.chapeau.apica.core.network.pipe.PipeTransitItem;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * BlockEntity des item pipes avec routage réseau intelligent.
 * Les items extraits sont pré-validés (destination existante) puis transitent
 * hop par hop à travers le réseau via des routes BFS pré-calculées.
 */
public class ItemPipeBlockEntity extends BlockEntity {
    // --- MK CONFIG ---
    public static final int MK1_BUFFER = 4;
    public static final int MK1_TRANSFER = 4;
    public static final int MK2_BUFFER = 8;
    public static final int MK2_TRANSFER = 8;
    public static final int MK3_BUFFER = 16;
    public static final int MK3_TRANSFER = 16;
    public static final int MK4_BUFFER = 32;
    public static final int MK4_TRANSFER = 32;

    private final int transferAmount;
    private final ItemStackHandler buffer;

    private int transferCooldown = 0;

    /** Items en transit avec route pré-calculée. */
    private final List<PipeTransitItem> transitItems = new ArrayList<>();

    /** Directions manuellement déconnectées par le joueur. */
    private final EnumSet<Direction> disconnectedDirections = EnumSet.noneOf(Direction.class);

    /** Directions en mode extraction (remplacent les propriétés BlockState EXTRACT_*). */
    private final EnumSet<Direction> extractingDirections = EnumSet.noneOf(Direction.class);

    /** Couleur de teinte du core (-1 = pas de teinte). */
    private int tintColor = -1;

    /** Donnees du filtre (null = pas de filtre installe). */
    @Nullable
    private ItemFilterData filterData = null;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        this(ApicaBlockEntities.ITEM_PIPE.get(), pos, state, MK1_BUFFER, MK1_TRANSFER);
    }

    public ItemPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                               int bufferSize, int transferAmount) {
        super(type, pos, state);
        this.transferAmount = transferAmount;
        this.buffer = new ItemStackHandler(bufferSize) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };

        DebugWandItem.addDisplay(this, () -> {
            StringBuilder str = new StringBuilder();
            for (int i = 0; i < buffer.getSlots(); i++) {
                ItemStack stack = buffer.getStackInSlot(i);
                if (!stack.isEmpty())
                    str.append(stack.getHoverName().getString()).append(": ").append(stack.getCount()).append("\n");
            }
            if (!transitItems.isEmpty()) {
                str.append("Transit: ").append(transitItems.size()).append(" items\n");
            }
            return "Item: " + str;
        }, new Vec3(0, 1, 0));
    }

    // --- Factory methods for MK versions ---

    public static ItemPipeBlockEntity createMk2(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(ApicaBlockEntities.ITEM_PIPE_MK2.get(), pos, state,
            MK2_BUFFER, MK2_TRANSFER);
    }

    public static ItemPipeBlockEntity createMk3(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(ApicaBlockEntities.ITEM_PIPE_MK3.get(), pos, state,
            MK3_BUFFER, MK3_TRANSFER);
    }

    public static ItemPipeBlockEntity createMk4(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(ApicaBlockEntities.ITEM_PIPE_MK4.get(), pos, state,
            MK4_BUFFER, MK4_TRANSFER);
    }

    // --- Server tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if (be.transferCooldown > 0) {
            be.transferCooldown--;
            return;
        }

        // 1. Avancer les items en transit d'un hop
        be.advanceTransitItems(level, pos, state);

        // 2. Extraire de nouveaux items (avec pre-validation)
        be.processExtractions(level, pos, state);

        be.transferCooldown = 8;
    }

    // --- Extraction avec pre-validation ---

    private void processExtractions(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (isBufferFull()) return;

        for (Direction dir : Direction.values()) {
            if (!AbstractPipeBlock.isConnected(state, dir)) continue;
            if (!isExtracting(dir)) continue;

            BlockPos neighborPos = pos.relative(dir);

            // Ne pas extraire depuis d'autres item pipes
            if (level.getBlockEntity(neighborPos) instanceof ItemPipeBlockEntity) continue;

            IItemHandler cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (cap == null) continue;

            extractWithPreValidation(cap, serverLevel, pos);
        }
    }

    /**
     * Extrait un item du handler source seulement si une destination valide existe dans le réseau.
     * L'item est immédiatement placé en transit avec une route pré-calculée.
     */
    private void extractWithPreValidation(IItemHandler handler, ServerLevel level, BlockPos myPos) {
        PipeNetwork network = ItemPipeNetworkManager.get(level).getNetworkAt(myPos);
        if (network == null) return;

        for (int i = 0; i < handler.getSlots() && !isBufferFull(); i++) {
            ItemStack simulated = handler.extractItem(i, transferAmount, true);
            if (simulated.isEmpty()) continue;

            // Pre-validation : chercher une destination dans le réseau
            PipeNetwork.RouteResult result = network.findDestination(myPos, simulated, level);
            if (result == null) continue;

            // Destination trouvée : extraire pour de vrai
            ItemStack extracted = handler.extractItem(i, transferAmount, false);
            if (extracted.isEmpty()) continue;

            // Créer l'item en transit avec la route
            PipeTransitItem transit = new PipeTransitItem(
                extracted, result.route(), 0, result.endpoint().machinePos()
            );
            transitItems.add(transit);
            setChanged();
            break; // Un seul slot extrait par tick
        }
    }

    // --- Avancement des items en transit ---

    private void advanceTransitItems(Level level, BlockPos pos, BlockState state) {
        if (transitItems.isEmpty()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        Iterator<PipeTransitItem> it = transitItems.iterator();
        while (it.hasNext()) {
            PipeTransitItem transit = it.next();

            if (transit.isAtEndOfRoute()) {
                // On est à la dernière pipe : tenter l'insertion dans la machine destination
                if (tryDeliverToDestination(transit, serverLevel)) {
                    it.remove();
                } else {
                    // Destination pleine : tenter re-route
                    tryReroute(transit, pos, serverLevel);
                }
                continue;
            }

            BlockPos nextHop = transit.nextHop();
            if (nextHop == null) continue;

            // Vérifier que le prochain hop est une pipe valide
            BlockEntity nextBe = level.getBlockEntity(nextHop);
            if (nextBe instanceof ItemPipeBlockEntity nextPipe) {
                if (!nextPipe.isBufferFull()) {
                    // Transférer l'item au prochain pipe
                    transit.advance();
                    nextPipe.transitItems.add(transit);
                    nextPipe.setChanged();
                    it.remove();
                }
                // Si le prochain pipe est plein : backpressure (attendre)
            } else {
                // Le prochain hop n'est plus une pipe valide : tenter re-route
                tryReroute(transit, pos, serverLevel);
            }
        }
    }

    /**
     * Tente d'insérer l'item en transit dans la machine destination.
     */
    private boolean tryDeliverToDestination(PipeTransitItem transit, ServerLevel level) {
        BlockPos destPos = transit.getDestinationPos();
        if (!level.hasChunkAt(destPos)) return false;

        // Trouver la face par laquelle on insère
        BlockPos myPos = transit.currentPos();
        Direction insertDir = getDirectionBetween(myPos, destPos);
        if (insertDir == null) return false;

        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, destPos, insertDir.getOpposite());
        if (handler == null) return false;

        ItemStack toInsert = transit.getStack().copy();
        ItemStack remaining = toInsert;
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }

        if (remaining.isEmpty()) {
            return true; // Tout inséré
        }

        // Insertion partielle : mettre le reste dans le buffer
        if (remaining.getCount() < toInsert.getCount()) {
            transit.getStack().setCount(remaining.getCount());
        }
        return false;
    }

    /**
     * Tente de re-router un item en transit dont la route est invalide.
     * Si aucune nouvelle route, l'item reste dans le buffer de ce pipe.
     */
    private void tryReroute(PipeTransitItem transit, BlockPos myPos, ServerLevel level) {
        PipeNetwork network = ItemPipeNetworkManager.get(level).getNetworkAt(myPos);
        if (network == null) return;

        PipeNetwork.RouteResult newRoute = network.findDestination(myPos, transit.getStack(), level);
        if (newRoute != null) {
            // Remplacer par un nouveau transit item avec la nouvelle route
            int idx = transitItems.indexOf(transit);
            if (idx >= 0) {
                transitItems.set(idx, new PipeTransitItem(
                    transit.getStack(), newRoute.route(), 0, newRoute.endpoint().machinePos()
                ));
            }
        }
        // Si pas de nouvelle route : l'item reste dans transitItems et on re-essaiera au prochain tick
    }

    /**
     * Retourne la direction entre deux positions adjacentes.
     */
    @Nullable
    private static Direction getDirectionBetween(BlockPos from, BlockPos to) {
        BlockPos diff = to.subtract(from);
        for (Direction dir : Direction.values()) {
            if (dir.getNormal().equals(diff)) return dir;
        }
        return null;
    }

    // --- Buffer helpers ---

    public ItemStack insertIntoBuffer(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < buffer.getSlots() && !remaining.isEmpty(); i++) {
            remaining = buffer.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    private boolean isBufferFull() {
        for (int i = 0; i < buffer.getSlots(); i++) {
            ItemStack stack = buffer.getStackInSlot(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return transitItems.size() >= buffer.getSlots();
    }

    public ItemStackHandler getBuffer() {
        return buffer;
    }

    /**
     * Retourne un IItemHandler wrappant le buffer avec verification du filtre.
     * Les items rejetés par le filtre ne peuvent pas être insérés via cette vue.
     * Utilisé par le système de capabilities pour que les blocs adjacents
     * (hoppers, autres pipes) respectent le filtre du pipe.
     */
    public IItemHandler getFilteredBuffer() {
        if (filterData == null) return buffer;
        final ItemFilterData filter = filterData;
        final ItemStackHandler inner = buffer;
        return new IItemHandler() {
            @Override
            public int getSlots() { return inner.getSlots(); }

            @Override
            public ItemStack getStackInSlot(int slot) { return inner.getStackInSlot(slot); }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!stack.isEmpty() && !filter.matches(stack)) return stack;
                return inner.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return inner.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) { return inner.getSlotLimit(slot); }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (!stack.isEmpty() && !filter.matches(stack)) return false;
                return inner.isItemValid(slot, stack);
            }
        };
    }

    // --- Disconnect / Tint ---

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

    public boolean isExtracting(Direction dir) {
        return extractingDirections.contains(dir);
    }

    public void setExtracting(Direction dir, boolean extracting) {
        if (extracting) {
            extractingDirections.add(dir);
        } else {
            extractingDirections.remove(dir);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getTintColor() { return tintColor; }

    public void setTintColor(int color) {
        this.tintColor = color;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasTint() { return tintColor != -1; }

    // --- Filter ---

    public boolean hasFilter() {
        return filterData != null;
    }

    @Nullable
    public ItemFilterData getFilter() {
        return filterData;
    }

    public void setFilter(ItemFilterData data) {
        this.filterData = data;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.invalidateCapabilities(worldPosition);
        }
    }

    public void removeFilter() {
        this.filterData = null;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.invalidateCapabilities(worldPosition);
        }
    }

    // --- Network registration on load ---

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ItemPipeNetworkManager manager = ItemPipeNetworkManager.get(serverLevel);
            if (manager.getNetworkAt(worldPosition) == null) {
                manager.onPipeAdded(worldPosition, serverLevel);
            }
        }
    }

    /**
     * Force la synchronisation des donnees du block entity vers le client.
     * Utilise par le packet handler du filtre apres modification des ghost slots/mode/priority.
     */
    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // --- Sync client ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
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
        tag.put("Buffer", buffer.serializeNBT(registries));

        // Sauvegarder les items en transit
        if (!transitItems.isEmpty()) {
            ListTag transitTag = new ListTag();
            for (PipeTransitItem transit : transitItems) {
                transitTag.add(transit.save(registries));
            }
            tag.put("TransitItems", transitTag);
        }

        int disconnectedBits = 0;
        for (Direction dir : disconnectedDirections) {
            disconnectedBits |= (1 << dir.ordinal());
        }
        if (disconnectedBits != 0) {
            tag.putInt("DisconnectedDirs", disconnectedBits);
        }
        int extractingBits = 0;
        for (Direction dir : extractingDirections) {
            extractingBits |= (1 << dir.ordinal());
        }
        if (extractingBits != 0) {
            tag.putInt("ExtractingDirs", extractingBits);
        }
        if (tintColor != -1) {
            tag.putInt("TintColor", tintColor);
        }
        if (filterData != null) {
            tag.put("FilterData", filterData.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Buffer")) {
            buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
        }

        // Charger les items en transit
        transitItems.clear();
        if (tag.contains("TransitItems")) {
            ListTag transitTag = tag.getList("TransitItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < transitTag.size(); i++) {
                transitItems.add(PipeTransitItem.load(transitTag.getCompound(i), registries));
            }
        }

        disconnectedDirections.clear();
        if (tag.contains("DisconnectedDirs")) {
            int bits = tag.getInt("DisconnectedDirs");
            for (Direction dir : Direction.values()) {
                if ((bits & (1 << dir.ordinal())) != 0) {
                    disconnectedDirections.add(dir);
                }
            }
        }
        extractingDirections.clear();
        if (tag.contains("ExtractingDirs")) {
            int bits = tag.getInt("ExtractingDirs");
            for (Direction dir : Direction.values()) {
                if ((bits & (1 << dir.ordinal())) != 0) {
                    extractingDirections.add(dir);
                }
            }
        }
        tintColor = tag.contains("TintColor") ? tag.getInt("TintColor") : -1;

        if (tag.contains("FilterData")) {
            filterData = ItemFilterData.fromTag(tag.getCompound("FilterData"), registries);
        } else {
            filterData = null;
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        requestModelDataUpdate();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        super.onDataPacket(net, pkt, registries);
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
