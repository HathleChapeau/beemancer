/**
 * ============================================================
 * [MagicHiveBlockEntity.java]
 * Description: BlockEntity ruche magique avec tracking des abeilles et breeding
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.bee.BeeLarvaItem;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.menu.MagicHiveMenu;
import com.chapeau.beemancer.core.breeding.BreedingManager;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
            // If removing from bee slot while bee is outside, despawn the bee
            if (slot < BEE_SLOTS && beeStates[slot] == BeeState.OUTSIDE) {
                despawnOutsideBee(slot);
            }
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        
        // If placing bee in slot, assign it to this hive
        if (slot < BEE_SLOTS) {
            if (!stack.isEmpty() && stack.is(BeemancerItems.MAGIC_BEE.get())) {
                MagicBeeItem.setAssignedHive(stack, worldPosition, slot);
                beeStates[slot] = BeeState.INSIDE;
                beeUUIDs[slot] = null;
            } else {
                beeStates[slot] = BeeState.EMPTY;
                beeUUIDs[slot] = null;
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
        if (slot < 0 || slot >= BEE_SLOTS) return;
        if (beeStates[slot] != BeeState.INSIDE) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        ItemStack beeItem = items.get(slot);
        if (beeItem.isEmpty() || !beeItem.is(BeemancerItems.MAGIC_BEE.get())) return;
        
        // Create bee entity
        MagicBeeEntity bee = com.chapeau.beemancer.core.registry.BeemancerEntities.MAGIC_BEE.get().create(level);
        if (bee == null) return;
        
        BlockPos spawnPos = worldPosition.above();
        bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
        
        // Load gene data
        var geneData = MagicBeeItem.getGeneData(beeItem);
        bee.getGeneData().copyFrom(geneData);
        for (var gene : geneData.getAllGenes()) {
            bee.setGene(gene);
        }
        
        // Assign hive
        bee.setAssignedHive(worldPosition, slot);
        
        serverLevel.addFreshEntity(bee);
        
        // Update state
        beeStates[slot] = BeeState.OUTSIDE;
        beeUUIDs[slot] = bee.getUUID();
        setChanged();
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
        
        // Mark bee as entering hive (prevents death notification)
        bee.markAsEnteredHive();
        
        // Capture bee back to item
        ItemStack beeItem = MagicBeeItem.captureFromEntity(bee);
        items.set(slot, beeItem);
        beeStates[slot] = BeeState.INSIDE;
        beeUUIDs[slot] = null;
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
                setChanged();
                return;
            }
        }
    }

    /**
     * Despawn outside bee when item is removed from slot
     */
    private void despawnOutsideBee(int slot) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        
        UUID uuid = beeUUIDs[slot];
        if (uuid == null) return;
        
        Entity entity = serverLevel.getEntity(uuid);
        if (entity instanceof MagicBeeEntity bee) {
            bee.markAsEnteredHive(); // Prevent death notification
            bee.discard();
        }
        
        beeStates[slot] = BeeState.EMPTY;
        beeUUIDs[slot] = null;
    }

    // --- Breeding ---
    
    public boolean isBreedingMode() {
        return breedingMode;
    }

    /**
     * Count bees with INSIDE state
     */
    private int countInsideBees() {
        int count = 0;
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeStates[i] == BeeState.INSIDE && !items.get(i).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get indices of bees with INSIDE state
     */
    private List<Integer> getInsideBeeIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < BEE_SLOTS; i++) {
            if (beeStates[i] == BeeState.INSIDE && !items.get(i).isEmpty()) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * Find first empty output slot
     */
    private int findEmptyOutputSlot() {
        for (int i = BEE_SLOTS; i < TOTAL_SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Attempt breeding
     */
    private void attemptBreeding(RandomSource random) {
        List<Integer> insideBees = getInsideBeeIndices();
        if (insideBees.size() < 2) return;
        
        int outputSlot = findEmptyOutputSlot();
        if (outputSlot < 0) return;
        
        // Select two random bees
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
        
        // Resolve offspring species
        String offspringSpecies = BreedingManager.resolveOffspringSpecies(
                species1.getId(), species2.getId(), random);
        
        // Apply lifetime cost to one parent (randomly chosen)
        int costParentSlot = random.nextBoolean() ? slot1 : slot2;
        ItemStack costBeeStack = items.get(costParentSlot);
        BeeGeneData costBeeData = MagicBeeItem.getGeneData(costBeeStack);
        int lifetimeCost = (int) (costBeeData.getMaxLifetime() * BreedingManager.LIFETIME_COST_RATIO);
        costBeeData.setRemainingLifetime(costBeeData.getRemainingLifetime() - lifetimeCost);
        MagicBeeItem.saveGeneData(costBeeStack, costBeeData);
        
        // If "nothing", no larva produced
        if ("nothing".equals(offspringSpecies)) {
            return;
        }
        
        // Create offspring gene data
        BeeGeneData offspringData = BreedingManager.createOffspringGeneData(
                parent1, parent2, offspringSpecies, random);
        
        // Create larva item
        ItemStack larva = BeeLarvaItem.createWithGenes(offspringData);
        items.set(outputSlot, larva);
        setChanged();
    }

    // --- Tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicHiveBlockEntity hive) {
        // Check breeding mode (crystal above)
        BlockState above = level.getBlockState(pos.above());
        hive.breedingMode = above.is(BeemancerBlocks.BREEDING_CRYSTAL.get());
        
        // Check for returning bees
        AABB searchBox = new AABB(pos).inflate(2);
        List<MagicBeeEntity> nearbyBees = level.getEntitiesOfClass(MagicBeeEntity.class, searchBox,
                bee -> bee.hasAssignedHive() && pos.equals(bee.getAssignedHivePos()));
        
        for (MagicBeeEntity bee : nearbyBees) {
            if (hive.canBeeEnter(bee) && bee.position().distanceTo(pos.getCenter()) < 1.5) {
                hive.addBee(bee);
                bee.discard();
            }
        }
        
        // Breeding logic (once per second)
        if (hive.breedingMode && hive.breedingCooldown <= 0) {
            RandomSource random = level.getRandom();
            // 5% chance per second
            if (random.nextDouble() < BreedingManager.BREEDING_CHANCE_PER_SECOND) {
                hive.attemptBreeding(random);
            }
            hive.breedingCooldown = 20; // Reset cooldown (1 second)
        }
        
        if (hive.breedingCooldown > 0) {
            hive.breedingCooldown--;
        }
        
        // TODO: Production logic - add items to output slots
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        
        // Save bee states
        ListTag statesTag = new ListTag();
        for (int i = 0; i < BEE_SLOTS; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("State", beeStates[i].ordinal());
            if (beeUUIDs[i] != null) {
                slotTag.putUUID("UUID", beeUUIDs[i]);
            }
            statesTag.add(slotTag);
        }
        tag.put("BeeStates", statesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        
        // Load bee states
        if (tag.contains("BeeStates")) {
            ListTag statesTag = tag.getList("BeeStates", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(statesTag.size(), BEE_SLOTS); i++) {
                CompoundTag slotTag = statesTag.getCompound(i);
                beeStates[i] = BeeState.values()[slotTag.getInt("State")];
                if (slotTag.hasUUID("UUID")) {
                    beeUUIDs[i] = slotTag.getUUID("UUID");
                }
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
            // Despawn all outside bees first
            for (int i = 0; i < BEE_SLOTS; i++) {
                if (beeStates[i] == BeeState.OUTSIDE) {
                    despawnOutsideBee(i);
                }
            }
            // Drop items
            Containers.dropContents(level, worldPosition, this);
        }
    }
}
