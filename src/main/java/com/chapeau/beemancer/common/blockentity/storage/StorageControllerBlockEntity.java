/**
 * ============================================================
 * [StorageControllerBlockEntity.java]
 * Description: BlockEntity pour le Storage Controller - gère le réseau de coffres et le multibloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | BeemancerBlockEntities   | Type du BlockEntity    | Constructeur                   |
 * | Level                    | Accès monde            | Flood fill, vérification coffres|
 * | StorageHelper            | Vérification coffres   | isStorageContainer             |
 * | StorageItemsSyncPacket   | Sync vers client       | Envoi items agrégés            |
 * | MultiblockController     | Interface multibloc    | Formation/destruction          |
 * | MultiblockPatterns       | Pattern storage        | Définition structure           |
 * | MultiblockValidator      | Validation             | tryFormStorage()               |
 * | MultiblockEvents         | Détection destruction  | Enregistrement contrôleur      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlock.java (création et interaction)
 * - StorageTerminalBlockEntity.java (récupération items agrégés)
 * - StorageControllerRenderer.java (rendu mode édition)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.altar.HoneyReservoirBlock;
import com.chapeau.beemancer.common.block.storage.ControllerPipeBlock;
import com.chapeau.beemancer.common.block.altar.HoneyedStoneBlock;
import com.chapeau.beemancer.common.block.storage.ControllerStats;
import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.common.block.storage.DeliveryTask;
import com.chapeau.beemancer.common.blockentity.storage.ControllerPipeBlockEntity;
import com.chapeau.beemancer.common.block.storage.StorageControllerBlock;
import com.chapeau.beemancer.common.block.storage.StorageEditModeHandler;
import com.chapeau.beemancer.common.entity.delivery.DeliveryBeeEntity;
import com.chapeau.beemancer.common.menu.storage.StorageControllerMenu;
import com.chapeau.beemancer.core.multiblock.BlockMatcher;
import com.chapeau.beemancer.core.multiblock.MultiblockController;
import com.chapeau.beemancer.core.multiblock.MultiblockEvents;
import com.chapeau.beemancer.core.multiblock.MultiblockPattern;
import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.network.packets.StorageItemsSyncPacket;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerTags;
import com.chapeau.beemancer.core.util.StorageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Unité centrale du réseau de stockage.
 * Implémente MultiblockController pour gérer la formation/destruction du multibloc.
 *
 * Fonctionnalités:
 * - Multibloc: formation 3x3x3 avec pipes, honeyed stone, reservoirs, terminal
 * - Mode édition: shift+clic droit pour toggle
 * - Enregistrement coffres: clic droit en mode édition (flood fill)
 * - Agrégation items: liste tous items de tous coffres enregistrés
 * - Synchronisation: temps réel si GUI ouverte, périodique sinon
 */
public class StorageControllerBlockEntity extends BlockEntity implements MultiblockController, MenuProvider {

    private static final int MAX_RANGE = 24;
    private static final int SYNC_INTERVAL = 40;

    // Multibloc
    private boolean storageFormed = false;
    private int multiblockRotation = 0;

    // Coffres enregistrés
    private final Set<BlockPos> registeredChests = new HashSet<>();

    // Mode édition
    private boolean editMode = false;
    private UUID editingPlayer = null;

    // Terminaux liés
    private final Set<BlockPos> linkedTerminals = new HashSet<>();

    // Cache items agrégés
    private List<ItemStack> aggregatedItems = new ArrayList<>();
    private int syncTimer = 0;
    private boolean needsSync = true;

    // Joueurs avec GUI ouverte (UUID -> terminal pos)
    private final Map<UUID, BlockPos> playersViewing = new HashMap<>();

    // === Delivery System ===
    private static final int MAX_ACTIVE_BEES = 2;
    private static final int DELIVERY_PROCESS_INTERVAL = 10;
    private static final int MAX_COMPLETED_IDS = 100;

