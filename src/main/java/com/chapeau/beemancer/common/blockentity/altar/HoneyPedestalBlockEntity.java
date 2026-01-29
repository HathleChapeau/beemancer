/**
 * ============================================================
 * [HoneyPedestalBlockEntity.java]
 * Description: BlockEntity pour stocker un item sur le pedestal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerBlockEntities  | Type de BlockEntity  | Enregistrement                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoneyPedestalBlock.java (clic droit pour placer/retirer item)
 * - AltarHeartBlockEntity.java (lecture des items pour craft)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.altar;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class HoneyPedestalBlockEntity extends BlockEntity {

    private ItemStack storedItem = ItemStack.EMPTY;

    public HoneyPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(BeemancerBlockEntities.HONEY_PEDESTAL.get(), pos, state);
    }

    /**
     * Retourne l'item stocke sur le pedestal.
     */
    public ItemStack getStoredItem() {
        return storedItem;
    }

    /**
     * Verifie si le pedestal est vide.
     */
    public boolean isEmpty() {
        return storedItem.isEmpty();
    }

    /**
     * Place un item sur le pedestal.
     * @param stack L'item a placer (sera copie avec count=1)
     * @return true si l'item a ete place
     */
    public boolean placeItem(ItemStack stack) {
        if (!storedItem.isEmpty() || stack.isEmpty()) {
            return false;
        }
        storedItem = stack.copyWithCount(1);
        setChanged();
        syncToClient();
        return true;
    }

    /**
     * Retire l'item du pedestal.
     * @return L'item retire ou ItemStack.EMPTY si vide
     */
    public ItemStack removeItem() {
        if (storedItem.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = storedItem.copy();
        storedItem = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return removed;
    }

    /**
     * Consomme l'item (pour le craft) sans le retourner.
     */
    public void consumeItem() {
        storedItem = ItemStack.EMPTY;
        setChanged();
        syncToClient();
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
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.save(registries, new CompoundTag()));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("StoredItem")) {
            storedItem = ItemStack.parse(registries, tag.getCompound("StoredItem"))
                    .orElse(ItemStack.EMPTY);
        } else {
            storedItem = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (!storedItem.isEmpty()) {
            tag.put("StoredItem", storedItem.save(registries, new CompoundTag()));
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
}
