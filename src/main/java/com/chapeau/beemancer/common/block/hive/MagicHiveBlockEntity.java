/**
 * ============================================================
 * [MagicHiveBlockEntity.java]
 * Description: BlockEntity ruche magique avec tracking des abeilles, breeding, cooldowns et loot
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicBeeEntity      | Entité abeille       | Gestion spawn/capture          |
 * | MagicBeeItem        | Item abeille         | Conversion entity<->item       |
 * | BeeLarvaItem        | Item larve           | Breeding output                |
 * | BreedingManager     | Logique breeding     | Calcul offspring               |
 * | BeeBehaviorManager  | Config comportement  | Cooldowns et paramètres        |
 * | FlowerSearchHelper  | Recherche fleurs     | Détection fleurs par rayon     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlock.java: Création et interaction
 * - MagicHiveMenu.java: Interface GUI
 * - MagicBeeEntity.java: Notifications
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import com.chapeau.beemancer.content.gene.flower.FlowerGene;
import com.chapeau.beemancer.core.behavior.BeeBehaviorConfig;
import com.chapeau.beemancer.core.behavior.BeeBehaviorManager;
import com.chapeau.beemancer.core.breeding.BreedingManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.util.FlowerSearchHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MagicHiveBlockEntity extends BlockEntity implements MenuProvider, net.minecraft.world.Container {
    public static final int BEE_SLOTS = 5;
    public static final int OUTPUT_SLOTS = 7;
    public static final int TOTAL_SLOTS = BEE_SLOTS + OUTPUT_SLOTS;
    
    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
    
    // Bee tracking
    public enum BeeState { EMPTY, INSIDE, OUTSIDE }
    private final BeeState[] beeStates = new BeeState[BEE_SLOTS];
    private final UUID[] beeUUIDs = new UUID[BEE_SLOTS];
    
    // Cooldowns et régénération par slot
    private final int[] beeCooldowns = new int[BEE_SLOTS];
    public int[] getBeeCooldowns(){ return beeCooldowns; }
    private final boolean[] beeNeedsHealing = new boolean[BEE_SLOTS];
    private final float[] beeCurrentHealth = new float[BEE_SLOTS];
    private final float[] beeMaxHealth = new float[BEE_SLOTS];
    
    // Flower detection: list of flower positions per bee slot
    @SuppressWarnings("unchecked")
    private final List<BlockPos>[] beeFlowers = new List[BEE_SLOTS];
    public List<BlockPos>[] getBeeFlowers(){ return beeFlowers; }
    private int flowerScanCooldown = 0;
    public int getFlowerScanCooldown(){ return flowerScanCooldown; }
    private static final int FLOWER_SCAN_INTERVAL = 100; // Scan every 5 seconds

    // Breeding
    private boolean breedingMode = false;
    private int breedingCooldown = 0;
    
    // ContainerData for GUI sync
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> breedingMode ? 1 : 0;
                default -> 0;
            };
        }
        
        @Override
        public void set(int index, int value) {
            if (index == 0) breedingMode = value != 0;
        }
        
        @Override
        public int getCount() {
            return 1;
        }
    };
    
    public MagicHiveBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.MAGIC_HIVE.get(), pos, state);
        Arrays.fill(beeStates, BeeState.EMPTY);
        Arrays.fill(beeCooldowns, 0);
        Arrays.fill(beeNeedsHealing, false);
        Arrays.fill(beeCurrentHealth, 0);
        Arrays.fill(beeMaxHealth, 10);
        for (int i = 0; i < BEE_SLOTS; i++) {
            beeFlowers[i] = new ArrayList<>();
        }
    }

    // --- Container Implementation ---

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // If removing a bee item while bee is outside, sync data first
        if (slot < BEE_SLOTS && beeStates[slot] == BeeState.OUTSIDE) {
            MagicBeeEntity bee = findBeeEntity(slot);
            if (bee != null) {
                // Sync entity data to item BEFORE removing
                syncEntityToItem(slot, bee);
                // Now despawn the entity (won't sync again since item will be gone)
                bee.markAsEnteredHive();
                bee.discard();
            }
            // Clear tracking state
            beeStates[slot] = BeeState.EMPTY;
            beeUUIDs[slot] = null;
            beeCooldowns[slot] = 0;
            beeNeedsHealing[slot] = false;
            beeFlowers[slot].clear();
        }
        
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        System.out.println("[DEBUG-HIVE] setItem: slot=" + slot + ", stack=" + stack);
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        
        if (slot < BEE_SLOTS) {
            if (!stack.isEmpty() && stack.is(BeemancerItems.MAGIC_BEE.get())) {
                MagicBeeItem.setAssignedHive(stack, worldPosition, slot);
                beeStates[slot] = BeeState.INSIDE;
                System.out.println("[DEBUG-HIVE] Bee INSIDE, cooldown=" + beeCooldowns[slot] + ", flowers=" + beeFlowers[slot].size());
                beeUUIDs[slot] = null;
                
                // Initialiser le cooldown et la santé
                BeeGeneData geneData = MagicBeeItem.getGeneData(stack);
                String speciesId = geneData.getGene(GeneCategory.SPECIES) != null 
                        ? geneData.getGene(GeneCategory.SPECIES).getId() : "common";
                BeeBehaviorConfig config = BeeBehaviorManager.getConfig(speciesId);
                
                beeCooldowns[slot] = config.getRandomRestCooldown(level != null ? level.getRandom() : RandomSource.create());
                beeMaxHealth[slot] = (float) config.getHealth();
                beeCurrentHealth[slot] = MagicBeeItem.getStoredHealth(stack, beeMaxHealth[slot]);
                beeNeedsHealing[slot] = beeCurrentHealth[slot] < beeMaxHealth[slot];
                
                // Scan for flowers for this bee
                scanFlowersForSlot(slot);
            } else {
                beeStates[slot] = BeeState.EMPTY;
                beeUUIDs[slot] = null;
                beeCooldowns[slot] = 0;
                beeNeedsHealing[slot] = false;
                beeFlowers[slot].clear();
            }
        }
        
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this 
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void clearContent() {
        items.clear();
        Arrays.fill(beeStates, BeeState.EMPTY);
        Arrays.fill(beeUUIDs, null);
        Arrays.fill(beeCooldowns, 0);
        Arrays.fill(beeNeedsHealing, false);
        for (int i = 0; i < BEE_SLOTS; i++) {
            beeFlowers[i].clear();
        }
    }

    // --- Flower Detection ---
    
    /**
     * Scans for flowers for a specific bee slot based on its FlowerGene and areaOfEffect.
     */
    private void scanFlowersForSlot(int slot) {
        if (level == null || slot < 0 || slot >= BEE_SLOTS) return;
        
        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) {
            beeFlowers[slot].clear();
            return;
        }
        
        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        Gene flowerGene = geneData.getGene(GeneCategory.FLOWER);
        
        if (!(flowerGene instanceof FlowerGene flower)) {
            beeFlowers[slot].clear();
            return;
        }
        
        TagKey<Block> flowerTag = flower.getFlowerTag();
        if (flowerTag == null) {
            beeFlowers[slot].clear();
            return;
        }
        
        // Get area of effect from config
        String speciesId = geneData.getGene(GeneCategory.SPECIES) != null 
                ? geneData.getGene(GeneCategory.SPECIES).getId() : "common";
        BeeBehaviorConfig config = BeeBehaviorManager.getConfig(speciesId);
        int radius = config.getAreaOfEffect();
        
        // Find all flowers
        List<BlockPos> flowers = FlowerSearchHelper.findAllFlowers(level, worldPosition, radius, flowerTag);
        beeFlowers[slot].clear();
        beeFlowers[slot].addAll(flowers);
    }
    
    /**
     * Scans for flowers for all bees periodically.
     */
    private void scanAllFlowers() {
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeStates[i] != BeeState.EMPTY) {
                scanFlowersForSlot(i);
            }
        }
    }
    
    /**
     * Gets the list of flowers for a specific bee slot.
     * Used by bees to know where to go.
     */
    public List<BlockPos> getFlowersForSlot(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(beeFlowers[slot]);
    }
    
    /**
     * Checks if there are any flowers available for a bee slot.
     */
    public boolean hasFlowersForSlot(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return false;
        return !beeFlowers[slot].isEmpty();
    }
    
    /**
     * Gets the next available flower for a bee (removes from list to avoid conflicts).
     * Returns null if no flowers available.
     */
    @Nullable
    public BlockPos getAndAssignFlower(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS || beeFlowers[slot].isEmpty()) {
            return null;
        }
        
        // Get first available flower and remove it from the list
        BlockPos flower = beeFlowers[slot].remove(0);
        
        // Verify it's still valid
        if (level != null) {
            ItemStack beeItem = items.get(slot);
            if (!beeItem.isEmpty()) {
                BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
                Gene flowerGene = geneData.getGene(GeneCategory.FLOWER);
                if (flowerGene instanceof FlowerGene fg) {
                    TagKey<Block> flowerTag = fg.getFlowerTag();
                    if (FlowerSearchHelper.isValidFlower(level, flower, flowerTag)) {
                        return flower;
                    }
                }
            }
        }
        
        // If not valid, try next one
        return getAndAssignFlower(slot);
    }
    
    /**
     * Returns a flower to the available list (if bee couldn't reach it).
     */
    public void returnFlower(int slot, BlockPos flower) {
        if (slot >= 0 && slot < BEE_SLOTS && flower != null) {
            // Add to end of list
            beeFlowers[slot].add(flower);
        }
    }

    // --- Bee Management ---

    public BeeState getBeeState(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return BeeState.EMPTY;
        return beeStates[slot];
    }

    public void setBeeState(int slot, BeeState state) {
        if (slot >= 0 && slot < BEE_SLOTS) {
            beeStates[slot] = state;
            setChanged();
        }
    }

    @Nullable
    public UUID getBeeUUID(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return null;
        return beeUUIDs[slot];
    }

    public void setBeeUUID(int slot, @Nullable UUID uuid) {
        if (slot >= 0 && slot < BEE_SLOTS) {
            beeUUIDs[slot] = uuid;
            setChanged();
        }
    }

    /**
     * Spawn bee from slot into the world
     */
    public void releaseBee(int slot) {
        System.out.println("[DEBUG-HIVE] releaseBee START: slot=" + slot);
        
        if (slot < 0 || slot >= BEE_SLOTS) {
            System.out.println("[DEBUG-HIVE] ABORT: invalid slot");
            return;
        }
        if (beeStates[slot] != BeeState.INSIDE) {
            System.out.println("[DEBUG-HIVE] ABORT: state=" + beeStates[slot] + " (not INSIDE)");
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            System.out.println("[DEBUG-HIVE] ABORT: not ServerLevel");
            return;
        }
        
        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) {
            System.out.println("[DEBUG-HIVE] ABORT: invalid item - " + beeItem);
            return;
        }
        
        if (!hasFlowersForSlot(slot)) {
            System.out.println("[DEBUG-HIVE] ABORT: no flowers");
            return;
        }
        
        System.out.println("[DEBUG-HIVE] Creating entity...");
        MagicBeeEntity bee = com.chapeau.beemancer.core.registry.BeemancerEntities.MAGIC_BEE.get().create(level);
        if (bee == null) {
            System.out.println("[DEBUG-HIVE] ABORT: create() returned null");
            return;
        }
        
        BlockPos spawnPos = worldPosition.above();
        System.out.println("[DEBUG-HIVE] Spawning at " + spawnPos);
        bee.setPos(spawnPos.getX(), spawnPos.getY() + 1, spawnPos.getZ());
        bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 0, 0);
        
        var geneData = MagicBeeItem.getGeneData(beeItem);
        bee.getGeneData().copyFrom(geneData);
        for (var gene : geneData.getAllGenes()) {
            bee.setGene(gene);
        }
        
        bee.setStoredHealth(beeCurrentHealth[slot]);
        bee.setAssignedHive(worldPosition, slot);
        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);
        
        boolean added = serverLevel.addFreshEntity(bee);
        System.out.println("[DEBUG-HIVE] addFreshEntity result: " + added + ", UUID=" + bee.getUUID());
        
        beeStates[slot] = BeeState.OUTSIDE;
        beeUUIDs[slot] = bee.getUUID();
        setChanged();
        System.out.println("[DEBUG-HIVE] releaseBee SUCCESS!");
    }

    /**
     * Called when bee re-enters the hive
     */
    public boolean canBeeEnter(MagicBeeEntity bee) {
        if (!bee.hasAssignedHive()) return false;
        if (!worldPosition.equals(bee.getAssignedHivePos())) return false;
        
        int slot = bee.getAssignedSlot();
        return slot >= 0 && slot < BEE_SLOTS && beeStates[slot] == BeeState.OUTSIDE;
    }

    public void addBee(MagicBeeEntity bee) {
        int slot = bee.getAssignedSlot();
        if (slot < 0 || slot >= BEE_SLOTS) return;
        
        bee.markAsEnteredHive();
        
        // Deposer le loot si pollinisee
        if (bee.isPollinated()) {
            depositPollinationLoot(bee);
        }

        // Capture bee back to item
        ItemStack beeItem = MagicBeeItem.captureFromEntity(bee);
        items.set(slot, beeItem);
        beeStates[slot] = BeeState.INSIDE;
                System.out.println("[DEBUG-HIVE] Bee INSIDE, cooldown=" + beeCooldowns[slot] + ", flowers=" + beeFlowers[slot].size());
        beeUUIDs[slot] = null;
        
        // Stocker la santé actuelle
        beeCurrentHealth[slot] = bee.getHealth();
        beeMaxHealth[slot] = bee.getMaxHealth();
        beeNeedsHealing[slot] = beeCurrentHealth[slot] < beeMaxHealth[slot];
        
        // Initialiser le cooldown
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        beeCooldowns[slot] = config.getRandomRestCooldown(level.getRandom());
        
        // Reset états
        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);
        
        // Rescan flowers after return
        scanFlowersForSlot(slot);
        
        setChanged();
    }
    
    /**
     * Dépose le loot de pollinisation dans les slots output
     */
    private void depositPollinationLoot(MagicBeeEntity bee) {
        if (level == null) return;
        
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        List<ItemStack> loot = config.rollPollinationLoot(level.getRandom());
        
        for (ItemStack stack : loot) {
            insertIntoOutputSlots(stack);
        }
    }
    
    /**
     * Insère un item dans les slots output
     */
    public void insertIntoOutputSlots(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        // D'abord essayer de stack avec des items existants
        for (int i = BEE_SLOTS; i < TOTAL_SLOTS && !stack.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, stack.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }
        
        // Ensuite chercher un slot vide
        for (int i = BEE_SLOTS; i < TOTAL_SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        
        setChanged();
    }

    /**
     * Called when outside bee dies
     */
    public void onBeeKilled(UUID beeUUID) {
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeUUID.equals(beeUUIDs[i]) && beeStates[i] == BeeState.OUTSIDE) {
                items.set(i, ItemStack.EMPTY);
                beeStates[i] = BeeState.EMPTY;
                beeUUIDs[i] = null;
                beeCooldowns[i] = 0;
                beeNeedsHealing[i] = false;
                beeFlowers[i].clear();
                setChanged();
                return;
            }
        }
    }

    /**
     * Synchronise les données d'une entité vers l'item dans le slot.
     * Met à jour: lifetime, santé. (Inventaire sync au retour seulement)
     *
     * @param slot Le slot de la bee
     * @param bee L'entité à synchroniser (peut être null si morte/introuvable)
     * @return true si sync réussie, false sinon
     */
    private boolean syncEntityToItem(int slot, @Nullable MagicBeeEntity bee) {
        if (slot < 0 || slot >= BEE_SLOTS) return false;

        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return false;

        if (bee == null) return false;

        // Sync gene data (includes lifetime)
        BeeGeneData entityGeneData = bee.getGeneData();
        MagicBeeItem.saveGeneData(beeItem, entityGeneData);

        // Sync health
        MagicBeeItem.setStoredHealth(beeItem, bee.getHealth());
        beeCurrentHealth[slot] = bee.getHealth();
        beeMaxHealth[slot] = bee.getMaxHealth();
        beeNeedsHealing[slot] = bee.getHealth() < bee.getMaxHealth();

        setChanged();
        return true;
    }

    /**
     * Trouve l'entité bee associée à un slot (si elle est dehors).
     */
    @Nullable
    private MagicBeeEntity findBeeEntity(int slot) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        if (slot < 0 || slot >= BEE_SLOTS) return null;
        if (beeStates[slot] != BeeState.OUTSIDE) return null;

        UUID uuid = beeUUIDs[slot];
        if (uuid == null) return null;

        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof MagicBeeEntity bee) {
            return bee;
        }
        return null;
    }

    private void despawnOutsideBee(int slot) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        UUID uuid = beeUUIDs[slot];
        if (uuid == null) return;

        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof MagicBeeEntity bee) {
            // Sync data from entity to item before despawn
            syncEntityToItem(slot, bee);
            bee.markAsEnteredHive();
            bee.discard();
        }

        beeStates[slot] = BeeState.EMPTY;
        beeUUIDs[slot] = null;
        beeCooldowns[slot] = 0;
        beeNeedsHealing[slot] = false;
        beeFlowers[slot].clear();
    }
    // --- Breeding ---
    
    public boolean isBreedingMode() {
        return breedingMode;
    }

    private int countInsideBees() {
        int count = 0;
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeStates[i] == BeeState.INSIDE && !items.get(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private List<Integer> getInsideBeeIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeStates[i] == BeeState.INSIDE && !items.get(i).isEmpty()) {
                indices.add(i);
            }
        }
        return indices;
    }

    private int findEmptyOutputSlot() {
        for (int i = BEE_SLOTS; i < TOTAL_SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void attemptBreeding(RandomSource random) {
        List<Integer> insideBees = getInsideBeeIndices();
        if (insideBees.size() < 2) return;
        
        int outputSlot = findEmptyOutputSlot();
        if (outputSlot < 0) return;
        
        int idx1 = random.nextInt(insideBees.size());
        int idx2;
        do {
            idx2 = random.nextInt(insideBees.size());
        } while (idx2 == idx1);
        
        int slot1 = insideBees.get(idx1);
        int slot2 = insideBees.get(idx2);
        
        ItemStack bee1Stack = items.get(slot1);
        ItemStack bee2Stack = items.get(slot2);
        
        BeeGeneData parent1 = MagicBeeItem.getGeneData(bee1Stack);
        BeeGeneData parent2 = MagicBeeItem.getGeneData(bee2Stack);
        
        Gene species1 = parent1.getGene(GeneCategory.SPECIES);
        Gene species2 = parent2.getGene(GeneCategory.SPECIES);
        
        if (species1 == null || species2 == null) return;
        
        String offspringSpecies = BreedingManager.resolveOffspringSpecies(
                species1.getId(), species2.getId(), random);
        
        int costParentSlot = random.nextBoolean() ? slot1 : slot2;
        ItemStack costBeeStack = items.get(costParentSlot);
        BeeGeneData costBeeData = MagicBeeItem.getGeneData(costBeeStack);
        int lifetimeCost = (int) (costBeeData.getMaxLifetime() * BreedingManager.LIFETIME_COST_RATIO);
        costBeeData.setRemainingLifetime(costBeeData.getRemainingLifetime() - lifetimeCost);
        MagicBeeItem.saveGeneData(costBeeStack, costBeeData);
        
        if ("nothing".equals(offspringSpecies)) {
            return;
        }
        
        BeeGeneData offspringData = BreedingManager.createOffspringGeneData(
                parent1, parent2, offspringSpecies, random);
        
        ItemStack larva = BeeLarvaItem.createWithGenes(offspringData);
        items.set(outputSlot, larva);
        setChanged();
    }

    // --- Tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicHiveBlockEntity hive) {
        // Check breeding mode
        BlockState above = level.getBlockState(pos.above());
        hive.breedingMode = above.is(BeemancerBlocks.BREEDING_CRYSTAL.get());
        
        // Periodic flower scan
        hive.flowerScanCooldown--;
        if (hive.flowerScanCooldown <= 0) {
            hive.scanAllFlowers();
            hive.flowerScanCooldown = FLOWER_SCAN_INTERVAL;
        }
        
        // Gérer les abeilles INSIDE
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (hive.beeStates[i] == BeeState.INSIDE && !hive.items.get(i).isEmpty()) {
                ItemStack beeItem = hive.items.get(i);
                BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
                String speciesId = geneData.getGene(GeneCategory.SPECIES) != null 
                        ? geneData.getGene(GeneCategory.SPECIES).getId() : "common";
                BeeBehaviorConfig config = BeeBehaviorManager.getConfig(speciesId);
                
                // Régénération si nécessaire
                if (hive.beeNeedsHealing[i]) {
                    float healAmount = config.getRegenerationRate() / 20.0f; // par tick
                    hive.beeCurrentHealth[i] = Math.min(
                            hive.beeCurrentHealth[i] + healAmount, 
                            hive.beeMaxHealth[i]);
                    
                    if (hive.beeCurrentHealth[i] >= hive.beeMaxHealth[i]) {
                        hive.beeNeedsHealing[i] = false;
                        // Mettre à jour l'item
                        MagicBeeItem.setStoredHealth(beeItem, hive.beeCurrentHealth[i]);
                    }
                }
                
                // Cooldown seulement si pas besoin de heal
                if (!hive.beeNeedsHealing[i] && hive.beeCooldowns[i] > 0) {
                    hive.beeCooldowns[i]--;
                }
                
                // Auto-release si cooldown terminé et pas de breeding mode
                if (hive.beeCooldowns[i] <= 0 && !hive.beeNeedsHealing[i] && !hive.breedingMode) {
                    hive.releaseBee(i);
                }
            }
        }
        
        // Check for returning bees
        AABB searchBox = new AABB(pos).inflate(2);
        List<MagicBeeEntity> nearbyBees = level.getEntitiesOfClass(MagicBeeEntity.class, searchBox,
                bee -> bee.hasAssignedHive() && pos.equals(bee.getAssignedHivePos()));
        
        for (MagicBeeEntity bee : nearbyBees) {
            if (hive.canBeeEnter(bee) && bee.isReturning() && bee.position().distanceTo(pos.getCenter()) < 1.5) {
                hive.addBee(bee);
                bee.discard();
            }
        }
        
        // Breeding logic
        if (hive.breedingMode && hive.breedingCooldown <= 0) {
            RandomSource random = level.getRandom();
            if (random.nextDouble() < BreedingManager.BREEDING_CHANCE_PER_SECOND) {
                hive.attemptBreeding(random);
            }
            hive.breedingCooldown = 20;
        }
        
        if (hive.breedingCooldown > 0) {
            hive.breedingCooldown--;
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        
        ListTag statesTag = new ListTag();
        for (int i = 0; i < BEE_SLOTS; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("State", beeStates[i].ordinal());
            if (beeUUIDs[i] != null) {
                slotTag.putUUID("UUID", beeUUIDs[i]);
            }
            slotTag.putInt("Cooldown", beeCooldowns[i]);
            slotTag.putBoolean("NeedsHealing", beeNeedsHealing[i]);
            slotTag.putFloat("CurrentHealth", beeCurrentHealth[i]);
            slotTag.putFloat("MaxHealth", beeMaxHealth[i]);
            statesTag.add(slotTag);
        }
        tag.put("BeeStates", statesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        
        if (tag.contains("BeeStates")) {
            ListTag statesTag = tag.getList("BeeStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(statesTag.size(), BEE_SLOTS); i++) {
                CompoundTag slotTag = statesTag.getCompound(i);
                beeStates[i] = BeeState.values()[slotTag.getInt("State")];
                if (slotTag.hasUUID("UUID")) {
                    beeUUIDs[i] = slotTag.getUUID("UUID");
                }
                beeCooldowns[i] = slotTag.getInt("Cooldown");
                beeNeedsHealing[i] = slotTag.getBoolean("NeedsHealing");
                beeCurrentHealth[i] = slotTag.getFloat("CurrentHealth");
                beeMaxHealth[i] = slotTag.getFloat("MaxHealth");
            }
        }
    }

    // --- Menu ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beemancer.magic_hive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagicHiveMenu(containerId, playerInventory, this, containerData);
    }

    // --- Drop Contents ---

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < BEE_SLOTS; i++) {
                if (beeStates[i] == BeeState.OUTSIDE) {
                    despawnOutsideBee(i);
                }
            }
            Containers.dropContents(level, worldPosition, this);
        }
    }
}