    private final ItemStackHandler essenceSlots = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(BeemancerTags.Items.ESSENCES);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }
    };

    private final Queue<DeliveryTask> deliveryQueue = new LinkedList<>();
    private final List<DeliveryTask> activeTasks = new ArrayList<>();
    private final Set<UUID> completedTaskIds = new LinkedHashSet<>();
    private int deliveryTimer = 0;

    // Honey consumption
    private static final int HONEY_CONSUME_INTERVAL = 20; // 1 seconde
    private int honeyConsumeTimer = 0;
    private boolean honeyDepleted = false;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ControllerStats.getFlightSpeed(essenceSlots);
                case 1 -> ControllerStats.getSearchSpeed(essenceSlots);
                case 2 -> ControllerStats.getCraftSpeed(essenceSlots);
                case 3 -> ControllerStats.getQuantity(essenceSlots);
                case 4 -> ControllerStats.getHoneyConsumption(essenceSlots, registeredChests.size());
                case 5 -> ControllerStats.getHoneyEfficiency(essenceSlots);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() { return 6; }
    };

    public StorageControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.STORAGE_CONTROLLER.get(), pos, blockState);
    }

    // ==================== MultiblockController ====================

    @Override
    public MultiblockPattern getPattern() {
        return MultiblockPatterns.STORAGE_CONTROLLER;
    }

    @Override
    public boolean isFormed() {
        return storageFormed;
    }

    @Override
    public BlockPos getControllerPos() {
        return worldPosition;
    }

    @Override
    public int getRotation() {
        return multiblockRotation;
    }

    @Override
    public void onMultiblockFormed() {
        storageFormed = true;
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(StorageControllerBlock.FORMED, true), 3);
            setFormedOnStructureBlocks(true);
            MultiblockEvents.registerActiveController(level, worldPosition);
            setChanged();
        }
    }

    @Override
    public void onMultiblockBroken() {
        storageFormed = false;
        if (level != null && !level.isClientSide()) {
            // Tuer toutes les abeilles de livraison liées à ce controller
            killAllDeliveryBees();

            // Vider la queue, les tâches actives et les IDs complétées
            deliveryQueue.clear();
            activeTasks.clear();
            completedTaskIds.clear();

            setFormedOnStructureBlocks(false);
            multiblockRotation = 0;
            if (level.getBlockState(worldPosition).hasProperty(StorageControllerBlock.FORMED)) {
                level.setBlock(worldPosition, getBlockState().setValue(StorageControllerBlock.FORMED, false), 3);
            }
            MultiblockEvents.unregisterController(worldPosition);
            setChanged();
        }
    }

    /**
     * Tue toutes les DeliveryBeeEntity liées à ce controller.
     * Recherche dans un rayon de MAX_RANGE autour du controller.
     */
    private void killAllDeliveryBees() {
        if (level == null || level.isClientSide()) return;

        List<DeliveryBeeEntity> bees = level.getEntitiesOfClass(
            DeliveryBeeEntity.class,
            new net.minecraft.world.phys.AABB(worldPosition).inflate(MAX_RANGE),
            bee -> worldPosition.equals(bee.getControllerPos())
        );
        for (DeliveryBeeEntity bee : bees) {
            bee.discard();
        }
    }

    /**
     * Tente de former le multibloc Storage Controller.
     * Essaie les 4 rotations horizontales (0°, 90°, 180°, 270°).
     * @return true si la formation a réussi
     */
    public boolean tryFormStorage() {
        if (level == null || level.isClientSide()) return false;

        int rotation = MultiblockValidator.validateWithRotations(getPattern(), level, worldPosition);

        if (rotation >= 0) {
            multiblockRotation = rotation;
            onMultiblockFormed();
            return true;
        }

        Beemancer.LOGGER.debug("Storage controller validation failed at {} - no valid rotation found",
            worldPosition);
        return false;
    }

    /**
     * Met à jour FORMED et FORMED_ROTATION sur tous les blocs structurels du multibloc.
     * Itère les offsets originaux du pattern puis applique la rotation du multibloc.
     * Calcule la rotation de chaque bloc en fonction de sa position dans la structure.
     */
    private void setFormedOnStructureBlocks(boolean formed) {
        if (level == null) return;

        for (MultiblockPattern.PatternElement element : getPattern().getElements()) {
            if (BlockMatcher.isAirMatcher(element.matcher())) continue;

            Vec3i originalOffset = element.offset();
            Vec3i rotatedOffset = MultiblockPattern.rotateY(originalOffset, multiblockRotation);
            BlockPos blockPos = worldPosition.offset(rotatedOffset);
            BlockState state = level.getBlockState(blockPos);

            // Calcul du spread pour les colonnes gauche/droite (x != 0)
            float spreadX = 0.0f;
            float spreadZ = 0.0f;
            if (formed && originalOffset.getX() != 0) {
                spreadX = rotatedOffset.getX() * -3.0f / 16.0f;
                spreadZ = rotatedOffset.getZ() * -3.0f / 16.0f;
            }

            // Controller Pipes: formed state + rotation via blockstate, spread via BlockEntity
            if (state.getBlock() instanceof ControllerPipeBlock) {
                int rotation = formed ? computeBlockRotation(originalOffset, state) : 0;
                BlockEntity be = level.getBlockEntity(blockPos);
                if (be instanceof ControllerPipeBlockEntity pipeBe) {
                    if (formed) {
                        pipeBe.setFormed(rotation, spreadX, spreadZ);
                    } else {
                        pipeBe.clearFormed();
                    }
                }
                // Set FORMED and FORMED_ROTATION in blockstate
                BlockState newState = state
                    .setValue(ControllerPipeBlock.FORMED, formed)
                    .setValue(ControllerPipeBlock.FORMED_ROTATION, rotation);
                if (!newState.equals(state)) {
                    level.setBlock(blockPos, newState, 3);
                }
                continue;
            }

            // Reservoirs: appliquer le spread
            if (state.getBlock() instanceof HoneyReservoirBlock) {
                BlockEntity be = level.getBlockEntity(blockPos);
                if (be instanceof HoneyReservoirBlockEntity reservoirBe) {
                    reservoirBe.setFormedSpread(spreadX, spreadZ);
                }
            }

            boolean changed = false;

            // Honeyed stone du bas (y<0): pas de formed (juste la pierre brute)
            boolean skipFormed = (state.getBlock() instanceof HoneyedStoneBlock)
                && originalOffset.getY() < 0;

            BooleanProperty formedProp = findFormedProperty(state);
            if (formedProp != null && !skipFormed && state.getValue(formedProp) != formed) {
                state = state.setValue(formedProp, formed);
                changed = true;
            }

            IntegerProperty rotProp = findFormedRotationProperty(state);
            if (rotProp != null) {
                int rotation = formed ? computeBlockRotation(originalOffset, state) : 0;
                if (state.getValue(rotProp) != rotation) {
                    state = state.setValue(rotProp, rotation);
                    changed = true;
                }
            }

            if (changed) {
                level.setBlock(blockPos, state, 3);
            }
        }
    }

    /**
     * Calcule la rotation à appliquer sur un bloc de la structure.
     * Les pipes ont une rotation spécifique (direction du coude + flip vertical),
     * les autres utilisent directement la rotation du multibloc.
     *
     * Pipes formed_rotation:
     * - 0-3: coude vertical vers le bas (ouverture en bas), Y rotation 0/90/180/270
     * - 4-7: coude vertical vers le haut (x=180 flip), Y rotation 0/90/180/270
     * Pipes du bas (y<0): ouverture vers le haut (valeurs 4-7)
     * Pipes du haut (y>0): ouverture vers le bas (valeurs 0-3)
     *
     * @param originalOffset L'offset original (non roté) dans le pattern
     * @param state Le BlockState actuel du bloc
     * @return La rotation à appliquer sur le bloc (0-3 pour la plupart, 0-7 pour les pipes)
     */
    private int computeBlockRotation(Vec3i originalOffset, BlockState state) {
        if (state.getBlock() instanceof ControllerPipeBlock) {
            // Le modèle de pipe formed a un coude vers +X, ouverture en bas.
            // Pipe à x=-1: coude vers +X = rotation de base 0
            // Pipe à x=+1: coude vers -X = rotation de base 2
            int baseRotation = (originalOffset.getX() < 0 ? 0 : 2);
            int yRotation = (baseRotation + multiblockRotation) & 3;
            // Pipes en bas (y<0): flip vertical (+4) pour que l'ouverture pointe vers le haut
            boolean bottom = originalOffset.getY() < 0;
            return bottom ? yRotation + 4 : yRotation;
        }
        return multiblockRotation;
    }

    /**
     * Cherche la propriété FORMED dans un BlockState.
     * Supporte les différents blocs du multibloc qui ont chacun leur propre constante FORMED.
     */
    @Nullable
    private static BooleanProperty findFormedProperty(BlockState state) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("formed") && prop instanceof BooleanProperty boolProp) {
                return boolProp;
            }
        }
        return null;
    }

    /**
     * Cherche la propriété FORMED_ROTATION dans un BlockState.
     */
    @Nullable
    private static IntegerProperty findFormedRotationProperty(BlockState state) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("formed_rotation") && prop instanceof IntegerProperty intProp) {
                return intProp;
            }
        }
        return null;
    }

    // === Mode Édition ===

    /**
     * Toggle le mode édition pour un joueur.
     */
    public boolean toggleEditMode(UUID playerId) {
        if (editMode && editingPlayer != null && editingPlayer.equals(playerId)) {
            editMode = false;
            editingPlayer = null;
            setChanged();
            syncToClient();
            return false;
        } else if (!editMode) {
            editMode = true;
            editingPlayer = playerId;
            setChanged();
            syncToClient();
            return true;
        }
        return false;
    }

    /**
     * Force la sortie du mode édition.
     */
    public void exitEditMode() {
        if (editMode) {
            if (editingPlayer != null) {
                StorageEditModeHandler.stopEditing(editingPlayer);
            }
            editMode = false;
            editingPlayer = null;
            setChanged();
            syncToClient();
        }
    }

    /**
     * Vérifie si un joueur peut éditer.
     */
    public boolean canEdit(UUID playerId) {
        return editMode && editingPlayer != null && editingPlayer.equals(playerId);
    }

    public boolean isEditMode() {
        return editMode;
    }

    @Nullable
    public UUID getEditingPlayer() {
        return editingPlayer;
    }

    // === Gestion des Coffres ===

    /**
     * Tente d'enregistrer ou de retirer un coffre.
     * Si le coffre est déjà enregistré, le retire.
     * Sinon, enregistre le coffre et ses adjacents (flood fill).
     *
     * @return true si l'opération a réussi
     */
    public boolean toggleChest(BlockPos chestPos) {
        if (level == null) return false;

        if (!isInRange(chestPos)) return false;
        if (!isChest(chestPos)) return false;

        if (registeredChests.contains(chestPos)) {
            registeredChests.remove(chestPos);
            setChanged();
            syncToClient();
            needsSync = true;
            return true;
        } else {
            registerChestWithNeighbors(chestPos);
            needsSync = true;
            return true;
        }
    }

    /**
     * Flood fill pour enregistrer un coffre et tous ses adjacents.
     */
    private void registerChestWithNeighbors(BlockPos startPos) {
        if (level == null) return;

        Queue<BlockPos> toCheck = new LinkedList<>();
        Set<BlockPos> checked = new HashSet<>();
        toCheck.add(startPos);

        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();

            if (checked.contains(current)) continue;
            checked.add(current);

            if (!isChest(current)) continue;
            if (registeredChests.contains(current)) continue;
            if (!isInRange(current)) continue;

            registeredChests.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!checked.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }
        }

        setChanged();
        syncToClient();
    }

    /**
     * Vérifie si une position est un coffre.
     */
    private boolean isChest(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        return StorageHelper.isStorageContainer(state);
    }

    /**
     * Vérifie si une position est dans le rayon d'action.
     */
    private boolean isInRange(BlockPos pos) {
        double distance = Math.sqrt(worldPosition.distSqr(pos));
        return distance <= MAX_RANGE;
    }

    /**
     * Retourne les coffres enregistrés.
     */
    public Set<BlockPos> getRegisteredChests() {
        return Collections.unmodifiableSet(registeredChests);
    }

    // === Gestion des Terminaux ===

    /**
     * Lie un terminal à ce controller.
     */
    public void linkTerminal(BlockPos terminalPos) {
        linkedTerminals.add(terminalPos);
        setChanged();
    }

    /**
     * Retire un terminal.
     */
    public void unlinkTerminal(BlockPos terminalPos) {
        linkedTerminals.remove(terminalPos);
        playersViewing.entrySet().removeIf(entry -> entry.getValue().equals(terminalPos));
        setChanged();
    }

    public Set<BlockPos> getLinkedTerminals() {
        return Collections.unmodifiableSet(linkedTerminals);
    }

    // === Agrégation Items ===

    /**
     * Retourne la liste agrégée de tous les items.
     * Les items identiques sont fusionnés avec leur quantité totale.
     */
    public List<ItemStack> getAggregatedItems() {
        return aggregatedItems;
    }

    /**
     * Force le recalcul des items agrégés.
     */
    public void refreshAggregatedItems() {
        if (level == null) return;

        Map<ItemStackKey, Integer> itemCounts = new HashMap<>();

        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : registeredChests) {
            if (!isChest(pos)) {
                toRemove.add(pos);
            }
        }
        if (!toRemove.isEmpty()) {
            registeredChests.removeAll(toRemove);
            setChanged();
            syncToClient();
        }

        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack stack = container.getItem(i);
                    if (!stack.isEmpty()) {
                        ItemStackKey key = new ItemStackKey(stack);
                        itemCounts.merge(key, stack.getCount(), Integer::sum);
                    }
                }
            }
        }

        aggregatedItems = new ArrayList<>();
        for (Map.Entry<ItemStackKey, Integer> entry : itemCounts.entrySet()) {
            ItemStack stack = entry.getKey().toStack();
            stack.setCount(entry.getValue());
            aggregatedItems.add(stack);
        }

        aggregatedItems.sort(Comparator.comparing(
            stack -> stack.getHoverName().getString()
        ));

        syncItemsToViewers();
    }

    /**
     * Envoie la liste des items agrégés aux joueurs qui ont le terminal ouvert.
     */
    private void syncItemsToViewers() {
        if (level == null || level.isClientSide()) return;

        for (Map.Entry<UUID, BlockPos> entry : playersViewing.entrySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                PacketDistributor.sendToPlayer(player,
                    new StorageItemsSyncPacket(entry.getValue(), aggregatedItems));
            }
        }
    }

    /**
     * Trouve un slot pour déposer un item.
     *
     * @return la position du coffre où déposer, ou null si aucun espace
     */
    @Nullable
    public BlockPos findSlotForItem(ItemStack stack) {
        if (level == null) return null;

        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, stack) &&
                        existing.getCount() < existing.getMaxStackSize()) {
                        return chestPos;
                    }
                }
            }
        }

        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).isEmpty()) {
                        return chestPos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Dépose un item dans le réseau de stockage.
     * Priorise les coffres qui contiennent déjà l'item (fusion puis slots vides du même coffre).
     *
     * @return le reste non déposé (vide si tout a été déposé)
     */
    public ItemStack depositItem(ItemStack stack) {
        if (level == null || stack.isEmpty()) return stack;

        ItemStack remaining = stack.copy();

        for (BlockPos chestPos : registeredChests) {
            if (remaining.isEmpty()) break;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                boolean hasItem = false;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (ItemStack.isSameItemSameComponents(container.getItem(i), remaining)) {
                        hasItem = true;
                        break;
                    }
                }

                if (hasItem) {
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack existing = container.getItem(i);
                        if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                            int space = existing.getMaxStackSize() - existing.getCount();
                            int toTransfer = Math.min(space, remaining.getCount());
                            if (toTransfer > 0) {
                                existing.grow(toTransfer);
                                remaining.shrink(toTransfer);
                                container.setChanged();
                            }
                        }
                        if (remaining.isEmpty()) break;
                    }

                    for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
                        if (container.getItem(i).isEmpty()) {
                            int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                            ItemStack toPlace = remaining.copy();
                            toPlace.setCount(toTransfer);
                            container.setItem(i, toPlace);
                            remaining.shrink(toTransfer);
                            container.setChanged();
                        }
                    }
                }
            }
        }

        for (BlockPos chestPos : registeredChests) {
            if (remaining.isEmpty()) break;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (container.getItem(i).isEmpty()) {
                        int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                        ItemStack toPlace = remaining.copy();
                        toPlace.setCount(toTransfer);
                        container.setItem(i, toPlace);
                        remaining.shrink(toTransfer);
                        container.setChanged();
                    }
                    if (remaining.isEmpty()) break;
                }
            }
        }

        needsSync = true;
        return remaining;
    }

    /**
     * Extrait un item du réseau de stockage.
     *
     * @return l'item extrait (peut être moins que demandé)
     */
    public ItemStack extractItem(ItemStack template, int count) {
        if (level == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (BlockPos chestPos : registeredChests) {
            if (needed <= 0) break;

            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, template)) {
                        int toTake = Math.min(needed, existing.getCount());
                        existing.shrink(toTake);
                        result.grow(toTake);
                        needed -= toTake;
                        container.setChanged();

                        if (existing.isEmpty()) {
                            container.setItem(i, ItemStack.EMPTY);
                        }
                    }
                    if (needed <= 0) break;
                }
            }
        }

        needsSync = true;
        return result;
    }

    // === Tick ===

    public static void serverTick(StorageControllerBlockEntity be) {
        be.syncTimer++;

        boolean hasViewers = !be.playersViewing.isEmpty();
        boolean shouldSync = hasViewers || (be.syncTimer >= SYNC_INTERVAL && be.needsSync);

        if (shouldSync) {
            be.refreshAggregatedItems();
            be.syncTimer = 0;
            be.needsSync = false;
        }

        // Honey consumption (only when formed)
        if (be.storageFormed) {
            be.honeyConsumeTimer++;
            if (be.honeyConsumeTimer >= HONEY_CONSUME_INTERVAL) {
                be.honeyConsumeTimer = 0;
                be.consumeHoney();
            }
        }

        // Delivery system
        be.deliveryTimer++;
        if (be.deliveryTimer >= DELIVERY_PROCESS_INTERVAL) {
            be.deliveryTimer = 0;
            be.processDeliveryQueue();
        }

        if (be.editMode && be.editingPlayer != null && be.level != null) {
            var server = be.level.getServer();
            if (server != null) {
                var player = server.getPlayerList().getPlayer(be.editingPlayer);
                if (player == null) {
                    be.exitEditMode();
                } else {
                    double distSqr = player.distanceToSqr(
                        be.worldPosition.getX() + 0.5,
                        be.worldPosition.getY() + 0.5,
                        be.worldPosition.getZ() + 0.5);
                    if (distSqr > MAX_RANGE * MAX_RANGE) {
                        be.exitEditMode();
                    }
                }
            }
        }

        if (be.level != null && be.level.getServer() != null) {
            be.playersViewing.keySet().removeIf(uuid ->
                be.level.getServer().getPlayerList().getPlayer(uuid) == null);
        }
    }

    public void addViewer(UUID playerId, BlockPos terminalPos) {
        playersViewing.put(playerId, terminalPos);
        if (level != null && !level.isClientSide()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                refreshAggregatedItems();
                PacketDistributor.sendToPlayer(player,
                    new StorageItemsSyncPacket(terminalPos, aggregatedItems));
            }
        }
    }

    public void removeViewer(UUID playerId) {
        playersViewing.remove(playerId);
    }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.storage_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new StorageControllerMenu(containerId, playerInv, essenceSlots, dataAccess);
    }

    public ItemStackHandler getEssenceSlots() {
        return essenceSlots;
    }

    // === Delivery System ===

    /**
     * Ajoute une tâche de livraison à la queue.
     * Appelé par le terminal quand un joueur demande un item ou dépose un item.
     */
    public void addDeliveryTask(DeliveryTask task) {
        deliveryQueue.add(task);
        setChanged();
    }

    /**
     * Trouve un coffre contenant l'item demandé.
     * @return la position du coffre, ou null si introuvable
     */
    @Nullable
    public BlockPos findChestWithItem(ItemStack template, int minCount) {
        if (level == null || template.isEmpty()) return null;

        for (BlockPos chestPos : registeredChests) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (be instanceof Container container) {
                int found = 0;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    ItemStack existing = container.getItem(i);
                    if (ItemStack.isSameItemSameComponents(existing, template)) {
                        found += existing.getCount();
                        if (found >= minCount) return chestPos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extrait un item d'un coffre spécifique pour une livraison.
     * Appelé par DeliveryPhaseGoal quand l'abeille arrive au coffre.
     */
    public ItemStack extractItemForDelivery(ItemStack template, int count, BlockPos chestPos) {
        if (level == null || template.isEmpty() || count <= 0) return ItemStack.EMPTY;

        BlockEntity be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) return ItemStack.EMPTY;

        ItemStack result = template.copy();
        result.setCount(0);
        int needed = count;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, template)) {
                int toTake = Math.min(needed, existing.getCount());
                existing.shrink(toTake);
                result.grow(toTake);
                needed -= toTake;
                container.setChanged();
                if (existing.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
            }
            if (needed <= 0) break;
        }

        needsSync = true;
        return result;
    }

    /**
     * Dépose un item dans un coffre spécifique pour une livraison.
     * Si chestPos est null, dépose dans n'importe quel coffre du réseau.
     * Appelé par DeliveryPhaseGoal quand l'abeille arrive au coffre.
     *
     * @return le reste non déposé
     */
    public ItemStack depositItemForDelivery(ItemStack stack, @Nullable BlockPos chestPos) {
        if (chestPos != null) {
            return depositIntoChest(stack, chestPos);
        }
        return depositItem(stack);
    }

    private ItemStack depositIntoChest(ItemStack stack, BlockPos chestPos) {
        if (level == null || stack.isEmpty()) return stack;

        BlockEntity be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) return stack;

        ItemStack remaining = stack.copy();

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toTransfer = Math.min(space, remaining.getCount());
                if (toTransfer > 0) {
                    existing.grow(toTransfer);
                    remaining.shrink(toTransfer);
                    container.setChanged();
                }
            }
        }

        for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                int toTransfer = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                ItemStack toPlace = remaining.copy();
                toPlace.setCount(toTransfer);
                container.setItem(i, toPlace);
                remaining.shrink(toTransfer);
                container.setChanged();
            }
        }

        needsSync = true;
        return remaining;
    }

    /**
     * Consomme du miel depuis les 2 HoneyReservoirs du multibloc.
     * Appelé toutes les secondes (20 ticks) quand le multibloc est formé.
     * Si pas assez de miel: honeyDepleted = true → bloque les livraisons.
     */
    private void consumeHoney() {
        if (level == null || level.isClientSide()) return;

        int consumptionPerSecond = ControllerStats.getHoneyConsumption(essenceSlots, registeredChests.size());
        int remaining = consumptionPerSecond;

        // Trouver les 2 reservoirs du multibloc (offsets x=-1 et x=+1, y=0, z=0 dans le pattern)
        // Les reservoirs sont aux offsets (-1,0,0) et (1,0,0) dans le pattern original
        for (int xOff : new int[]{-1, 1}) {
            if (remaining <= 0) break;
            Vec3i rotatedOffset = MultiblockPattern.rotateY(new Vec3i(xOff, 0, 0), multiblockRotation);
            BlockPos reservoirPos = worldPosition.offset(rotatedOffset);
            BlockEntity be = level.getBlockEntity(reservoirPos);
            if (be instanceof HoneyReservoirBlockEntity reservoir) {
                FluidStack drained = reservoir.drain(remaining, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    remaining -= drained.getAmount();
                }
            }
        }

        boolean wasDepleted = honeyDepleted;
        honeyDepleted = remaining > 0;
        if (wasDepleted != honeyDepleted) {
            syncToClient();
        }
    }

    public boolean isHoneyDepleted() {
        return honeyDepleted;
    }

    /**
     * Traite la queue de livraison: spawne des bees pour les tâches en attente.
     * Appelé depuis serverTick().
     *
     * Logique:
     * 1. Nettoyer les tâches terminées (enregistrer dans completedTaskIds)
     * 2. Trier la queue par priorité
     * 3. Pour chaque slot d'abeille libre:
     *    a. Trouver la première tâche QUEUED dont les dépendances sont satisfaites
     *    b. Si la quantité dépasse la capacité de l'abeille, splitter la tâche
     *    c. Spawner l'abeille
     */
    private void processDeliveryQueue() {
        if (level == null || level.isClientSide() || !storageFormed || honeyDepleted) return;

        // Nettoyer les tâches terminées ou échouées → enregistrer les IDs complétées
        activeTasks.removeIf(task -> {
            if (task.getState() == DeliveryTask.DeliveryState.COMPLETED) {
                completedTaskIds.add(task.getTaskId());
                return true;
            }
            if (task.getState() == DeliveryTask.DeliveryState.FAILED) {
                return true;
            }
            return false;
        });

        // Limiter la taille de completedTaskIds pour éviter fuite mémoire
        if (completedTaskIds.size() > MAX_COMPLETED_IDS) {
            Iterator<UUID> it = completedTaskIds.iterator();
            while (completedTaskIds.size() > MAX_COMPLETED_IDS / 2 && it.hasNext()) {
                it.next();
                it.remove();
            }
        }

        // Trier la queue par priorité (lower = higher priority)
        List<DeliveryTask> sortedQueue = new ArrayList<>(deliveryQueue);
        sortedQueue.sort(Comparator.comparingInt(DeliveryTask::getPriority));
        deliveryQueue.clear();
        deliveryQueue.addAll(sortedQueue);

        // Spawner des abeilles pour les tâches éligibles
        while (activeTasks.size() < MAX_ACTIVE_BEES) {
            DeliveryTask eligible = findEligibleTask();
            if (eligible == null) break;

            deliveryQueue.remove(eligible);

            // Splitter si la quantité dépasse la capacité de l'abeille
            int beeCapacity = eligible.getTemplate().getMaxStackSize();
            if (eligible.getCount() > beeCapacity) {
                DeliveryTask remaining = eligible.splitRemaining(beeCapacity);
                if (remaining != null) {
                    deliveryQueue.add(remaining);
                }
            }

            boolean spawned = spawnDeliveryBee(eligible);
            if (spawned) {
                eligible.setState(DeliveryTask.DeliveryState.FLYING);
                eligible.setAssignedBeeId(null);
                activeTasks.add(eligible);
            } else {
                eligible.setState(DeliveryTask.DeliveryState.FAILED);
            }
        }
    }

    /**
     * Trouve la première tâche éligible dans la queue (QUEUED + dépendances satisfaites).
     */
    private DeliveryTask findEligibleTask() {
        for (DeliveryTask task : deliveryQueue) {
            if (task.getState() == DeliveryTask.DeliveryState.QUEUED
                && task.isReady(completedTaskIds)) {
                return task;
            }
        }
        return null;
    }

    /**
     * Spawne une abeille de livraison pour exécuter une tâche.
     */
    private boolean spawnDeliveryBee(DeliveryTask task) {
        if (!(level instanceof ServerLevel serverLevel)) return false;

        BlockPos spawnPos;
        BlockPos returnPos;

        if (task.getType() == DeliveryTask.DeliveryType.EXTRACT) {
            // EXTRACT: spawn en bas, retour en haut
            spawnPos = getSpawnPosBottom();
            returnPos = getSpawnPosTop();
        } else {
            // DEPOSIT: spawn en haut (avec items), retour en bas
            spawnPos = getSpawnPosTop();
            returnPos = getSpawnPosBottom();
        }

        if (spawnPos == null || returnPos == null) return false;

        DeliveryBeeEntity bee = BeemancerEntities.DELIVERY_BEE.get().create(serverLevel);
        if (bee == null) return false;

        bee.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);

        ItemStack carried = ItemStack.EMPTY;
        if (task.getType() == DeliveryTask.DeliveryType.DEPOSIT) {
            carried = task.getTemplate().copyWithCount(task.getCount());
        }

        bee.initDeliveryTask(
            worldPosition,
            task.getTargetChest(),
            returnPos,
            task.getTerminalPos(),
            task.getTemplate(),
            task.getCount(),
            task.getType(),
            carried,
            ControllerStats.getFlightSpeedMultiplier(essenceSlots),
            ControllerStats.getSearchSpeedMultiplier(essenceSlots)
        );

        serverLevel.addFreshEntity(bee);
        return true;
    }

    /**
     * Position de spawn en bas (honeyed stone sous le controller).
     */
    @Nullable
    private BlockPos getSpawnPosBottom() {
        Vec3i offset = MultiblockPattern.rotateY(new Vec3i(0, -1, 0), multiblockRotation);
        return worldPosition.offset(offset);
    }

    /**
     * Position de spawn en haut (honeyed stone au-dessus du controller).
     */
    @Nullable
    private BlockPos getSpawnPosTop() {
        Vec3i offset = MultiblockPattern.rotateY(new Vec3i(0, 1, 0), multiblockRotation);
        return worldPosition.offset(offset);
    }

    // === Lifecycle ===

    @Override
    public void setRemoved() {
        super.setRemoved();
        killAllDeliveryBees();
        MultiblockEvents.unregisterController(worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (storageFormed && level != null && !level.isClientSide()) {
            MultiblockEvents.registerActiveController(level, worldPosition);
        }
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        // Multibloc
        tag.putBoolean("StorageFormed", storageFormed);
        tag.putInt("MultiblockRotation", multiblockRotation);

        // Coffres enregistrés
        ListTag chestsTag = new ListTag();
        for (BlockPos pos : registeredChests) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            chestsTag.add(posTag);
        }
        tag.put("RegisteredChests", chestsTag);

        // Terminaux liés
        ListTag terminalsTag = new ListTag();
        for (BlockPos pos : linkedTerminals) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            terminalsTag.add(posTag);
        }
        tag.put("LinkedTerminals", terminalsTag);

        // Mode édition
        tag.putBoolean("EditMode", editMode);
        if (editingPlayer != null) {
            tag.putUUID("EditingPlayer", editingPlayer);
        }

        // Honey state
        tag.putBoolean("HoneyDepleted", honeyDepleted);

        // Essence slots
        tag.put("EssenceSlots", essenceSlots.serializeNBT(registries));

        // Delivery queue (active tasks remises en queue au rechargement)
        ListTag queueTag = new ListTag();
        for (DeliveryTask task : deliveryQueue) {
            queueTag.add(task.save(registries));
        }
        for (DeliveryTask task : activeTasks) {
            // Remettre les tâches actives en queue (bees seront re-spawned)
            task.setState(DeliveryTask.DeliveryState.QUEUED);
            task.setAssignedBeeId(null);
            queueTag.add(task.save(registries));
        }
        tag.put("DeliveryQueue", queueTag);

        // Completed task IDs (pour vérification dépendances cross-save)
        ListTag completedTag = new ListTag();
        for (UUID id : completedTaskIds) {
            CompoundTag idTag = new CompoundTag();
            idTag.putUUID("Id", id);
            completedTag.add(idTag);
        }
        tag.put("CompletedTaskIds", completedTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        // Multibloc
        storageFormed = tag.getBoolean("StorageFormed");
        multiblockRotation = tag.getInt("MultiblockRotation");

        // Coffres enregistrés
        registeredChests.clear();
        ListTag chestsTag = tag.getList("RegisteredChests", Tag.TAG_COMPOUND);
        for (int i = 0; i < chestsTag.size(); i++) {
            NbtUtils.readBlockPos(chestsTag.getCompound(i), "Pos").ifPresent(registeredChests::add);
        }

        // Terminaux liés
        linkedTerminals.clear();
        ListTag terminalsTag = tag.getList("LinkedTerminals", Tag.TAG_COMPOUND);
        for (int i = 0; i < terminalsTag.size(); i++) {
            NbtUtils.readBlockPos(terminalsTag.getCompound(i), "Pos").ifPresent(linkedTerminals::add);
        }

        // Mode édition
        editMode = tag.getBoolean("EditMode");
        if (tag.hasUUID("EditingPlayer")) {
            editingPlayer = tag.getUUID("EditingPlayer");
        } else {
            editingPlayer = null;
        }

        // Honey state
        honeyDepleted = tag.getBoolean("HoneyDepleted");

        // Essence slots
        if (tag.contains("EssenceSlots")) {
            essenceSlots.deserializeNBT(registries, tag.getCompound("EssenceSlots"));
        }

        // Delivery queue
        deliveryQueue.clear();
        activeTasks.clear();
        if (tag.contains("DeliveryQueue")) {
            ListTag queueTag = tag.getList("DeliveryQueue", Tag.TAG_COMPOUND);
            for (int i = 0; i < queueTag.size(); i++) {
                DeliveryTask task = DeliveryTask.load(queueTag.getCompound(i), registries);
                deliveryQueue.add(task);
            }
        }

        // Completed task IDs
        completedTaskIds.clear();
        if (tag.contains("CompletedTaskIds")) {
            ListTag completedTag = tag.getList("CompletedTaskIds", Tag.TAG_COMPOUND);
            for (int i = 0; i < completedTag.size(); i++) {
                completedTaskIds.add(completedTag.getCompound(i).getUUID("Id"));
            }
        }
    }

    // === Sync Client ===

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === Utilitaires ===

    /**
     * Clé pour identifier des ItemStacks identiques (même item, mêmes composants).
     */
    private static final class ItemStackKey {
        private final ItemStack template;

        ItemStackKey(ItemStack stack) {
            this.template = stack.copyWithCount(1);
        }

        ItemStack toStack() {
            return template.copy();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ItemStackKey that = (ItemStackKey) o;
            return ItemStack.isSameItemSameComponents(template, that.template);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(template);
        }
    }
}
