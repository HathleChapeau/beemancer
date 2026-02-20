/**
 * ============================================================
 * [StorageBarrelBlockEntity.java]
 * Description: BlockEntity pour le barrel de stockage mono-item
 * ============================================================
 *
 * FONCTIONNEMENT:
 * - Stocke un seul type d'item en grande quantite
 * - 4 tiers: MK1=54, MK2=108, MK3=216, MK4=432 stacks
 * - Void upgrade: surplus detruit quand barrel plein
 * - Expose IItemHandler pour automation (hoppers, pipes)
 * ============================================================
 */
package com.chapeau.apica.common.blockentity.storage;

import com.chapeau.apica.common.block.storage.StorageBarrelBlock;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorageBarrelBlockEntity extends BlockEntity {

    // Capacite en stacks par tier
    private static final int[] STACK_CAPACITIES = {0, 54, 108, 216, 432};

    private ItemStack storedItem = ItemStack.EMPTY;
    private int storedCount = 0;
    private boolean voidUpgrade = false;

    public StorageBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ApicaBlockEntities.STORAGE_BARREL.get(), pos, state);
    }

    // --- Tier ---

    public int getTier() {
        if (getBlockState().getBlock() instanceof StorageBarrelBlock barrel) {
            return barrel.getTier();
        }
        return 1;
    }

    public int getMaxStacks() {
        int tier = getTier();
        return tier >= 1 && tier <= 4 ? STACK_CAPACITIES[tier] : STACK_CAPACITIES[1];
    }

    public int getMaxItems() {
        if (storedItem.isEmpty()) return getMaxStacks() * 64;
        return getMaxStacks() * storedItem.getMaxStackSize();
    }

    // --- Void Upgrade ---

    public boolean hasVoidUpgrade() {
        return voidUpgrade;
    }

    public void setVoidUpgrade(boolean value) {
        this.voidUpgrade = value;
        setChanged();
        syncToClient();
    }

    // --- Storage Operations ---

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public int getStoredCount() {
        return storedCount;
    }

    /**
     * Insere des items dans le barrel.
     * @param stack L'item a inserer
     * @param fullStack true pour inserer tout le stack, false pour 1 seul item
     * @return Le nombre d'items effectivement inseres
     */
    public int insertItem(ItemStack stack, boolean fullStack) {
        if (stack.isEmpty()) return 0;

        // Verifier compatibilite
        if (!storedItem.isEmpty() && !ItemStack.isSameItemSameComponents(storedItem, stack)) {
            return 0;
        }

        int maxItems = getMaxItemsFor(stack);
        int toInsert = fullStack ? stack.getCount() : 1;
        int canInsert = maxItems - storedCount;

        if (canInsert <= 0) {
            if (voidUpgrade) {
                // Void: accepter tout mais ne stocker que le max
                return toInsert;
            }
            return 0;
        }

        int actualInsert = Math.min(toInsert, canInsert);
        int voided = 0;

        if (voidUpgrade && toInsert > canInsert) {
            // Void le surplus
            voided = toInsert - canInsert;
        }

        if (storedItem.isEmpty()) {
            storedItem = stack.copyWithCount(1);
        }
        storedCount += actualInsert;

        setChanged();
        syncToClient();
        return actualInsert + voided;
    }

    /**
     * Extrait des items du barrel.
     * @param singleItem true pour 1 item, false pour 1 stack complet
     * @return Les items extraits
     */
    public ItemStack extractItem(boolean singleItem) {
        if (storedItem.isEmpty() || storedCount <= 0) return ItemStack.EMPTY;

        int maxStack = storedItem.getMaxStackSize();
        int toExtract = singleItem ? 1 : Math.min(storedCount, maxStack);

        ItemStack result = storedItem.copyWithCount(toExtract);
        storedCount -= toExtract;

        if (storedCount <= 0) {
            storedCount = 0;
            storedItem = ItemStack.EMPTY;
        }

        setChanged();
        syncToClient();
        return result;
    }

    private int getMaxItemsFor(ItemStack stack) {
        return getMaxStacks() * stack.getMaxStackSize();
    }

    // --- IItemHandler for automation ---

    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return 1;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (storedItem.isEmpty()) return ItemStack.EMPTY;
            return storedItem.copyWithCount(Math.min(storedCount, storedItem.getMaxStackSize()));
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (!storedItem.isEmpty() && !ItemStack.isSameItemSameComponents(storedItem, stack)) {
                return stack;
            }

            int maxItems = getMaxItemsFor(stack.isEmpty() ? storedItem : stack);
            int canInsert = maxItems - storedCount;

            if (voidUpgrade) {
                // Accept everything, void surplus
                if (!simulate) {
                    int actualStore = Math.min(stack.getCount(), canInsert);
                    if (actualStore > 0) {
                        if (storedItem.isEmpty()) storedItem = stack.copyWithCount(1);
                        storedCount += actualStore;
                        setChanged();
                        syncToClient();
                    }
                }
                return ItemStack.EMPTY; // All accepted (voided or stored)
            }

            if (canInsert <= 0) return stack;

            int toInsert = Math.min(stack.getCount(), canInsert);
            if (!simulate) {
                if (storedItem.isEmpty()) storedItem = stack.copyWithCount(1);
                storedCount += toInsert;
                setChanged();
                syncToClient();
            }

            if (toInsert >= stack.getCount()) return ItemStack.EMPTY;
            return stack.copyWithCount(stack.getCount() - toInsert);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (storedItem.isEmpty() || storedCount <= 0 || amount <= 0) return ItemStack.EMPTY;

            int toExtract = Math.min(amount, Math.min(storedCount, storedItem.getMaxStackSize()));
            ItemStack result = storedItem.copyWithCount(toExtract);

            if (!simulate) {
                storedCount -= toExtract;
                if (storedCount <= 0) {
                    storedCount = 0;
                    storedItem = ItemStack.EMPTY;
                }
                setChanged();
                syncToClient();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (voidUpgrade) return Integer.MAX_VALUE;
            return getMaxItems();
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return storedItem.isEmpty() || ItemStack.isSameItemSameComponents(storedItem, stack);
        }
    };

    public IItemHandler getAutomationHandler() {
        return automationHandler;
    }

    // --- Upgrade support ---

    /**
     * Vide le barrel sans drop (retourne les donnees pour transfert).
     * Utilise lors de l'upgrade de tier.
     */
    public BarrelData clearForUpgrade() {
        BarrelData data = new BarrelData(storedItem.copy(), storedCount, voidUpgrade);
        this.storedItem = ItemStack.EMPTY;
        this.storedCount = 0;
        this.voidUpgrade = false;
        setChanged();
        return data;
    }

    /**
     * Restaure les donnees d'un ancien barrel apres upgrade.
     */
    public void restoreFromUpgrade(BarrelData data) {
        this.storedItem = data.item().copy();
        this.storedCount = data.count();
        this.voidUpgrade = data.hasVoid();
        setChanged();
        syncToClient();
    }

    /**
     * Donnees de barrel pour transfert lors d'un upgrade.
     */
    public record BarrelData(ItemStack item, int count, boolean hasVoid) {}

    // --- Drop contents ---

    public void dropContents(Level level, BlockPos pos) {
        if (!storedItem.isEmpty() && storedCount > 0) {
            int maxStack = storedItem.getMaxStackSize();
            int remaining = storedCount;
            while (remaining > 0) {
                int dropCount = Math.min(remaining, maxStack);
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        storedItem.copyWithCount(dropCount));
                remaining -= dropCount;
            }
        }

        if (voidUpgrade) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    new ItemStack(ApicaItems.VOID_UPGRADE.get()));
        }
    }

    // --- Sync ---

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

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.save(registries));
            tag.putInt("StoredCount", storedCount);
        }
        if (voidUpgrade) {
            tag.putBoolean("VoidUpgrade", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            storedItem = ItemStack.parse(registries, tag.getCompound("StoredItem")).orElse(ItemStack.EMPTY);
            storedCount = tag.getInt("StoredCount");
        } else {
            storedItem = ItemStack.EMPTY;
            storedCount = 0;
        }
        voidUpgrade = tag.getBoolean("VoidUpgrade");
    }
}
