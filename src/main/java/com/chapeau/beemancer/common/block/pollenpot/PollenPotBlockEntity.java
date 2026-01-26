/**
 * ============================================================
 * [PollenPotBlockEntity.java]
 * Description: BlockEntity pour stocker jusqu'à 16 pollens d'un seul type
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerBlockEntities  | Type de BlockEntity  | Enregistrement                 |
 * | BeemancerTags           | Tag des pollens      | Validation items               |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - PollenPotBlock.java
 * - PollenPotEvents.java
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.pollenpot;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class PollenPotBlockEntity extends BlockEntity {
    public static final int MAX_POLLEN = 16;

    private ItemStack storedPollenType = ItemStack.EMPTY;
    private int pollenCount = 0;

    public PollenPotBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.POLLEN_POT.get(), pos, state);
    }

    /**
     * Vérifie si un item est un pollen valide.
     */
    public static boolean isPollen(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BeemancerTags.Items.POLLENS);
    }

    /**
     * Vérifie si le pot peut accepter ce type de pollen.
     */
    public boolean canAcceptPollen(ItemStack pollen) {
        if (!isPollen(pollen)) return false;
        if (pollenCount >= MAX_POLLEN) return false;
        if (isEmpty()) return true;
        return ItemStack.isSameItem(storedPollenType, pollen);
    }

    /**
     * Ajoute du pollen au pot.
     * @return true si le pollen a été ajouté
     */
    public boolean addPollen(ItemStack pollen) {
        if (!canAcceptPollen(pollen)) return false;

        if (isEmpty()) {
            storedPollenType = pollen.copyWithCount(1);
        }
        pollenCount++;
        setChanged();
        syncToClient();
        return true;
    }

    /**
     * Retire un pollen du pot.
     * @return L'item retiré ou ItemStack.EMPTY si vide
     */
    public ItemStack removePollen() {
        if (isEmpty()) return ItemStack.EMPTY;

        ItemStack removed = storedPollenType.copy();
        pollenCount--;

        if (pollenCount <= 0) {
            storedPollenType = ItemStack.EMPTY;
            pollenCount = 0;
        }

        setChanged();
        syncToClient();
        return removed;
    }

    public boolean isEmpty() {
        return pollenCount <= 0 || storedPollenType.isEmpty();
    }

    public int getPollenCount() {
        return pollenCount;
    }

    public ItemStack getStoredPollenType() {
        return storedPollenType;
    }

    public Item getPollenItem() {
        return storedPollenType.getItem();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // =========================================================================
    // NBT SERIALIZATION
    // =========================================================================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("PollenCount", pollenCount);
        if (!storedPollenType.isEmpty()) {
            tag.put("PollenType", storedPollenType.save(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pollenCount = tag.getInt("PollenCount");
        if (tag.contains("PollenType")) {
            storedPollenType = ItemStack.parse(registries, tag.getCompound("PollenType"))
                    .orElse(ItemStack.EMPTY);
        } else {
            storedPollenType = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("PollenCount", pollenCount);
        if (!storedPollenType.isEmpty()) {
            tag.put("PollenType", storedPollenType.save(registries, new CompoundTag()));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    /**
     * Retourne le niveau de signal pour les comparators (0-15).
     */
    public int getComparatorOutput() {
        if (isEmpty()) return 0;
        return Mth.ceil((float) pollenCount / MAX_POLLEN * 15.0f);
    }
}
