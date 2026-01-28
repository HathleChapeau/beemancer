/**
 * ============================================================
 * [MagicHiveBlockEntity.java]
 * Description: BlockEntity ruche magique - orchestrateur principal
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HiveBeeSlot         | Données slot         | État de chaque abeille         |
 * | HiveFlowerPool      | Pool fleurs          | Gestion des fleurs partagées   |
 * | MagicBeeEntity      | Entité abeille       | Spawn/capture                  |
 * | MagicBeeItem        | Item abeille         | Conversion entity<->item       |
 * | BreedingManager     | Logique breeding     | Calcul offspring               |
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
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
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

    // Inventaire
    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // Slots d'abeilles (refactorisé)
    private final HiveBeeSlot[] beeSlots = new HiveBeeSlot[BEE_SLOTS];

    // Pool de fleurs (refactorisé)
    private final HiveFlowerPool flowerPool = new HiveFlowerPool();

    // Breeding
    private boolean breedingMode = false;
    private int breedingCooldown = 0;

    // Legacy compatibility
    public enum BeeState { EMPTY, INSIDE, OUTSIDE }

    // ContainerData for GUI sync
    public final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? (breedingMode ? 1 : 0) : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) breedingMode = value != 0;
        }

        @Override
        public int getCount() { return 1; }
    };

    public MagicHiveBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.MAGIC_HIVE.get(), pos, state);
        for (int i = 0; i < BEE_SLOTS; i++) {
            beeSlots[i] = new HiveBeeSlot();
        }
    }

    // ==================== Container Implementation ====================

    @Override
    public int getContainerSize() { return TOTAL_SLOTS; }

    @Override
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < BEE_SLOTS && beeSlots[slot].isOutside()) {
            handleBeeRemoval(slot);
        }
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Si c'est un slot d'abeille et qu'on le vide, gérer l'entité correspondante
        if (slot < BEE_SLOTS && (stack.isEmpty() || !stack.is(BeemancerItems.MAGIC_BEE.get()))) {
            if (beeSlots[slot].isOutside()) {
                // L'abeille est dehors, la supprimer
                MagicBeeEntity bee = findBeeEntity(slot);
                if (bee != null) {
                    bee.markAsEnteredHive();
                    bee.discard();
                }
            }
            returnAssignedFlower(slot);
            beeSlots[slot].clear();
        }

        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }

        if (slot < BEE_SLOTS) {
            if (!stack.isEmpty() && stack.is(BeemancerItems.MAGIC_BEE.get())) {
                initializeBeeSlot(slot, stack);
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
        for (HiveBeeSlot slot : beeSlots) slot.clear();
        flowerPool.clear();
    }

    // ==================== Bee Slot Initialization ====================

    private void initializeBeeSlot(int slot, ItemStack beeItem) {
        MagicBeeItem.setAssignedHive(beeItem, worldPosition, slot);

        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        String speciesId = getSpeciesId(geneData);
        BeeBehaviorConfig config = BeeBehaviorManager.getConfig(speciesId);

        HiveBeeSlot beeSlot = beeSlots[slot];
        beeSlot.setState(HiveBeeSlot.State.INSIDE);
        beeSlot.setBeeUUID(null);
        beeSlot.setCooldown(config.getRandomRestCooldown(level != null ? level.getRandom() : RandomSource.create()));
        beeSlot.setMaxHealth((float) config.getHealth());
        beeSlot.setCurrentHealth(MagicBeeItem.getStoredHealth(beeItem, beeSlot.getMaxHealth()));
        beeSlot.setNeedsHealing(beeSlot.getCurrentHealth() < beeSlot.getMaxHealth());

        triggerFlowerScan();
    }

    private void handleBeeRemoval(int slot) {
        MagicBeeEntity bee = findBeeEntity(slot);
        if (bee != null) {
            syncEntityToItem(slot, bee);
            bee.markAsEnteredHive();
            bee.discard();
        }
        returnAssignedFlower(slot);
        beeSlots[slot].clear();
    }

    // ==================== Flower Management ====================

    private void triggerFlowerScan() {
        if (level == null) return;

        Set<TagKey<Block>> flowerTags = new HashSet<>();
        int maxRadius = 0;

        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeSlots[i].isEmpty()) continue;
            ItemStack beeItem = items.get(i);
            if (beeItem.isEmpty()) continue;

            BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
            Gene flowerGene = geneData.getGene(GeneCategory.FLOWER);
            if (flowerGene instanceof FlowerGene fg && fg.getFlowerTag() != null) {
                flowerTags.add(fg.getFlowerTag());
            }

            BeeBehaviorConfig config = BeeBehaviorManager.getConfig(getSpeciesId(geneData));
            maxRadius = Math.max(maxRadius, config.getAreaOfEffect());
        }

        List<BlockPos> assigned = new ArrayList<>();
        for (HiveBeeSlot slot : beeSlots) {
            if (slot.hasAssignedFlower()) assigned.add(slot.getAssignedFlower());
        }

        flowerPool.scanFlowers(level, worldPosition, flowerTags, maxRadius, assigned);
    }

    @Nullable
    public BlockPos getAndAssignFlower(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return null;

        ItemStack beeItem = items.get(slot);
        TagKey<Block> flowerTag = null;
        if (!beeItem.isEmpty()) {
            BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
            Gene flowerGene = geneData.getGene(GeneCategory.FLOWER);
            if (flowerGene instanceof FlowerGene fg) {
                flowerTag = fg.getFlowerTag();
            }
        }

        RandomSource random = level != null ? level.getRandom() : RandomSource.create();
        BlockPos flower = flowerPool.assignRandomFlower(level, flowerTag, random);

        if (flower != null) {
            beeSlots[slot].setAssignedFlower(flower);
        }
        return flower;
    }

    public void returnFlower(int slot, BlockPos flower) {
        if (slot >= 0 && slot < BEE_SLOTS) {
            beeSlots[slot].clearAssignedFlower();
        }
        flowerPool.returnFlower(flower);
    }

    private void returnAssignedFlower(int slot) {
        if (slot >= 0 && slot < BEE_SLOTS && beeSlots[slot].hasAssignedFlower()) {
            flowerPool.returnFlower(beeSlots[slot].getAssignedFlower());
            beeSlots[slot].clearAssignedFlower();
        }
    }

    public boolean hasFlowersForSlot(int slot) {
        return flowerPool.hasFlowers();
    }

    // ==================== Bee State Accessors (Legacy) ====================

    public BeeState getBeeState(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return BeeState.EMPTY;
        return switch (beeSlots[slot].getState()) {
            case EMPTY -> BeeState.EMPTY;
            case INSIDE -> BeeState.INSIDE;
            case OUTSIDE -> BeeState.OUTSIDE;
        };
    }

    public boolean isBreedingMode() { return breedingMode; }
    public int[] getBeeCooldowns() {
        int[] cooldowns = new int[BEE_SLOTS];
        for (int i = 0; i < BEE_SLOTS; i++) cooldowns[i] = beeSlots[i].getCooldown();
        return cooldowns;
    }

    /**
     * Retourne les états de tous les slots d'abeilles (pour debug HUD).
     */
    public HiveBeeSlot.State[] getBeeStates() {
        HiveBeeSlot.State[] states = new HiveBeeSlot.State[BEE_SLOTS];
        for (int i = 0; i < BEE_SLOTS; i++) states[i] = beeSlots[i].getState();
        return states;
    }
    public int getFlowerScanCooldown() { return flowerPool.getScanCooldown(); }
    public List<BlockPos>[] getBeeFlowers() {
        @SuppressWarnings("unchecked")
        List<BlockPos>[] result = new List[BEE_SLOTS];
        for (int i = 0; i < BEE_SLOTS; i++) {
            result[i] = new ArrayList<>(flowerPool.getAvailableFlowers());
        }
        return result;
    }

    // ==================== Bee Release/Entry ====================

    public void releaseBee(int slot) {
        if (slot < 0 || slot >= BEE_SLOTS) return;
        if (!beeSlots[slot].isInside()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;
        if (!hasFlowersForSlot(slot)) return;

        MagicBeeEntity bee = BeemancerEntities.MAGIC_BEE.get().create(level);
        if (bee == null) return;

        BlockPos spawnPos = worldPosition.above();
        bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 0, 0);

        BeeGeneData geneData = MagicBeeItem.getGeneData(beeItem);
        bee.getGeneData().copyFrom(geneData);
        for (Gene gene : geneData.getAllGenes()) bee.setGene(gene);

        bee.setStoredHealth(beeSlots[slot].getCurrentHealth());
        bee.setAssignedHive(worldPosition, slot);
        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);

        serverLevel.addFreshEntity(bee);

        beeSlots[slot].setState(HiveBeeSlot.State.OUTSIDE);
        beeSlots[slot].setBeeUUID(bee.getUUID());
        setChanged();
    }

    public boolean canBeeEnter(MagicBeeEntity bee) {
        if (!bee.hasAssignedHive() || !worldPosition.equals(bee.getAssignedHivePos())) return false;
        int slot = bee.getAssignedSlot();
        return slot >= 0 && slot < BEE_SLOTS && beeSlots[slot].isOutside();
    }

    public void addBee(MagicBeeEntity bee) {
        int slot = bee.getAssignedSlot();
        if (slot < 0 || slot >= BEE_SLOTS) return;

        bee.markAsEnteredHive();

        if (bee.isPollinated()) {
            depositPollinationLoot(bee);
        }

        returnAssignedFlower(slot);

        ItemStack beeItem = MagicBeeItem.captureFromEntity(bee);
        items.set(slot, beeItem);

        HiveBeeSlot beeSlot = beeSlots[slot];
        beeSlot.setState(HiveBeeSlot.State.INSIDE);
        beeSlot.setBeeUUID(null);
        beeSlot.setCurrentHealth(bee.getHealth());
        beeSlot.setMaxHealth(bee.getMaxHealth());
        beeSlot.setNeedsHealing(bee.getHealth() < bee.getMaxHealth());
        beeSlot.setCooldown(bee.getBehaviorConfig().getRandomRestCooldown(level.getRandom()));

        bee.setPollinated(false);
        bee.setEnraged(false);
        bee.setReturning(false);

        triggerFlowerScan();
        setChanged();
    }

    public void onBeeKilled(UUID beeUUID) {
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeUUID.equals(beeSlots[i].getBeeUUID()) && beeSlots[i].isOutside()) {
                returnAssignedFlower(i);
                items.set(i, ItemStack.EMPTY);
                beeSlots[i].clear();
                setChanged();
                return;
            }
        }
    }

    // ==================== Loot & Output ====================

    private void depositPollinationLoot(MagicBeeEntity bee) {
        if (level == null) return;
        BeeBehaviorConfig config = bee.getBehaviorConfig();
        for (ItemStack stack : config.rollPollinationLoot(level.getRandom())) {
            insertIntoOutputSlots(stack);
        }
    }

    public void insertIntoOutputSlots(ItemStack stack) {
        if (stack.isEmpty()) return;

        for (int i = BEE_SLOTS; i < TOTAL_SLOTS && !stack.isEmpty(); i++) {
            ItemStack existing = items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int toAdd = Math.min(existing.getMaxStackSize() - existing.getCount(), stack.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                }
            }
        }

        for (int i = BEE_SLOTS; i < TOTAL_SLOTS && !stack.isEmpty(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        setChanged();
    }

    // ==================== Breeding ====================

    private void attemptBreeding(RandomSource random) {
        List<Integer> insideBees = new ArrayList<>();
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeSlots[i].isInside() && !items.get(i).isEmpty()) {
                insideBees.add(i);
            }
        }
        if (insideBees.size() < 2) return;

        int outputSlot = -1;
        for (int i = BEE_SLOTS; i < TOTAL_SLOTS; i++) {
            if (items.get(i).isEmpty()) { outputSlot = i; break; }
        }
        if (outputSlot < 0) return;

        int idx1 = random.nextInt(insideBees.size());
        int idx2;
        do { idx2 = random.nextInt(insideBees.size()); } while (idx2 == idx1);

        int slot1 = insideBees.get(idx1), slot2 = insideBees.get(idx2);
        BeeGeneData parent1 = MagicBeeItem.getGeneData(items.get(slot1));
        BeeGeneData parent2 = MagicBeeItem.getGeneData(items.get(slot2));

        Gene species1 = parent1.getGene(GeneCategory.SPECIES);
        Gene species2 = parent2.getGene(GeneCategory.SPECIES);
        if (species1 == null || species2 == null) return;

        String offspringSpecies = BreedingManager.resolveOffspringSpecies(species1.getId(), species2.getId(), random);

        int costSlot = random.nextBoolean() ? slot1 : slot2;
        BeeGeneData costData = MagicBeeItem.getGeneData(items.get(costSlot));
        int lifetimeCost = (int) (costData.getMaxLifetime() * BreedingManager.LIFETIME_COST_RATIO);
        costData.setRemainingLifetime(costData.getRemainingLifetime() - lifetimeCost);
        MagicBeeItem.saveGeneData(items.get(costSlot), costData);

        if ("nothing".equals(offspringSpecies)) return;

        BeeGeneData offspringData = BreedingManager.createOffspringGeneData(parent1, parent2, offspringSpecies, random);
        items.set(outputSlot, BeeLarvaItem.createWithGenes(offspringData));
        setChanged();
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicHiveBlockEntity hive) {
        hive.breedingMode = level.getBlockState(pos.above()).is(BeemancerBlocks.BREEDING_CRYSTAL.get());

        if (hive.flowerPool.tickScanCooldown()) {
            hive.triggerFlowerScan();
        }

        for (int i = 0; i < BEE_SLOTS; i++) {
            hive.tickBeeSlot(i);
        }

        hive.checkReturningBees(level, pos);
        hive.tickBreeding(level.getRandom());
    }

    private void tickBeeSlot(int slot) {
        HiveBeeSlot beeSlot = beeSlots[slot];
        if (!beeSlot.isInside() || items.get(slot).isEmpty()) return;

        BeeGeneData geneData = MagicBeeItem.getGeneData(items.get(slot));
        BeeBehaviorConfig config = BeeBehaviorManager.getConfig(getSpeciesId(geneData));

        if (beeSlot.needsHealing()) {
            beeSlot.heal(config.getRegenerationRate() / 20.0f);
            if (!beeSlot.needsHealing()) {
                MagicBeeItem.setStoredHealth(items.get(slot), beeSlot.getCurrentHealth());
            }
        }

        if (!beeSlot.needsHealing()) {
            beeSlot.decrementCooldown();
        }

        if (beeSlot.isCooldownComplete() && !beeSlot.needsHealing() && !breedingMode) {
            releaseBee(slot);
        }
    }

    private void checkReturningBees(Level level, BlockPos pos) {
        AABB searchBox = new AABB(pos).inflate(2);
        List<MagicBeeEntity> nearbyBees = level.getEntitiesOfClass(MagicBeeEntity.class, searchBox,
                bee -> bee.hasAssignedHive() && pos.equals(bee.getAssignedHivePos()));

        for (MagicBeeEntity bee : nearbyBees) {
            if (canBeeEnter(bee) && bee.isReturning() && bee.position().distanceTo(pos.getCenter()) < 1.5) {
                addBee(bee);
                bee.discard();
            }
        }
    }

    private void tickBreeding(RandomSource random) {
        if (breedingMode && breedingCooldown <= 0) {
            if (random.nextDouble() < BreedingManager.BREEDING_CHANCE_PER_SECOND) {
                attemptBreeding(random);
            }
            breedingCooldown = 20;
        }
        if (breedingCooldown > 0) breedingCooldown--;
    }

    // ==================== Helpers ====================

    private String getSpeciesId(BeeGeneData geneData) {
        Gene species = geneData.getGene(GeneCategory.SPECIES);
        return species != null ? species.getId() : "meadow";
    }

    @Nullable
    private MagicBeeEntity findBeeEntity(int slot) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        if (slot < 0 || slot >= BEE_SLOTS || !beeSlots[slot].isOutside()) return null;
        UUID uuid = beeSlots[slot].getBeeUUID();
        if (uuid == null) return null;
        Entity entity = serverLevel.getEntity(uuid);
        return entity instanceof MagicBeeEntity bee ? bee : null;
    }

    private void syncEntityToItem(int slot, @Nullable MagicBeeEntity bee) {
        if (slot < 0 || slot >= BEE_SLOTS || bee == null) return;
        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;

        MagicBeeItem.saveGeneData(beeItem, bee.getGeneData());
        MagicBeeItem.setStoredHealth(beeItem, bee.getHealth());
        beeSlots[slot].setCurrentHealth(bee.getHealth());
        beeSlots[slot].setMaxHealth(bee.getMaxHealth());
        beeSlots[slot].setNeedsHealing(bee.getHealth() < bee.getMaxHealth());
        setChanged();
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);

        ListTag slotsTag = new ListTag();
        for (int i = 0; i < BEE_SLOTS; i++) {
            slotsTag.add(beeSlots[i].save());
        }
        tag.put("BeeSlots", slotsTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);

        if (tag.contains("BeeSlots")) {
            ListTag slotsTag = tag.getList("BeeSlots", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(slotsTag.size(), BEE_SLOTS); i++) {
                beeSlots[i].load(slotsTag.getCompound(i));
            }
        } else if (tag.contains("BeeStates")) {
            // Legacy loading
            ListTag statesTag = tag.getList("BeeStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(statesTag.size(), BEE_SLOTS); i++) {
                CompoundTag slotTag = statesTag.getCompound(i);
                beeSlots[i].setState(HiveBeeSlot.State.values()[slotTag.getInt("State")]);
                if (slotTag.hasUUID("UUID")) beeSlots[i].setBeeUUID(slotTag.getUUID("UUID"));
                beeSlots[i].setCooldown(slotTag.getInt("Cooldown"));
                beeSlots[i].setNeedsHealing(slotTag.getBoolean("NeedsHealing"));
                beeSlots[i].setCurrentHealth(slotTag.getFloat("CurrentHealth"));
                beeSlots[i].setMaxHealth(slotTag.getFloat("MaxHealth"));
            }
        }
    }

    // ==================== Menu ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.beemancer.magic_hive");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MagicHiveMenu(containerId, playerInventory, this, containerData);
    }

    // ==================== Cleanup ====================

    public void dropContents() {
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < BEE_SLOTS; i++) {
                if (beeSlots[i].isOutside()) {
                    MagicBeeEntity bee = findBeeEntity(i);
                    if (bee != null) {
                        syncEntityToItem(i, bee);
                        bee.markAsEnteredHive();
                        bee.discard();
                    }
                    returnAssignedFlower(i);
                    beeSlots[i].clear();
                }
            }
            Containers.dropContents(level, worldPosition, this);
        }
    }
}
